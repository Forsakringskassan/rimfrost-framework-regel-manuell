package se.fk.rimfrost.framework.regel.manuell.base;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.Utfall;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellResponseTest extends AbstractRegelManuellTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "JA, 5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-05.1, FRMM-FR-05.3: Korrekt utfall (JA) skickas i RegelResponse till replyTo-topic vid avslutning")
   void should_return_correct_regel_response_utfall(Utfall expectedUtfall, String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      Assertions.assertEquals(expectedUtfall, regelResponse.getData().getUtfall());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-05.3: Handläggnings-ID inkluderas i RegelResponse till replyTo-topic")
   void should_return_correct_regel_response_handlaggning_id(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      Assertions.assertEquals(handlaggningId, regelResponse.getData().getHandlaggningId());
   }

}
