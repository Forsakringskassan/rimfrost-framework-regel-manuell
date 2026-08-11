package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.List;
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
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import se.fk.rimfrost.framework.sid.exception.SidException;
import se.fk.rimfrost.framework.sid.model.ImmutableIdtyp;

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

   @Inject
   SidAdapter sidAdapter;

   @Inject
   OulAdapter oulAdapter;

   @Override
   public T read(UUID handlaggningId)
   {
      var handlaggning = getHandlaggning(handlaggningId);
      if (checkSid(handlaggning))
      {
         unassignUppgift(handlaggningId);
         throw new RegelManuellException(Response.Status.FORBIDDEN, "Skyddad identitet");
      }
      var result = regelService.readData(handlaggning);
      var underlag = RegelUtils.createUnderlag("GetResponse", 1, result, objectMapper);
      var handlaggningUpdate = createHandlaggningUpdate(handlaggning, underlag);
      updateHandlaggning(handlaggningUpdate);
      return result;
   }

   @Override
   public void update(UUID handlaggningId, Y request)
   {
      var handlaggning = getHandlaggning(handlaggningId);
      var handlaggningUpdate = regelService.updateData(handlaggning, request);
      updateHandlaggning(handlaggningUpdate);
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

   private void updateHandlaggning(HandlaggningUpdate handlaggningUpdate)
   {
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error in update for handlaggningsId: {}", handlaggningUpdate.id(), e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
   }

   /**
    * Checks whether any individ on the handläggning has a protected identity (SID).
    * Returns {@code true} if SID is detected, {@code false} otherwise.
    * Maps {@link SidException} to an appropriate HTTP status on service errors.
    */
   private boolean checkSid(Handlaggning handlaggning)
   {
      try
      {
         return sidAdapter.containsSid(extractIndivider(handlaggning));
      }
      catch (SidException e)
      {
         LOGGER.error("Error checking SID for handlaggningsId: {}", handlaggning.id(), e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
   }

   /**
    * Unassigns the OUL uppgift for the given handläggning so it returns to an unassigned state.
    * Best-effort: errors are logged and swallowed so they never affect the HTTP response.
    */
   private void unassignUppgift(UUID handlaggningId)
   {
      var commonData = dataStorage.getManuellRegelCommonData(handlaggningId);
      if (commonData == null)
      {
         LOGGER.warn("No common data found for handlaggningId: {}, skipping unassign", handlaggningId);
         return;
      }
      var oulUppgiftId = commonData.oulUppgiftId();
      if (oulUppgiftId == null)
      {
         LOGGER.warn("No oulUppgiftId found for handlaggningId: {}, skipping unassign", handlaggningId);
         return;
      }
      try
      {
         oulAdapter.unassignOperativUppgift(oulUppgiftId);
      }
      catch (Exception e)
      {
         LOGGER.error("Failed to unassign uppgift {} for handlaggningId: {}", oulUppgiftId, handlaggningId, e);
      }
   }

   /**
    * Extracts individer from the handläggning's yrkande and maps them to the SID adapter's
    * {@link se.fk.rimfrost.framework.sid.model.Idtyp} type.
    */
   private List<se.fk.rimfrost.framework.sid.model.Idtyp> extractIndivider(Handlaggning handlaggning)
   {
      return handlaggning.yrkande().individYrkandeRoller()
            .stream().<se.fk.rimfrost.framework.sid.model.Idtyp> map(roll -> ImmutableIdtyp.builder()
                  .typId(roll.individ().typId())
                  .varde(roll.individ().varde())
                  .build())
            .toList();
   }

   private static Response.Status toHttpStatus(HandlaggningException e) {
      return switch (e.getErrorType()) {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

   private static Response.Status toHttpStatus(SidException e) {
      return switch (e.getErrorType()) {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}
