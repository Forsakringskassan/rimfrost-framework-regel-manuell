package se.fk.rimfrost.framework.regel.manuell.logic;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import se.fk.rimfrost.framework.regel.RegelErrorInformation;

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
