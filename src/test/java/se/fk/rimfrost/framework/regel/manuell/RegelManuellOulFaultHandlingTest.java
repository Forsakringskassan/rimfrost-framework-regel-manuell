package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.oul.logic.exception.OulServiceException;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellOulFaultHandlingTest extends AbstractRegelManuellTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, ERROR"
   })
   @DisplayName("FRMM-FR-06.1: Felrespons skickas och handläggningsärende uppdateras inte när OUL-skapande misslyckas")
   void should_send_error_response_on_oul_create_uppgift_failure(String handlaggningId, Utfall expectedUtfall)
         throws Exception
   {
      Mockito.doThrow(new OulServiceException(OulServiceException.ErrorType.SERVICE_UNAVAILABLE, "OUL is down"))
            .when(oulUppgiftService).createOulUppgift(any());

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);

      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-06.4: HTTP 500 returneras vid oväntat fel i OUL-tjänsten under avslutning av uppgift")
   void should_return_500_on_oul_end_uppgift_failure_during_done(String handlaggningId) throws Exception
   {
      Mockito.doThrow(new OulServiceException(OulServiceException.ErrorType.UNEXPECTED_ERROR, "OUL is broken"))
            .when(oulUppgiftService).endOulUppgift(any(), any());

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);

      given()
            .when()
            .post(basePath() + "/" + handlaggningId + "/done")
            .then()
            .statusCode(500);
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-06.7: HTTP 404 fel från OUL-tjänsten hanteras som lyckat anrop under avslutning av uppgift")
   void should_return_204_on_oul_end_uppgift_not_found_failure_during_done(String handlaggningId) throws Exception
   {
      Mockito.doThrow(new OulServiceException(OulServiceException.ErrorType.NOT_FOUND, "OUL id not found"))
            .when(oulUppgiftService).endOulUppgift(any(), any());

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);

      given()
            .when()
            .post(basePath() + "/" + handlaggningId + "/done")
            .then()
            .statusCode(204);

      var response = regelKafkaConnector.waitForRegelResponse();
      assertEquals(Utfall.JA, response.getData().getUtfall());
   }
}
