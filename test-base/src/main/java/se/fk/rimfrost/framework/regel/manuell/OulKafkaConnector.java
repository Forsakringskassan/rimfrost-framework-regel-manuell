package se.fk.rimfrost.framework.regel.manuell;

import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.framework.oul.logic.dto.Idtyp;
import se.fk.rimfrost.framework.regel.test.KafkaConnector;
import se.fk.rimfrost.framework.oul.integration.kafka.OulKafkaMapper;

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

   public void clear()
   {
      inMemoryConnector.sink(oulRequestsChannel).clear();
      inMemoryConnector.sink(oulStatusControlChannel).clear();
   }

   public OperativtUppgiftslagerRequestMessage waitForOulRequestMessage()
   {
      return (OperativtUppgiftslagerRequestMessage) waitForMessages(oulRequestsChannel)
            .getFirst().getPayload();
   }

   public OperativtUppgiftslagerStatusMessage waitForOulStatusMessage()
   {
      return (OperativtUppgiftslagerStatusMessage) waitForMessages(oulStatusControlChannel)
            .getFirst().getPayload();
   }

   public void simulateOulResponse(String handlaggningId, String uppgiftId)
   {
      var msg = new OperativtUppgiftslagerResponseMessage();
      msg.setHandlaggningId(handlaggningId);
      msg.setUppgiftId(uppgiftId);
      inMemoryConnector.source(oulResponsesChannel).send(msg);
   }

   public void simulateOulStatus(String handlaggningId, String uppgiftId, Idtyp utforarId, String status)
   {
      var msg = new OperativtUppgiftslagerStatusMessage();
      msg.setStatus(status);
      msg.setUppgiftId(uppgiftId);
      msg.setHandlaggningId(handlaggningId);
      msg.setUtforarId(oulKafkaMapper.toApiIdtyp(utforarId));
      inMemoryConnector.source(oulStatusNotificationChannel).send(msg);
   }

}
