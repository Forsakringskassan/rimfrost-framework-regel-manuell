package se.fk.rimfrost.framework.regel.manuell.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.Utfall;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellResponseTest extends AbstractRegelManuellTest
{
   @ParameterizedTest
   @CsvSource(
   {
         "JA, 5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-05.1, FRMM-FR-05.3: Korrekt utfall (JA) skickas i RegelResponse till response topic vid avslutning")
   void should_return_correct_regel_response_utfall(Utfall expectedUtfall, String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
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
   @DisplayName("FRMM-FR-05.3: Handläggnings-ID inkluderas i RegelResponse till response topic")
   void should_return_correct_regel_response_handlaggning_id(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId);
      waitForRegelRequestProcessed(handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      Assertions.assertEquals(handlaggningId, regelResponse.getData().getHandlaggningId());
   }

}
