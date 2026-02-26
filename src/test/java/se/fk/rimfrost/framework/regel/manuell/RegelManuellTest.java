package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.http.RequestMethod;
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
      @QuarkusTestResource(WireMockTestResource.class),
      @QuarkusTestResource(StorageDataTestResource.class)
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

   private void sendPostRegelManuell(String kundbehovsflodeId)
   {
      given().when().post("/regel/manuell/{kundbehovsflodeId}/done", kundbehovsflodeId).then().statusCode(204);
   }

   private void sendRegelRequest(String kundbehovsflodeId) throws Exception
   {
      RegelRequestMessagePayload payload = new RegelRequestMessagePayload();
      RegelRequestMessagePayloadData data = new RegelRequestMessagePayloadData();
      data.setKundbehovsflodeId(kundbehovsflodeId);
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
         "5367f6b8-cc4a-11f0-8de9-199901011234, JA",
         "5367f6b8-cc4a-11f0-8de9-199901011234, NEJ"
   })
   void TestRegelManuell(String kundbehovsflodeId, Utfall expectedUtfall) throws Exception
   {
      Mockito.reset(regelManuellService);
      wiremockServer.resetRequests();
      this.inMemoryConnector.sink(oulRequestsChannel).clear();
      this.inMemoryConnector.sink(oulStatusControlChannel).clear();
      this.inMemoryConnector.sink(regelResponsesChannel).clear();

      System.out.printf("Starting TestRegelManuell. %S%n", kundbehovsflodeId);

      // Send regel request to start workflow
      sendRegelRequest(kundbehovsflodeId);

      //
      // Verify GET kundbehovsflöde requested
      //
      var kundbehovsflodeRequests = waitForWireMockRequest(wiremockServer,
            kundbehovsflodeEndpoint + kundbehovsflodeId, 1);
      assertEquals(1, kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.GET)).count());

      //
      // Verify oul message produced
      //
      var messages = waitForMessages(oulRequestsChannel);
      assertEquals(1, messages.size());

      var message = messages.getFirst().getPayload();
      assertInstanceOf(OperativtUppgiftslagerRequestMessage.class, message);

      var oulRequestMessage = (OperativtUppgiftslagerRequestMessage) message;
      assertEquals(kundbehovsflodeId, oulRequestMessage.getKundbehovsflodeId());
      assertEquals("VAH", oulRequestMessage.getKundbehov());
      assertEquals("TestUppgiftBeskrivning", oulRequestMessage.getBeskrivning());
      assertEquals("TestUppgiftNamn", oulRequestMessage.getRegel());
      assertEquals("C", oulRequestMessage.getVerksamhetslogik());
      assertEquals("ANSVARIG_HANDLAGGARE", oulRequestMessage.getRoll());
      assertTrue(oulRequestMessage.getUrl().contains("/regel/manuell"));

      // Clear previous requests
      wiremockServer.resetRequests();

      //
      // Send mocked OUL response
      //
      OperativtUppgiftslagerResponseMessage oulResponseMessage = new OperativtUppgiftslagerResponseMessage();
      oulResponseMessage.setKundbehovsflodeId(kundbehovsflodeId);
      oulResponseMessage.setUppgiftId("11e53b18-e9ac-4707-825b-a1cb80689c29");
      inMemoryConnector.source(oulResponsesChannel).send(oulResponseMessage);

      //
      // Verify PUT kundbehovsflöde requested
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockServer, kundbehovsflodeEndpoint + kundbehovsflodeId, 1);
      var putRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();

      assertEquals(1, kundbehovsflodeRequests.size());
      assertEquals(1, putRequests.size());

      var sentJson = putRequests.getFirst().getBodyAsString();
      var sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(UUID.fromString(kundbehovsflodeId), sentPutKundbehovsflodeRequest.getUppgift().getKundbehovsflodeId());
      assertEquals(UppgiftStatus.PLANERAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus());
      assertNull(sentPutKundbehovsflodeRequest.getUppgift().getUtforarId());
      // TODO: Add more checks of sentPutKundbehovsflodeRequest content

      // Clear previous requests
      wiremockServer.resetRequests();

      //
      // mock status update from OUL
      //
      OperativtUppgiftslagerStatusMessage oulStatusMessage = new OperativtUppgiftslagerStatusMessage();
      oulStatusMessage.setStatus(Status.NY);
      oulStatusMessage.setUppgiftId(oulResponseMessage.getUppgiftId());
      oulStatusMessage.setKundbehovsflodeId(kundbehovsflodeId);
      oulStatusMessage.setUtforarId("383cc515-4c55-479b-a96b-244734ef1336");
      inMemoryConnector.source(oulStatusNotificationChannel).send(oulStatusMessage);

      //
      // verify expected actions from manual rule as result of new status reported
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockServer, kundbehovsflodeEndpoint + kundbehovsflodeId, 1);
      putRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();

      assertEquals(1, kundbehovsflodeRequests.size());
      assertEquals(1, putRequests.size());

      sentJson = putRequests.getFirst().getBodyAsString();
      sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(UUID.fromString(kundbehovsflodeId), sentPutKundbehovsflodeRequest.getUppgift().getKundbehovsflodeId());
      assertEquals(UppgiftStatus.PLANERAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus());
      assertNotNull(sentPutKundbehovsflodeRequest.getUppgift().getUtforarId());
      // TODO: Add more checks of sentPutKundbehovsflodeRequest content

      // Clear previous requests
      wiremockServer.resetRequests();

      //
      // mock GET operation requested from portal FE
      //
      var getUtokadUppgiftsbeskrivningResponse = sendGetUtokadUppgiftsbeskrivning();

      //
      // Verify GET operation response
      //
      assertEquals("TestUtokadUppgiftsbeskrivning", getUtokadUppgiftsbeskrivningResponse.getBeskrivning());

      // Clear previous requests
      wiremockServer.resetRequests();

      //
      // Configure regelManuellService methods with expected values
      //
      Mockito.when(regelManuellService.decideUtfall(Mockito.any())).thenReturn(expectedUtfall);
      Mockito.doNothing().when(regelManuellService).handleRegelDone(UUID.fromString(kundbehovsflodeId));

      //
      // mock POST operation from portal FE
      //
      sendPostRegelManuell(kundbehovsflodeId);

      //
      // verify that rule performed requests to kundbehovsflode
      //
      kundbehovsflodeRequests = waitForWireMockRequest(wiremockServer, kundbehovsflodeEndpoint + kundbehovsflodeId, 1);
      putRequests = kundbehovsflodeRequests.stream().filter(p -> p.getMethod().equals(RequestMethod.PUT)).toList();

      assertEquals(1, kundbehovsflodeRequests.size());
      assertEquals(1, putRequests.size());

      sentJson = putRequests.getLast().getBodyAsString();
      sentPutKundbehovsflodeRequest = mapper.readValue(sentJson, PutKundbehovsflodeRequest.class);
      assertEquals(UppgiftStatus.AVSLUTAD, sentPutKundbehovsflodeRequest.getUppgift().getUppgiftStatus());

      //
      // verify kafka status message sent to oul
      //
      messages = waitForMessages(oulStatusControlChannel);
      assertEquals(1, messages.size());

      message = messages.getFirst().getPayload();
      assertInstanceOf(OperativtUppgiftslagerStatusMessage.class, message);

      oulStatusMessage = (OperativtUppgiftslagerStatusMessage) message;
      assertEquals(oulResponseMessage.getUppgiftId(), oulStatusMessage.getUppgiftId());
      assertEquals(Status.AVSLUTAD, oulStatusMessage.getStatus());

      //
      // verify kafka manuell response message sent to VAH
      //
      messages = waitForMessages(regelResponsesChannel);
      assertEquals(1, messages.size());

      message = messages.getFirst().getPayload();
      assertInstanceOf(RegelResponseMessagePayload.class, message);

      var regelManuellResponseMessagePayload = (RegelResponseMessagePayload) message;
      assertEquals(kundbehovsflodeId, regelManuellResponseMessagePayload.getData().getKundbehovsflodeId());
      assertEquals(expectedUtfall, regelManuellResponseMessagePayload.getData().getUtfall());

      //
      // verify expected mock call counts
      //
      Mockito.verify(regelManuellService, Mockito.times(1)).decideUtfall(Mockito.any());
      Mockito.verify(regelManuellService, Mockito.times(1)).handleRegelDone(UUID.fromString(kundbehovsflodeId));
   }
}
