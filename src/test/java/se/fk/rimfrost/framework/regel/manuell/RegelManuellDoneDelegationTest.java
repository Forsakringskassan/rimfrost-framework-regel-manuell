package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellTestService;
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

   @Test
   @DisplayName("FRMM-FR-04.6: done() delegerar avslutningslogiken till regelimplementationens done()-metod")
   void done_should_delegate_to_regel_service_done()
   {
      var handlaggningId = UUID.randomUUID();

      service.done(handlaggningId);

      verify(regelService).done(handlaggningId);
   }
}
