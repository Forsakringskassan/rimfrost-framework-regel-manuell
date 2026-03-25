package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.common.QuarkusTestResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.OperativtUppgiftslagerRequestMessage;
import se.fk.rimfrost.OperativtUppgiftslagerResponseMessage;
import se.fk.rimfrost.OperativtUppgiftslagerStatusMessage;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.*;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayload;
import se.fk.rimfrost.framework.regel.RegelRequestMessagePayloadData;
import se.fk.rimfrost.framework.regel.RegelResponseMessagePayload;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.test.RegelTest;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockTestResource.class)
})
public class RegelManuellTest extends RegelTest
{
   private static final String oulRequestsChannel = "operativt-uppgiftslager-requests";
   private static final String oulResponsesChannel = "operativt-uppgiftslager-responses";
   private static final String oulStatusNotificationChannel = "operativt-uppgiftslager-status-notification";
   private static final String oulStatusControlChannel = "operativt-uppgiftslager-status-control";

   @InjectMock
   RegelManuellServiceInterface regelManuellService;

   @BeforeAll
   static void setup()
   {
      setupRegelManuellTest();
   }

   static void setupRegelManuellTest()
   {
      Properties props = new Properties();
      try (InputStream in = RegelManuellTest.class.getResourceAsStream("/test.properties"))
      {
         if (in == null)
         {
            throw new RuntimeException("Could not find /test.properties in classpath");
         }
         props.load(in);
      }
      catch (IOException e)
      {
         throw new RuntimeException("Failed to load test.properties", e);
      }
   }

   private GetUtokadUppgiftsbeskrivningResponse sendGetUtokadUppgiftsbeskrivning()
   {
      return given().when().get("/regel/manuell/utokadUppgiftsbeskrivning").then().statusCode(200).extract()
            .as(GetUtokadUppgiftsbeskrivningResponse.class);
   }

   private void sendPostRegelManuellHandlaggningDone(String handlaggningId)
   {
      given().when().post("/regel/manuell/{handlaggningId}/done", handlaggningId).then().statusCode(204);
   }

   private void sendRegelRequest(String handlaggningId) throws Exception
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

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, JA, 383cc515-4c55-479b-a96b-244734ef1336, 11e53b18-e9ac-4707-825b-a1cb80689c29",
         "5367f6b8-cc4a-11f0-8de9-199901011234, NEJ , 383cc515-4c55-479b-a96b-244734ef1336, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void TestRegelManuell(String handlaggningId, Utfall expectedUtfall, String utforarId, String uppgiftId) throws Exception
   {
      System.out.printf("Starting TestRegelManuell. %S%n", handlaggningId);

      resetState();

      //
      // Workflow start
      //
      sendRegelRequest(handlaggningId);
      verifyInitialHandlaggningGet(handlaggningId);
      verifyOulRequestProduced(handlaggningId);

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
      simulateOulStatus(handlaggningId, uppgiftId, utforarId);
      verifyUppgiftStatusUpdate(handlaggningId, utforarId, UppgiftStatus.PLANERAD);

      //
      // Verify utökad uppgiftsbeskrivning
      //
      verifyUtokadUppgiftsbeskrivning();

      //
      // Verify handläggning DONE
      //
      wiremockServer.resetRequests();
      mockRegelService(expectedUtfall, handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      verifyUppgiftStatusUpdate(handlaggningId, utforarId, UppgiftStatus.AVSLUTAD);
      verifyOulStatusMessage(uppgiftId, Status.AVSLUTAD);
      verifyRegelResponse(handlaggningId, expectedUtfall);

      // Verify interactions
      verifyMocks(handlaggningId);
   }

   private LoggedRequest getLastPutRequest(String handlaggningId)
   {
      var requests = waitForWireMockRequest(wiremockServer, handlaggningEndpoint + handlaggningId, 1);

      return requests.stream()
            .filter(r -> r.getMethod().equals(RequestMethod.PUT))
            .reduce((first, second) -> second)
            .orElseThrow();
   }

   private void resetState()
   {
      Mockito.reset(regelManuellService);
      wiremockServer.resetRequests();
      inMemoryConnector.sink(oulRequestsChannel).clear();
      inMemoryConnector.sink(oulStatusControlChannel).clear();
      inMemoryConnector.sink(regelResponsesChannel).clear();
   }

   private void verifyInitialHandlaggningGet(String handlaggningId)
   {
      var requests = waitForWireMockRequest(wiremockServer, handlaggningEndpoint + handlaggningId, 1);
      assertEquals(1, requests.stream().filter(p -> p.getMethod().equals(RequestMethod.GET)).count());
   }

   private void verifyOulRequestProduced(String handlaggningId)
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

   private void simulateOulResponse(String handlaggningId, String uppgiftId)
   {
      var msg = new OperativtUppgiftslagerResponseMessage();
      msg.setHandlaggningId(handlaggningId);
      msg.setUppgiftId(uppgiftId);

      inMemoryConnector.source(oulResponsesChannel).send(msg);
   }

   private void simulateOulStatus(String handlaggningId, String uppgiftId, String utforarId)
   {
      var msg = new OperativtUppgiftslagerStatusMessage();
      msg.setStatus(Status.NY);
      msg.setUppgiftId(uppgiftId);
      msg.setHandlaggningId(handlaggningId);
      msg.setUtforarId(utforarId);

      inMemoryConnector.source(oulStatusNotificationChannel).send(msg);
   }

   private void verifyUppgiftStatusUpdate(String handlaggningId, String utforarId, UppgiftStatus expectedUppgiftStatus)
         throws Exception
   {
      var request = getLastPutRequest(handlaggningId);

      var dto = mapper.readValue(request.getBodyAsString(), PutHandlaggningRequest.class);
      var uppgift = dto.getHandlaggning().getUppgift();

      assertEquals(expectedUppgiftStatus, uppgift.getUppgiftStatus());
      assertEquals(UUID.fromString(utforarId), uppgift.getUtforarId());
   }

   private void verifyUtokadUppgiftsbeskrivning()
   {
      var response = sendGetUtokadUppgiftsbeskrivning();
      assertEquals("TestUtokadUppgiftsbeskrivning", response.getBeskrivning());
   }

   private void mockRegelService(Utfall utfall, String handlaggningId)
   {
      Mockito.when(regelManuellService.decideUtfall(Mockito.any())).thenReturn(utfall);
      Mockito.doNothing().when(regelManuellService)
            .handleRegelDone(UUID.fromString(handlaggningId));
   }

   private void verifyCompletedUppgiftUpdate(String handlaggningId, String utforarId) throws Exception
   {
      var request = getLastPutRequest(handlaggningId);

      var dto = mapper.readValue(request.getBodyAsString(), PutHandlaggningRequest.class);
      var uppgift = dto.getHandlaggning().getUppgift();

      assertEquals(UppgiftStatus.AVSLUTAD, uppgift.getUppgiftStatus());
      assertEquals(UUID.fromString(utforarId), uppgift.getUtforarId());
   }

   private void verifyOulStatusMessage(String uppgiftId, Status expectedStatus)
   {
      var msg = (OperativtUppgiftslagerStatusMessage) waitForMessages(oulStatusControlChannel)
            .getFirst().getPayload();

      assertEquals(uppgiftId, msg.getUppgiftId());
      assertEquals(expectedStatus, msg.getStatus());
   }

   private void verifyRegelResponse(String handlaggningId, Utfall utfall)
   {
      var msg = (RegelResponseMessagePayload) waitForMessages(regelResponsesChannel)
            .getFirst().getPayload();

      assertEquals(handlaggningId, msg.getData().getHandlaggningId());
      assertEquals(utfall, msg.getData().getUtfall());
   }

   private void verifyMocks(String handlaggningId)
   {
      Mockito.verify(regelManuellService).decideUtfall(Mockito.any());
      Mockito.verify(regelManuellService)
            .handleRegelDone(UUID.fromString(handlaggningId));
   }

}
