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
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.regel.manuell.logic.dto.Informationsaccess;
import se.fk.rimfrost.framework.regel.manuell.logic.dto.RegelManuellReadResponse;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
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
   SidAdapter sidAdapter;

   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   RegelConfigProvider regelConfigProvider;

   @Override
   public RegelManuellReadResponse<T> read(UUID handlaggningId)
   {
      var handlaggning = getHandlaggning(handlaggningId);
      if (checkSid(handlaggning))
      {
         unassignUppgift(handlaggningId);
         throw new RegelManuellException(Response.Status.FORBIDDEN, "Skyddad identitet");
      }
      var informationsaccess = buildInformationsaccess(handlaggning);
      var data = regelService.readData(handlaggning);
      return new RegelManuellReadResponse<>(data, informationsaccess);
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

   private Uppgift getUppgift(UUID handlaggningId)
   {
      return oulUppgiftService.getCorrelationData(handlaggningId).uppgift();
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

   /**
    * Delegates the handläggning update to {@link HandlaggningAdapter} and maps
    * {@link HandlaggningException} to an appropriate HTTP status.
    */
   private void updateHandlaggning(HandlaggningUpdate handlaggningUpdate)
   {
      try
      {
         handlaggningAdapter.updateHandlaggning(handlaggningUpdate);
      }
      catch (HandlaggningException e)
      {
         if (e.getErrorType() == HandlaggningException.ErrorType.CONFLICT)
         {
            LOGGER.error(
                  "Version conflict while attempting to update handlaggning with id: {}. Programming fault in regel service?",
                  handlaggningUpdate.handlaggning().id(), e);
            throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR,
                  "Version conflict while attempting to update handlaggning with id: " + handlaggningUpdate.handlaggning().id()
                        + ". Programming fault in regel service?",
                  e);
         }

         LOGGER.error("Error in update for handlaggningsId: {}", handlaggningUpdate.handlaggning().id(), e);
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
    * Delegates to {@link OulUppgiftService#tryUnassignOulUppgift}, which logs and swallows any
    * failure so it never affects the HTTP response (FRMM-FR-08.8).
    */
   private void unassignUppgift(UUID handlaggningId)
   {
      var commonData = oulUppgiftService.getCorrelationData(handlaggningId);
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
      oulUppgiftService.tryUnassignOulUppgift(oulUppgiftId);
   }

   /**
    * Builds the informationsaccess data returned alongside the rule-specific response (FRMM-FR-09).
    * Throws HTTP 500 when no correlation data is stored, since the uppgift cannot be identified
    * without it.
    */
   private Informationsaccess buildInformationsaccess(Handlaggning handlaggning)
   {
      var correlation = oulUppgiftService.getCorrelationData(handlaggning.id());
      if (correlation == null)
      {
         LOGGER.error("Correlation data not found for handlaggningId: {}", handlaggning.id());
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Correlation data not found");
      }
      var regel = regelConfigProvider.getConfig().getRegel();
      return new Informationsaccess(
            correlation.oulUppgiftId(),
            regel.getId(),
            String.valueOf(regel.getVersion()));
   }

   /**
    * Extracts individer from the handläggning's yrkande and maps them to the SID adapter's
    * {@link se.fk.rimfrost.framework.sid.model.Idtyp} type.
    */
   private List<se.fk.rimfrost.framework.sid.model.Idtyp> extractIndivider(Handlaggning handlaggning)
   {
      return handlaggning.yrkande().rollerIYrkande()
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
