package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.oul.model.OperativUppgift;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestStatus;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

import static org.mockito.ArgumentMatchers.any;

/**
 * Regression tests for null-safety in the OUL Kafka message pipeline.
 */
@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellNullSafetyTest extends AbstractRegelManuellTest
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
               .status(RegelManuellTestStatus.PLANERAD.name())
               .build();
      });
      Mockito.when(oulAdapter.endOperativUppgift(any(), any())).thenAnswer(invocation -> ImmutableOperativUppgift.builder()
            .uppgiftId(UUID.randomUUID())
            .handlaggningId(UUID.randomUUID())
            .status(RegelManuellTestStatus.AVSLUTAD.name())
            .build());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901021234, 11e53b18-e9ac-4707-825b-a1cb80689c30, Idtyp_typId, Idtyp_varde"
   })
   void done_succeeds_when_oul_status_has_null_cloudevent_attributes(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws InterruptedException
   {
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();

      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId,
            RegelManuellTestStatus.PLANERAD, null);
      Thread.sleep(1000);

      sendPostRegelManuellHandlaggningDone(handlaggningId);
      regelKafkaConnector.waitForRegelResponse();
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901031234, 11e53b18-e9ac-4707-825b-a1cb80689c31, Idtyp_typId, Idtyp_varde"
   })
   void done_succeeds_when_oul_uppgift_id_is_null(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws Exception
   {
      var nullUppgiftId = Mockito.mock(OperativUppgift.class);
      Mockito.when(nullUppgiftId.getUppgiftId()).thenReturn(null);
      Mockito.when(nullUppgiftId.getHandlaggningId()).thenReturn(UUID.fromString(handlaggningId));
      Mockito.when(nullUppgiftId.getStatus()).thenReturn("NY");
      Mockito.doReturn(nullUppgiftId).when(oulAdapter).createOperativUppgift(any());

      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();

      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId,
            RegelManuellTestStatus.PLANERAD);
      Thread.sleep(1000);

      sendPostRegelManuellHandlaggningDone(handlaggningId);
      regelKafkaConnector.waitForRegelResponse();
   }
}
