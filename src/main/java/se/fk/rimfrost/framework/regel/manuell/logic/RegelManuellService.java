package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.kundbehovsflode.adapter.dto.ImmutableKundbehovsflodeRequest;
import se.fk.rimfrost.framework.kundbehovsflode.adapter.dto.KundbehovsflodeResponse;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.regel.logic.ImmutableProcessRegelResponse;
import se.fk.rimfrost.framework.regel.logic.ProcessRegelResponse;
import se.fk.rimfrost.framework.regel.logic.RegelRequestHandlerBase;
import se.fk.rimfrost.framework.regel.logic.RegelServiceInterface;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.manuell.presentation.rest.RegelManuellUppgiftDoneHandler;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.dto.FSSAinformation;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.logic.entity.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public abstract class RegelManuellService extends RegelRequestHandlerBase
      implements OulHandlerInterface, RegelManuellUppgiftDoneHandler, RegelServiceInterface
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelManuellService.class);

   @ConfigProperty(name = "kafka.subtopic")
   String oulReplyToSubTopic;

   @Inject
   protected OulKafkaProducer oulKafkaProducer;

   protected final Map<UUID, CloudEventData> cloudevents = new HashMap<>();
   protected final Map<UUID, RegelData> regelDatas = new HashMap<>();

   @Override
   public ProcessRegelResponse processRegel(KundbehovsflodeResponse kundbehovsflodeResponse)
   {

      var ersattninglist = new ArrayList<ErsattningData>();
      for (var ersattning : kundbehovsflodeResponse.ersattning())
      {
         var ersattningData = ImmutableErsattningData.builder()
               .id(ersattning.ersattningsId())
               .beslutsutfall(Beslutsutfall.valueOf(ersattning.beslutsutfall()))
               .build();
         ersattninglist.add(ersattningData);
      }

      return ImmutableProcessRegelResponse.builder()
            .ersattningar(ersattninglist)
            .underlag(new ArrayList<>())
            .build();

   }

   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      var kundbehovsflodeResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(
            ImmutableKundbehovsflodeRequest.builder()
                  .kundbehovsflodeId(request.kundbehovsflodeId())
                  .build());

      var processRegelResponse = processRegel(kundbehovsflodeResponse);

      var cloudevent = createCloudEvent(request);

      var regelData = ImmutableRegelData.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now())
            .uppgiftStatus(UppgiftStatus.PLANERAD)
            .fssaInformation(FSSAinformation.HANDLAGGNING_PAGAR)
            .ersattningar(processRegelResponse.ersattningar())
            .underlag(processRegelResponse.underlag())
            .build();

      cloudevents.put(regelData.kundbehovsflodeId(), cloudevent);
      regelDatas.put(regelData.kundbehovsflodeId(), regelData);

      var regelConfig = regelConfigProvider.getConfig();
      var oulMessageRequest = ImmutableOulMessageRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .kundbehov(kundbehovsflodeResponse.formanstyp())
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
      var bekraftaBeslutData = regelDatas.get(oulResponse.kundbehovsflodeId());
      var updatedRegelData = ImmutableRegelData.builder()
            .from(bekraftaBeslutData)
            .uppgiftId(oulResponse.uppgiftId())
            .build();
      regelDatas.put(updatedRegelData.kundbehovsflodeId(), updatedRegelData);
      updateKundbehovsFlode(updatedRegelData);
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      RegelData regelData = regelDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(oulStatus.uppgiftId()))
            .findFirst()
            .orElse(regelDatas.get(oulStatus.kundbehovsflodeId()));

      if (regelData == null)
      {
         /* This may happen if regelData was cleaned up during handleUppgiftDone
          * and a notification was sent to OUL that the task was finished.
          */
         if (oulStatus.uppgiftStatus() != se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus.AVSLUTAD)
         {
            LOGGER.error(
                  "RegelData for kundbehovsflodeId {} was not found during OUL status update for uppgift {} with uppgiftStatus {}",
                  oulStatus.kundbehovsflodeId(), oulStatus.uppgiftId(), oulStatus.uppgiftStatus());
         }

         return;
      }

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .utforarId(oulStatus.utforarId())
            .uppgiftStatus(toUppgiftStatus(oulStatus.uppgiftStatus()))
            .build();

      regelDatas.put(regelData.kundbehovsflodeId(), updatedRegelData);
      updateKundbehovsFlode(updatedRegelData);
   }

   @Override
   public void handleUppgiftDone(UUID kundbehovsflodeId)
   {
      var regelData = regelDatas.get(kundbehovsflodeId);

      var updatedRegelDataBuilder = ImmutableRegelData.builder()
            .from(regelData);

      updatedRegelDataBuilder.uppgiftStatus(UppgiftStatus.AVSLUTAD);

      var updatedRegelData = updatedRegelDataBuilder.build();

      oulKafkaProducer.sendOulStatusUpdate(updatedRegelData.uppgiftId(), Status.AVSLUTAD);

      regelDatas.put(kundbehovsflodeId, updatedRegelData);
      var cloudevent = cloudevents.get(updatedRegelData.kundbehovsflodeId());

      sendResponse(regelData, cloudevent, decideUtfall(updatedRegelData));

      updateKundbehovsFlode(updatedRegelData);
   }

   protected abstract Utfall decideUtfall(RegelData regelData);

   private UppgiftStatus toUppgiftStatus(se.fk.rimfrost.framework.oul.logic.dto.UppgiftStatus uppgiftStatus) {
        return switch (uppgiftStatus) {
            case NY -> UppgiftStatus.PLANERAD;
            case TILLDELAD -> UppgiftStatus.TILLDELAD;
            case AVSLUTAD -> UppgiftStatus.AVSLUTAD;
            default -> throw new IllegalStateException("Unexpected value: " + uppgiftStatus);
        };
    }
}
