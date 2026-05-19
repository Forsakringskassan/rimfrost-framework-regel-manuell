package se.fk.rimfrost.framework.regel.manuell.base;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.InjectMock;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.Status;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
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
      assertEquals(2, handlaggningPutUpdate.getHandlaggning().getVersion());
      assertEquals("1", handlaggningPutUpdate.getHandlaggning().getUppgift().getUppgiftStatus());
      assertEquals(1, handlaggningPutUpdate.getHandlaggning().getUppgift().getVersion());
   }
}
