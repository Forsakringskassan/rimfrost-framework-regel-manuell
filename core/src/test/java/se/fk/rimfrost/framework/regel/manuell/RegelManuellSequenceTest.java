package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Status;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockTestResource.class)
})
public class RegelManuellSequenceTest extends AbstractRegelManuellTest
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 383cc515-4c55-479b-a96b-244734ef1336, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void TestRegelManuellSequenceTest(String handlaggningId, String utforarId, String uppgiftId)
         throws Exception
   {
      System.out.printf("Starting TestRegelManuell. %S%n", handlaggningId);
      resetState();

      //
      // Workflow start
      //
      sendRegelRequest(handlaggningId);
      verifyGetHandlaggningProduced(handlaggningId);
      verifyOulRequestProduced();

      // Clear previous requests
      wiremockServer.resetRequests();

      //
      // Send mocked OUL response
      //
      simulateOulResponse(handlaggningId, uppgiftId);

      //
      // Status update from OUL:
      // verify that rule updates uppgift status in new PUT handlaggning
      //
      simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      verifyPutHandlaggningProduced(handlaggningId);

      //
      // Verify utökad uppgiftsbeskrivning
      //
      verifyUtokadUppgiftsbeskrivningProduced();

      //
      // Verify handläggning DONE
      //
      wiremockServer.resetRequests();
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      verifyPutHandlaggningProduced(handlaggningId);
      verifyOulStatusMessageProduced();
      verifyRegelResponseProduced();
   }

}
