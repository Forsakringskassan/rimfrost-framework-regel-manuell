package se.fk.rimfrost.framework.regel.manuell.helpers;

import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.oul.logic.dto.Idtyp;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaMapper;
import se.fk.rimfrost.framework.regel.KafkaConnector;

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

   private final OulKafkaMapper oulKafkaMapper;

   public OulKafkaConnector(InMemoryConnector inMemoryConnector,
         OulKafkaMapper oulKafkaMapper)
   {
      super(inMemoryConnector);
      this.oulKafkaMapper = oulKafkaMapper;
   }

   public static final String oulRequestsChannel = "operativt-uppgiftslager-requests";
   public static final String oulResponsesChannel = "operativt-uppgiftslager-responses";
   public static final String oulStatusNotificationChannel = "operativt-uppgiftslager-status-notification";
   public static final String oulStatusControlChannel = "operativt-uppgiftslager-status-control";

   /**
    * Clears all messages from relevant OUL Kafka channels.
    *
    * <p>Ensures a clean state before test execution by clearing both request
    * and status control sinks in the in-memory connector.
    */
   public void clear()
   {
      inMemoryConnector.sink(oulRequestsChannel).clear();
      inMemoryConnector.sink(oulStatusControlChannel).clear();
   }

   /**
    * Waits for and returns the first OUL request message
    *
    * @return the first {@link OperativtUppgiftslagerRequestMessage} received
    */
   public OperativtUppgiftslagerRequestMessage waitForOulRequestMessage()
   {
      return (OperativtUppgiftslagerRequestMessage) waitForMessages(oulRequestsChannel)
            .getFirst().getPayload();
   }

   /**
    * Waits for and returns the first OUL status control message
    *
    * @return the first {@link OperativtUppgiftslagerStatusMessage} received
    */
   public OperativtUppgiftslagerStatusMessage waitForOulStatusMessage()
   {
      return (OperativtUppgiftslagerStatusMessage) waitForMessages(oulStatusControlChannel)
            .getFirst().getPayload();
   }

   /**
    * Simulates an OUL response message by publishing it to the in-memory response channel.
    *
    * @param handlaggningId identifier for the handlaggning
    * @param uppgiftId      identifier for the task
    */
   public void simulateOulResponse(String handlaggningId, String uppgiftId)
   {
      var msg = new OperativtUppgiftslagerResponseMessage();
      msg.setHandlaggningId(handlaggningId);
      msg.setUppgiftId(uppgiftId);
      inMemoryConnector.source(oulResponsesChannel).send(msg);
   }

   /**
    * Simulates an OUL status notification message.
    * *
    *
    * @param handlaggningId identifier for the handlaggning
    * @param uppgiftId      identifier for the task
    * @param utforarId      utforare identifier
    * @param status         current
    */
   public void simulateOulStatus(String handlaggningId, String uppgiftId, Idtyp utforarId, Status status)
   {
      var msg = new OperativtUppgiftslagerStatusMessage();
      msg.setStatus(status);
      msg.setUppgiftId(uppgiftId);
      msg.setHandlaggningId(handlaggningId);
      msg.setUtforarId(oulKafkaMapper.toApiIdtyp(utforarId));
      inMemoryConnector.source(oulStatusNotificationChannel).send(msg);
   }

}
