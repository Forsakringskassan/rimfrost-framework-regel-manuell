package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.SkipInject;
import io.quarkus.arc.Arc;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import se.fk.rimfrost.adapter.permissions.adapter.PermissionsAdapter;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.RollIYrkande;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelAccessGuard;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.manuell.logic.identity.UserIdentityHolder;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.regel.oul.logic.exception.OulServiceException;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusComponentTest(
{
      RegelAccessGuard.class, UserIdentityHolder.class
})
public class RegelManuellAccessGuardTest
{
   private static final String ID_TYP = "PERSONNR";
   private static final String ID_VARDE = "19901010-1234";
   private static final String NOT_ASSIGNED_PATTERN = "(?i).*not assigned to uppgift.*";
   private static final String UNRESOLVED_IDENTITY_PATTERN = "(?i).*could not resolve user identity.*";

   @Inject
   RegelAccessGuard guard;

   @Inject
   UserIdentityHolder userIdentityHolder;

   @InjectMock
   OulUppgiftService oulUppgiftService;

   @InjectMock
   SidAdapter sidAdapter;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   PermissionsAdapter permissionsAdapter;

   @Test
   @DisplayName("FRMM-FR-10.2: Åtkomst beviljas när handläggaren är tilldelad uppgiften och ingen SID detekteras")
   void verifyAccess_should_grant_access_when_assigned_and_no_sid() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(sidAdapter.containsSid(any())).thenReturn(false);
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      var oulUppgiftId = UUID.randomUUID();
      givenCorrelationData(oulUppgiftId);
      givenOulUppgiftUtforare(oulUppgiftId, ID_TYP, ID_VARDE);

      assertDoesNotThrow(() -> guard.verifyAccess(handlaggning));
   }

   @Test
   @DisplayName("verifyAccess(UUID) läser handläggningen och verifierar åtkomst")
   void verifyAccess_should_read_handlaggning_when_called_with_id() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(handlaggning.id())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      var oulUppgiftId = UUID.randomUUID();
      givenCorrelationData(oulUppgiftId);
      givenOulUppgiftUtforare(oulUppgiftId, ID_TYP, ID_VARDE);

      assertDoesNotThrow(() -> guard.verifyAccess(handlaggning.id()));

      verify(handlaggningAdapter).readHandlaggning(handlaggning.id());
      verify(oulUppgiftService).getCorrelationData(handlaggning.id());
   }

   @Test
   @DisplayName("HTTP 500 returneras när korrelationsdata saknas")
   void verifyAccess_should_throw_internal_server_error_when_correlation_data_missing()
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(null);

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, ex.getStatus());
      verifyNoInteractions(sidAdapter, permissionsAdapter);
   }

   @Test
   @DisplayName("FRMM-FR-10.5: HTTP 401 returneras när request-kontexten inte är aktiv och identiteten inte kan läsas")
   void verifyAccess_should_throw_unauthorized_when_request_context_not_active()
   {
      var handlaggning = handlaggningWithIndivider();
      var requestContext = Arc.container().requestContext();
      requestContext.deactivate();
      try
      {
         var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

         assertEquals(Response.Status.UNAUTHORIZED, ex.getStatus());
         assertTrue(ex.getMessage().matches(UNRESOLVED_IDENTITY_PATTERN));
         verify(oulUppgiftService, never()).getCorrelationData(any());
      }
      finally
      {
         requestContext.activate();
      }
   }

   @Test
   @DisplayName("FRMM-FR-10.5: HTTP 401 returneras när ingen handläggaridentitet finns i UserIdentityHolder")
   void verifyAccess_should_throw_unauthorized_when_user_identity_missing()
   {
      var handlaggning = handlaggningWithIndivider();

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.UNAUTHORIZED, ex.getStatus());
      assertTrue(ex.getMessage().matches(UNRESOLVED_IDENTITY_PATTERN));
      verify(oulUppgiftService, never()).getCorrelationData(any());
   }

   @Test
   @DisplayName("FRMM-FR-10.1/10.2: HTTP 403 returneras när OUL-uppgiften saknar utförare")
   void verifyAccess_should_throw_forbidden_when_oul_uppgift_has_no_utforare() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      var oulUppgiftId = UUID.randomUUID();
      givenCorrelationData(oulUppgiftId);
      givenOulUppgiftWithoutUtforare(oulUppgiftId);

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      assertTrue(ex.getMessage().matches(NOT_ASSIGNED_PATTERN));
      verifyNoInteractions(sidAdapter, permissionsAdapter);
   }

   @ParameterizedTest
   @EnumSource(OulServiceException.ErrorType.class)
   @DisplayName("FRMM-FR-06.4: Fel från OUL vid hämtning av utförare mappas till väldefinierade HTTP-statuskoder")
   void verifyAccess_should_throw_with_mapped_status_when_get_utforare_throws(
         @SkipInject OulServiceException.ErrorType errorType)
         throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID());
      when(oulUppgiftService.getOulUppgiftUtforare(any())).thenThrow(new OulServiceException(errorType, "oul error"));

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(expectedStatus(errorType), ex.getStatus());
      verifyNoInteractions(sidAdapter, permissionsAdapter);
   }

   @ParameterizedTest
   @CsvSource(
   {
         "PERSONNR, 19901010-9999",
         "SAMORDNINGSNR, 19901010-1234"
   })
   @DisplayName("FRMM-FR-10.1/10.2: HTTP 403 returneras när uppgiften är tilldelad en annan handläggare (annat värde eller annan id-typ)")
   void verifyAccess_should_throw_forbidden_when_assigned_to_other_handlaggare(@SkipInject String utforarTypId,
         @SkipInject String utforarVarde) throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      var oulUppgiftId = UUID.randomUUID();
      givenCorrelationData(oulUppgiftId);
      givenOulUppgiftUtforare(oulUppgiftId, utforarTypId, utforarVarde);

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      assertTrue(ex.getMessage().matches(NOT_ASSIGNED_PATTERN));
   }

   @Test
   @DisplayName("FRMM-FR-10.4: SID-tjänsten anropas inte när handläggaren inte är tilldelad uppgiften")
   void verifyAccess_should_not_call_sid_adapter_when_not_assigned() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, "19700101-0000");
      var oulUppgiftId = UUID.randomUUID();
      givenCorrelationData(oulUppgiftId);
      givenOulUppgiftUtforare(oulUppgiftId, ID_TYP, ID_VARDE);

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      verify(sidAdapter, never()).containsSid(any());
   }

   @Test
   @DisplayName("FRMM-FR-08.2: Individer i yrkandet mappas till SID-adapterns Idtyp")
   void verifyAccess_should_pass_mapped_individer_to_sid_adapter() throws Exception
   {
      var handlaggning = buildHandlaggning(List.of(
            rollIYrkande("PERSONNR", "19800101-1111"),
            rollIYrkande("SAMORDNINGSNR", "19800161-2222")));
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      var oulUppgiftId = UUID.randomUUID();
      givenCorrelationData(oulUppgiftId);
      givenOulUppgiftUtforare(oulUppgiftId, ID_TYP, ID_VARDE);

      guard.verifyAccess(handlaggning);

      verify(sidAdapter).containsSid(List.of(
            sidIdtyp("PERSONNR", "19800101-1111"),
            sidIdtyp("SAMORDNINGSNR", "19800161-2222")));
   }

   private Handlaggning handlaggningWithIndivider()
   {
      return buildHandlaggning(List.of(rollIYrkande("some-type-id", "19800101-1111")));
   }

   private RollIYrkande rollIYrkande(String typId, String varde)
   {
      var individ = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(individ.typId()).thenReturn(typId);
      when(individ.varde()).thenReturn(varde);
      var roll = mock(RollIYrkande.class);
      when(roll.individ()).thenReturn(individ);
      return roll;
   }

   private static se.fk.rimfrost.framework.sid.model.Idtyp sidIdtyp(String typId, String varde)
   {
      return se.fk.rimfrost.framework.sid.model.ImmutableIdtyp.builder().typId(typId).varde(varde).build();
   }

   private Handlaggning buildHandlaggning(List<RollIYrkande> roller)
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.rollerIYrkande()).thenReturn(roller);
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   private void givenCorrelationData(UUID oulUppgiftId)
   {
      var correlationData = mock(OulCorrelationData.class);
      when(correlationData.oulUppgiftId()).thenReturn(oulUppgiftId);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(correlationData);
   }

   private void givenOulUppgiftUtforare(UUID oulUppgiftId, String typId, String varde) throws OulServiceException
   {
      when(oulUppgiftService.getOulUppgiftUtforare(oulUppgiftId)).thenReturn(Optional.of(
            se.fk.rimfrost.framework.regel.oul.logic.entity.ImmutableIdtyp.builder().typId(typId).varde(varde).build()));
   }

   private void givenOulUppgiftWithoutUtforare(UUID oulUppgiftId) throws OulServiceException
   {
      when(oulUppgiftService.getOulUppgiftUtforare(oulUppgiftId)).thenReturn(Optional.empty());
   }

   private void givenHandlaggareIdentity(String idTyp, String idVarde)
   {
      userIdentityHolder.setIdentity(ImmutableIdtyp.builder().typId(idTyp).varde(idVarde).build());
   }

   private static Response.Status expectedStatus(OulServiceException.ErrorType errorType)
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
