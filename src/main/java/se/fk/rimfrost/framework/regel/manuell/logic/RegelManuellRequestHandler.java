package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.handlaggning.model.*;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelRequestHandlerBase;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.entity.*;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableManuellRegelCommonData;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;

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

   public HandlaggningUpdate createHandlaggningUpdate(Handlaggning handlaggning, UUID aktivitetId, UUID kogitoprocInstanceId)
   {
      var uppgiftSpecifikation = ImmutableUppgiftSpecifikation.builder()
            .id(regelConfig.getSpecifikation().getId())
            .version(regelConfig.getSpecifikation().getVersion())
            .build();

      var uppgift = ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(1)
            .aktivitetId(aktivitetId)
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now()) // TODO: Figure out when this should be set and to what this should be set to
            .uppgiftStatus(UppgiftStatus.PLANERAD)
            .fSSAinformation(FSSAinformation.HANDLAGGNING_PAGAR)
            .uppgiftSpecifikation(uppgiftSpecifikation)
            .build();

      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(handlaggning.yrkande())
            .processInstansId(kogitoprocInstanceId)
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(uppgift)
            .build();
   }

   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      var handlaggning = handlaggningAdapter.readHandlaggning(request.handlaggningId());

      var handlaggningUpdate = createHandlaggningUpdate(handlaggning, request.aktivitetId(), request.kogitoprocinstanceid());

      var cloudevent = createCloudEvent(request);

      var commonRegelData = ImmutableManuellRegelCommonData.builder()
            .cloudEventData(cloudevent)
            .handlaggningUpdate(handlaggningUpdate)
            .build();
      dataStorage.setManuellRegelCommonData(request.handlaggningId(), commonRegelData);

      var oulMessageRequest = ImmutableOulMessageRequest.builder()
            .handlaggningId(request.handlaggningId())
            .individer(
                  handlaggning.yrkande().individYrkandeRoller().stream().map(Yrkande.IndividYrkandeRoll::individId).toList())
            .regel(regelConfig.getSpecifikation().getNamn())
            .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
            .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
            .roll(regelConfig.getSpecifikation().getRoll())
            .url(regelConfig.getUppgift().getPath())
            .replyToTopic(oulReplyToSubTopic)
            .build();
      oulKafkaProducer.sendOulRequest(oulMessageRequest);
   }

   @Override
   public void handleOulResponse(OulResponse oulResponse)
   {
      var commonRegelData = dataStorage.getManuellRegelCommonData(oulResponse.handlaggningId());

      var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
            .from(commonRegelData)
            .oulUppgiftId(oulResponse.uppgiftId())
            .build();
      dataStorage.setManuellRegelCommonData(oulResponse.handlaggningId(), updatedCommonRegelData);
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      var commonRegelData = dataStorage.getManuellRegelCommonData(oulStatus.handlaggningId());

      if (commonRegelData == null)
      {
         /* This may happen if commonRegelData was cleaned up during handleUppgiftDone
          * and a notification was sent to OUL that the task was finished.
          */
         if (oulStatus.uppgiftStatus() != se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus.AVSLUTAD)
         {
            LOGGER.error(
                  "CommonRegelData for handlaggningId {} was not found during OUL status update for uppgift {} with uppgiftStatus {}",
                  oulStatus.handlaggningId(), oulStatus.uppgiftId(), oulStatus.uppgiftStatus());
         }

         return;
      }

      var handlaggningUpdate = commonRegelData.handlaggning();

      var updatedUppgift = ImmutableUppgift.builder()
            .from(handlaggningUpdate.uppgift())
            .utforarId(oulStatus.utforarId())
            .uppgiftStatus(toUppgiftStatus(oulStatus.uppgiftStatus()))
            .build();

      var updatedHandlaggning = ImmutableHandlaggningUpdate.builder()
            .from(handlaggningUpdate)
            .uppgift(updatedUppgift)
            .build();

      var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
            .from(commonRegelData)
            .handlaggningUpdate(updatedHandlaggning)
            .build();
      dataStorage.setManuellRegelCommonData(oulStatus.handlaggningId(), updatedCommonRegelData);
      handlaggningAdapter.updateHandlaggning(updatedHandlaggning);
   }

   @Override
   public void handleUppgiftDone(UUID handlaggningId, Utfall utfall)
   {
      var commonRegelData = dataStorage.getManuellRegelCommonData(handlaggningId);
      HandlaggningUpdate handlaggningUpdate = commonRegelData.handlaggningUpdate();
      CloudEventData cloudevent = commonRegelData.cloudEventData();
      UUID oulUppgiftId = commonRegelData.oulUppgiftId();

      var updatedUppgift = ImmutableUppgift.builder()
            .from(handlaggningUpdate.uppgift())
            .uppgiftStatus(UppgiftStatus.AVSLUTAD)
            .build();

      var updatedHandlaggning = ImmutableHandlaggningUpdate.builder()
            .from(handlaggningUpdate)
            .uppgift(updatedUppgift)
            .build();

      handlaggningAdapter.updateHandlaggning(updatedHandlaggning);

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
         oulKafkaProducer.sendOulStatusUpdate(oulUppgiftId, Status.AVSLUTAD);
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

   private UppgiftStatus toUppgiftStatus(se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus uppgiftStatus) {
        return switch (uppgiftStatus) {
            case NY -> UppgiftStatus.PLANERAD;
            case TILLDELAD -> UppgiftStatus.TILLDELAD;
            case AVSLUTAD -> UppgiftStatus.AVSLUTAD;
            default -> throw new IllegalStateException("Unexpected value: " + uppgiftStatus);
        };
    }
}
