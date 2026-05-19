package se.fk.rimfrost.framework.regel.manuell.base;

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
import se.fk.rimfrost.framework.regel.Utfall;

import static org.mockito.ArgumentMatchers.any;
import static se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestData.newHandlaggningIdtyp;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellResponseTest extends AbstractRegelManuellTest
{

   @InjectMock
   OulAdapter oulAdapter;

   @BeforeEach
   void stubOulAdapter() throws Exception
   {
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenAnswer(invocation ->
      {
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
         "JA, 5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_return_correct_regel_response_utfall(Utfall expectedUtfall, String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(), uppgiftStatusProvider.getPlaneradId());
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      Assertions.assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_return_correct_regel_response_handlaggning_id(String handlaggningId, String uppgiftId)
         throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, newHandlaggningIdtyp(), uppgiftStatusProvider.getPlaneradId());
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      Assertions.assertEquals(handlaggningId, regelResponse.getData().getHandlaggningId());
   }

}
