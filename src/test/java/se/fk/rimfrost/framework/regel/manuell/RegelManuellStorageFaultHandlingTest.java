package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.regel.RegelTestData;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestStatus;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.storage.CloudEventDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableManuellRegelCommonData;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

   @InjectMock
   CloudEventDataStorage cloudEventDataStorage;

   @InjectMock
   OulAdapter oulAdapter;

   private void stubOulAdapter(UUID handlaggningId) throws Exception
   {
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenReturn(
            ImmutableOperativUppgift.builder()
                  .uppgiftId(UUID.randomUUID())
                  .handlaggningId(handlaggningId)
                  .status("NY")
                  .build());
   }

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
            .uppgiftStatus(RegelManuellTestStatus.PLANERAD.name())
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
         throws Exception
   {
      stubOulAdapter(UUID.fromString(handlaggningId));
      Mockito.doThrow(new IllegalStateException()).when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_WRITE_FAILURE, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde, ERROR"
   })
   void should_send_error_response_on_read_failure_during_oul_status_update(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde,
         Utfall expectedUtfall) throws Exception
   {
      stubOulAdapter(UUID.fromString(handlaggningId));
      Mockito.doThrow(new IllegalStateException()).when(storage).getManuellRegelCommonData(eq(UUID.fromString(handlaggningId)));
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, null, RegelManuellTestStatus.PLANERAD);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_READ_FAILURE, regelResponse.getData().getError().getFelkod());
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
         Utfall expectedUtfall) throws Exception
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doThrow(new IllegalStateException()).when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, null, RegelManuellTestStatus.PLANERAD);
      Mockito.verify(storage, Mockito.timeout(5000)).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_WRITE_FAILURE, regelResponse.getData().getError().getFelkod());
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
         Utfall expectedUtfall) throws Exception
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());
      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, null, RegelManuellTestStatus.PLANERAD);
      Mockito.verify(storage, Mockito.timeout(5000)).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_WRITE_FAILURE, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, ERROR"
   })
   void should_use_cloudevent_attributes_from_oul_status_in_error_response(
         String handlaggningId,
         String uppgiftId,
         Utfall expectedUtfall) throws Exception
   {
      stubOulAdapter(UUID.fromString(handlaggningId));
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      // Allow initial write from handleRegelRequest, throw on the subsequent write from handleOulStatus
      Mockito.doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());

      regelKafkaConnector.sendRegelRequest(handlaggningId);

      var oulRequestCaptor = ArgumentCaptor.forClass(CreateOperativUppgiftRequest.class);
      Mockito.verify(oulAdapter, Mockito.timeout(5000)).createOperativUppgift(oulRequestCaptor.capture());
      var cloudeventAttributes = oulRequestCaptor.getValue().getCloudeventAttributes();

      var utforarId = ImmutableIdtyp.builder()
            .typId("Idtyp_typId")
            .varde("Idtyp_varde")
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, null, RegelManuellTestStatus.PLANERAD,
            cloudeventAttributes);
      Thread.sleep(1000); // Sleep 1 second to ensure that kafka messages is processed

      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_MANUELL_REGEL_COMMON_DATA_WRITE_FAILURE, regelResponse.getData().getError().getFelkod());
      // Verify the error response carries the kogitoprocinstanceid from the embedded OUL status attributes
      assertEquals(RegelTestData.newRegelRequestMessagePayload(handlaggningId).getKogitoprocinstanceid(),
            regelResponse.getKogitoprocinstanceid());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_try_to_end_operativ_uppgift_when_initial_storage_write_fails(String handlaggningId) throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenReturn(
            ImmutableOperativUppgift.builder()
                  .uppgiftId(oulUppgiftId)
                  .handlaggningId(UUID.fromString(handlaggningId))
                  .status(RegelManuellTestStatus.PLANERAD.name())
                  .build());
      Mockito.doThrow(new IllegalStateException()).when(storage)
            .setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());

      regelKafkaConnector.sendRegelRequest(handlaggningId);
      regelKafkaConnector.waitForRegelResponse();

      Mockito.verify(oulAdapter).endOperativUppgift(oulUppgiftId, "Internal error");
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_not_try_to_end_operativ_uppgift_when_get_handlaggning_fails(String handlaggningId) throws Exception
   {
      var server = WireMockRegelManuell.getWireMockServer();
      var failureStub = server.stubFor(
            WireMock.get(WireMock.urlPathMatching("/handlaggning/.*" + handlaggningId + ".*"))
                  .atPriority(1)
                  .willReturn(WireMock.serverError()));
      try
      {
         regelKafkaConnector.sendRegelRequest(handlaggningId);
         regelKafkaConnector.waitForRegelResponse();

         Mockito.verify(oulAdapter, Mockito.never()).endOperativUppgift(Mockito.any(), Mockito.any());
      }
      finally
      {
         server.removeStub(failureStub);
      }
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde"
   })
   void should_try_to_end_operativ_uppgift_when_oul_status_update_storage_read_fails(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws Exception
   {
      Mockito.doThrow(new IllegalStateException())
            .when(storage).getManuellRegelCommonData(eq(UUID.fromString(handlaggningId)));

      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, null, RegelManuellTestStatus.PLANERAD);
      regelKafkaConnector.waitForRegelResponse();

      Mockito.verify(oulAdapter).endOperativUppgift(UUID.fromString(uppgiftId), "Internal error");
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde"
   })
   void should_try_to_end_operativ_uppgift_when_oul_status_update_storage_write_fails(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws Exception
   {
      Mockito.when(storage.getManuellRegelCommonData(eq(UUID.fromString(handlaggningId))))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doThrow(new IllegalStateException())
            .when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)), Mockito.any());

      var utforarId = ImmutableIdtyp.builder()
            .typId(idtypTypId)
            .varde(idtypVarde)
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, null, RegelManuellTestStatus.PLANERAD);
      regelKafkaConnector.waitForRegelResponse();

      Mockito.verify(oulAdapter).endOperativUppgift(UUID.fromString(uppgiftId), "Internal error");
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_try_to_delete_cloud_event_data_on_write_failure_during_initial_storage_write(String handlaggningId)
         throws Exception
   {
      stubOulAdapter(UUID.fromString(handlaggningId));
      Mockito.doThrow(new IllegalStateException()).when(storage).setManuellRegelCommonData(eq(UUID.fromString(handlaggningId)),
            Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      Mockito.verify(cloudEventDataStorage, Mockito.timeout(5000)).deleteCloudEventData(eq(UUID.fromString(handlaggningId)));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, 11e53b18-e9ac-4707-825b-a1cb80689c29, Idtyp_typId, Idtyp_varde"
   })
   void should_try_to_delete_storage_data_when_get_handlaggning_fails_on_oul_status_update(
         String handlaggningId,
         String uppgiftId,
         String idtypTypId,
         String idtypVarde) throws Exception
   {
      var server = WireMockRegelManuell.getWireMockServer();
      var failureStub = server.stubFor(
            WireMock.get(WireMock.urlPathMatching("/handlaggning/.*" + handlaggningId + ".*"))
                  .atPriority(1)
                  .willReturn(WireMock.serverError()));
      try
      {
         var utforarId = ImmutableIdtyp.builder()
               .typId(idtypTypId)
               .varde(idtypVarde)
               .build();
         oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, null, RegelManuellTestStatus.PLANERAD);
         Mockito.verify(cloudEventDataStorage, Mockito.timeout(5000)).deleteCloudEventData(eq(UUID.fromString(handlaggningId)));
         Mockito.verify(storage, Mockito.timeout(5000)).deleteManuellRegelCommonData(eq(UUID.fromString(handlaggningId)));
      }
      finally
      {
         server.removeStub(failureStub);
      }
   }
}
