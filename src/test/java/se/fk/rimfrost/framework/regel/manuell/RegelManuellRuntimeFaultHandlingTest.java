package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
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

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @Test
   @DisplayName("FRMM-FR-06.5: Felrespons skickas vid oväntad exception under initial uppdatering av handläggningsärende")
   public void should_send_error_response_on_unexpected_exception_during_initial_handlaggning_update() throws Exception
   {
      var handlaggningId = UUID.randomUUID();
      Mockito.doThrow(new RuntimeException()).when(handlaggningAdapter).updateHandlaggning(Mockito.any());
      regelKafkaConnector.sendRegelRequest(handlaggningId.toString(), responseTopic);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(Utfall.ERROR, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_OTHER, regelResponse.getData().getError().getFelkod());
      assertTrue(regelResponse.getData().getError().getFelmeddelande().matches("(?).*unexpected internal error.*"));
   }
}
