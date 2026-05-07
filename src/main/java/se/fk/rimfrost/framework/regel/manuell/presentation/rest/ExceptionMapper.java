package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.ErrorResponse;

public class ExceptionMapper
{
   Logger logger = LoggerFactory.getLogger(ExceptionMapper.class);

   @ConfigProperty(name = "rimfrost.framework.regel.manuell.rest.exception.catchall.response.message", defaultValue = "Internal Server Error")
   String errorResponseMessage;

   @ServerExceptionMapper
   public Response toResponse(final Exception e)
   {
      logger.error("Request terminated due to unexpected exception", e);

      var errorResponse = new ErrorResponse();
      errorResponse.setCode(500);
      errorResponse.setMessage(errorResponseMessage);

      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
   }
}
