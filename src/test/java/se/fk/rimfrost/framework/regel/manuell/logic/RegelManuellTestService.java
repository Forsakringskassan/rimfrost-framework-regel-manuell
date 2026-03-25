package se.fk.rimfrost.framework.regel.manuell.logic;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.Utfall;

import java.util.UUID;

@ApplicationScoped
@DefaultBean
public class RegelManuellTestService implements RegelManuellServiceInterface
{
   @Override
   public Utfall decideUtfall(HandlaggningUpdate handlaggningUpdate)
   {
      return Utfall.JA;
   }

   @Override
   public void handleRegelDone(UUID handlaggningId)
   {
      // noop
   }
}
