package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.WireMockHandlaggning;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellHandlaggningTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellTestService;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellHandlaggningTest extends AbstractRegelManuellHandlaggningTest
{
   @Inject
   RegelManuellTestService regelManuellTestService;

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String regelResponseTopic;

   @ParameterizedTest
   @EnumSource(value = Utfall.class, names =
   {
         "JA", "NEJ"
   })
   @DisplayName("FRMM-FR-05.7: regelutfall sparas i handläggningsärendet vid POST /done")
   void should_put_handlaggning_with_regelutfall(Utfall utfall) throws Exception
   {
      var handlaggningId = "5367f6b8-cc4a-11f0-8de9-199901011234";
      regelManuellTestService.setNextUtfall(utfall);
      regelKafkaConnector.sendRegelRequest(handlaggningId, regelResponseTopic);
      waitForRegelRequestProcessed(handlaggningId);
      sendPostRegelManuellHandlaggningDone(handlaggningId);
      var uppgift = WireMockHandlaggning.getUppgiftFromLastPutHandlaggning(handlaggningId);
      Assertions.assertEquals(utfall.getValue(), uppgift.getRegelutfall().getVarde());
   }
}
