package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.RegelFelkod;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableManuellRegelCommonData;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;
import java.time.OffsetDateTime;
import java.util.UUID;
import se.fk.rimfrost.framework.regel.RegelTestData;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellStorageFaultHandlingTest extends AbstractRegelManuellTest
{
   private static ManuellRegelCommonData manuellRegelCommonDataStorage;

   @InjectMock
   ManuellRegelCommonDataStorage storage;

   @BeforeAll
   void setUp()
   {

      var uppgiftSpecification = ImmutableUppgiftSpecifikation.builder()
            .id(UUID.randomUUID())
            .version(1)
            .build();

      var uppgift = ImmutableUppgift.builder()
            .id(UUID.randomUUID())
            .version(1)
            .aktivitetId(UUID.randomUUID())
            .skapadTs(OffsetDateTime.now())
            .planeradTs(OffsetDateTime.now())
            .uppgiftStatus("1")
            .fSSAinformation("FSSAinformation.HANDLAGGNING_PAGAR")
            .uppgiftSpecifikation(uppgiftSpecification)
            .build();

      manuellRegelCommonDataStorage = ImmutableManuellRegelCommonData.builder()
            .uppgift(uppgift)
            .build();
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, ERROR"
   })
   void should_send_error_response_on_write_failure_during_initial_storage_write(String handlaggningId, Utfall expectedUtfall)
   {
      Mockito.doThrow(new IllegalStateException()).when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.OTHER, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_not_crash_on_read_failure_during_oul_response(String handlaggningId, String uppgiftId) throws InterruptedException
   {
      Mockito.doThrow(new IllegalStateException()).when(storage).getManuellRegelCommonData(eq(UUID.fromString(handlaggningId)));
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      // TODO: Any better way to verify this case?
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, ERROR"
   })
   void should_send_error_response_on_write_failure_during_oul_response(String handlaggningId, String uppgiftId,
         Utfall expectedUtfall) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doThrow(new IllegalStateException()).when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.OTHER, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29"
   })
   void should_send_oul_status_avbruten_on_write_failure_during_oul_response(
         String handlaggningId,
         String uppgiftId) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doNothing().doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      Thread.sleep(1000);
      var oulStatusMessage = oulKafkaConnector.waitForOulStatusMessage();
      assertEquals(uppgiftId, oulStatusMessage.getUppgiftId());
      assertEquals(Status.AVBRUTEN, oulStatusMessage.getStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, ERROR"
   })
   void should_send_error_response_on_write_failure_during_oul_response_alongside_avbruten(
         String handlaggningId,
         String uppgiftId,
         Utfall expectedUtfall) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doNothing().doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId);
      Thread.sleep(1000);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.OTHER, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, ERROR"
   })
   void should_use_cloudevent_attributes_from_oul_response_in_error_response(
         String handlaggningId,
         String uppgiftId,
         Utfall expectedUtfall) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doNothing().doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var oulRequest = oulKafkaConnector.waitForOulRequestMessage();
      oulKafkaConnector.simulateOulResponse(handlaggningId, uppgiftId, oulRequest.getCloudeventAttributes());
      Thread.sleep(1000);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.OTHER, regelResponse.getData().getError().getFelkod());
      assertEquals(RegelTestData.newRegelRequestMessagePayload(handlaggningId).getKogitoprocinstanceid(),
            regelResponse.getKogitoprocinstanceid());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde"
   })
   void should_not_crash_on_read_failure_during_oul_status_update(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws InterruptedException
   {
      Mockito.doThrow(new IllegalStateException()).when(storage).getManuellRegelCommonData(eq(UUID.fromString(handlaggningId)));
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      // TODO: Any better way to verify this case?
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde, ERROR"
   })
   void should_send_error_response_on_write_failure_during_oul_status_update(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde,
         Utfall expectedUtfall) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doThrow(new IllegalStateException()).when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.OTHER, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde"
   })
   void should_send_oul_status_avbruten_on_write_failure_during_oul_status_update(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doNothing().doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      Thread.sleep(1000);
      var oulStatusMessage = oulKafkaConnector.waitForOulStatusMessage();
      assertEquals(uppgiftId, oulStatusMessage.getUppgiftId());
      assertEquals(Status.AVBRUTEN, oulStatusMessage.getStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde, ERROR"
   })
   void should_send_error_response_on_write_failure_during_oul_status_update_alongside_avbruten(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde,
         Utfall expectedUtfall) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doNothing().doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY);
      Thread.sleep(1000);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.OTHER, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, ERROR"
   })
   void should_use_cloudevent_attributes_from_oul_status_in_error_response(
         String handlaggningId,
         String uppgiftId,
         Utfall expectedUtfall) throws InterruptedException
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      // Allow initial write from handleRegelRequest, throw on the subsequent write from handleOulStatus
      Mockito.doNothing().doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());

      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var oulRequest = oulKafkaConnector.waitForOulRequestMessage();

      var utforarId = ImmutableIdtyp.builder()
            .typId("Idtyp_typId")
            .varde("Idtyp_varde")
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, Status.NY,
            oulRequest.getCloudeventAttributes());
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed

      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.OTHER, regelResponse.getData().getError().getFelkod());
      // Verify the error response carries the kogitoprocinstanceid from the embedded OUL status attributes
      assertEquals(RegelTestData.newRegelRequestMessagePayload(handlaggningId).getKogitoprocinstanceid(),
            regelResponse.getKogitoprocinstanceid());
   }
}
