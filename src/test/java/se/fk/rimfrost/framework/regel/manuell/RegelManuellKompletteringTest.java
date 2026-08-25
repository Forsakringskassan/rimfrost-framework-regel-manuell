package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.KompletteringKontrollInterface;
import se.fk.rimfrost.framework.regel.logic.KompletteringOulHandler;
import se.fk.rimfrost.framework.regel.logic.dto.ImmutableKompletteringUnderlag;
import se.fk.rimfrost.framework.regel.logic.dto.KompletteringUnderlag;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellOulTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellKompletteringTest extends AbstractRegelManuellOulTest
{

   @ConfigProperty(name = "mp.messaging.outgoing.regel-responses.topic")
   String responseTopic;

   @InjectMock
   KompletteringKontrollInterface kompletteringKontroll;

   @InjectMock
   KompletteringOulHandler kompletteringOulHandler;

   private static List<KompletteringUnderlag> oneKomplettering()
   {
      return List.of(ImmutableKompletteringUnderlag.builder()
            .underlagTyp("TEST_TYP")
            .beskrivning("Test beskrivning")
            .build());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-09.1, FRMM-FR-09.2: kompletteringOulHandler.initiate() anropas och regel-OUL-uppgift skapas inte när komplettering krävs")
   void should_initiate_komplettering_and_not_create_oul_task_when_komplettering_required(String handlaggningId)
         throws Exception
   {
      Mockito.when(kompletteringKontroll.checkKomplettering(any())).thenReturn(oneKomplettering());

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);

      Mockito.verify(kompletteringOulHandler, Mockito.timeout(5000)).initiate(any(), any(), any(), any());
      Mockito.verify(oulAdapter, Mockito.never()).createOperativUppgift(any());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-09.3: Normalt regelflöde fortsätter och regel-OUL-uppgift skapas när checkKomplettering returnerar tom lista")
   void should_create_oul_task_when_no_komplettering_required(String handlaggningId) throws Exception
   {
      Mockito.when(kompletteringKontroll.checkKomplettering(any())).thenReturn(List.of());

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);

      Mockito.verify(oulAdapter, Mockito.timeout(5000)).createOperativUppgift(any());
      Mockito.verify(kompletteringOulHandler, Mockito.never()).initiate(any(), any(), any(), any());
   }

   @ParameterizedTest
   @CsvSource(
   {
         "5367f6b8-cc4a-11f0-8de9-199901011234"
   })
   @DisplayName("FRMM-FR-09.4: Felrespons skickas via Kafka och ingen OUL-uppgift lämnas öppen när initiate() kastar OulException")
   void should_send_error_response_when_komplettering_initiate_fails(String handlaggningId) throws Exception
   {
      Mockito.when(kompletteringKontroll.checkKomplettering(any())).thenReturn(oneKomplettering());
      Mockito.doThrow(new OulException(OulException.ErrorType.SERVICE_UNAVAILABLE, "OUL down"))
            .when(kompletteringOulHandler).initiate(any(), any(), any(), any());

      regelKafkaConnector.sendRegelRequest(handlaggningId, responseTopic);

      var regelResponse = regelKafkaConnector.waitForRegelResponse();
      assertEquals(Utfall.ERROR, regelResponse.getData().getUtfall());
   }

}
