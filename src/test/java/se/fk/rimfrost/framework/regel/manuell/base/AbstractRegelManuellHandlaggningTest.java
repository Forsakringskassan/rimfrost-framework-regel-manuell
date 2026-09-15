package se.fk.rimfrost.framework.regel.manuell.base;

import com.github.tomakehurst.wiremock.http.RequestMethod;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellHandlaggningTest extends AbstractRegelManuellTest
{
   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-01.2: GET-anrop skickas mot handläggningstjänsten vid inkommande RegelDataRequest")
   void should_create_initial_handlaggning_request(String handlaggningId)
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var handlaggningRequests = WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
      Assertions.assertEquals(1, handlaggningRequests.size());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-05.2, FRMM-FR-05.6: Handläggningsärendet uppdateras med slutstatus AVSLUTAD och utfördTs vid avslutning")
   void should_put_handlaggning_with_uppgiftstatus_avslutad(String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var uppgift = se.fk.rimfrost.framework.regel.WireMockHandlaggning.getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(RegelManuellTestStatus.AVSLUTAD.name(), uppgift.getUppgiftStatus());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-05.6: Utföringstidsstämpel sätts på handläggningsärendet vid avslutning")
   void should_put_handlaggning_with_utford_ts(String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      waitForRegelRequestProcessed(handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var uppgift = se.fk.rimfrost.framework.regel.WireMockHandlaggning.getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertNotNull(uppgift.getUtfordTs());
   }
}
