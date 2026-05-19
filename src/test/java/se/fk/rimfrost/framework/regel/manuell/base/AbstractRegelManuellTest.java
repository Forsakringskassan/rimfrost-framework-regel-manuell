package se.fk.rimfrost.framework.regel.manuell.base;

import jakarta.inject.Inject;
import java.util.concurrent.TimeUnit;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.oul.presentation.kafka.OulKafkaMapper;
import se.fk.rimfrost.framework.regel.RegelTestBase;
import se.fk.rimfrost.framework.regel.manuell.helpers.OulKafkaConnector;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;

/**
 * Base class for manual Regel tests
 *
 * <p>This test base provides:
 * <ul>
 *   <li>Preconfigured Kafka in-memory test support via {@link OulKafkaConnector}</li>
 *   <li>Access to OUL Kafka channel constants used in manual test scenarios</li>
 *   <li>Integration with WireMock for external HTTP service simulation</li>
 *   <li>Common setup and cleanup logic for test isolation</li>
 * </ul>
 *
 * <p><b>Test isolation:</b>
 * Each test resets both:
 * <ul>
 *   <li>WireMock request history</li>
 *   <li>In-memory Kafka state</li>
 * </ul>
 *
 * <p>This ensures that tests are independent and do not leak state across executions.
 */
@SuppressWarnings(
{
      "SameParameterValue", "unused"
})
@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellTest extends RegelTestBase
{

   private static final String oulStatusNotificationChannel = "operativt-uppgiftslager-status-notification";

   // Add protected getters so subclasses can access them

   protected static String getOulStatusNotificationChannel()
   {
      return oulStatusNotificationChannel;
   }

   /**
    * Kafka test connector for OUL message simulation and verification.
    */
   protected OulKafkaConnector oulKafkaConnector;

   @ConfigProperty(name = "regel.manuell.base-path")
   String basePath;

   protected String basePath()
   {
      return basePath;
   }

   /**
    * Mapper used to convert between domain and API ID types for OUL messages.
    */
   @Inject
   OulKafkaMapper oulKafkaMapper;

   /**
    * Adapter for handling case/handling operations in test scenarios.
    */
   @SuppressWarnings("unused")
   @Inject
   HandlaggningAdapter handlaggningAdapter;

   /**
    * Clears all framework-owned storage tables before each test to ensure isolation.
    * Exported in the test JAR so service tests extending this class get cleanup automatically.
    */
   @Inject
   StorageTestCleaner storageTestCleaner;

   /**
    * Resets external system state before each test execution.
    *
    * <p>This includes:
    * <ul>
    *   <li>Resetting all WireMock request history</li>
    *   <li>Initializing the OUL Kafka connector if needed</li>
    *   <li>Clearing all in-memory Kafka messages to ensure test isolation</li>
    * </ul>
    *
    * <p><b>Important:</b> The {@link InMemoryConnector} may retain state across tests,
    * even when recreated, so explicit clearing is required.
    */
   @SuppressWarnings("JavadocReference")
   @BeforeEach
   void regelManuellResetState()
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
      if (oulKafkaConnector == null)
      {
         oulKafkaConnector = new OulKafkaConnector(inMemoryConnector, oulKafkaMapper);
      }
      //
      // Have to clear even when connectors are new, since the inMemoryConnector is not necessarily empty
      //
      oulKafkaConnector.clear();
      storageTestCleaner.clearAll();
   }

   //
   // Rest assured helpers
   //

   protected void waitForRegelManuellReady(String handlaggningId)
   {
      await().atMost(5, TimeUnit.SECONDS)
            .until(() -> given().when().get(basePath() + "/{handlaggningId}", handlaggningId)
                  .getStatusCode() == 200);
   }

   protected void sendPostRegelManuellHandlaggningDone(String handlaggningId)
   {
      given()
            .when()
            .post(this.basePath() + "/" + handlaggningId + "/done")
            .then()
            .statusCode(204);
   }

   protected GetUtokadUppgiftsbeskrivningResponse sendGetUtokadUppgiftsbeskrivning()
   {
      return given().when().get(basePath() + "/utokadUppgiftsbeskrivning").then().statusCode(200).extract()
            .as(GetUtokadUppgiftsbeskrivningResponse.class);
   }

}
