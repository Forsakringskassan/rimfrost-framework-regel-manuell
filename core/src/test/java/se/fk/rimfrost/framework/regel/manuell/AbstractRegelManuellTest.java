package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.quarkus.test.InjectMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.Mockito;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.regel.*;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.framework.regel.test.AbstractRegelTest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.PutHandlaggningRequest;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.UppgiftStatus;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AbstractRegelManuellTest extends AbstractRegelTest
{

   private static final String oulRequestsChannel = "operativt-uppgiftslager-requests";
   private static final String oulResponsesChannel = "operativt-uppgiftslager-responses";
   private static final String oulStatusNotificationChannel = "operativt-uppgiftslager-status-notification";
   private static final String oulStatusControlChannel = "operativt-uppgiftslager-status-control";

   @InjectMock
   RegelManuellServiceInterface<String, String> regelManuellService;

   protected void resetState()
   {
      Mockito.reset();
      wiremockServer.resetRequests();
      inMemoryConnector.sink(oulRequestsChannel).clear();
      inMemoryConnector.sink(oulStatusControlChannel).clear();
      inMemoryConnector.sink(regelResponsesChannel).clear();
   }

   protected void sendRegelRequest(String handlaggningId)
   {
      RegelRequestMessagePayload payload = new RegelRequestMessagePayload();
      RegelRequestMessagePayloadData data = new RegelRequestMessagePayloadData();
      data.setHandlaggningId(handlaggningId);
      data.setAktivitetId("9b9d8261-559b-48db-b8bb-cbf61401c0ae");
      payload.setSpecversion(se.fk.rimfrost.framework.regel.SpecVersion.NUMBER_1_DOT_0);
      payload.setId("99994567-89ab-4cde-9012-3456789abcde");
      payload.setSource("TestSource-001");
      payload.setType(regelRequestsChannel);
      payload.setKogitoprocid("234567");
      payload.setKogitorootprocid("123456");
      payload.setKogitorootprociid("77774567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoparentprociid("88884567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocinstanceid("66664567-89ab-4cde-9012-3456789abcde");
      payload.setKogitoprocist("345678");
      payload.setKogitoprocversion("111");
      payload.setKogitoproctype(se.fk.rimfrost.framework.regel.KogitoProcType.BPMN);
      payload.setKogitoprocrefid("56789");
      payload.setData(data);
      inMemoryConnector.source(regelRequestsChannel).send(payload);
   }

   //
   // Handläggning
   //

   protected void verifyGetHandlaggningProduced(String handlaggningId)
   {
      var requests = waitForWireMockRequest(wiremockServer, handlaggningEndpoint + handlaggningId, 1);
      assertEquals(1, requests.stream().filter(p -> p.getMethod().equals(RequestMethod.GET)).count());
   }

   protected void verifyPutHandlaggningProduced(String handlaggningId)
   {
      var requests = waitForWireMockRequest(wiremockServer, handlaggningEndpoint + handlaggningId, 1);
      assertEquals(1, requests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).count());
   }

   protected void verifyPutHandlaggningContentUppgiftStatus(String handlaggningId, String utforarId,
         UppgiftStatus expectedUppgiftStatus) throws Exception
   {
      var request = getLastHandlaggningPutRequest(handlaggningId);
      var dto = mapper.readValue(request.getBodyAsString(), PutHandlaggningRequest.class);
      var uppgift = dto.getHandlaggning().getUppgift();

      assertEquals(expectedUppgiftStatus, uppgift.getUppgiftStatus());
      assertEquals(UUID.fromString(utforarId), uppgift.getUtforarId());
   }

   private LoggedRequest getLastHandlaggningPutRequest(String handlaggningId)
   {
      var requests = waitForWireMockRequest(wiremockServer, handlaggningEndpoint + handlaggningId, 2);
      return requests.stream()
            .filter(r -> r.getMethod().equals(RequestMethod.PUT))
            .reduce((first, second) -> second)
            .orElseThrow();
   }

   //
   // Operativt uppgiftslager
   //

   protected void verifyOulStatusMessageProduced()
   {
      Assertions.assertEquals(1, waitForMessages(oulStatusControlChannel).size());
   }

   protected void verifyOulStatusMessageContent(String uppgiftId, Status expectedStatus)
   {
      var msg = (OperativtUppgiftslagerStatusMessage) waitForMessages(oulStatusControlChannel)
            .getFirst().getPayload();
      Assertions.assertEquals(uppgiftId, msg.getUppgiftId());
      Assertions.assertEquals(expectedStatus, msg.getStatus());
   }

   protected void verifyOulRequestProduced()
   {
      var messages = waitForMessages(oulRequestsChannel);
      Assertions.assertEquals(1, messages.size());
   }

   protected void verifyOulRequestContent(String handlaggningId)
   {
      var messages = waitForMessages(oulRequestsChannel);
      assertEquals(1, messages.size());

      var payload = (OperativtUppgiftslagerRequestMessage) messages.getFirst().getPayload();

      assertEquals(handlaggningId, payload.getHandlaggningId());
      assertEquals("TestUppgiftBeskrivning", payload.getBeskrivning());
      assertEquals("TestUppgiftNamn", payload.getRegel());
      assertEquals("C", payload.getVerksamhetslogik());
      assertEquals("ANSVARIG_HANDLAGGARE", payload.getRoll());
      assertTrue(payload.getUrl().contains("/regel/manuell"));
   }

   protected void simulateOulResponse(String handlaggningId, String uppgiftId)
   {
      var msg = new OperativtUppgiftslagerResponseMessage();
      msg.setHandlaggningId(handlaggningId);
      msg.setUppgiftId(uppgiftId);
      inMemoryConnector.source(oulResponsesChannel).send(msg);
   }

   protected void simulateOulStatus(String handlaggningId, String uppgiftId, String utforarId, Status status)
   {
      var msg = new OperativtUppgiftslagerStatusMessage();
      msg.setStatus(status);
      msg.setUppgiftId(uppgiftId);
      msg.setHandlaggningId(handlaggningId);
      msg.setUtforarId(utforarId);
      inMemoryConnector.source(oulStatusNotificationChannel).send(msg);
   }

   //
   // Regel response
   //

   protected void verifyRegelResponseProduced()
   {
      Assertions.assertEquals(1, waitForMessages(regelResponsesChannel).size());
   }

   protected void verifyRegelResponseContent(String handlaggningId, Utfall utfall)
   {
      var msg = (RegelResponseMessagePayload) waitForMessages(regelResponsesChannel)
            .getFirst().getPayload();
      Assertions.assertEquals(handlaggningId, msg.getData().getHandlaggningId());
      Assertions.assertEquals(utfall, msg.getData().getUtfall());
   }

   //
   // Handläggning done
   //

   protected void sendPostRegelManuellHandlaggningDone(String handlaggningId)
   {
      given().when().post("/regel/manuell/{handlaggningId}/done", handlaggningId).then().statusCode(204);
   }

   //
   // Utökad uppgiftsbeskrivning
   //

   protected GetUtokadUppgiftsbeskrivningResponse sendGetUtokadUppgiftsbeskrivning()
   {
      return given().when().get("/regel/manuell/utokadUppgiftsbeskrivning").then().statusCode(200).extract()
            .as(GetUtokadUppgiftsbeskrivningResponse.class);
   }

   protected void verifyUtokadUppgiftsbeskrivningProduced()
   {
      assertNotNull(sendGetUtokadUppgiftsbeskrivning());
   }

   protected void verifyUtokadUppgiftsbeskrivningContent()
   {
      var response = sendGetUtokadUppgiftsbeskrivning();
      Assertions.assertEquals("TestUtokadUppgiftsbeskrivning", response.getBeskrivning());
   }

   //
   // Mocking
   //
   protected void verifyMocks(String handlaggningId)
   {
      Mockito.verify(regelManuellService)
            .done(UUID.fromString(handlaggningId));
   }

   protected void mockRegelService(Utfall utfall, String handlaggningId)
   {
      Mockito.doNothing().when(regelManuellService)
            .done(UUID.fromString(handlaggningId));
   }

}
