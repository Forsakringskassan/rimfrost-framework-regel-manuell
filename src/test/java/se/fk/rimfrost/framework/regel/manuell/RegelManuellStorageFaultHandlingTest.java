package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellStorageFaultHandlingTest extends AbstractRegelManuellTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-06.1: OUL-uppgiften avslutas inte när läsning av handläggningsärende misslyckas vid start")
   void should_not_try_to_end_operativ_uppgift_when_get_handlaggning_fails(String handlaggningId) throws Exception
   {
      var server = WireMockRegelManuell.getWireMockServer();
      var failureStub = server.stubFor(
            WireMock.get(WireMock.urlPathMatching("/handlaggning/.*" + handlaggningId + ".*"))
                  .atPriority(1)
                  .willReturn(WireMock.serverError()));
      try
      {
         regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
         regelKafkaConnector.waitForRegelResponse();

         Mockito.verify(oulUppgiftService, Mockito.never()).endOulUppgift(any(), any());
      }
      finally
      {
         server.removeStub(failureStub);
      }
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-06.5: HTTP 500 returneras när läsning av korrelationsdata misslyckas under done")
   void should_return_500_status_on_correlation_data_read_failure_during_done_request(String handlaggningId)
   {
      Mockito.when(oulUppgiftService.getCorrelationData(any())).thenReturn(null);

      var response = given()
            .when()
            .post(basePath() + "/" + handlaggningId + "/done")
            .then()
            .statusCode(500)
            .extract()
            .body()
            .as(RestErrorResponse.class);
      assertEquals(500, response.code());
   }

   record RestErrorResponse(int code, String message)
   {
   }
}
