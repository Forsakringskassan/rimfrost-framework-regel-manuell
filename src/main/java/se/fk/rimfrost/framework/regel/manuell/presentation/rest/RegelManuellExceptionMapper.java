package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.ErrorResponse;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;

public class RegelManuellExceptionMapper
{

   @ServerExceptionMapper
   public Response toResponse(RegelManuellException exception)
   {
      var body = new ErrorResponse(
            exception.getHttpStatusCode(),
            exception.getMessage());

      return Response.status(exception.getHttpStatusCode())
            .entity(body)
            .type(MediaType.APPLICATION_JSON)
            .build();
   }
}
