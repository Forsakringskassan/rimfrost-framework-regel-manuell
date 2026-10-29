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
import se.fk.rimfrost.adapter.permissions.adapter.PermissionsAdapter;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelAccessGuard;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.manuell.logic.identity.UserIdentityHolder;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import java.util.List;
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
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

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
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

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
   @DisplayName("FRMM-FR-10.1/10.2: HTTP 403 returneras när korrelationsdata saknar uppgift")
   void verifyAccess_should_throw_forbidden_when_uppgift_is_null()
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID(), null);

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      assertTrue(ex.getMessage().matches(NOT_ASSIGNED_PATTERN));
   }

   @Test
   @DisplayName("FRMM-FR-10.1/10.2: HTTP 403 returneras när uppgiften saknar utförare")
   void verifyAccess_should_throw_forbidden_when_utforare_is_null()
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      var uppgift = mock(Uppgift.class);
      when(uppgift.utforarId()).thenReturn(null);
      givenCorrelationData(UUID.randomUUID(), uppgift);

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      assertTrue(ex.getMessage().matches(NOT_ASSIGNED_PATTERN));
   }

   @ParameterizedTest
   @CsvSource(
   {
         "PERSONNR, 19901010-9999",
         "SAMORDNINGSNR, 19901010-1234"
   })
   @DisplayName("FRMM-FR-10.1/10.2: HTTP 403 returneras när uppgiften är tilldelad en annan handläggare (annat värde eller annan id-typ)")
   void verifyAccess_should_throw_forbidden_when_assigned_to_other_handlaggare(@SkipInject String utforarTypId,
         @SkipInject String utforarVarde)
   {
      var handlaggning = handlaggningWithIndivider();
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID(), utforarTypId, utforarVarde);

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
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

      var ex = assertThrows(RegelManuellException.class, () -> guard.verifyAccess(handlaggning));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      verify(sidAdapter, never()).containsSid(any());
   }

   @Test
   @DisplayName("FRMM-FR-08.2: Individer i yrkandet mappas till SID-adapterns Idtyp")
   void verifyAccess_should_pass_mapped_individer_to_sid_adapter() throws Exception
   {
      var handlaggning = buildHandlaggning(List.of(
            individYrkandeRoll("PERSONNR", "19800101-1111"),
            individYrkandeRoll("SAMORDNINGSNR", "19800161-2222")));
      givenHandlaggareIdentity(ID_TYP, ID_VARDE);
      givenCorrelationData(UUID.randomUUID(), ID_TYP, ID_VARDE);

      guard.verifyAccess(handlaggning);

      verify(sidAdapter).containsSid(List.of(
            sidIdtyp("PERSONNR", "19800101-1111"),
            sidIdtyp("SAMORDNINGSNR", "19800161-2222")));
   }

   private Handlaggning handlaggningWithIndivider()
   {
      return buildHandlaggning(List.of(individYrkandeRoll("some-type-id", "19800101-1111")));
   }

   private IndividYrkandeRoll individYrkandeRoll(String typId, String varde)
   {
      var individ = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(individ.typId()).thenReturn(typId);
      when(individ.varde()).thenReturn(varde);
      var roll = mock(IndividYrkandeRoll.class);
      when(roll.individ()).thenReturn(individ);
      return roll;
   }

   private static se.fk.rimfrost.framework.sid.model.Idtyp sidIdtyp(String typId, String varde)
   {
      return se.fk.rimfrost.framework.sid.model.ImmutableIdtyp.builder().typId(typId).varde(varde).build();
   }

   private Handlaggning buildHandlaggning(List<IndividYrkandeRoll> roller)
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.individYrkandeRoller()).thenReturn(roller);
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   private Uppgift buildTilldeladUppgift(String typId, String varde)
   {
      var utforarId = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(utforarId.typId()).thenReturn(typId);
      when(utforarId.varde()).thenReturn(varde);
      var uppgift = mock(Uppgift.class);
      when(uppgift.utforarId()).thenReturn(utforarId);
      return uppgift;
   }

   private void givenCorrelationData(UUID oulUppgiftId, String typId, String varde)
   {
      givenCorrelationData(oulUppgiftId, buildTilldeladUppgift(typId, varde));
   }

   private void givenCorrelationData(UUID oulUppgiftId, Uppgift uppgift)
   {
      var correlationData = mock(OulCorrelationData.class);
      when(correlationData.oulUppgiftId()).thenReturn(oulUppgiftId);
      when(correlationData.uppgift()).thenReturn(uppgift);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(correlationData);
   }

   private void givenHandlaggareIdentity(String idTyp, String idVarde)
   {
      userIdentityHolder.setIdentity(ImmutableIdtyp.builder().typId(idTyp).varde(idVarde).build());
   }
}
