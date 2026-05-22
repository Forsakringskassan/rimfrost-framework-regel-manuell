package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestStatus;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.UUID;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellDoneFaultHandlingTest extends AbstractRegelManuellTest
{

   @InjectMock
   OulAdapter oulAdapter;

   private void stubOulAdapter(UUID handlaggningId) throws Exception
   {
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenReturn(
            ImmutableOperativUppgift.builder()
                  .uppgiftId(UUID.randomUUID())
                  .handlaggningId(handlaggningId)
                  .status(RegelManuellTestStatus.PLANERAD.name())
                  .build());
      Mockito.when(oulAdapter.endOperativUppgift(any(), any())).thenReturn(
            ImmutableOperativUppgift.builder()
                  .uppgiftId(UUID.randomUUID())
                  .handlaggningId(handlaggningId)
                  .status(RegelManuellTestStatus.AVSLUTAD.name())
                  .build());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 404, 404",
         "5367f6b8-cc4a-11f0-8de9-199901011234, 400, 400",
         "5367f6b8-cc4a-11f0-8de9-199901011234, 503, 503",
         "5367f6b8-cc4a-11f0-8de9-199901011234, 500, 500"
   })
   void done_should_return_mapped_status_when_read_handlaggning_fails(
         String handlaggningId, int handlaggningHttpStatus, int expectedDoneStatus) throws Exception
   {
      stubOulAdapter(UUID.fromString(handlaggningId));
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 1);

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

         // endOperativUppgift sits after readHandlaggning in handleUppgiftDone — it must not run.
         Mockito.verify(oulAdapter, Mockito.never()).endOperativUppgift(any(), any());
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
   void done_should_return_500_and_still_send_regel_response_when_final_update_handlaggning_fails(
         String handlaggningId) throws Exception
   {
      stubOulAdapter(UUID.fromString(handlaggningId));
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.PUT, 1);

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
