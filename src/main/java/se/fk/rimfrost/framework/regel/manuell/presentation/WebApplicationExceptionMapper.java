package se.fk.github.manuellregelratttillforsakring.presentation.rest;

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
      logger.warn("Request terminated with WebbApplicationException", e);

      var errorResponse = new ErrorResponse();
      errorResponse.setCode(e.getResponse().getStatus());
      errorResponse.setMessage(e.getResponse() != null ? e.getResponse().getStatusInfo().getReasonPhrase() : null);

      return Response.status(e.getResponse().getStatus()).entity(errorResponse).build();
   }
}
