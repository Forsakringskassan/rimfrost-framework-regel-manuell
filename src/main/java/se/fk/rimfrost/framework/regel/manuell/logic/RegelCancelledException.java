package se.fk.rimfrost.framework.regel.manuell.logic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;

import java.util.UUID;

@SuppressFBWarnings(value =
{
      "EI_EXPOSE_REP"
})
public class RegelCancelledException extends RuntimeException
{
   private final RegelErrorInformation regelErrorInformation;
   private final UUID handlaggningId;
   private final CloudEventData cloudEventData;

   public RegelCancelledException(String message)
   {
      this(null, null, null, message);
   }

   public RegelCancelledException(UUID handlaggningId, CloudEventData cloudEventData, RegelErrorInformation regelErrorInformation,
         String message)
   {
      super(message);

      this.handlaggningId = handlaggningId;
      this.cloudEventData = cloudEventData;
      this.regelErrorInformation = regelErrorInformation;
   }

   public UUID getHandlaggningId()
   {
      return handlaggningId;
   }

   public CloudEventData getCloudEventData()
   {
      return cloudEventData;
   }

   public RegelErrorInformation getRegelErrorInformation()
   {
      return regelErrorInformation;
   }
}
