package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.handlaggning.adapter.dto.ImmutableHandlaggningRequest;
import se.fk.rimfrost.framework.handlaggning.adapter.dto.HandlaggningResponse;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.regel.logic.RegelRequestHandlerBase;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.ImmutableRegelData;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.RegelData;
import se.fk.rimfrost.framework.regel.manuell.presentation.rest.RegelManuellUppgiftDoneHandler;
import se.fk.rimfrost.framework.regel.logic.dto.FSSAinformation;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.logic.entity.*;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableManuellRegelCommonData;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;

@SuppressWarnings("unused")
@ApplicationScoped
public class RegelManuellRequestHandler extends RegelRequestHandlerBase
      implements OulHandlerInterface, RegelManuellUppgiftDoneHandler, RegelRequestHandlerInterface
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelManuellRequestHandler.class);

   @ConfigProperty(name = "kafka.subtopic")
   String oulReplyToSubTopic;

   @Inject
   protected OulKafkaProducer oulKafkaProducer;

   @Inject
   RegelManuellServiceInterface regelService;

   @Inject
   ManuellRegelCommonDataStorage dataStorage;

   public RegelData createRegelData(HandlaggningResponse handlaggningResponse, UUID aktivitetId)
   {
      var uppgiftData = ImmutableUppgiftData.builder()
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now())
            .uppgiftStatus(UppgiftStatus.PLANERAD)
            .fssaInformation(FSSAinformation.HANDLAGGNING_PAGAR)
            .build();

      var ersattninglist = new ArrayList<ErsattningData>();
      for (var ersattning : handlaggningResponse.ersattning())
      {
         var ersattningData = ImmutableErsattningData.builder()
               .id(ersattning.ersattningsId())
               .beslutsutfall(Beslutsutfall.valueOf(ersattning.beslutsutfall()))
               .build();
         ersattninglist.add(ersattningData);
      }

      return ImmutableRegelData.builder()
            .aktivitetId(aktivitetId)
            .uppgiftData(uppgiftData)
            .ersattningar(ersattninglist)
            .underlag(new ArrayList<>())
            .build();
   }

   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      var handlaggningResponse = handlaggningAdapter.getHandlaggningInfo(
            ImmutableHandlaggningRequest.builder()
                  .handlaggningId(request.handlaggningId())
                  .build());

      var regelData = createRegelData(handlaggningResponse, request.aktivitetId());

      var cloudevent = createCloudEvent(request);

      var commonRegelData = ImmutableManuellRegelCommonData.builder()
            .cloudEventData(cloudevent)
            .regelData(regelData)
            .build();
      dataStorage.setManuellRegelCommonData(request.handlaggningId(), commonRegelData);

      var oulMessageRequest = ImmutableOulMessageRequest.builder()
            .handlaggningId(request.handlaggningId())
            .yrkande(handlaggningResponse.formanstyp())
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
      var regelData = commonRegelData.regelData();

      var updatedUppgiftData = ImmutableUppgiftData.builder()
            .from(regelData.uppgiftData())
            .uppgiftId(oulResponse.uppgiftId())
            .build();

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .uppgiftData(updatedUppgiftData)
            .build();

      var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
            .from(commonRegelData)
            .regelData(updatedRegelData)
            .build();
      dataStorage.setManuellRegelCommonData(oulResponse.handlaggningId(), updatedCommonRegelData);

      putHandlaggning(oulResponse.handlaggningId(), updatedUppgiftData, regelData.underlag());
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      var commonRegelData = dataStorage.getManuellRegelCommonData(oulStatus.handlaggningId());
      RegelData regelData = commonRegelData.regelData();

      if (regelData == null)
      {
         /* This may happen if regelData was cleaned up during handleUppgiftDone
          * and a notification was sent to OUL that the task was finished.
          */
         if (oulStatus.uppgiftStatus() != se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus.AVSLUTAD)
         {
            LOGGER.error(
                  "RegelData for handlaggningId {} was not found during OUL status update for uppgift {} with uppgiftStatus {}",
                  oulStatus.handlaggningId(), oulStatus.uppgiftId(), oulStatus.uppgiftStatus());
         }

         return;
      }

      var updatedUppgiftData = ImmutableUppgiftData.builder()
            .from(regelData.uppgiftData())
            .utforarId(oulStatus.uppgiftId())
            .uppgiftStatus(toUppgiftStatus(oulStatus.uppgiftStatus()))
            .build();

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .uppgiftData(updatedUppgiftData)
            .build();

      var updatedCommonRegelData = ImmutableManuellRegelCommonData.builder()
            .from(commonRegelData)
            .regelData(updatedRegelData)
            .build();
      dataStorage.setManuellRegelCommonData(oulStatus.handlaggningId(), updatedCommonRegelData);

      putHandlaggning(oulStatus.handlaggningId(), updatedUppgiftData, regelData.underlag());
   }

   @Override
   public void handleUppgiftDone(UUID handlaggningId)
   {
      var commonRegelData = dataStorage.getManuellRegelCommonData(handlaggningId);
      RegelData regelData = commonRegelData.regelData();
      CloudEventData cloudevent = commonRegelData.cloudEventData();

      var updatedUppgiftData = ImmutableUppgiftData.builder()
            .from(regelData.uppgiftData())
            .uppgiftStatus(UppgiftStatus.AVSLUTAD)
            .build();

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .uppgiftData(updatedUppgiftData)
            .build();

      putHandlaggning(handlaggningId, updatedUppgiftData, regelData.underlag());

      sendResponse(handlaggningId, cloudevent, regelService.decideUtfall(updatedRegelData));

      DelayedException delayedException = new DelayedException();
      // We do this before cleaning up CloudEvent and RegelData instances
      // since the rule could possibly have dependency on them during
      // callback execution.
      try
      {
         regelService.handleRegelDone(handlaggningId);
      }
      catch (Exception e)
      {
         delayedException.addSuppressed(e);
      }

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
         oulKafkaProducer.sendOulStatusUpdate(updatedRegelData.uppgiftData().uppgiftId(), Status.AVSLUTAD);
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
