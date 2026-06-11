package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.oul.model.CreateOperativUppgiftRequest;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellOulTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

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
   public void should_send_correct_erbjudande_values_with_oul_create_request(String handlaggningId) throws Exception
   {
      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);
      var oulRequestCaptor = ArgumentCaptor.forClass(CreateOperativUppgiftRequest.class);
      Mockito.verify(oulAdapter, Mockito.timeout(5000)).createOperativUppgift(oulRequestCaptor.capture());
      var oulRequest = oulRequestCaptor.getValue();
      Assertions.assertEquals("f35c574d-e2a3-42ac-9ccb-835a24e692fe", oulRequest.getErbjudande().getId());
      Assertions.assertEquals("Test", oulRequest.getErbjudande().getNamn());
   }
}
