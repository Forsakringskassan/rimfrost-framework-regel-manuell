package se.fk.rimfrost.framework.regel.manuell.base;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.mockito.Mockito;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.oul.model.ImmutableProcessInfo;
import se.fk.rimfrost.framework.regel.RegelTestBase;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.ImmutableCloudEventData;
import se.fk.rimfrost.framework.regel.oul.logic.entity.ImmutableOulCorrelationData;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;

/**
 * Base class for manual Regel tests.
 *
 * <p>This test base provides:
 * <ul>
 *   <li>Preconfigured Kafka in-memory test support</li>
 *   <li>Integration with WireMock for external HTTP service simulation</li>
 *   <li>A single {@link InjectMock} for {@link OulUppgiftService} with default stubs — the
 *       correct public API boundary between this framework and {@code rimfrost-framework-regel-oul}</li>
 *   <li>Common setup and cleanup logic for test isolation</li>
 * </ul>
 *
 * <p><b>Mock boundary:</b>
 * {@link OulUppgiftService} is mocked at the {@link InjectMock} level. This is the public
 * contract this framework depends on. The storage and persistence internals of
 * {@code rimfrost-framework-regel-oul} are tested in that module against a real PostgreSQL
 * devservices container — there is no value in re-testing them here.
 *
 * <p><b>Default stubs (configured before each test):</b>
 * <ul>
 *   <li>{@code createOulUppgift(any())} — returns a pre-built {@code OperativUppgift}</li>
 *   <li>{@code getCorrelationData(any())} — returns a pre-built {@code OulCorrelationData}
 *       with the configured response topic as {@code replyTopic}</li>
 *   <li>{@code endOulUppgift} and {@code cleanupCorrelation} — Mockito void defaults (do nothing)</li>
 * </ul>
 *
 * <p>Tests that need to simulate OUL failures override these defaults with
 * {@code thenThrow(OulException)} for the relevant method.
 *
 * <p><b>Synchronisation:</b>
 * After {@code sendRegelRequest}, the Kafka consumer thread processes the message asynchronously.
 * Tests that call {@code /done} must wait for the consumer thread to finish by calling
 * {@link #waitForRegelRequestProcessed(String)}. The synchronisation signal is the WireMock
 * handläggning {@code GET} request, which is made synchronously in {@code handleRegelRequest}
 * before {@code createOulUppgift} is called. Because {@code getCorrelationData} is stubbed to
 * return a fixed value for any UUID, {@code handleUppgiftDone} succeeds regardless of whether
 * {@code createOulUppgift} has been called — the only requirement is that the Kafka message
 * has been consumed.
 *
 * <p><b>Test isolation:</b>
 * Each test resets:
 * <ul>
 *   <li>WireMock request history</li>
 *   <li>In-memory Kafka state</li>
 *   <li>All {@link OulUppgiftService} stubs</li>
 * </ul>
 */
@SuppressWarnings(
{
      "SameParameterValue", "unused"
})
@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellTest extends RegelTestBase
{

   @ConfigProperty(name = "regel.manuell.base-path")
   String basePath;

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   protected String basePath()
   {
      return basePath;
   }

   /** Adapter for handling case/handling operations in test scenarios. */
   @SuppressWarnings("unused")
   @Inject
   HandlaggningAdapter handlaggningAdapter;

   /** Mocked OUL uppgift service — the public API boundary with rimfrost-framework-regel-oul. */
   @InjectMock
   protected OulUppgiftService oulUppgiftService;

   /**
    * Resets external system state and reconfigures default stubs before each test.
    *
    * <p>This includes:
    * <ul>
    *   <li>Resetting all WireMock request history</li>
    *   <li>Initializing the OUL Kafka connector if needed</li>
    *   <li>Clearing all in-memory Kafka messages to ensure test isolation</li>
    *   <li>Configuring default {@link OulUppgiftService} stubs</li>
    * </ul>
    *
    * <p><b>Important:</b> The {@link InMemoryConnector} may retain state across tests,
    * even when recreated, so explicit clearing is required.
    */
   @SuppressWarnings("JavadocReference")
   @BeforeEach
   void regelManuellResetState() throws Exception
   {
      super.regelResetState();
      if (inMemoryConnector == null)
      {
         throw new IllegalStateException("inMemoryConnector not injected");
      }
      var server = WireMockRegelManuell.getWireMockServer();
      if (server == null)
      {
         throw new IllegalStateException("WireMock not initialized");
      }
      server.resetRequests();
      configureOulUppgiftServiceMocks();
   }

   /**
    * Configures default stubs on {@link OulUppgiftService} before each test.
    *
    * <p>Quarkus resets all {@link InjectMock} mocks before each test, so stubbing must be
    * re-established here. Subclasses that need to simulate OUL failures override individual
    * stubs with {@code thenThrow(OulException)} after calling {@code super.regelManuellResetState()}.
    */
   private void configureOulUppgiftServiceMocks() throws Exception
   {
      Mockito.when(oulUppgiftService.createOulUppgift(any()))
            .thenReturn(defaultOperativUppgift());

      Mockito.when(oulUppgiftService.getCorrelationData(any()))
            .thenReturn(defaultOulCorrelationData());
   }

   private se.fk.rimfrost.framework.oul.model.OperativUppgift defaultOperativUppgift()
   {
      var processInfo = ImmutableProcessInfo.builder()
            .replyTopic(responseTopic)
            .cloudeventAttributes(Map.of())
            .build();
      return ImmutableOperativUppgift.builder()
            .uppgiftId(UUID.fromString(WireMockRegelManuell.DEFAULT_UPPGIFT_ID))
            .handlaggningId(UUID.randomUUID())
            .status(RegelManuellTestStatus.PLANERAD.name())
            .processInfo(processInfo)
            .build();
   }

   private se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData defaultOulCorrelationData()
   {
      var cloudEventData = ImmutableCloudEventData.builder()
            .id(UUID.randomUUID())
            .kogitorootprociid(UUID.randomUUID())
            .kogitoparentprociid(UUID.randomUUID())
            .kogitoprocinstanceid(UUID.randomUUID())
            .kogitorootprocid("test-root-proc")
            .kogitoprocid("test-proc")
            .kogitoprocist("test-proc-ist")
            .kogitoprocversion("1.0")
            .type(responseTopic)
            .source("test-source")
            .build();
      var uppgiftSpecifikation = ImmutableUppgiftSpecifikation.builder()
            .id(UUID.fromString(WireMockRegelManuell.DEFAULT_UPPGIFT_ID))
            .version(1)
            .build();
      var uppgift = ImmutableUppgift.builder()
            .id(UUID.fromString(WireMockRegelManuell.DEFAULT_UPPGIFT_ID))
            .version(1)
            .skapadTs(OffsetDateTime.now())
            .aktivitetId(UUID.randomUUID())
            .fSSAinformation("")
            .uppgiftSpecifikation(uppgiftSpecifikation)
            .uppgiftStatus(RegelManuellTestStatus.PLANERAD.name())
            .build();
      return ImmutableOulCorrelationData.builder()
            .oulUppgiftId(UUID.fromString(WireMockRegelManuell.DEFAULT_UPPGIFT_ID))
            .uppgift(uppgift)
            .replyTopic(responseTopic)
            .cloudEventData(cloudEventData)
            .build();
   }

   /**
    * Synchronisation barrier between the test thread and the Kafka consumer thread.
    *
    * <p>After {@code sendRegelRequest}, the consumer thread runs
    * {@link se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellRequestHandler#handleRegelRequest}
    * asynchronously. The WireMock handläggning {@code GET} request is the earliest reliable
    * signal that the consumer thread has processed the message — it is made synchronously inside
    * {@code handleRegelRequest} before {@code createOulUppgift} is called. Tests that call
    * {@code /done} must wait for this signal before proceeding.
    *
    * @param handlaggningId the handlaggning UUID as a string
    */
   protected void waitForRegelRequestProcessed(String handlaggningId)
   {
      WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
   }

   //
   // Rest assured helpers
   //

   /**
    * Polls until the manuell regel REST endpoint returns HTTP 200 for the given handläggning.
    *
    * @param handlaggningId the handlaggning UUID as a string
    */
   protected void waitForRegelManuellReady(String handlaggningId)
   {
      await().until(() -> given().when().get(basePath() + "/{handlaggningId}", handlaggningId)
            .getStatusCode() == 200);
   }

   /**
    * Sends a {@code POST /done} for the given handläggning and asserts HTTP 204.
    *
    * @param handlaggningId the handlaggning UUID as a string
    */
   protected void sendPostRegelManuellHandlaggningDone(String handlaggningId)
   {
      given()
            .when()
            .post(this.basePath() + "/" + handlaggningId + "/done")
            .then()
            .statusCode(204);
   }

   /**
    * Sends a {@code GET /utokadUppgiftsbeskrivning} and returns the deserialized response.
    *
    * @return the utökad uppgiftsbeskrivning response
    */
   protected GetUtokadUppgiftsbeskrivningResponse sendGetUtokadUppgiftsbeskrivning()
   {
      return given().when().get(basePath() + "/utokadUppgiftsbeskrivning").then().statusCode(200).extract()
            .as(GetUtokadUppgiftsbeskrivningResponse.class);
   }

}
