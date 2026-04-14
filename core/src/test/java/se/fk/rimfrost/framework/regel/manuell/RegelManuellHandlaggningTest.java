package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.regel.logic.UppgiftStatus;
import static se.fk.rimfrost.framework.regel.manuell.RegelManuellTestData.newHandlaggningApiIdtyp;
import static se.fk.rimfrost.framework.regel.manuell.RegelManuellTestData.newHandlaggningIdtyp;
import static se.fk.rimfrost.framework.regel.test.WireMockHandlaggning.getUppgiftFromLastPutHandlaggning;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellHandlaggningTest extends RegelManuellTest
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_create_initial_handlaggning_request(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var handlaggningRequests = WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
      Assertions.assertEquals(1, handlaggningRequests.size());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgiftstatus_planerad(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(), Status.NY);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages are processed
      var uppgift = getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(UppgiftStatus.PLANERAD, uppgift.getUppgiftStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234 , 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgiftstatus_avslutad(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(), Status.TILLDELAD);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages are processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var uppgift = getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(UppgiftStatus.AVSLUTAD, uppgift.getUppgiftStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234 , 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgift_correct_utforar_id(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(), Status.NY);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages are processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var uppgift = getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(newHandlaggningApiIdtyp(), uppgift.getUtforarId());
   }
}
