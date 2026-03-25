package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUnderlag;
import se.fk.rimfrost.framework.handlaggning.model.Underlag;
import se.fk.rimfrost.framework.regel.Utfall;

public abstract class RegelManuellServiceBase
{

   @Inject
   RegelManuellUppgiftDoneHandler uppgiftDoneHandler;

   @Inject
   protected ObjectMapper objectMapper;

   protected void sendRegelResponse(UUID handlaggningId, Utfall utfall)
   {
      uppgiftDoneHandler.handleUppgiftDone(handlaggningId, utfall);
   }

   protected Underlag createUnderlag(String typ, int version, Object object)
   {
      try
      {
         return ImmutableUnderlag.builder()
               .typ(typ)
               .version(version)
               .data(objectMapper.writeValueAsString(object))
               .build();
      }
      catch (JsonProcessingException e)
      {
         throw new InternalError("Could not parse object to String", e);
      }
   }

}
