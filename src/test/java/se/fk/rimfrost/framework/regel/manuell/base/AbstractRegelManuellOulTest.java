package se.fk.rimfrost.framework.regel.manuell.base;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.InjectMock;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.regel.RegelTestData;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellOulTest extends AbstractRegelManuellTest
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
   void should_create_correct_oul_request(String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var oulRequestCaptor = ArgumentCaptor.forClass(CreateOperativUppgiftRequest.class);
      Mockito.verify(oulAdapter, Mockito.timeout(5000)).createOperativUppgift(oulRequestCaptor.capture());
      var oulRequest = oulRequestCaptor.getValue();
      Assertions.assertEquals(handlaggningId, oulRequest.getHandlaggningId().toString());
      Assertions.assertEquals("TestUppgiftBeskrivning", oulRequest.getBeskrivning());
      Assertions.assertEquals("TestUppgiftNamn", oulRequest.getRegel());
      Assertions.assertEquals("C", oulRequest.getVerksamhetslogik());
      Assertions.assertEquals("ANSVARIG_HANDLAGGARE", oulRequest.getRoll());
      Assertions.assertTrue(oulRequest.getUrl().contains(basePath()));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   void should_include_cloudevent_attributes_in_oul_request(String handlaggningId) throws Exception
   {
      var testRequest = RegelTestData.newRegelRequestMessagePayload(handlaggningId);
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      var oulRequestCaptor = ArgumentCaptor.forClass(CreateOperativUppgiftRequest.class);
      Mockito.verify(oulAdapter, Mockito.timeout(5000)).createOperativUppgift(oulRequestCaptor.capture());
      var attributes = oulRequestCaptor.getValue().getCloudeventAttributes();
      Assertions.assertNotNull(attributes);
      Assertions.assertEquals(testRequest.getId(), attributes.get("id"));
      Assertions.assertEquals(testRequest.getKogitoprocinstanceid(), attributes.get("kogitoprocinstanceid"));
      Assertions.assertEquals(testRequest.getKogitorootprociid(), attributes.get("kogitorootprociid"));
      Assertions.assertEquals(testRequest.getKogitoparentprociid(), attributes.get("kogitoparentprociid"));
      Assertions.assertEquals(testRequest.getKogitorootprocid(), attributes.get("kogitorootprocid"));
      Assertions.assertEquals(testRequest.getKogitoprocid(), attributes.get("kogitoprocid"));
      Assertions.assertEquals(testRequest.getKogitoprocist(), attributes.get("kogitoprocist"));
      Assertions.assertEquals(testRequest.getKogitoprocversion(), attributes.get("kogitoprocversion"));
      Assertions.assertNotNull(attributes.get("type"));
      Assertions.assertNotNull(attributes.get("source"));
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
      oulKafkaConnector.simulateOulStatus(handlaggningId, uppgiftId, utforarId, uppgiftStatusProvider.getPlaneradId());
      //
      // verify PUT handlaggning
      //
      var handlaggningPutUpdate = WireMockRegelManuell.getLastPutHandlaggning(handlaggningId);
      assertEquals(handlaggningId, handlaggningPutUpdate.getHandlaggning().getId().toString());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals(uppgiftStatusProvider.getPlaneradId(),
            handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getUppgift().getVersion());
   }
}
