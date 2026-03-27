package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Inject;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableUnderlag;
import se.fk.rimfrost.framework.handlaggning.model.Underlag;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;

//Middleware that handles the read and update operations to handlaggningService.
//T is the GET operations response body and Y is the PATCH operations body.
public abstract class RegelManuellMiddlewareService<T, Y> implements RegelManuellMiddlewareServiceInterface<T, Y>
{

   @Inject
   RegelManuellServiceInterface<T, Y> regelService;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   ObjectMapper objectMapper;

   @Inject
   ManuellRegelCommonDataStorage dataStorage;

   @Override
   public T read(UUID handlaggningId)
   {
      var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
      var result = regelService.readData(handlaggning);
      var underlag = createUnderlag("GetResponse", 1, result);
      var handlaggningUpdate = createHandlaggningUpdate(handlaggning, underlag);
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

   private HandlaggningUpdate createHandlaggningUpdate(Handlaggning handlaggning, Underlag underlag)
   {
      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .processInstansId(handlaggning.processInstansId())
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .version(handlaggning.version() + 1)
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .yrkande(handlaggning.yrkande())
            .uppgift(getUppgift(handlaggning.id()))
            .addUnderlag(underlag)
            .build();
   }

   private Uppgift getUppgift(UUID handlaggningId)
   {
      return dataStorage.getManuellRegelCommonData(handlaggningId).uppgift();
   }

}
