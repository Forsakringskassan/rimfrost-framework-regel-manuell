package se.fk.rimfrost.framework.regel.manuell.base;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellUtokadUppgiftsbeskrivningTest extends AbstractRegelManuellTest
{

   @Test
   @DisplayName("FRMM-FR-02.2: Utökad uppgiftsbeskrivning returneras korrekt via GET /utokadUppgiftsbeskrivning")
   void should_return_correct_utokad_uppgiftsbeskrivning()
   {
      var response = sendGetUtokadUppgiftsbeskrivning();
      Assertions.assertEquals("TestUtokadUppgiftsbeskrivning", response.getBeskrivning());
   }

}
