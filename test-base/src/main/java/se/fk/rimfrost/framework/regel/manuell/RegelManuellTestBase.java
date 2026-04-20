package se.fk.rimfrost.framework.regel.manuell;

import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaMapper;
import se.fk.rimfrost.framework.regel.test.AbstractRegelTest;

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

   protected OulKafkaConnector oulKafkaConnector;

   @Inject
   OulKafkaMapper oulKafkaMapper;

   @SuppressWarnings("unused")
   @Inject
   HandlaggningAdapter handlaggningAdapter;

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
