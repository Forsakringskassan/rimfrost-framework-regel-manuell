package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.regel.Utfall;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockTestResource.class)
})
public class RegelManuellResponseTest extends AbstractRegelManuellTest
{

   @BeforeEach
   void setup()
   {
      resetState();
   }

   @ParameterizedTest
   @CsvSource(
   {
         "JA, 5367f6b8-cc4a-11f0-8de9-199901011234, 383cc515-4c55-479b-a96b-244734ef1336, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_return_correct_regel_response(Utfall expectedUtfall, String handlaggningId, String utforarId, String uppgiftId)
   {
      sendRegelRequest(handlaggningId);
      simulateOulResponse(handlaggningId, uppgiftId);
      simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      mockRegelService(expectedUtfall, handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      verifyRegelResponseContent(handlaggningId, expectedUtfall);
   }

}
