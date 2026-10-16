package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.ContextNotActiveException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.adapter.permissions.adapter.PermissionsAdapter;
import se.fk.rimfrost.adapter.permissions.adapter.PermissionsException;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.regel.manuell.logic.identity.UserIdentityHolder;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import se.fk.rimfrost.framework.sid.exception.SidException;
import se.fk.rimfrost.framework.sid.model.Idtyp;
import se.fk.rimfrost.framework.sid.model.ImmutableIdtyp;

import java.util.List;
import java.util.UUID;

/**
 * Public utility class for verifying that a user has access to an uppgift.
 * The class verifies when verifyAccess is called that the user stored in
 * {@link UserIdentityHolder} is assigned to the uppgift and has the correct
 * SID permissions for accessing the uppgift.
 */
@ApplicationScoped
public class RegelAccessGuard
{
   private static final Logger LOGGER = LoggerFactory.getLogger(RegelAccessGuard.class);

   @Inject
   OulUppgiftService oulUppgiftService;

   @Inject
   SidAdapter sidAdapter;

   @Inject
   HandlaggningAdapter handlaggningAdapter;

   @Inject
   PermissionsAdapter permissionsAdapter;

   @Inject
   UserIdentityHolder userIdentityHolder;

   /**
    * Verifies that the user has access to the uppgift associated with the provided handläggning.
    * User identity is fetched from {@link UserIdentityHolder} and uppgift is fetched from correlation
    * data. If the user identity cannot be resolved or the user does not have access to the uppgift
    * a RegelManuellException will be thrown.
    * @param handlaggningId The id of the handläggning the uppgift is associated with
    */
   public void verifyAccess(UUID handlaggningId)
   {
      var handlaggning = getHandlaggning(handlaggningId);
      verifyAccess(handlaggning);
   }

   /**
    * Verifies that the user has access to the uppgift associated with the provided handläggning.
    * User identity is fetched from {@link UserIdentityHolder} and uppgift is fetched from correlation
    * data. If the user identity cannot be resolved or the user does not have access to the uppgift
    * a RegelManuellException will be thrown.
    * @param handlaggning The handläggning the uppgift is associated with
    */
   public void verifyAccess(Handlaggning handlaggning)
   {
      var handlaggareId = resolveUserIdentity();

      if (handlaggareId == null)
      {
         LOGGER.warn("Could not resolve user identity");
         throw new RegelManuellException(Response.Status.UNAUTHORIZED, "Could not resolve user identity");
      }

      var correlationData = getCorrelationData(handlaggning.id());
      var uppgift = correlationData.uppgift();

      if (uppgift == null || !isAssignedToUppgift(uppgift.utforarId(), handlaggareId))
      {
         LOGGER.info("Handläggare is not assigned to uppgift for handläggning with id: {}", handlaggning.id());
         throw new RegelManuellException(Response.Status.FORBIDDEN, "Handläggare is not assigned to uppgift");
      }

      if (checkSid(handlaggning) && !hasSidPermission(handlaggareId))
      {
         unassignUppgift(correlationData, handlaggning.id());
         throw new RegelManuellException(Response.Status.FORBIDDEN,
               "Handläggare does not have permission to access this uppgift");
      }
   }

   private OulCorrelationData getCorrelationData(UUID handlaggningId)
   {
      var correlationData = oulUppgiftService.getCorrelationData(handlaggningId);

      if (correlationData == null)
      {
         LOGGER.error("Correlation data not found for handlaggningId: {}", handlaggningId);
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR, "Correlation data not found");
      }

      return correlationData;
   }

   private Handlaggning getHandlaggning(UUID handlaggningId)
   {
      try
      {
         return handlaggningAdapter.readHandlaggning(handlaggningId);
      }
      catch (HandlaggningException e)
      {
         LOGGER.error("Error reading handläggning for id: {}", handlaggningId, e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
   }

   /**
    * Resolves the inloggad handläggare's identity via {@link UserIdentityHolder}.
    * @return The user identity stored in {@link UserIdentityHolder} or null if the holder is not available.
    */
   private se.fk.rimfrost.framework.regel.manuell.logic.entity.Idtyp resolveUserIdentity()
   {
      try
      {
         return userIdentityHolder.getIdentity();
      }
      catch (ContextNotActiveException e)
      {
         LOGGER.warn("UserIdentityHolder is not available due to context not being active. Falling back to no user identity", e);
         return null;
      }
   }

   private boolean isAssignedToUppgift(se.fk.rimfrost.framework.handlaggning.model.Idtyp utforarId,
         se.fk.rimfrost.framework.regel.manuell.logic.entity.Idtyp handlaggareId)
   {
      return utforarId != null && handlaggareId != null && utforarId.typId().equals(handlaggareId.typId())
            && utforarId.varde().equals(handlaggareId.varde());
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
         LOGGER.error("Error checking SID for handläggning id: {}", handlaggning.id(), e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
   }

   /**
    * Extracts individer from the handläggning's yrkande and maps them to the SID adapter's
    * {@link se.fk.rimfrost.framework.sid.model.Idtyp} type.
    */
   private List<Idtyp> extractIndivider(Handlaggning handlaggning)
   {
      return handlaggning.yrkande().individYrkandeRoller()
            .stream().<se.fk.rimfrost.framework.sid.model.Idtyp> map(roll -> ImmutableIdtyp.builder()
                  .typId(roll.individ().typId())
                  .varde(roll.individ().varde())
                  .build())
            .toList();
   }

   /**
    * Checks whether the provided handläggare has SID-behörighet (FRMM-FR-08.9).
    * Returns {@code true} if the handläggare has permission, {@code false} otherwise.
    * Maps {@link PermissionsException} to an appropriate HTTP status on service errors.
    */
   private boolean hasSidPermission(se.fk.rimfrost.framework.regel.manuell.logic.entity.Idtyp handlaggareId)
   {
      try
      {
         return permissionsAdapter.hasSidPermission(handlaggareId.typId(), handlaggareId.varde());
      }
      catch (PermissionsException e)
      {
         LOGGER.error("Error checking SID permission for idTyp: {}", handlaggareId.typId(), e);
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
   }

   /**
    * Unassigns the OUL uppgift for the given handläggning so it returns to an unassigned state.
    * Delegates to {@link OulUppgiftService#tryUnassignOulUppgift}, which logs and swallows any
    * failure so it never affects the HTTP response (FRMM-FR-08.8).
    */
   private void unassignUppgift(OulCorrelationData correlationData, UUID handlaggningId)
   {
      var oulUppgiftId = correlationData.oulUppgiftId();
      if (oulUppgiftId == null)
      {
         LOGGER.warn("No oulUppgiftId found in correlation data for handläggning with id: {}. Skipping unassign", handlaggningId);
         return;
      }
      oulUppgiftService.tryUnassignOulUppgift(oulUppgiftId);
   }

   private static Response.Status toHttpStatus(PermissionsException e)
   {
      return externalServiceErrorToHttpStatus(e.getErrorType().name());
   }

   private static Response.Status toHttpStatus(SidException e)
   {
      return externalServiceErrorToHttpStatus(e.getErrorType().name());
   }

   private static Response.Status toHttpStatus(HandlaggningException e)
   {
      return externalServiceErrorToHttpStatus(e.getErrorType().name());
   }

   private static Response.Status externalServiceErrorToHttpStatus(String errorType)
   {
      return switch(errorType){case"NOT_FOUND"->Response.Status.NOT_FOUND;case"BAD_REQUEST"->Response.Status.BAD_REQUEST;case"SERVICE_UNAVAILABLE"->Response.Status.SERVICE_UNAVAILABLE;default->Response.Status.INTERNAL_SERVER_ERROR;};
   }
}
