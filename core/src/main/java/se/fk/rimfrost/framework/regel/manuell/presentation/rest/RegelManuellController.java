package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.RegelManuellControllerApi;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceInterface;

@SuppressWarnings("unused")
public abstract class RegelManuellController<T, Y> implements RegelManuellControllerApi
{
   @Inject
   RegelManuellMiddlewareServiceInterface<T, Y> regelManuellMiddlewareService;

   @Inject
   RegelConfigProvider regelConfigProvider;

   @GET
   @Path("/utokadUppgiftsbeskrivning")
   @Override
   public GetUtokadUppgiftsbeskrivningResponse getUtokadUppgiftsbeskrivning()
   {
      var regelConfig = regelConfigProvider.getConfig();

      GetUtokadUppgiftsbeskrivningResponse response = new GetUtokadUppgiftsbeskrivningResponse();
      response.setBeskrivning(regelConfig.getUtokadUppgiftsbeskrivning().getBeskrivning());

      return response;
   }

   @GET
   @Path("/{handlaggningId}")
   public Response getData(UUID handlaggningId)
   {
      var result = regelManuellMiddlewareService.read(handlaggningId);
      return Response.ok(result).build();
   }

   @PATCH
   @Path("/{handlaggningId}")
   public void patch(@PathParam("handlaggningId") UUID handlaggningId, Y request)
   {
      regelManuellMiddlewareService.update(handlaggningId, request);
   }

   @POST
   @Path("/{handlaggningId}/done")
   @Override
   public void markDone(
         @PathParam("handlaggningId") UUID handlaggningId)
   {
      regelManuellMiddlewareService.done(handlaggningId);
   }
}
