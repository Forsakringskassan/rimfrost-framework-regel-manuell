package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Underlag;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;

//Middleware that handles the read and update operations to handlaggningService.
//T is the GET operations response body and Y is the PATCH operations body.
@SuppressWarnings("unused")
public abstract class RegelManuellMiddlewareService<T, Y> implements RegelManuellMiddlewareServiceInterface<T, Y>
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelManuellMiddlewareService.class);

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
      var handlaggning = getHandlaggning(handlaggningId);
      var result = regelService.readData(handlaggning);
      var underlag = RegelUtils.createUnderlag("GetResponse", 1, result, objectMapper);
      var handlaggningUpdate = createHandlaggningUpdate(handlaggning, underlag);
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error updating handlaggning for handlaggningsId: " + handlaggningId, e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
      return result;
   }

   @Override
   public void update(UUID handlaggningId, Y request)
   {
      try
      {
         var handlaggning = handlaggningAdapter.readHandlaggning(handlaggningId);
         var handlaggningUpdate = regelService.updateData(handlaggning, request);
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error in update for handlaggningsId: " + handlaggningId, e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
   }

   @Override
   public void done(UUID handlaggningId)
   {
      regelService.done(handlaggningId);
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

   private Handlaggning getHandlaggning(UUID handlaggningId)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error reading handlaggning for handlaggningsId: " + handlaggningId, e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
   }

   private static Response.Status toHttpStatus(HandlaggningException e) {
      return switch (e.getErrorType()) {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}
