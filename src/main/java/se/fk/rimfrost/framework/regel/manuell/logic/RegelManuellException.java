package se.fk.rimfrost.framework.regel.manuell.logic;

public class RegelManuellException extends RuntimeException
{
   private final int httpStatusCode;

   public RegelManuellException(int httpStatusCode, String message, Exception e)
   {
      super(message, e);
      this.httpStatusCode = httpStatusCode;
   }

   public RegelManuellException(int httpStatusCode, String message)
   {
      super(message);
      this.httpStatusCode = httpStatusCode;
   }

   public int getHttpStatusCode()
   {
      return httpStatusCode;
   }
}
