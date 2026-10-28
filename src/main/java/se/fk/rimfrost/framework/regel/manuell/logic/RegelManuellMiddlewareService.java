package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.regel.manuell.logic.dto.Informationsaccess;
import se.fk.rimfrost.framework.regel.manuell.logic.dto.RegelManuellReadResponse;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;

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
   RegelAccessGuard regelAccessGuard;

   @Inject
   RegelConfigProvider regelConfigProvider;

   @Inject
   OulUppgiftService oulUppgiftService;

   @Override
   public RegelManuellReadResponse<T> read(UUID handlaggningId)
   {
      var handlaggning = getHandlaggning(handlaggningId);
      regelAccessGuard.verifyAccess(handlaggning);
      var informationsaccess = buildInformationsaccess(handlaggning);
      var data = regelService.readData(handlaggning);
      return new RegelManuellReadResponse<>(data, informationsaccess);
   }

   @Override
   public void update(UUID handlaggningId, Y request)
   {
      var handlaggning = getHandlaggning(handlaggningId);
      regelAccessGuard.verifyAccess(handlaggning);
      var handlaggningUpdate = regelService.updateData(handlaggning, request);
      updateHandlaggning(handlaggningUpdate);
   }

   @Override
   public void done(UUID handlaggningId)
   {
      regelAccessGuard.verifyAccess(handlaggningId);
      regelService.done(handlaggningId);
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
         if (e.getErrorType() == HandlaggningException.ErrorType.CONFLICT)
         {
            LOGGER.error(
                  "Version conflict while attempting to update handlaggning with id: {}. Programming fault in regel service?",
                  handlaggningUpdate.id(), e);
            throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR,
                  "Version conflict while attempting to update handlaggning with id: " + handlaggningUpdate.id()
                        + ". Programming fault in regel service?",
                  e);
         }

         LOGGER.error("Error in update for handlaggningsId: {}", handlaggningUpdate.id(), e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
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

   private static Response.Status toHttpStatus(HandlaggningException e) {
      return switch (e.getErrorType()) {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }
}
