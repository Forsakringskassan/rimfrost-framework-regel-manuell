package se.fk.rimfrost.framework.regel.manuell.base;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.RegelTestData;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellOulTest extends AbstractRegelManuellTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-02.2, FRMM-FR-02.3: OUL-uppgiften skapas med korrekt regelnamn, beskrivning, affärslogik, roll och URL")
   void should_create_correct_oul_request(String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var captor = ArgumentCaptor.forClass(OulUppgiftSpec.class);
      Mockito.verify(oulUppgiftService, Mockito.timeout(5000)).createOulUppgift(captor.capture());
      var spec = captor.getValue();
      Assertions.assertEquals(handlaggningId, spec.handlaggningId().toString());
      Assertions.assertEquals("TestUppgiftBeskrivning", spec.beskrivning());
      Assertions.assertEquals("TestUppgiftNamn", spec.regel());
      Assertions.assertEquals("C", spec.verksamhetslogik());
      Assertions.assertEquals("ANSVARIG_HANDLAGGARE", spec.roll());
      Assertions.assertTrue(spec.url().contains(basePath()));
      Assertions.assertNotNull(spec.erbjudande());
      Assertions.assertNotNull(spec.erbjudande().getId());
      Assertions.assertNotNull(spec.erbjudande().getNamn());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-02.5: OUL-skapandeanropet innehåller rätt replyTo-topic från regelförfrågan")
   void should_include_reply_topic_in_oul_spec(String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var captor = ArgumentCaptor.forClass(OulUppgiftSpec.class);
      Mockito.verify(oulUppgiftService, Mockito.timeout(5000)).createOulUppgift(captor.capture());
      assertEquals(responseTopic, captor.getValue().replyTo());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-02.4: CloudEvent-attributen från regelförfrågan inkluderas i OUL-skapandeanropet")
   void should_include_cloudevent_attributes_in_oul_request(String handlaggningId) throws Exception
   {
      var testRequest = RegelTestData.newRegelRequestMessagePayload(handlaggningId, responseTopic);
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var captor = ArgumentCaptor.forClass(OulUppgiftSpec.class);
      Mockito.verify(oulUppgiftService, Mockito.timeout(5000)).createOulUppgift(captor.capture());
      var attributes = captor.getValue().cloudEventAttributes();
      Assertions.assertNotNull(attributes);
      Assertions.assertEquals(testRequest.getId(), attributes.get("id"));
      Assertions.assertEquals(testRequest.getKogitoprocinstanceid(), attributes.get("kogitoprocinstanceid"));
      Assertions.assertEquals(testRequest.getKogitorootprociid(), attributes.get("kogitorootprociid"));
      Assertions.assertEquals(testRequest.getKogitoparentprociid(), attributes.get("kogitoparentprociid"));
      Assertions.assertEquals(testRequest.getKogitorootprocid(), attributes.get("kogitorootprocid"));
      Assertions.assertEquals(testRequest.getKogitoprocid(), attributes.get("kogitoprocid"));
      Assertions.assertEquals(testRequest.getKogitoprocist(), attributes.get("kogitoprocist"));
      Assertions.assertEquals(testRequest.getKogitoprocversion(), attributes.get("kogitoprocversion"));
      Assertions.assertNotNull(attributes.get("type"));
      Assertions.assertNotNull(attributes.get("source"));
   }
}
