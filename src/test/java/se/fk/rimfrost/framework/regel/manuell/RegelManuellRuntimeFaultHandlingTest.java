package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgift;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUppgiftSpecifikation;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.framework.oul.model.ImmutableOperativUppgift;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.base.RegelManuellTestStatus;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableManuellRegelCommonData;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellRuntimeFaultHandlingTest extends AbstractRegelManuellTest
{
   private static ManuellRegelCommonData manuellRegelCommonDataStorage;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   OulAdapter oulAdapter;

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
            .uppgiftStatus(RegelManuellTestStatus.PLANERAD.name())
            .fSSAinformation("FSSAinformation.HANDLAGGNING_PAGAR")
            .uppgiftSpecifikation(uppgiftSpecification)
            .build();

      manuellRegelCommonDataStorage = ImmutableManuellRegelCommonData.builder()
            .uppgift(uppgift)
            .build();
   }

   private void stubOulAdapter(UUID handlaggningId) throws Exception
   {
      Mockito.when(oulAdapter.createOperativUppgift(any())).thenReturn(
            ImmutableOperativUppgift.builder()
                  .uppgiftId(UUID.randomUUID())
                  .handlaggningId(handlaggningId)
                  .status("NY")
                  .build());
   }

   @Test
   public void should_send_error_response_on_unexpected_exception_during_initial_handlaggning_update() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      stubOulAdapter(handlaggningId);
      Mockito.doThrow(new RuntimeException()).when(handlaggningAdapter).updateHandlaggning(Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId.toString());
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(Utfall.ERROR, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, regelResponse.getData().getError().getFelkod());
      assertTrue(regelResponse.getData().getError().getFelmeddelande().matches("(?).*unexpected internal error.*"));
   }

   @Test
   public void should_send_error_response_on_unexpected_exception_during_oul_status_update() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      var uppgiftId = UUID.randomUUID();
      Mockito.when(storage.getManuellRegelCommonData(eq(handlaggningId)))
            .thenReturn(manuellRegelCommonDataStorage);
      Mockito.doThrow(new RuntimeException()).when(handlaggningAdapter).updateHandlaggning(Mockito.any());
      var utforarId = ImmutableIdtyp.builder()
            .typId("Idtyp_typId")
            .varde("Idtyp_varde")
            .build();
      oulKafkaConnector.simulateOulStatus(handlaggningId.toString(), uppgiftId.toString(), utforarId,
            null, RegelManuellTestStatus.PLANERAD);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(Utfall.ERROR, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, regelResponse.getData().getError().getFelkod());
      assertTrue(regelResponse.getData().getError().getFelmeddelande().matches("(?).*unexpected internal error.*"));
   }
}
