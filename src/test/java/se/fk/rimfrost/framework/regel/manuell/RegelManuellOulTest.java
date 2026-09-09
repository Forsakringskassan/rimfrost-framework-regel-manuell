package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.ErbjudandeReferensdataTestService;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellOulTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulUppgiftSpec;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellOulTest extends AbstractRegelManuellOulTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-01.3, FRMM-FR-02.1: Erbjudandenamn slås upp från referensdata och inkluderas i OUL-skapandeanropet")
   public void should_send_correct_erbjudande_values_with_oul_create_request(String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var captor = ArgumentCaptor.forClass(OulUppgiftSpec.class);
      Mockito.verify(oulUppgiftService, Mockito.timeout(5000)).createOulUppgift(captor.capture());
      var spec = captor.getValue();
      Assertions.assertEquals("f35c574d-e2a3-42ac-9ccb-835a24e692fe", spec.erbjudande().getId());
      Assertions.assertEquals(ErbjudandeReferensdataTestService.DEFAULT_ERBJUDANDE_NAMN,
            spec.erbjudande().getNamn());
   }
}
