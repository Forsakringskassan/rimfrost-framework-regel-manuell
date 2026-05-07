package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.ErrorResponse;

public class WebApplicationExceptionMapper
{
   Logger logger = LoggerFactory.getLogger(WebApplicationExceptionMapper.class);

   @ServerExceptionMapper
   public Response toResponse(final WebApplicationException e)
   {
      logger.warn("Request terminated with WebApplicationException", e);

      int responseStatus = 500;
      String responseMessage = "Internal Server Error";

      var exceptionResponse = e.getResponse();
      if (exceptionResponse != null)
      {
         responseStatus = exceptionResponse.getStatus();
         responseMessage = exceptionResponse.getStatusInfo().getReasonPhrase();
      }

      var errorResponse = new ErrorResponse();
      errorResponse.setCode(responseStatus);
      errorResponse.setMessage(responseMessage);

      return Response.status(responseStatus).entity(errorResponse).build();
   }
}
