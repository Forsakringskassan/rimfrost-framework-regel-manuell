package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelAccessGuard;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellTestService;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies FRMM-FR-04.4: PATCH /{handlaggningId} delegates to updateData() and forwards its result.
 */
@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellUpdateDelegationTest
{
   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   RegelManuellTestService regelService;

   @InjectMock
   RegelAccessGuard regelAccessGuard;

   @Test
   @DisplayName("FRMM-FR-04.4: update() delegerar till updateData() med korrekt handlaggning och förfråganstyp Y")
   void update_should_delegate_to_updateData_with_correct_arguments() throws Exception
   {
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      var handlaggningUpdate = mock(HandlaggningUpdate.class);
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(regelService.updateData(handlaggning, "testRequest")).thenReturn(handlaggningUpdate);

      service.update(handlaggning.id(), "testRequest");

      verify(regelService).updateData(handlaggning, "testRequest");
      verify(handlaggningAdapter).updateHandlaggning(handlaggningUpdate);
   }

   @Test
   @DisplayName("FRMM-FR-10.1: Varken updateData() eller updateHandlaggning() anropas när åtkomstkontrollen nekar")
   void update_should_not_update_when_access_denied() throws Exception
   {
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      doThrow(new RegelManuellException(Response.Status.FORBIDDEN, "denied"))
            .when(regelAccessGuard).verifyAccess(handlaggning);

      var ex = assertThrows(RegelManuellException.class, () -> service.update(handlaggning.id(), "testRequest"));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      verify(regelService, never()).updateData(any(), any());
      verify(handlaggningAdapter, never()).updateHandlaggning(any());
   }
}
