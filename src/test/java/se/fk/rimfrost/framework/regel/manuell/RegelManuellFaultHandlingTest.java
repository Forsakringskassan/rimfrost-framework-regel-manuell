package se.fk.rimfrost.framework.regel.manuell;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.RegelFelkod;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellFaultHandlingTest extends AbstractRegelManuellTest
{
   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901014444, ERROR"
   })
   void should_send_error_response_on_initial_handlaggning_read_failure(String handlaggningId, Utfall expectedUtfall)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.HANDLAGGNING_READ_FAILURE, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901015555, ERROR"
   })
   void should_send_error_response_on_initial_handlaggning_write_failure(String handlaggningId, Utfall expectedUtfall)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.HANDLAGGNING_WRITE_FAILURE, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901014444, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde, ERROR"
   })
   void should_send_error_response_on_handlaggning_read_failure_during_oul_status_with_status_new(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde,
         Utfall expectedUtfall) throws JsonProcessingException
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
      // verify response
      //
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.HANDLAGGNING_READ_FAILURE, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901015555, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde, ERROR"
   })
   void should_send_error_response_on_handlaggning_write_failure_during_oul_status_with_status_new(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde,
         Utfall expectedUtfall) throws JsonProcessingException
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
      // verify response
      //
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.HANDLAGGNING_WRITE_FAILURE, regelResponse.getData().getError().getFelkod());
   }
}
