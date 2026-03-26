package se.fk.rimfrost.framework.regel.manuell.logic;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;

import java.util.UUID;

@ApplicationScoped
@DefaultBean
public class RegelManuellTestService implements RegelManuellServiceInterface<String, String>
{

   @Override
   public String readData(Handlaggning handlaggning)
   {
      return "";
   }

   @Override
   public HandlaggningUpdate updateData(Handlaggning handlaggning, String request)
   {
      return ImmutableHandlaggningUpdate.builder().build();
   }

   @Override
   public void done(UUID handlaggningId)
   {
      //nope
   }
}
