package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.oul.model.Erbjudande;
import se.fk.rimfrost.framework.oul.model.ImmutableErbjudande;
import se.fk.rimfrost.framework.referensdata.ErbjudandeReferensdataInterface;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProviderYaml;
import se.fk.rimfrost.framework.regel.integration.kafka.RegelKafkaProducer;
import se.fk.rimfrost.framework.regel.integration.kafka.dto.ImmutableRegelResponse;
import se.fk.rimfrost.framework.regel.logic.CloudEventAttributesMapper;
import se.fk.rimfrost.framework.regel.logic.RegelCancelledException;
import se.fk.rimfrost.framework.regel.logic.RegelMapper;
import se.fk.rimfrost.framework.regel.logic.config.RegelConfig;
import se.fk.rimfrost.framework.regel.logic.dto.RegelDataRequest;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.ImmutableOulUppgiftSpec;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.regel.presentation.kafka.RegelRequestHandlerInterface;

/**
 * Handles incoming regel requests and done-callbacks for the manuell regel flow.
 *
 * <p>Delegates OUL uppgift creation (including correlation persistence and
 * handläggning updates) to {@link OulUppgiftService}. When the handläggare
 * marks the task as done, this handler reads correlation state, ends the OUL
 * uppgift, sends the final Kafka response, and performs cleanup.
 */
@SuppressWarnings("unused")
@ApplicationScoped
public class RegelManuellRequestHandler
      implements RegelRequestHandlerInterface, RegelManuellUppgiftDoneHandler
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelManuellRequestHandler.class);

   private static final String AVSLUTAD = "AVSLUTAD";

   @ConfigProperty(name = "kafka.source")
   String kafkaSource;

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   RegelKafkaProducer regelKafkaProducer;

   @Inject
   RegelMapper regelMapper;

   @Inject
   RegelConfigProviderYaml regelConfigProvider;

   @Inject
   ErbjudandeReferensdataInterface erbjudandeReferensdata;

   @Inject
   OulUppgiftService oulUppgiftService;

   private RegelConfig regelConfig;

   @PostConstruct
   void initRegelManuellRequestHandler()
   {
      this.regelConfig = regelConfigProvider.getConfig();
   }

   /**
    * Handles an incoming {@link RegelDataRequest} by reading the handläggning,
    * building an OUL uppgift specification, and delegating creation to
    * {@link OulUppgiftService}. On any failure an error response is sent on the
    * request's {@code replyTo} topic.
    */
   @Override
   public void handleRegelRequest(RegelDataRequest request)
   {
      CloudEventData cloudEvent = null;
      try
      {
         cloudEvent = ImmutableCloudEventData.builder()
               .from(buildBaseCloudEvent(request))
               .type(responseTopic)
               .source(kafkaSource)
               .build();
         var handlaggning = getHandlaggning(request.handlaggningId(), cloudEvent);
         var erbjudandeNamn = erbjudandeReferensdata.getErbjudandeNamn(handlaggning.yrkande().erbjudandeId());

         var spec = ImmutableOulUppgiftSpec.builder()
               .handlaggningId(request.handlaggningId())
               .handlaggning(handlaggning)
               .replyTo(request.replyTo())
               .cloudEventData(cloudEvent)
               .cloudEventAttributes(CloudEventAttributesMapper.toAttributes(cloudEvent))
               .regel(regelConfig.getSpecifikation().getNamn())
               .beskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
               .verksamhetslogik(regelConfig.getSpecifikation().getVerksamhetslogik())
               .roll(regelConfig.getSpecifikation().getRoll())
               .url(regelConfig.getUppgift().getPath())
               .erbjudande(buildErbjudande(handlaggning.yrkande().erbjudandeId(), erbjudandeNamn))
               .aktivitetId(request.aktivitetId())
               .uppgiftSpecifikationId(regelConfig.getSpecifikation().getId())
               .uppgiftSpecifikationVersion(regelConfig.getSpecifikation().getVersion())
               .build();

         oulUppgiftService.createOulUppgift(spec);
      }
      catch (Exception e)
      {
         LOGGER.error("Regel run cancelled due to error", e);
         var regelErrorInformation = buildRegelErrorInformation(RegelFelkod.RIMFROST_OTHER,
               "Regel failed due to unexpected internal error. Handlaggning id: " + request.handlaggningId());
         if (e instanceof RegelCancelledException ex)
         {
            regelErrorInformation = ex.getRegelErrorInformation();
         }
         sendErrorResponse(request.handlaggningId(), cloudEvent, regelErrorInformation, request.replyTo());
      }
   }

   /**
    * Handles a done-callback from the REST layer by reading correlation state,
    * ending the OUL uppgift, sending the final {@code RegelResponse}, cleaning up
    * correlation data, and performing the final handläggning update.
    *
    * <p>If any pre-condition read (correlation storage, handläggning) or
    * {@link se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService#endOulUppgift}
    * fails, the method throws {@link RegelManuellException} (HTTP 5xx) and no Kafka
    * response is sent. The handläggare can retry the {@code POST /done} call.
    *
    * <p>Once {@code endOulUppgift} succeeds, the Kafka response is sent before
    * cleanup and the final handläggning update, so a failure in those subsequent
    * steps does not prevent response delivery.
    */
   @Override
   public void handleUppgiftDone(UUID handlaggningId, Utfall utfall)
   {
      OulCorrelationData correlation = oulUppgiftService.getCorrelationData(handlaggningId);

      if (correlation == null)
      {
         LOGGER.error("Failed to read correlation data in handleUppgiftDone for handlaggningId: {}", handlaggningId);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Failed to read correlation data");
      }

      Handlaggning handlaggning;
      try
      {
         handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error in handleUppgiftDone() while trying to read handlaggning with id: {}", handlaggningId, e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }

      try
      {
         oulUppgiftService.endOulUppgift(correlation.oulUppgiftId(), "Uppgift klar");
      }
      catch (OulException e)
      {
         LOGGER.error("Error in handleUppgiftDone() while trying to end operativ uppgift for handlaggningId: {}",
               handlaggningId, e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }

      sendRegelSuccessResponse(handlaggningId, correlation.cloudEventData(), utfall, correlation.replyTopic());

      oulUppgiftService.cleanupCorrelation(handlaggningId);

      var uppgift = correlation.uppgift();
      var updatedUppgift = ImmutableUppgift.builder()
            .from(uppgift)
            .version(uppgift.version() + 1)
            .uppgiftStatus(AVSLUTAD)
            .utfordTs(OffsetDateTime.now())
            .build();
      var handlaggningUpdate = ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(handlaggning.yrkande())
            .processInstansId(handlaggning.processInstansId())
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(updatedUppgift)
            .build();
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (Exception e)
      {
         LOGGER.error("Error in handleUppgiftDone() while updating handlaggning for id: {}", handlaggningId, e);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, e.getMessage(), e);
      }
   }

   private CloudEventData buildBaseCloudEvent(RegelDataRequest request)
   {
      return ImmutableCloudEventData.builder()
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
   }

   private Handlaggning getHandlaggning(UUID handlaggningId, CloudEventData cloudEventData)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         var message = String.format("Failed to read handlaggning. handlaggningId: %s, kogitoprocId: %s",
               handlaggningId, cloudEventData.kogitoprocinstanceid());
         var regelErrorInformation = buildRegelErrorInformation(RegelFelkod.RIMFROST_HANDLAGGNING_READ_FAILURE, message);
         throw new RegelCancelledException(regelErrorInformation, message, e);
      }
   }

   private Erbjudande buildErbjudande(String id, String namn)
   {
      return ImmutableErbjudande.builder()
            .id(id)
            .namn(namn)
            .build();
   }

   private void sendErrorResponse(UUID handlaggningId, CloudEventData cloudEventData,
         RegelErrorInformation regelErrorInformation, String replyTo)
   {
      if (handlaggningId == null || cloudEventData == null || regelErrorInformation == null)
      {
         LOGGER.warn(
               "Could not send error response. Missing one or more required parameters. handlaggningId: {}, cloudEventData: {}, regelErrorInformation: {}",
               handlaggningId, cloudEventData, regelErrorInformation);
         return;
      }
      try
      {
         var regelResponse = regelMapper.toRegelResponse(handlaggningId, cloudEventData, regelErrorInformation);
         regelKafkaProducer.sendRegelResponse(regelResponse, Objects.requireNonNull(replyTo));
      }
      catch (IllegalStateException e)
      {
         LOGGER.error("Failed to send error response for handlaggning. handlaggningId: {}, regelErrorInformation: {}",
               handlaggningId, regelErrorInformation, e);
      }
   }

   private void sendRegelSuccessResponse(UUID handlaggningId,
         CloudEventData cloudEventData,
         Utfall utfall, String replyTopic)
   {
      try
      {
         var regelResponse = ImmutableRegelResponse.builder()
               .id(cloudEventData.id())
               .handlaggningId(handlaggningId)
               .kogitoparentprociid(cloudEventData.kogitoparentprociid())
               .kogitorootprociid(cloudEventData.kogitorootprociid())
               .kogitoprocid(cloudEventData.kogitoprocid())
               .kogitorootprocid(cloudEventData.kogitorootprocid())
               .kogitoprocinstanceid(cloudEventData.kogitoprocinstanceid())
               .kogitoprocist(cloudEventData.kogitoprocist())
               .kogitoprocversion(cloudEventData.kogitoprocversion())
               .utfall(utfall)
               .type(cloudEventData.type())
               .source(cloudEventData.source())
               .build();
         regelKafkaProducer.sendRegelResponse(regelResponse, Objects.requireNonNull(replyTopic));
      }
      catch (IllegalStateException e)
      {
         LOGGER.error("Failed to send regel response for handlaggning. handlaggningId: {}, utfall: {}",
               handlaggningId, utfall, e);
      }
   }

   private RegelErrorInformation buildRegelErrorInformation(String felkod, String meddelande)
   {
      var info = new RegelErrorInformation();
      info.setFelkod(felkod);
      info.setFelmeddelande(meddelande);
      return info;
   }

   private static Response.Status toHttpStatus(HandlaggningException e)
   {
      return switch (e.getErrorType())
      {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

   private static Response.Status toHttpStatus(OulException e)
   {
      return switch (e.getErrorType())
      {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}
