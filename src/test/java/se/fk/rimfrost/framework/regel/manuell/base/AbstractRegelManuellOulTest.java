package se.fk.rimfrost.framework.regel.manuell.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellOulTest extends AbstractRegelManuellTest
{

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_create_correct_oul_request(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var oulRequest = oulKafkaConnector.waitForOulRequestMessage();
      Assertions.assertEquals(handlaggningId, oulRequest.getHandlaggningId());
      Assertions.assertEquals("TestUppgiftBeskrivning", oulRequest.getBeskrivning());
      Assertions.assertEquals("TestUppgiftNamn", oulRequest.getRegel());
      Assertions.assertEquals("C", oulRequest.getVerksamhetslogik());
      Assertions.assertEquals("ANSVARIG_HANDLAGGARE", oulRequest.getRoll());
      Assertions.assertTrue(oulRequest.getUrl().contains(basePath));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_send_oul_status_uppgift_avslutad(String handlaggningId, String uppgiftId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var oulStatusMessage = oulKafkaConnector.waitForOulStatusMessage();
      Assertions.assertEquals(uppgiftId, oulStatusMessage.getUppgiftId());
      Assertions.assertEquals(Status.AVSLUTAD, oulStatusMessage.getStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde"
   })
   void oul_status_should_put_handlaggning_with_status_new(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws JsonProcessingException
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      //
      // mock status update from OUL
      //
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockRegelManuell.getLastPutHandlaggning(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals("1", handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getUppgift().getVersion());
   }

}
