package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.store.storage.types.StorageManager;
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
import se.fk.rimfrost.framework.regel.manuell.storage.entity.CommonRegelData;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.RegelManuellDataStorage;
import se.fk.rimfrost.framework.storage.DataStorageProvider;
import se.fk.rimfrost.framework.storage.StorageManagerProvider;

@SuppressWarnings("unused")
public abstract class RegelManuellService extends RegelRequestHandlerBase
      implements OulHandlerInterface, RegelManuellUppgiftDoneHandler, RegelServiceInterface
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelManuellService.class);

   @ConfigProperty(name = "kafka.subtopic")
   String oulReplyToSubTopic;

   @Inject
   protected OulKafkaProducer oulKafkaProducer;

   @Inject
   DataStorageProvider<? extends RegelManuellDataStorage> dataStorageProvider;

   @Inject
   StorageManagerProvider storageManagerProvider;

   protected StorageManager storageManager;

   protected CommonRegelData commonRegelData;

   /*
    * Note: The name of the @PostConstruct method should if
    * possible be kept as init<classname> in order to avoid
    * being shadowed by any @PostConstruct methods in any
    * inheriting class that happens to have the same method
    * name.
    */
   @PostConstruct
   void initRegelManuellService()
   {
      this.storageManager = storageManagerProvider.getStorageManager();
      this.commonRegelData = dataStorageProvider.getDataStorage().getCommonRegelData();
   }

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

      synchronized (commonRegelData.getLock())
      {
         var cloudevents = commonRegelData.getCloudEvents();
         var regelDatas = commonRegelData.getRegelDatas();

         cloudevents.put(regelData.kundbehovsflodeId(), cloudevent);
         regelDatas.put(regelData.kundbehovsflodeId(), regelData);
         storageManager.store(commonRegelData);
      }

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
      var regelData = commonRegelData.getRegelData(oulResponse.kundbehovsflodeId());

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .uppgiftId(oulResponse.uppgiftId())
            .build();

      synchronized (commonRegelData.getLock())
      {
         var regelDatas = commonRegelData.getRegelDatas();
         regelDatas.put(updatedRegelData.kundbehovsflodeId(), updatedRegelData);
         storageManager.store(regelDatas);
      }

      updateKundbehovsFlode(updatedRegelData);
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      RegelData regelData;

      synchronized (commonRegelData.getLock())
      {
         var regelDatas = dataStorageProvider.getDataStorage().getCommonRegelData().getRegelDatas();

         regelData = regelDatas.values()
               .stream()
               .filter(r -> r.uppgiftId().equals(oulStatus.uppgiftId()))
               .findFirst()
               .orElse(regelDatas.get(oulStatus.kundbehovsflodeId()));
      }

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

      synchronized (commonRegelData.getLock())
      {
         var regelDatas = dataStorageProvider.getDataStorage().getCommonRegelData().getRegelDatas();
         regelDatas.put(regelData.kundbehovsflodeId(), updatedRegelData);
         storageManager.store(regelDatas);
      }

      updateKundbehovsFlode(updatedRegelData);
   }

   @Override
   public void handleUppgiftDone(UUID kundbehovsflodeId)
   {
      RegelData regelData = commonRegelData.getRegelData(kundbehovsflodeId);
      CloudEventData cloudevent = commonRegelData.getCloudEventData(kundbehovsflodeId);

      var updatedRegelDataBuilder = ImmutableRegelData.builder()
            .from(regelData);

      updatedRegelDataBuilder.uppgiftStatus(UppgiftStatus.AVSLUTAD);

      var updatedRegelData = updatedRegelDataBuilder.build();

      updateKundbehovsFlode(updatedRegelData);

      sendResponse(regelData, cloudevent, decideUtfall(updatedRegelData));

      synchronized (commonRegelData.getLock())
      {
         // Send update request within lock scope to guarantee that OUL status update
         // doesn't accidentally restore regelData.
         oulKafkaProducer.sendOulStatusUpdate(updatedRegelData.uppgiftId(), Status.AVSLUTAD);

         var regelDatas = commonRegelData.getRegelDatas();
         var cloudevents = commonRegelData.getCloudEvents();
         cloudevents.remove(kundbehovsflodeId);
         regelDatas.remove(kundbehovsflodeId);
         storageManager.store(commonRegelData);
      }
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
