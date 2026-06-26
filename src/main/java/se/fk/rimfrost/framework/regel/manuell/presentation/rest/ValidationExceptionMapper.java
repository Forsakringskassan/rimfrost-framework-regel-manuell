package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.ErrorResponse;

public class ValidationExceptionMapper
{
   Logger logger = LoggerFactory.getLogger(ValidationExceptionMapper.class);

   @ServerExceptionMapper
   public Response toResponse(ValidationException exception)
   {
      logger.warn("Request terminated with ValidationException", exception);

      var errorResponse = new ErrorResponse();
      errorResponse.setCode(400);
      errorResponse.setMessage("Bad Request");

      return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).type(MediaType.APPLICATION_JSON).build();
   }
}
