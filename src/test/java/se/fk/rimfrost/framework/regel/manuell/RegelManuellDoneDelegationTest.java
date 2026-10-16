package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Verifies FRMM-FR-04.6: POST /{handlaggningId}/done delegates to the rule implementation's done().
 */
@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellDoneDelegationTest
{
   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   RegelManuellTestService regelService;

   @InjectMock
   RegelAccessGuard regelAccessGuard;

   @Test
   @DisplayName("FRMM-FR-04.6: done() delegerar avslutningslogiken till regelimplementationens done()-metod")
   void done_should_delegate_to_regel_service_done()
   {
      var handlaggningId = UUID.randomUUID();

      service.done(handlaggningId);

      verify(regelService).done(handlaggningId);
   }

   @Test
   @DisplayName("FRMM-FR-09.1: done() delegeras inte när åtkomstkontrollen nekar")
   void done_should_not_delegate_when_access_denied()
   {
      var handlaggningId = UUID.randomUUID();
      doThrow(new RegelManuellException(Response.Status.FORBIDDEN, "denied"))
            .when(regelAccessGuard).verifyAccess(handlaggningId);

      var ex = assertThrows(RegelManuellException.class, () -> service.done(handlaggningId));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      verify(regelService, never()).done(any());
   }
}
