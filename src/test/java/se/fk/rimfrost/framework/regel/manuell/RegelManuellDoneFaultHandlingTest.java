package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.Utfall;
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
public class RegelManuellDoneFaultHandlingTest extends AbstractRegelManuellTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 404, 404",
         "5367f6b8-cc4a-11f0-8de9-199901011234, 400, 400",
         "5367f6b8-cc4a-11f0-8de9-199901011234, 500, 500"
   })
   @DisplayName("FRMM-FR-06.4: HTTP-statuskod från handläggningstjänsten (404/400/500) returneras korrekt vid fel under done")
   void done_should_return_mapped_status_when_read_handlaggning_fails(
         String handlaggningId, int handlaggningHttpStatus, int expectedDoneStatus) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);

      var server = WireMockRegelManuell.getWireMockServer();
      StubMapping failureStub = server.stubFor(
            WireMock.get(WireMock.urlPathMatching("/handlaggning/.*" + handlaggningId + ".*"))
                  .atPriority(1)
                  .willReturn(WireMock.aResponse().withStatus(handlaggningHttpStatus)));
      try
      {
         given()
               .when()
               .post(basePath() + "/" + handlaggningId + "/done")
               .then()
               .statusCode(expectedDoneStatus);

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
   @DisplayName("FRMM-FR-05.5: RegelResponse skickas trots att sista handläggningsuppdateringen misslyckas")
   void done_should_return_500_and_still_send_regel_response_when_final_update_handlaggning_fails(
         String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);

      var server = WireMockRegelManuell.getWireMockServer();
      StubMapping failureStub = server.stubFor(
            WireMock.put(WireMock.urlPathMatching("/handlaggning/.*" + handlaggningId + ".*"))
                  .atPriority(1)
                  .willReturn(WireMock.serverError()));
      try
      {
         given()
               .when()
               .post(basePath() + "/" + handlaggningId + "/done")
               .then()
               .statusCode(500);

         var regelResponse = regelKafkaConnector.waitForRegelResponse();
         assertEquals(handlaggningId, regelResponse.getData().getHandlaggningId());
         assertEquals(Utfall.JA, regelResponse.getData().getUtfall());
      }
      finally
      {
         server.removeStub(failureStub);
      }
   }
}
