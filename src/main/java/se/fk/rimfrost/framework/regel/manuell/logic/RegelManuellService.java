package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaProducer;
import se.fk.rimfrost.framework.oul.integration.kafka.dto.ImmutableOulMessageRequest;
import se.fk.rimfrost.framework.oul.logic.dto.OulResponse;
import se.fk.rimfrost.framework.oul.logic.dto.OulStatus;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulHandlerInterface;
import se.fk.rimfrost.framework.oul.presentation.rest.OulUppgiftDoneHandler;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProviderYaml;
import se.fk.rimfrost.framework.regel.integration.kafka.RegelKafkaProducer;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.KundbehovsflodeAdapter;
import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.dto.ImmutableKundbehovsflodeRequest;
import se.fk.rimfrost.framework.regel.logic.RegelMapper;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.dto.UppgiftStatus;
import se.fk.rimfrost.framework.regel.logic.entity.*;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.FSSAinformation;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public class RegelManuellService implements RegelRequestHandlerInterface, OulHandlerInterface, OulUppgiftDoneHandler
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ConfigProperty(name = "kafka.source")
   String kafkaSource;

   @Inject
   protected RegelMapper regelMapper;

   @Inject
   protected RegelManuellMapper regelManuellMapper;

   @Inject
   protected KundbehovsflodeAdapter kundbehovsflodeAdapter;

   @Inject
   protected RegelConfigProviderYaml regelConfigProvider;

   @Inject
   protected RegelKafkaProducer regelKafkaProducer;

   @Inject
   protected OulKafkaProducer oulKafkaProducer;

   protected final Map<UUID, CloudEventData> cloudevents = new HashMap<>();
   protected final Map<UUID, RegelData> regelDatas = new HashMap<>();

   public void handleRegelRequest(RegelDataRequest request)
   {
      var kundbehovsflodeRequest = ImmutableKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .build();
      var kundbehovflodesResponse = kundbehovsflodeAdapter.getKundbehovsflodeInfo(kundbehovsflodeRequest);
      var cloudeventData = ImmutableCloudEventData.builder()
            .id(request.id())
            .kogitoparentprociid(request.kogitoparentprociid())
            .kogitoprocid(request.kogitoprocid())
            .kogitoprocinstanceid(request.kogitoprocinstanceid())
            .kogitoprocist(request.kogitoprocist())
            .kogitoprocversion(request.kogitoprocversion())
            .kogitorootprocid(request.kogitorootprocid())
            .kogitorootprociid(request.kogitorootprociid())
            .type(responseTopic)
            .source(kafkaSource)
            .build();
      var ersattninglist = new ArrayList<ErsattningData>();
      for (var ersattning : kundbehovflodesResponse.ersattning())
      {
         var ersattningData = ImmutableErsattningData.builder()
               .id(ersattning.ersattningsId())
               .build();
         ersattninglist.add(ersattningData);
      }
      var regelData = ImmutableRegelData.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .cloudeventId(cloudeventData.id())
            .ersattningar(ersattninglist)
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now())
            .uppgiftStatus(UppgiftStatus.PLANERAD)
            .fssaInformation(FSSAinformation.HANDLAGGNING_PAGAR)
            .underlag(new ArrayList<>())
            .build();
      cloudevents.put(cloudeventData.id(), cloudeventData);
      regelDatas.put(regelData.kundbehovsflodeId(), regelData);

      var regelConfig = regelConfigProvider.getConfig();
      var oulMessageRequest = ImmutableOulMessageRequest.builder()
            .kundbehovsflodeId(request.kundbehovsflodeId())
            .kundbehov(kundbehovflodesResponse.formanstyp())
            .regel(regelConfig.getSpecifikation().getNamn())
            .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
            .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
            .roll(regelConfig.getSpecifikation().getRoll())
            .url(regelConfig.getUppgift().getPath())
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
      updateKundbehovsflodeInfo(updatedRegelData);
   }

   @Override
   public void handleOulStatus(OulStatus oulStatus)
   {
      RegelData regelData = regelDatas.values()
            .stream()
            .filter(r -> r.uppgiftId().equals(oulStatus.uppgiftId()))
            .findFirst()
            .orElse(regelDatas.get(oulStatus.kundbehovsflodeId()));
      updateKundbehovsflodeInfo(regelData);
   }

   public void updateKundbehovsflodeInfo(RegelData regelData)
   {
      var request = regelManuellMapper.toUpdateKundbehovsflodeRequest(regelData, regelConfigProvider.getConfig());
      kundbehovsflodeAdapter.updateKundbehovsflodeInfo(request);
   }

   @Override
   public void handleUppgiftDone(UUID kundbehovsflodeId)
   {
      var regelData = regelDatas.get(kundbehovsflodeId);

      var updatedRegelDataBuilder = ImmutableRegelData.builder()
            .from(regelData);

      updatedRegelDataBuilder.uppgiftStatus(UppgiftStatus.AVSLUTAD);

      var updatedRegelData = updatedRegelDataBuilder.build();
      regelDatas.put(kundbehovsflodeId, updatedRegelData);

      var utfall = regelData.ersattningar().stream().allMatch(e -> e.beslutsutfall() == Beslutsutfall.JA) ? Utfall.JA
            : Utfall.NEJ;
      var cloudevent = cloudevents.get(updatedRegelData.cloudeventId());
      var regelResponse = regelMapper.toRegelResponse(kundbehovsflodeId, cloudevent, utfall);
      oulKafkaProducer.sendOulStatusUpdate(updatedRegelData.uppgiftId(), Status.AVSLUTAD);
      regelKafkaProducer.sendRegelResponse(regelResponse);

      updateKundbehovsflodeInfo(updatedRegelData);
   }
}
