package se.fk.rimfrost.framework.regel.manuell.base;

import com.github.tomakehurst.wiremock.http.RequestMethod;

import io.quarkus.test.InjectMock;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

import static org.mockito.ArgumentMatchers.any;
import static se.fk.rimfrost.framework.regel.WireMockHandlaggning.getUppgiftFromLastPutHandlaggning;
import static se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestData.newHandlaggningApiIdtyp;
import static se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestData.newHandlaggningIdtyp;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellHandlaggningTest extends AbstractRegelManuellTest
{

   @InjectMock
   OulAdapter oulAdapter;

   @BeforeEach
   void stubOulAdapter() throws Exception
   {
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenAnswer(invocation -> {
         CreateOperativUppgiftRequest req = invocation.getArgument(0, CreateOperativUppgiftRequest.class);
         return ImmutableOperativUppgift.builder()
               .uppgiftId(UUID.randomUUID())
               .handlaggningId(req.getHandlaggningId())
               .status("NY")
               .build();
      });
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_create_initial_handlaggning_request(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var handlaggningRequests = WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
      Assertions.assertEquals(1, handlaggningRequests.size());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgiftstatus_planerad(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(),
            uppgiftStatusProvider.getPlaneradId());
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages are processed
      var uppgift = getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(uppgiftStatusProvider.getPlaneradId(), uppgift.getUppgiftStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234 , 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgiftstatus_avslutad(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(),
            uppgiftStatusProvider.getTilldeladId());
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages are processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var uppgift = getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(uppgiftStatusProvider.getAvslutadId(), uppgift.getUppgiftStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234 , 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_put_handlaggning_with_uppgift_correct_utforar_id(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(),
            uppgiftStatusProvider.getPlaneradId());
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages are processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var uppgift = getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(newHandlaggningApiIdtyp(), uppgift.getUtforarId());
   }
}
