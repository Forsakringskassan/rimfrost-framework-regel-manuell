package se.fk.rimfrost.framework.regel.manuell.helpers;

import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.framework.oul.logic.dto.Idtyp;
import se.fk.rimfrost.framework.regel.KafkaConnector;
import se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestStatus;

/**
 * Test utility connector for interacting with Operativt Uppgiftslager (OUL)
 * Kafka integration using an {@link InMemoryConnector}.
 *
 * <p>This class is used in tests to:
 * <ul>
 *   <li>Inspect outgoing Kafka messages from the system under test</li>
 *   <li>Simulate incoming OUL responses and status updates</li>
 *   <li>Reset in-memory Kafka state between test runs</li>
 * </ul>
 *
 * <p>It wraps Kafka channels used by the OUL integration and provides
 * convenience methods for sending and receiving kafka messages.
 *
 * <p><b>Kafka channels:</b>
 * <ul>
 *   <li>{@code operativt-uppgiftslager-requests} - outgoing request messages</li>
 *   <li>{@code operativt-uppgiftslager-responses} - simulated responses</li>
 *   <li>{@code operativt-uppgiftslager-status-notification} - simulated status events</li>
 *   <li>{@code operativt-uppgiftslager-status-control} - outgoing status control messages</li>
 * </ul>
 */
public class OulKafkaConnector extends KafkaConnector
{

   public OulKafkaConnector(InMemoryConnector inMemoryConnector)
   {
      super(inMemoryConnector);
   }

   public static final String oulStatusNotificationChannel = "operativt-uppgiftslager-status-notification";

   /**
    * Simulates an OUL status notification message with default test CloudEvent attributes.
    *
    * @param handlaggningId identifier for the handlaggning
    * @param uppgiftId      identifier for the task
    * @param utforarId      utforare identifier
    * @param status         current status
    */
   public void simulateOulStatus(String handlaggningId, String uppgiftId, Idtyp utforarId, OffsetDateTime planeradTill,
         RegelManuellTestStatus status)
   {
      simulateOulStatus(handlaggningId, uppgiftId, utforarId, planeradTill, status, testCloudeventAttributes());
   }

   /**
    * Simulates an OUL status notification message with explicit CloudEvent attributes.
    *
    * <p>Use this overload when the test needs to verify that specific CloudEvent attributes
    * are correctly propagated through the handler, for example by passing the attributes
    * from the outgoing OUL request that triggered this status.
    *
    * @param handlaggningId      identifier for the handlaggning
    * @param uppgiftId           identifier for the task
    * @param utforarId           utforare identifier
    * @param status              current status
    * @param cloudeventAttributes CloudEvent correlation attributes to embed in the message
    */
   public void simulateOulStatus(String handlaggningId, String uppgiftId, Idtyp utforarId, OffsetDateTime planeradTill,
         RegelManuellTestStatus status, Map<String, String> cloudeventAttributes)
   {
      var msg = new OperativtUppgiftslagerStatusMessage();
      msg.setStatus(status.name());
      msg.setUppgiftId(uppgiftId);
      msg.setHandlaggningId(handlaggningId);
      msg.setUtforarId(toMessageIdtyp(utforarId));
      msg.setPlaneradTill(planeradTill);
      msg.setCloudeventAttributes(cloudeventAttributes);
      inMemoryConnector.source(oulStatusNotificationChannel).send(msg);
   }

   private static se.fk.rimfrost.Idtyp toMessageIdtyp(Idtyp idtyp)
   {
      if (idtyp == null)
      {
         return null;
      }
      var result = new se.fk.rimfrost.Idtyp();
      result.setTypId(idtyp.typId());
      result.setVarde(idtyp.varde());
      return result;
   }

   private static Map<String, String> testCloudeventAttributes()
   {
      return Map.of(
            "id", UUID.randomUUID().toString(),
            "kogitorootprociid", "00000000-0000-0000-0000-000000000001",
            "kogitoparentprociid", "00000000-0000-0000-0000-000000000002",
            "kogitoprocinstanceid", "00000000-0000-0000-0000-000000000003",
            "kogitorootprocid", "test-root-proc",
            "kogitoprocid", "test-proc",
            "kogitoprocist", "test-proc-ist",
            "kogitoprocversion", "1.0",
            "type", "test-type",
            "source", "test-source");
   }

}
