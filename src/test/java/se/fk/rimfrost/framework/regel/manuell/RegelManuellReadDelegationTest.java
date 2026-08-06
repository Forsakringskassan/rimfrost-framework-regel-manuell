package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellTestService;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies FRMM-FR-04.2: GET /{handlaggningId} delegates to readData() and returns its result.
 */
@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellReadDelegationTest
{
   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   SidAdapter sidAdapter;

   @InjectMock
   ManuellRegelCommonDataStorage dataStorage;

   @InjectMock
   RegelManuellTestService regelService;

   @Test
   @DisplayName("FRMM-FR-04.2: read() delegerar till readData() och returnerar regelimplementationens svar")
   void read_should_delegate_to_readData_and_return_its_result() throws Exception
   {
      var handlaggning = buildHandlaggning();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      when(regelService.readData(handlaggning)).thenReturn("expectedResult");
      givenSuccessfulDataStorage();

      var result = service.read(handlaggning.id());

      assertEquals("expectedResult", result);
      verify(regelService).readData(handlaggning);
   }

   @Test
   @DisplayName("FRMM-FR-04.3: read() skapar ett GetResponse-underlag och uppdaterar handläggningsärendet")
   void read_should_create_get_response_underlag_and_update_handlaggning() throws Exception
   {
      var handlaggning = buildHandlaggning();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      when(regelService.readData(handlaggning)).thenReturn("expectedResult");
      givenSuccessfulDataStorage();

      service.read(handlaggning.id());

      var captor = ArgumentCaptor.forClass(HandlaggningUpdate.class);
      verify(handlaggningAdapter).updateHandlaggning(captor.capture());
      var update = captor.getValue();
      assertEquals(2, update.version());
      assertEquals(1, update.underlag().size());
      assertEquals("GetResponse", update.underlag().getFirst().typ());
   }

   private Handlaggning buildHandlaggning()
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.individYrkandeRoller()).thenReturn(Collections.emptyList());
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(1);
      when(handlaggning.processInstansId()).thenReturn(UUID.randomUUID());
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   private void givenSuccessfulDataStorage()
   {
      var data = mock(ManuellRegelCommonData.class);
      when(data.uppgift()).thenReturn(mock(Uppgift.class));
      when(dataStorage.getManuellRegelCommonData(any())).thenReturn(data);
   }
}
