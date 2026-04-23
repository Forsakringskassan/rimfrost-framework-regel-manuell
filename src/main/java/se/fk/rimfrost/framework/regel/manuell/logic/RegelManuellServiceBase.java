package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import se.fk.rimfrost.framework.regel.Utfall;

@SuppressWarnings("unused")
public abstract class RegelManuellServiceBase
{

   @Inject
   RegelManuellUppgiftDoneHandler uppgiftDoneHandler;

   @Inject
   protected ObjectMapper objectMapper;

   @SuppressWarnings("SameParameterValue")
   protected void sendRegelResponse(UUID handlaggningId, Utfall utfall)
   {
      uppgiftDoneHandler.handleUppgiftDone(handlaggningId, utfall);
   }
}
