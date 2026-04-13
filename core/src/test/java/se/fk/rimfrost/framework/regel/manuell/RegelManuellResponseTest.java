package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.UppgiftStatus;
import static se.fk.rimfrost.framework.regel.manuell.RegelManuellTestData.newHandlaggningIdtyp;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellResponseTest extends RegelManuellTest
{

   @BeforeEach
   void setup()
   {
      resetState();
   }

   @ParameterizedTest
   @CsvSource(
   {
         "JA, 5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_return_correct_regel_response_utfall(Utfall expectedUtfall, String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(), UppgiftStatus.PLANERAD);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      Assertions.assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_return_correct_regel_response_handlaggning_id(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(), UppgiftStatus.PLANERAD);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      Assertions.assertEquals(handlaggningId, regelResponse.getData().getHandlaggningId());
   }

}
