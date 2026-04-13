package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.UppgiftStatus;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellOulTest extends RegelManuellTest
{

   @BeforeEach
   void setup()
   {
      resetState();
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_create_correct_oul_request(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var oulRequest = oulKafkaConnector.waitForOulRequestMessage();
      Assertions.assertEquals(handlaggningId, oulRequest.getHandlaggningId());
      Assertions.assertEquals("TestUppgiftBeskrivning", oulRequest.getBeskrivning());
      Assertions.assertEquals("TestUppgiftNamn", oulRequest.getRegel());
      Assertions.assertEquals("C", oulRequest.getVerksamhetslogik());
      Assertions.assertEquals("ANSVARIG_HANDLAGGARE", oulRequest.getRoll());
      Assertions.assertTrue(oulRequest.getUrl().contains("/regel/manuell"));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "JA, 5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_send_oul_status_uppgift_avslutad(Utfall expectedUtfall, String handlaggningId, String uppgiftId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      mockRegelService(expectedUtfall, handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var oulStatusMessage = oulKafkaConnector.waitForOulStatusMessage();
      Assertions.assertEquals(uppgiftId, oulStatusMessage.getUppgiftId());
      Assertions.assertEquals(UppgiftStatus.AVSLUTAD, oulStatusMessage.getStatus());
   }

}
