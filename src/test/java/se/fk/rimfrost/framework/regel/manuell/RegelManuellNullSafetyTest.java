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
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

import static org.mockito.ArgumentMatchers.any;

/**
 * Regression tests for null-safety bugs in the OUL Kafka message pipeline.
 *
 * <p>Covers two related bugs discovered in rimfrost-regel-rtf-manuell 1.1.0:
 * <ol>
 *   <li>Bug 1: handleUppgiftDone crashes with NPE when oulUppgiftId is null</li>
 *   <li>Bug 2+3: OulKafkaMapper passes null cloudeventAttributes downstream, causing
 *       CloudEventAttributesMapper.toCloudEventData(null) to throw NPE</li>
 * </ol>
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
               .status("NY")
               .build();
      });
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
      // Bug 2+3: null cloudeventAttributes → OulKafkaMapper passes null through →
      // CloudEventAttributesMapper.toCloudEventData(null) throws NPE → handleOulStatus crashes.
      // Before fix: the NPE propagates and prevents /done from completing (500).
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId,
            uppgiftStatusProvider.getPlaneradId(), null);
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
         String idtypVarde) throws InterruptedException
   {
      // Bug 1: handleUppgiftDone passed oulUppgiftId (which is @Nullable) directly to
      // sendOulStatusUpdate, causing NPE when oulUppgiftId was never stored.
      // Reproduce by returning null uppgiftId from createOperativUppgift.
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenAnswer(invocation -> {
         CreateOperativUppgiftRequest req = invocation.getArgument(0, CreateOperativUppgiftRequest.class);
         return ImmutableOperativUppgift.builder()
               .uppgiftId(null)
               .handlaggningId(req.getHandlaggningId())
               .status("NY")
               .build();
      });

      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();

      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId,
            uppgiftStatusProvider.getPlaneradId());
      Thread.sleep(1000);

      // Before fix: sendOulStatusUpdate(null, ...) throws NPE → /done returns 500.
      // After fix: handleUppgiftDone uses handlaggningId instead of oulUppgiftId → 204.
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      regelKafkaConnector.waitForRegelResponse();
   }
}
