package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellRuntimeFaultHandlingTest extends AbstractRegelManuellTest
{

   @Test
   @DisplayName("FRMM-FR-06.1: Felrespons skickas via Kafka vid oväntad exception under OUL-skapande")
   public void should_send_error_response_on_unexpected_exception_during_oul_create_uppgift() throws Exception
   {
      Mockito.doThrow(new RuntimeException()).when(oulUppgiftService).createOulUppgift(Mockito.any());
      regelKafkaConnector.sendRegelRequest(WireMockRegelManuell.DEFAULT_HANDLAGGNING_ID);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(Utfall.ERROR, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, regelResponse.getData().getError().getFelkod());
      assertTrue(regelResponse.getData().getError().getFelmeddelande().matches("(?i).*unexpected internal error.*"));
   }
}
