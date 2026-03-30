package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockTestResource.class)
})
public class RegelManuellHandlaggningTest extends AbstractRegelManuellTest
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
   void should_create_initial_handlaggning_request(String handlaggningId)
   {
      sendRegelRequest(handlaggningId);
      verifyGetHandlaggningProduced(handlaggningId);
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234 , 383cc515-4c55-479b-a96b-244734ef1336 , 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgiftstatus_planerad(String handlaggningId, String utforarId, String uppgiftId)
         throws Exception
   {
      sendRegelRequest(handlaggningId);
      simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      verifyPutHandlaggningContentUppgiftStatus(handlaggningId, utforarId, UppgiftStatus.PLANERAD);
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234 , 383cc515-4c55-479b-a96b-244734ef1336 , 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgiftstatus_avslutad(String handlaggningId, String utforarId, String uppgiftId)
         throws Exception
   {
      sendRegelRequest(handlaggningId);
      simulateOulResponse(handlaggningId, uppgiftId);
      simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      mockRegelService(Utfall.JA, handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      verifyPutHandlaggningContentUppgiftStatus(handlaggningId, utforarId, UppgiftStatus.AVSLUTAD);
   }

}
