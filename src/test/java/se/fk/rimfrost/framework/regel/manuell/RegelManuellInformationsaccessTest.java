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
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellTestService;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies FRMM-FR-09: GET /{handlaggningId} returns informationsaccess data alongside the
 * rule-specific response.
 */
@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellInformationsaccessTest
{
   // Values from src/test/resources/config-test.yaml
   private static final UUID REGEL_ID = UUID.fromString("a11f3429-3a6b-4389-ba1c-c2747b0fb45b");
   private static final String REGEL_VERSION = "1";

   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   SidAdapter sidAdapter;

   @InjectMock
   OulUppgiftService oulUppgiftService;

   @InjectMock
   RegelManuellTestService regelService;

   @Test
   @DisplayName("FRMM-FR-09.1: read() returnerar regelimplementationens svar i data")
   void read_should_return_rule_response_as_data() throws Exception
   {
      var handlaggning = givenHandlaggning();
      givenCorrelationData(UUID.randomUUID());
      when(regelService.readData(handlaggning)).thenReturn("expectedResult");

      var result = service.read(handlaggning.id());

      assertEquals("expectedResult", result.data());
   }

   @Test
   @DisplayName("FRMM-FR-09.2: uppgiftId är OUL-uppgiftens id från korrelationsdatan")
   void read_should_return_oul_uppgift_id() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = givenHandlaggning();
      givenCorrelationData(oulUppgiftId);

      var result = service.read(handlaggning.id());

      assertEquals(oulUppgiftId, result.informationsaccess().uppgiftId());
   }

   @Test
   @DisplayName("FRMM-FR-09.2: uppgiftId är null när ingen korrelationsdata finns")
   void read_should_return_null_uppgift_id_without_correlation_data() throws Exception
   {
      var handlaggning = givenHandlaggning();
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(null);

      var result = service.read(handlaggning.id());

      assertNull(result.informationsaccess().uppgiftId());
   }

   @Test
   @DisplayName("FRMM-FR-09.3: regelId och regelVersion hämtas från regelns config.yaml")
   void read_should_return_regel_id_and_version_from_config() throws Exception
   {
      var handlaggning = givenHandlaggning();
      givenCorrelationData(UUID.randomUUID());

      var result = service.read(handlaggning.id());

      assertEquals(REGEL_ID, result.informationsaccess().regelId());
      assertEquals(REGEL_VERSION, result.informationsaccess().regelVersion());
   }

   @Test
   @DisplayName("FRMM-FR-09.4: containern returneras inte och readData() anropas inte när SID-träff detekteras")
   void read_should_not_return_container_when_sid_detected() throws Exception
   {
      var handlaggning = givenHandlaggning();
      givenCorrelationData(UUID.randomUUID());
      when(sidAdapter.containsSid(any())).thenReturn(true);

      assertThrows(RegelManuellException.class, () -> service.read(handlaggning.id()));
      verify(regelService, never()).readData(any());
   }

   private Handlaggning givenHandlaggning() throws Exception
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.rollerIYrkande()).thenReturn(Collections.emptyList());
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(1);
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      return handlaggning;
   }

   private void givenCorrelationData(UUID oulUppgiftId)
   {
      var correlationData = mock(OulCorrelationData.class);
      when(correlationData.uppgift()).thenReturn(mock(Uppgift.class));
      when(correlationData.oulUppgiftId()).thenReturn(oulUppgiftId);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(correlationData);
   }
}
