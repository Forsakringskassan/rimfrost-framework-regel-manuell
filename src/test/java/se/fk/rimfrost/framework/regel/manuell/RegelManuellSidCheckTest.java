package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.fk.rimfrost.adapter.permissions.adapter.PermissionsAdapter;
import se.fk.rimfrost.adapter.permissions.adapter.PermissionsException;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.UppgiftSpecifikation;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.framework.regel.manuell.logic.identity.UserIdentityHolder;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import se.fk.rimfrost.framework.sid.exception.SidException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellSidCheckTest
{
   private static final String ID_TYP = "PERSONNR";
   private static final String ID_VARDE = "19901010-1234";

   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   SidAdapter sidAdapter;

   @InjectMock
   PermissionsAdapter permissionsAdapter;

   @InjectMock
   OulUppgiftService oulUppgiftService;

   @Inject
   UserIdentityHolder userIdentityHolder;

   @InjectMock
   RegelManuellServiceInterface<String, String> regelManuellService;

   @Test
   @DisplayName("FRMM-FR-08.3: HTTP 403 returneras och readData() anropas inte när SID detekteras och handläggaren saknar SID-rättigheter")
   void read_should_throw_forbidden_when_sid_detected() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();

      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(oulUppgiftId, ID_TYP, ID_VARDE);

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      assertTrue(ex.getMessage().matches("(?i).*does not have permission to access this uppgift.*"));
      verify(regelManuellService, never()).readData(any());
   }

   @Test
   @DisplayName("FRMM-FR-08.6: OUL-uppgiften unassignas innan HTTP 403 returneras när SID detekteras och handläggaren saknar SID-rättigheter")
   void read_should_unassign_uppgift_when_sid_detected() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(oulUppgiftId, ID_TYP, ID_VARDE);

      assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      verify(oulUppgiftService).tryUnassignOulUppgift(eq(oulUppgiftId));
      verify(regelManuellService, never()).readData(any());
   }

   @Test
   @DisplayName("FRMM-FR-08.1: Unassign utförs inte när ingen SID detekteras")
   void read_should_not_unassign_when_no_sid() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      when(regelManuellService.readData(handlaggning)).thenReturn("value");
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

      assertDoesNotThrow(() -> service.read(UUID.randomUUID()));

      verify(oulUppgiftService, never()).tryUnassignOulUppgift(any());
   }

   @Test
   @DisplayName("FRMM-FR-08.8: Fel vid unassign loggas men påverkar inte HTTP 403-svaret")
   void read_should_still_throw_forbidden_when_unassign_fails() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(oulUppgiftId, ID_TYP, ID_VARDE);
      doNothing().when(oulUppgiftService).tryUnassignOulUppgift(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      assertTrue(ex.getMessage().matches("(?i).*does not have permission to access this uppgift.*"));
      verify(regelManuellService, never()).readData(any());
   }

   @Test
   @DisplayName("FRMM-FR-08.7: Unassign hoppas över utan fel när inget uppgifts-ID finns lagrat")
   void read_should_skip_unassign_when_oul_uppgift_id_is_null() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(null, ID_TYP, ID_VARDE);

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      verify(oulUppgiftService, never()).tryUnassignOulUppgift(any());
      verify(regelManuellService, never()).readData(any());
   }

   @Test
   @DisplayName("FRMM-FR-08.1: Anropet fortsätter till readData() när ingen SID detekteras")
   void read_should_proceed_when_no_sid() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      when(regelManuellService.readData(handlaggning)).thenReturn("value");
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

      assertDoesNotThrow(() -> service.read(UUID.randomUUID()));
      verify(regelManuellService).readData(handlaggning);
   }

   @Test
   @DisplayName("FRMM-FR-08.9/08.10: readData() anropas normalt när SID detekteras men handläggaren har SID-rättigheter")
   void read_should_proceed_when_sid_detected_but_handlaggare_has_permission() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      when(permissionsAdapter.hasSidPermission(any(), any())).thenReturn(true);
      when(regelManuellService.readData(handlaggning)).thenReturn("value");
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(oulUppgiftId, ID_TYP, ID_VARDE);

      assertDoesNotThrow(() -> service.read(UUID.randomUUID()));

      verify(oulUppgiftService, never()).tryUnassignOulUppgift(any());
      verify(regelManuellService).readData(handlaggning);
   }

   @Test
   @DisplayName("FRMM-FR-08.9: hasSidPermission anropas med handläggarens identitet när SID detekteras")
   void read_should_check_permission_with_handlaggare_id_when_sid_detected() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      when(regelManuellService.readData(handlaggning)).thenReturn("value");
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(oulUppgiftId, ID_TYP, ID_VARDE);

      assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      verify(permissionsAdapter).hasSidPermission(eq("PERSONNR"), eq("19901010-1234"));
   }

   @ParameterizedTest
   @EnumSource(PermissionsException.ErrorType.class)
   @DisplayName("FRMM-FR-06.4: Fel från behörighetstjänsten mappas till väldefinierade HTTP-statuskoder")
   void read_should_throw_with_mapped_status_when_permissions_throws(PermissionsException.ErrorType errorType)
         throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(oulUppgiftId, ID_TYP, ID_VARDE);
      doThrow(new PermissionsException(errorType, "permissions error")).when(permissionsAdapter)
            .hasSidPermission(any(), any());

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(expectedStatusForPermissions(errorType), ex.getStatus());
   }

   @ParameterizedTest
   @EnumSource(SidException.ErrorType.class)
   @DisplayName("FRMM-FR-06.4: Fel från SID-tjänsten mappas till väldefinierade HTTP-statuskoder")
   void read_should_throw_with_mapped_status_when_sid_throws(SidException.ErrorType errorType)
         throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      doThrow(new SidException(errorType, "sid error")).when(sidAdapter).containsSid(any());
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(expectedStatus(errorType), ex.getStatus());
   }

   @Test
   @DisplayName("FRMM-FR-08.2: SID-kontrollen hanterar tom individlista i yrkande utan fel")
   void read_should_proceed_when_individYrkandeRoller_is_empty() throws Exception
   {
      var handlaggning = handlaggningWithoutIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      when(regelManuellService.readData(handlaggning)).thenReturn("value");
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

      assertDoesNotThrow(() -> service.read(UUID.randomUUID()));
   }

   private Handlaggning handlaggningWithIndivider() throws HandlaggningException
   {
      var individ = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(individ.typId()).thenReturn("some-type-id");
      when(individ.varde()).thenReturn("19901010-1234");
      var roll = mock(IndividYrkandeRoll.class);
      when(roll.individ()).thenReturn(individ);
      return buildHandlaggning(List.of(roll));
   }

   private Handlaggning handlaggningWithoutIndivider() throws HandlaggningException
   {
      return buildHandlaggning(Collections.emptyList());
   }

   private Handlaggning buildHandlaggning(List<IndividYrkandeRoll> roller)
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.individYrkandeRoller()).thenReturn(roller);
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(1);
      when(handlaggning.processInstansId()).thenReturn(UUID.randomUUID());
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   private Uppgift buildTilldeladUppgift(String typId, String varde)
   {
      se.fk.rimfrost.framework.handlaggning.model.Idtyp utforarId = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(utforarId.typId()).thenReturn(typId);
      when(utforarId.varde()).thenReturn(varde);

      var uppgift = buildUppgift();
      when(uppgift.utforarId()).thenReturn(utforarId);

      return uppgift;
   }

   private Uppgift buildUppgift()
   {
      var uppgiftSpecifikation = mock(UppgiftSpecifikation.class);

      var uppgift = mock(Uppgift.class);
      when(uppgift.id()).thenReturn(UUID.randomUUID());
      when(uppgift.version()).thenReturn(1);
      when(uppgift.skapadTs()).thenReturn(OffsetDateTime.now());
      when(uppgift.utfordTs()).thenReturn(null);
      when(uppgift.planeradTs()).thenReturn(null);
      when(uppgift.utforarId()).thenReturn(null);
      when(uppgift.uppgiftStatus()).thenReturn("Planerad");
      when(uppgift.aktivitetId()).thenReturn(UUID.randomUUID());
      when(uppgift.fSSAinformation()).thenReturn(UUID.randomUUID().toString());
      when(uppgift.uppgiftSpecifikation()).thenReturn(uppgiftSpecifikation);

      return uppgift;
   }

   private void givenCorrelationData(UUID oulUppgiftId, String typId, String varde)
   {
      var correlationData = mock(OulCorrelationData.class);
      var uppgift = buildTilldeladUppgift(typId, varde);
      when(correlationData.oulUppgiftId()).thenReturn(oulUppgiftId);
      when(correlationData.uppgift()).thenReturn(uppgift);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(correlationData);
   }

   private void givenHandlaggareIdentity(String idTyp, String idVarde)
   {
      userIdentityHolder.setIdentity(
            se.fk.rimfrost.framework.regel.manuell.logic.entity.ImmutableIdtyp.builder().typId(idTyp).varde(idVarde).build());
   }

   private static Response.Status expectedStatus(SidException.ErrorType errorType)
   {
      return switch (errorType)
      {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

   private static Response.Status expectedStatusForPermissions(PermissionsException.ErrorType errorType)
   {
      return switch (errorType)
      {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}
