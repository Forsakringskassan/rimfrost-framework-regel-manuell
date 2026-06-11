package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.oul.model.ImmutableProcessInfo;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestStatus;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

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

   @InjectMock
   OulAdapter oulAdapter;

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   private void stubCreateOperativUppgiftSuccess(UUID handlaggningId) throws Exception
   {
      var processInfo = ImmutableProcessInfo.builder()
            .replyTopic(responseTopic)
            .cloudeventAttributes(Map.of())
            .build();
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenReturn(
            ImmutableOperativUppgift.builder()
                  .uppgiftId(UUID.randomUUID())
                  .handlaggningId(handlaggningId)
                  .status(RegelManuellTestStatus.PLANERAD.name())
                  .processInfo(processInfo)
                  .build());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, ERROR"
   })
   void should_send_error_response_on_oul_create_uppgift_failure(String handlaggningId, Utfall expectedUtfall) throws Exception
   {
      Mockito.when(oulAdapter.createOperativUppgift(any()))
            .thenThrow(new OulException(OulException.ErrorType.SERVICE_UNAVAILABLE, "OUL is down"));

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);

      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_return_500_on_oul_end_uppgift_failure_during_done(String handlaggningId, String uppgiftId) throws Exception
   {
      stubCreateOperativUppgiftSuccess(UUID.fromString(handlaggningId));
      Mockito.when(oulAdapter.endOperativUppgift(any(), any()))
            .thenThrow(new OulException(OulException.ErrorType.UNEXPECTED_ERROR, "OUL is broken"));

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);

      given()
            .when()
            .post(basePath() + "/" + handlaggningId + "/done")
            .then()
            .statusCode(500);
   }
}
