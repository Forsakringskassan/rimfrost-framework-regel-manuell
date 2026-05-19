package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableCreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.OperativUppgift;
import se.fk.rimfrost.framework.oul.logic.OulHandlerInterface;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulMessageHandler;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.logic.RegelRequestHandlerBase;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.manuell.storage.CloudEventDataStorage;
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
   ManuellRegelCommonDataStorage dataStorage;

   @Inject
   CloudEventDataStorage cloudEventDataStorage;

   @Inject
   UppgiftStatusProvider uppgiftStatusProvider;

   @Inject
   OulAdapter oulAdapter;

   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      try
      {
         var cloudevent = createCloudEvent(request);
         var uppgift = createUppgift(request.aktivitetId());
         var handlaggning = getHandlaggning(request.handlaggningId(), cloudevent);
         var handlaggningUpdate = createHandlaggningUpdate(handlaggning, uppgift, request.kogitoprocinstanceid(),
               handlaggning.version() + 1);
         updateHandlaggning(handlaggningUpdate, cloudevent);
         writeCloudEventData(request.handlaggningId(), cloudevent);

         var commonRegelData = ImmutableManuellRegelCommonData.builder()
               .uppgift(uppgift)
               .build();

         writeManuellRegelCommonData(request.handlaggningId(), commonRegelData);

         var oulCreateRequest = ImmutableCreateOperativUppgiftRequest.builder()
               .handlaggningId(request.handlaggningId())
               .version("1")
               .individer(
                     handlaggning.yrkande().individYrkandeRoller().stream()
                           .map(r -> toIdtyp(r.individ()))
                           .toList())
               .regel(regelConfig.getSpecifikation().getNamn())
               .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
               .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
               .roll(regelConfig.getSpecifikation().getRoll())
               .url(regelConfig.getUppgift().getPath())
               .subTopic(oulReplyToSubTopic)
               .cloudeventAttributes(CloudEventAttributesMapper.toAttributes(cloudevent))
               .build();

         var operativUppgift = createOperativUppgift(oulCreateRequest, cloudevent);

         var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
         .from(commonRegelData)
         .oulUppgiftId(operativUppgift.getUppgiftId())
         .build();

         writeManuellRegelCommonData(operativUppgift.getHandlaggningId(), updatedCommonRegelData);
      }
      catch (RegelCancelledException e)
      {
         LOGGER.error("Regel run cancelled due to error", e);

         sendErrorResponse(e.getHandlaggningId(), e.getCloudEventData(), e.getRegelErrorInformation());
         return;
      }
   }

   private se.fk.rimfrost.framework.oul.model.Idtyp toIdtyp(Idtyp idtyp)
   {
      return se.fk.rimfrost.framework.oul.model.ImmutableIdtyp.builder()
            .typId(idtyp.typId())
            .varde(idtyp.varde())
            .build();
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      CloudEventData cloudEventData = null;
      try
      {
         cloudEventData = CloudEventAttributesMapper.toCloudEventData(oulStatus.cloudeventAttributes());

         ManuellRegelCommonData commonRegelData = readManuellRegelCommonData(oulStatus.handlaggningId());

         if (commonRegelData == null)
         {

            /* This may happen if commonRegelData was cleaned up during handleUppgiftDone
             * and a notification was sent to OUL that the task was finished.
             */

            if (!Objects.equals(oulStatus.uppgiftStatus(), uppgiftStatusProvider.getAvslutadId()))
            {
               LOGGER.error(
                     "CommonRegelData for handlaggningId {} was not found during OUL status update for uppgift {} with uppgiftStatus {}",
                     oulStatus.handlaggningId(), oulStatus.uppgiftId(), oulStatus.uppgiftStatus());
            }

            return;
         }
         var uppgift = commonRegelData.uppgift();
         Handlaggning handlaggning = getHandlaggning(oulStatus.handlaggningId(), cloudEventData);

         var updatedUppgift = ImmutableUppgift.builder()
               .from(uppgift)
               .version(uppgift.version() + 1)
               .utforarId(toHandlaggningModelIdtyp(Objects.requireNonNull(oulStatus.utforarId())))
               .uppgiftStatus(oulStatus.uppgiftStatus())
               .build();

         var handlaggningUpdate = createHandlaggningUpdate(handlaggning, updatedUppgift, handlaggning.processInstansId(),
               handlaggning.version());

         var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
               .from(commonRegelData)
               .uppgift(updatedUppgift)
               .build();

         writeManuellRegelCommonData(oulStatus.handlaggningId(), updatedCommonRegelData);

         updateHandlaggning(handlaggningUpdate, cloudEventData);
      }
      catch (RegelCancelledException e)
      {
         LOGGER.error("Regel run in handleOulStatus cancelled due to error", e);
         sendErrorResponse(e.getHandlaggningId(), cloudEventData, e.getRegelErrorInformation());
         return;
      }
   }

   @Override
   public void handleUppgiftDone(UUID handlaggningId, Utfall utfall)
   {
      var cloudEventData = readCloudEventData(handlaggningId);

      ManuellRegelCommonData commonRegelData;
      try
      {
         commonRegelData = readManuellRegelCommonData(handlaggningId);
      }
      catch (RegelCancelledException e)
      {
         LOGGER.error("Failed to read commonRegelData in handleUppgiftDone for handlaggningId: {}", handlaggningId, e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), e);
      }

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

      sendResponse(handlaggningId, cloudEventData, utfall);

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
         this.cloudEventDataStorage.deleteCloudEventData(handlaggningId);
      }
      catch (Exception e)
      {
         delayedException.addSuppressed(e);
      }

      try
      {
         oulAdapter.endOperativUppgift(handlaggningId, "Uppgift klar");
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

   private void sendErrorResponse(UUID handlaggningId, CloudEventData cloudEventData,
         RegelErrorInformation regelErrorInformation)
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

   private RegelErrorInformation createRegelErrorInformation(String felkod, String meddelande)
   {
      RegelErrorInformation regelErrorInformation = new RegelErrorInformation();
      regelErrorInformation.setFelkod(felkod);
      regelErrorInformation.setFelmeddelande(meddelande);

      return regelErrorInformation;
   }

   private Handlaggning getHandlaggning(UUID handlaggningId, CloudEventData cloudEventData)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         var message = String.format(
               "Failed to read handlaggning. handlaggningId: %s, kogitoprocId: %s", handlaggningId,
               cloudEventData.kogitoprocinstanceid());
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.RIMFROST_HANDLAGGNING_READ_FAILURE, message);
         var exception = new RegelCancelledException(handlaggningId, cloudEventData, regelErrorInformation, message);
         exception.addSuppressed(e);

         throw exception;
      }
   }

   private void updateHandlaggning(HandlaggningUpdate handlaggningUpdate,
         CloudEventData cloudEventData)
   {
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         var message = String.format(
               "Failed to write handlaggning update. handlaggningId: %s, kogitoprocId: %s",
               handlaggningUpdate.id(), cloudEventData.kogitoprocinstanceid());
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.RIMFROST_HANDLAGGNING_WRITE_FAILURE, message);
         var exception = new RegelCancelledException(handlaggningUpdate.id(), cloudEventData, regelErrorInformation, message);
         exception.addSuppressed(e);

         throw exception;
      }
   }

   private void writeCloudEventData(UUID handlaggningId,
         CloudEventData cloudEventData)
   {
      try
      {
         this.cloudEventDataStorage.setCloudEventData(handlaggningId, cloudEventData);
      }
      catch (Exception e)
      {
         var message = String.format(
               "Failed to write CloudEventData to correlation storage. handlaggningId: %s, kogitoprocId: %s",
               handlaggningId, cloudEventData.kogitoprocinstanceid());
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.RIMFROST_CLOUD_EVENT_DATA_WRITE_FAILURE, message);
         var exception = new RegelCancelledException(handlaggningId, cloudEventData, regelErrorInformation, message);
         exception.addSuppressed(e);

         throw exception;
      }
   }

   private CloudEventData readCloudEventData(UUID handlaggningId)
   {
      try
      {
         return cloudEventDataStorage.getCloudEventData(handlaggningId);
      }
      catch (Exception e)
      {
         LOGGER.error("Failed to read CloudEventData from correlation storage. handlaggningId: {}", handlaggningId, e);
         return null;
      }
   }

   private void writeManuellRegelCommonData(UUID handlaggningId, ManuellRegelCommonData manuellRegelCommonData)
   {
      try
      {
         dataStorage.setManuellRegelCommonData(handlaggningId, manuellRegelCommonData);
      }
      catch (Exception e)
      {
         var cloudEventData = readCloudEventData(handlaggningId);
         var message = String.format(
               "Failed to write ManuellRegelCommonData update to data storage. handlaggningId: %s",
               handlaggningId);
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_WRITE_FAILURE,
               message);
         var exception = new RegelCancelledException(handlaggningId, cloudEventData, regelErrorInformation, message);
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
         var cloudEventData = readCloudEventData(handlaggningId);
         var message = String.format(
               "Failed to read ManuellRegelCommonData from data storage. handlaggningId: %s",
               handlaggningId);
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_READ_FAILURE,
               message);
         var exception = new RegelCancelledException(handlaggningId, cloudEventData, regelErrorInformation, message);
         exception.addSuppressed(e);

         throw exception;
      }
   }

   private OperativUppgift createOperativUppgift(CreateOperativUppgiftRequest oulRequest, CloudEventData cloudEventData)
   {
      try
      {
         return oulAdapter.createOperativUppgift(oulRequest);
      }
      catch (OulException e)
      {
         var message = String.format(
               "Failed to create operativ uppgift. handlaggningId: %s, kogitoprocId: %s reason: %s",
               oulRequest.getHandlaggningId(), cloudEventData.kogitoprocinstanceid(), e.getMessage());
         var regelErrorInformation = createRegelErrorInformation(RegelFelkod.RIMFROST_OTHER, message);
         var exception = new RegelCancelledException(oulRequest.getHandlaggningId(), cloudEventData, regelErrorInformation,
               message);
         exception.addSuppressed(e);

         throw exception;
      }
   }
}
