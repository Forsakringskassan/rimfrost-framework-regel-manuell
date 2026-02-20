package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
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
import se.fk.rimfrost.framework.regel.logic.RegelRequestHandlerBase;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.ImmutableRegelData;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.RegelData;
import se.fk.rimfrost.framework.regel.manuell.presentation.rest.RegelManuellUppgiftDoneHandler;
import se.fk.rimfrost.framework.regel.logic.dto.FSSAinformation;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.logic.entity.*;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.CommonRegelData;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.RegelManuellDataStorage;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;
import se.fk.rimfrost.framework.storage.DataStorageProvider;
import se.fk.rimfrost.framework.storage.StorageManagerProvider;

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
   DataStorageProvider<? extends RegelManuellDataStorage> dataStorageProvider;

   @Inject
   StorageManagerProvider storageManagerProvider;

   @Inject
   RegelManuellServiceInterface regelService;

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

   public RegelData createRegelData(KundbehovsflodeResponse kundbehovsflodeResponse)
   {
      var uppgiftData = ImmutableUppgiftData.builder()
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now())
            .uppgiftStatus(UppgiftStatus.PLANERAD)
            .fssaInformation(FSSAinformation.HANDLAGGNING_PAGAR)
            .build();

      var ersattninglist = new ArrayList<ErsattningData>();
      for (var ersattning : kundbehovsflodeResponse.ersattning())
      {
         var ersattningData = ImmutableErsattningData.builder()
               .id(ersattning.ersattningsId())
               .beslutsutfall(Beslutsutfall.valueOf(ersattning.beslutsutfall()))
               .build();
         ersattninglist.add(ersattningData);
      }

      return ImmutableRegelData.builder()
            .uppgiftData(uppgiftData)
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

      var regelData = createRegelData(kundbehovsflodeResponse);

      var cloudevent = createCloudEvent(request);

      synchronized (commonRegelData.getLock())
      {
         var cloudevents = commonRegelData.getCloudEvents();
         var regelDatas = commonRegelData.getRegelDatas();

         cloudevents.put(request.kundbehovsflodeId(), cloudevent);
         regelDatas.put(request.kundbehovsflodeId(), regelData);
         storageManager.store(commonRegelData);
      }

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

      var updatedUppgiftData = ImmutableUppgiftData.builder()
            .from(regelData.uppgiftData())
            .uppgiftId(oulResponse.uppgiftId())
            .build();

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .uppgiftData(updatedUppgiftData)
            .build();

      synchronized (commonRegelData.getLock())
      {
         var regelDatas = commonRegelData.getRegelDatas();
         regelDatas.put(oulResponse.kundbehovsflodeId(), updatedRegelData);
         storageManager.store(regelDatas);
      }

      putKundbehovsflode(oulResponse.kundbehovsflodeId(), updatedUppgiftData, regelData.underlag());
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
               .filter(r -> r.uppgiftData().uppgiftId() != null && r.uppgiftData().uppgiftId().equals(oulStatus.uppgiftId()))
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

      var updatedUppgiftData = ImmutableUppgiftData.builder()
            .from(regelData.uppgiftData())
            .utforarId(oulStatus.uppgiftId())
            .uppgiftStatus(toUppgiftStatus(oulStatus.uppgiftStatus()))
            .build();

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .uppgiftData(updatedUppgiftData)
            .build();

      synchronized (commonRegelData.getLock())
      {
         var regelDatas = dataStorageProvider.getDataStorage().getCommonRegelData().getRegelDatas();
         regelDatas.put(oulStatus.kundbehovsflodeId(), updatedRegelData);
         storageManager.store(regelDatas);
      }

      putKundbehovsflode(oulStatus.kundbehovsflodeId(), updatedUppgiftData, regelData.underlag());
   }

   @Override
   public void handleUppgiftDone(UUID kundbehovsflodeId)
   {
      RegelData regelData = commonRegelData.getRegelData(kundbehovsflodeId);
      CloudEventData cloudevent = commonRegelData.getCloudEventData(kundbehovsflodeId);

      var updatedUppgiftData = ImmutableUppgiftData.builder()
            .from(regelData.uppgiftData())
            .uppgiftStatus(UppgiftStatus.AVSLUTAD)
            .build();

      var updatedRegelData = ImmutableRegelData.builder()
            .from(regelData)
            .uppgiftData(updatedUppgiftData)
            .build();

      putKundbehovsflode(kundbehovsflodeId, updatedUppgiftData, regelData.underlag());

      sendResponse(kundbehovsflodeId, cloudevent, regelService.decideUtfall(updatedRegelData));

      DelayedException delayedException = new DelayedException();
      // We do this before cleaning up CloudEvent and RegelData instances
      // since the rule could possibly have dependency on them during
      // callback execution.
      try
      {
         regelService.handleRegelDone(kundbehovsflodeId);
      }
      catch (Exception e)
      {
         delayedException.addSuppressed(e);
      }

      synchronized (commonRegelData.getLock())
      {
         // Send update request within lock scope to guarantee that OUL status update
         // doesn't accidentally restore regelData.
         try
         {
            oulKafkaProducer.sendOulStatusUpdate(updatedRegelData.uppgiftData().uppgiftId(), Status.AVSLUTAD);
         }
         catch (Exception e)
         {
            delayedException.addSuppressed(e);
         }

         try
         {
            var regelDatas = commonRegelData.getRegelDatas();
            var cloudevents = commonRegelData.getCloudEvents();
            cloudevents.remove(kundbehovsflodeId);
            regelDatas.remove(kundbehovsflodeId);
            storageManager.store(commonRegelData);
         }
         catch (Exception e)
         {
            delayedException.addSuppressed(e);
         }
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
