package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.error.RegelFelkod;
import se.fk.rimfrost.framework.regel.logic.RegelCancelledException;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellFaultHandlingTest extends AbstractRegelManuellTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901014444, ERROR"
   })
   @DisplayName("FRMM-FR-06.1: Felrespons skickas via Kafka när läsning av handläggningsärende misslyckas vid start")
   void should_send_error_response_on_initial_handlaggning_read_failure(String handlaggningId, Utfall expectedUtfall)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_HANDLAGGNING_READ_FAILURE, regelResponse.getData().getError().getFelkod());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234, ERROR"
   })
   @DisplayName("FRMM-FR-06.1: Felrespons skickas via Kafka när uppdatering av handläggningsärende misslyckas vid OUL-skapande")
   void should_send_error_response_on_initial_handlaggning_write_failure(String handlaggningId, Utfall expectedUtfall)
         throws Exception
   {
      var info = new RegelErrorInformation();
      info.setFelkod(RegelFelkod.RIMFROST_HANDLAGGNING_WRITE_FAILURE);
      info.setFelmeddelande("Handlaggning update failed");
      Mockito.when(oulUppgiftService.createOulUppgift(any()))
            .thenThrow(new RegelCancelledException(info, "Handlaggning update failed", null));

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
      assertEquals(RegelFelkod.RIMFROST_HANDLAGGNING_WRITE_FAILURE, regelResponse.getData().getError().getFelkod());
   }
}
