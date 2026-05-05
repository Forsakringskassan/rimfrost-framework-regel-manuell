package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.OulMessageRequest;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.RegelFelkod;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelRequestHandlerBase;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.entity.*;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableManuellRegelCommonData;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;
import se.fk.rimfrost.framework.uppgiftstatusprovider.UppgiftStatusProvider;

@SuppressWarnings("unused")
@ApplicationScoped
public class RegelManuellRequestHandler extends RegelRequestHandlerBase
      implements OulHandlerInterface, RegelRequestHandlerInterface, RegelManuellUppgiftDoneHandler
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelManuellRequestHandler.class);

   @ConfigProperty(name = "kafka.subtopic")
   String oulReplyToSubTopic;

   @Inject
   protected OulKafkaProducer oulKafkaProducer;

   @Inject
   ManuellRegelCommonDataStorage dataStorage;

   @Inject
   UppgiftStatusProvider uppgiftStatusProvider;

   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      try
      {
         Handlaggning handlaggning;
         var cloudevent = createCloudEvent(request);
         var uppgift = createUppgift(request.aktivitetId());

         try
         {
            handlaggning = handlaggningAdapter.readHandlaggning(request.handlaggningId());
         }
         catch (HandlaggningException e)
         {
            sendErrorResponse(request.handlaggningId(), cloudevent, RegelFelkod.HANDLAGGNING_READ_FAILURE, e.getMessage());
            return;
         }

         try
         {
            var handlaggningUpdate = createHandlaggningUpdate(handlaggning, uppgift, request.kogitoprocinstanceid(),
                  handlaggning.version() + 1);
            handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
         }
         catch (HandlaggningException e)
         {
            sendErrorResponse(request.handlaggningId(), cloudevent, RegelFelkod.HANDLAGGNING_WRITE_FAILURE, e.getMessage());
            return;
         }

         var commonRegelData = ImmutableManuellRegelCommonData.builder()
               .cloudEventData(cloudevent)
               .uppgift(uppgift)
               .build();

         writeManuellRegelCommonData(request.handlaggningId(), commonRegelData);

         var oulMessageRequest = ImmutableOulMessageRequest.builder()
               .handlaggningId(request.handlaggningId())
               .individer(
                     handlaggning.yrkande().individYrkandeRoller().stream()
                           .map(r -> toIdtyp(r.individ()))
                           .toList())
               .regel(regelConfig.getSpecifikation().getNamn())
               .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
               .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
               .roll(regelConfig.getSpecifikation().getRoll())
               .url(regelConfig.getUppgift().getPath())
               .replyToTopic(oulReplyToSubTopic)
               .build();

         sendOULRequest(oulMessageRequest, cloudevent);
      }
      catch (RegelCancelledException e)
      {
         LOGGER.error("Regel run cancelled due to error", e);

         sendErrorResponse(e.getHandlaggningId(), e.getCloudEventData(), e.getRegelErrorInformation());
         return;
      }
   }

   private se.fk.rimfrost.framework.oul.logic.dto.Idtyp toIdtyp(Idtyp idtyp)
   {
      return se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp.builder()
            .typId(idtyp.typId())
            .varde(idtyp.varde())
            .build();
   }

   @Override
   public void handleOulResponse(OulResponse oulResponse)
   {
      try
      {
         ManuellRegelCommonData commonRegelData = readManuellRegelCommonData(oulResponse.handlaggningId());

         var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
               .from(commonRegelData)
               .oulUppgiftId(oulResponse.uppgiftId())
               .build();

         writeManuellRegelCommonData(oulResponse.handlaggningId(), updatedCommonRegelData);
      }
      catch (RegelCancelledException e)
      {
         LOGGER.error("Regel run cancelled due to error", e);

         // TODO: How to handle created uppgift in OUL?

         sendErrorResponse(e.getHandlaggningId(), e.getCloudEventData(), e.getRegelErrorInformation());
         return;
      }
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      try
      {
         ManuellRegelCommonData commonRegelData = readManuellRegelCommonData(oulStatus.handlaggningId());

         if (commonRegelData == null)
         {
            /* This may happen if commonRegelData was cleaned up during handleUppgiftDone
             * and a notification was sent to OUL that the task was finished.
             */
            if (!Objects.equals(oulStatus.uppgiftStatus(), se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus.AVSLUTAD))
            {
               LOGGER.error(
                     "CommonRegelData for handlaggningId {} was not found during OUL status update for uppgift {} with uppgiftStatus {}",
                     oulStatus.handlaggningId(), oulStatus.uppgiftId(), oulStatus.uppgiftStatus());
            }

            return;
         }

         var uppgift = commonRegelData.uppgift();
         try
         {
            var handlaggning = handlaggningAdapter.readHandlaggning(oulStatus.handlaggningId());

            var updatedUppgift = ImmutableUppgift.builder()
                  .from(uppgift)
                  .version(uppgift.version() + 1)
                  .utforarId(toHandlaggningModelIdtyp(Objects.requireNonNull(oulStatus.utforarId())))
                  .uppgiftStatus(toUppgiftStatus(oulStatus.uppgiftStatus()))
                  .build();

            var handlaggningUpdate = createHandlaggningUpdate(handlaggning, updatedUppgift, handlaggning.processInstansId(),
                  handlaggning.version());

            var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
                  .from(commonRegelData)
                  .uppgift(updatedUppgift)
                  .build();
            dataStorage.setManuellRegelCommonData(oulStatus.handlaggningId(), updatedCommonRegelData);
            handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
         }
         catch (HandlaggningException e)
         {
            LOGGER.error("Error in handleOulStatus() while trying to update handlaggning with id: {}", oulStatus.handlaggningId(),
                  e);
         }
      }
      catch (RegelCancelledException e)
      {
         LOGGER.error("Regel run cancelled due to error", e);

         // TODO: How to handle created uppgift in OUL?

         sendErrorResponse(e.getHandlaggningId(), e.getCloudEventData(), e.getRegelErrorInformation());
         return;
      }
   }

   @Override
   public void handleUppgiftDone(UUID handlaggningId, Utfall utfall)
   {
      var commonRegelData = dataStorage.getManuellRegelCommonData(handlaggningId);
      CloudEventData cloudevent = commonRegelData.cloudEventData();

      try
      {
         var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
         var uppgift = commonRegelData.uppgift();

         var updatedUppgift = ImmutableUppgift.builder()
               .from(uppgift)
               .version(uppgift.version() + 1)
               .uppgiftStatus(uppgiftStatusProvider.getAvslutadId())
               .build();

         var handlaggningUpdate = createHandlaggningUpdate(handlaggning, updatedUppgift, handlaggning.processInstansId(),
               handlaggning.version());
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error in handleUppgiftDone() while trying to update handlaggning with id: {}", handlaggningId, e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }

      sendResponse(handlaggningId, cloudevent, utfall);

      DelayedException delayedException = new DelayedException();
      // We do this before cleaning up CloudEvent and RegelData instances
      // since the rule could possibly have dependency on them during
      // callback execution.
      try
      {
         dataStorage.deleteManuellRegelCommonData(handlaggningId);
      }
      catch (Exception e)
      {
         delayedException.addSuppressed(e);
      }

      try
      {
         oulKafkaProducer.sendOulStatusUpdate(commonRegelData.oulUppgiftId(), Status.AVSLUTAD);
      }
      catch (Exception e)
      {
         delayedException.addSuppressed(e);
      }

      if (delayedException.getSuppressed().length > 0)
      {
         throw delayedException;
      }
   }

   private static Response.Status toHttpStatus(HandlaggningException e) {
      return switch (e.getErrorType()) {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

   private void sendErrorResponse(UUID handlaggningId, CloudEventData cloudevent, RegelFelkod regelFelkod, String errorMessage)
   {
      var errorInfo = new RegelErrorInformation();
      errorInfo.setFelkod(regelFelkod);
      errorInfo.setFelmeddelande(errorMessage);
      sendResponse(handlaggningId, cloudevent, errorInfo);
   }

   private HandlaggningUpdate createHandlaggningUpdate(Handlaggning handlaggning, Uppgift uppgift, UUID kogitoprocInstanceId,
         int version)
   {
      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(version)
            .yrkande(handlaggning.yrkande())
            .processInstansId(kogitoprocInstanceId)
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(uppgift)
            .build();
   }

   private Uppgift createUppgift(UUID aktivitetId)
   {
      return ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(1)
            .aktivitetId(aktivitetId)
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now()) // TODO: Figure out when this should be set and to what this should be set to
            .uppgiftStatus(uppgiftStatusProvider.getPlaneradId())
            .fSSAinformation("FSSAinformation.HANDLAGGNING_PAGAR") // TODO
            .uppgiftSpecifikation(createUppgiftSpecifikation())
            .build();
   }

   private UppgiftSpecifikation createUppgiftSpecifikation()
   {
      return ImmutableUppgiftSpecifikation.builder()
            .id(regelConfig.getSpecifikation().getId())
            .version(regelConfig.getSpecifikation().getVersion())
            .build();
   }

   private Idtyp toHandlaggningModelIdtyp(se.fk.rimfrost.framework.oul.logic.dto.Idtyp idtyp)
   {
      return ImmutableIdtyp.builder()
            .typId(idtyp.typId())
            .varde(idtyp.varde())
            .build();
   }

   @SuppressWarnings("UnnecessaryDefault")
   private String toUppgiftStatus(se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus uppgiftStatus)
   {
      return switch(uppgiftStatus){case se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus.NY->uppgiftStatusProvider.getPlaneradId();case se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus.TILLDELAD->uppgiftStatusProvider.getTilldeladId();case se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus.AVSLUTAD->uppgiftStatusProvider.getAvslutadId();default->throw new IllegalArgumentException("Unsupported uppgift status: "+uppgiftStatus);};
   }

   private void sendErrorResponse(UUID handlaggningId, CloudEventData cloudEventData, RegelErrorInformation regelErrorInformation)
   {
      if (handlaggningId == null || cloudEventData == null || regelErrorInformation == null)
      {
         LOGGER.warn(
               "Could not send error response. Missing one or more required parameters. handlaggningId: {}, cloudEventData: {}, regelErrorInformation: {}",
               handlaggningId, cloudEventData, regelErrorInformation);
         return;
      }

      sendResponse(handlaggningId, cloudEventData, regelErrorInformation);
   }

   private RegelErrorInformation createRegelErrorInformation(RegelFelkod felkod, String meddelande)
   {
      RegelErrorInformation regelErrorInformation = new RegelErrorInformation();
      regelErrorInformation.setFelkod(felkod);
      regelErrorInformation.setFelmeddelande(meddelande);

      return regelErrorInformation;
   }

   private void writeManuellRegelCommonData(UUID handlaggningId, ManuellRegelCommonData manuellRegelCommonData)
   {
      try
      {
         dataStorage.setManuellRegelCommonData(handlaggningId, manuellRegelCommonData);
      }
      catch (Exception e)
      {
         var message = String.format(
               "Failed to write ManuellRegelCommonData update to data storage. handlaggningId: %s, kogitoprocId: %s",
               handlaggningId, manuellRegelCommonData.cloudEventData().kogitoprocinstanceid());
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.OTHER, message);
         var exception = new RegelCancelledException(handlaggningId, manuellRegelCommonData.cloudEventData(),
               regelErrorInformation, message);
         exception.addSuppressed(e);

         throw exception;
      }
   }

   private ManuellRegelCommonData readManuellRegelCommonData(UUID handlaggningId)
   {
      try
      {
         return dataStorage.getManuellRegelCommonData(handlaggningId);
      }
      catch (Exception e)
      {
         var message = String.format(
               "Failed to read ManuellRegelCommonData from data storage. handlaggningId: %s",
               handlaggningId);
         var exception = new RegelCancelledException(message);
         exception.addSuppressed(e);

         // TODO: How to handle this case? We do not have access to cloudEventData...
         throw exception;
      }
   }

   private void sendOULRequest(OulMessageRequest oulMessageRequest, CloudEventData cloudEventData)
   {
      try
      {
         oulKafkaProducer.sendOulRequest(oulMessageRequest);
      }
      catch (IllegalStateException e)
      {
         var message = String.format(
               "Failed to send oul uppgift creation request. handlaggningId: %s, kogitoprocId: %s",
               oulMessageRequest.handlaggningId(), cloudEventData.kogitoprocinstanceid());
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.OTHER, message);
         var exception = new RegelCancelledException(oulMessageRequest.handlaggningId(), cloudEventData, regelErrorInformation,
               message);
         exception.addSuppressed(e);

         throw exception;
      }
   }
}
