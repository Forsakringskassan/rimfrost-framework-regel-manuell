package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockTestResource.class)
})
public class RegelManuellUtokadUppgiftsbeskrivningTest extends AbstractRegelManuellTest
{

   @BeforeEach
   void setup()
   {
      resetState();
   }

   @Test
   void should_return_correct_utokad_uppgiftsbeskrivning()
   {
      verifyUtokadUppgiftsbeskrivningContent();
   }

}
