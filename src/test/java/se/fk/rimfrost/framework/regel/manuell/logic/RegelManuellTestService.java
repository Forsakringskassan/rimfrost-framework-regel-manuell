package se.fk.rimfrost.framework.regel.manuell.logic;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;

import java.util.UUID;

@SuppressWarnings("unused")
@ApplicationScoped
@DefaultBean
public class RegelManuellTestService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<String, String>
{
   @Inject
   OulUppgiftService oulUppgiftService;

   @Override
   public String readData(Handlaggning handlaggning)
   {
      return "read";
   }

   private Utfall nextUtfall = Utfall.JA;

   /** Sets the utfall that will be sent on the next {@link #done} call. Defaults to {@link Utfall#JA}. */
   public void setNextUtfall(Utfall utfall)
   {
      this.nextUtfall = utfall;
   }

   /** Sends a regel response using the configured {@link #nextUtfall}, then resets it to {@link Utfall#JA}. */
   @Override
   public void done(UUID handlaggningId)
   {
      sendRegelResponse(handlaggningId, nextUtfall);
   }

   /** Builds a {@link HandlaggningUpdate} from current correlation data for test use. */
   @Override
   public HandlaggningUpdate updateData(Handlaggning handlaggning, String request)
   {
      return ImmutableHandlaggningUpdate.builder()
            .handlaggning(handlaggning)
            .uppgift(oulUppgiftService.getCorrelationData(handlaggning.id()).uppgift())
            .build();
   }

}
