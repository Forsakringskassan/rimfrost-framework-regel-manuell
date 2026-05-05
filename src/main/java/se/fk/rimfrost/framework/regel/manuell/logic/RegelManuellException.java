package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.ws.rs.core.Response.Status;

public class RegelManuellException extends RuntimeException
{
   private final Status status;

   public RegelManuellException(Status status, String message, Exception e)
   {
      super(message, e);
      this.status = status;
   }

   public RegelManuellException(Status status, String message)
   {
      super(message);
      this.status = status;
   }

   public Status getStatus()
   {
      return status;
   }
}
