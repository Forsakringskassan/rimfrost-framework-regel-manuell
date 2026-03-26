package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUnderlag;
import se.fk.rimfrost.framework.handlaggning.model.Underlag;

public abstract class LoggingService<T, Y> implements LoggingServiceInterface<T, Y>
{

   @Inject
   RegelManuellServiceInterface<T, Y> regelService;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   HandlaggningMapper handlaggningMapper;

   @Inject
   ObjectMapper objectMapper;

   @Override
   public T read(UUID handlaggningId)
   {
      var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
      var result = regelService.readData(handlaggning);
      var underlag = createUnderlag("GetResponse", 1, result);
      var handlaggningUpdate = handlaggningMapper.toHandlaggningUpdate(handlaggning, underlag);
      handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      return result;
   }

   @Override
   public void update(UUID handlaggningId, Y request)
   {
      var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
      var handlaggningUpdate = regelService.updateData(handlaggning, request);
      handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
   }

   @Override
   public void done(UUID handlaggningId)
   {
      regelService.done(handlaggningId);
   }

   private Underlag createUnderlag(String typ, int version, Object object)
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
