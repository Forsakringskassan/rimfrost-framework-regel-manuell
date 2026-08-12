package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellTestService;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}
