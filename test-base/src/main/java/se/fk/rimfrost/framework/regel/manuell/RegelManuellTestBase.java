package se.fk.rimfrost.framework.regel.manuell;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaMapper;
import se.fk.rimfrost.framework.regel.test.AbstractRegelTest;

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
@SuppressWarnings("SameParameterValue")
public abstract class RegelManuellTestBase extends AbstractRegelTest
{

   private static final String oulRequestsChannel = "operativt-uppgiftslager-requests";
   private static final String oulResponsesChannel = "operativt-uppgiftslager-responses";
   private static final String oulStatusNotificationChannel = "operativt-uppgiftslager-status-notification";
   private static final String oulStatusControlChannel = "operativt-uppgiftslager-status-control";

   // Add protected getters so subclasses can access them

   protected static String getOulRequestsChannel()
   {
      return oulRequestsChannel;
   }

   protected static String getOulResponsesChannel()
   {
      return oulResponsesChannel;
   }

   protected static String getOulStatusNotificationChannel()
   {
      return oulStatusNotificationChannel;
   }

   protected static String getOulStatusControlChannel()
   {
      return oulStatusControlChannel;
   }

   /** Kafka test connector for OUL message simulation and verification. */
   protected OulKafkaConnector oulKafkaConnector;

   /** Mapper used to convert between domain and API ID types for OUL messages. */
   @Inject
   OulKafkaMapper oulKafkaMapper;

   /** Adapter for handling case/handling operations in test scenarios. */
   @SuppressWarnings("unused")
   @Inject
   HandlaggningAdapter handlaggningAdapter;

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
   @BeforeEach
   void resetState()
   {
      WireMockRegelManuell.getWireMockServer().resetRequests();
      if (oulKafkaConnector == null)
      {
         oulKafkaConnector = new OulKafkaConnector(inMemoryConnector, oulKafkaMapper);
      }
      //
      // Have to clear even when connectors are new, since the inMemoryCpnnector is not necessarily empty
      //
      oulKafkaConnector.clear();
   }

}
