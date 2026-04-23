package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import java.util.UUID;

@SuppressWarnings("unused")
public class AbstractMockedRegelManuellTest extends AbstractRegelManuellTest
{

   @InjectMock
   protected RegelManuellServiceInterface<?, ?> regelManuellService;

   protected RegelManuellServiceInterface<?, ?> getRegelManuellService()
   {
      return regelManuellService;
   }

   protected void mockRegelService(Utfall utfall, String handlaggningId)
   {
      Mockito.doNothing().when(regelManuellService)
            .done(UUID.fromString(handlaggningId));
   }
}
