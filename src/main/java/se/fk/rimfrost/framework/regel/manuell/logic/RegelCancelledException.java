package se.fk.rimfrost.framework.regel.manuell.logic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
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

   public RegelCancelledException(RegelErrorInformation regelErrorInformation, String message, Throwable cause)
   {
      super(message, cause);

      this.regelErrorInformation = regelErrorInformation;
   }

   public RegelErrorInformation getRegelErrorInformation()
   {
      return regelErrorInformation;
   }
}
