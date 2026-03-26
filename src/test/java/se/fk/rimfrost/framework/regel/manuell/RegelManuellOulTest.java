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
public class RegelManuellOulTest extends AbstractRegelManuellTest
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
      sendRegelRequest(handlaggningId);
      verifyOulRequestContent(handlaggningId);
   }

   @ParameterizedTest
   @CsvSource(
   {
         "JA, 5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_send_oul_status_uppgift_avslutad(Utfall expectedUtfall, String handlaggningId, String uppgiftId)
   {
      sendRegelRequest(handlaggningId);
      simulateOulResponse(handlaggningId, uppgiftId);
      mockRegelService(expectedUtfall, handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      verifyOulStatusMessageContent(uppgiftId, Status.AVSLUTAD);
   }

}
