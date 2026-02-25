package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.RegelData;

import java.util.UUID;

@ApplicationScoped
public class RegelManuellTestService implements RegelManuellServiceInterface
{
   @Override
   public Utfall decideUtfall(RegelData regelData)
   {
      return Utfall.JA;
   }

   @Override
   public void handleRegelDone(UUID kundbehovsflodeId)
   {
      // noop
   }
}
