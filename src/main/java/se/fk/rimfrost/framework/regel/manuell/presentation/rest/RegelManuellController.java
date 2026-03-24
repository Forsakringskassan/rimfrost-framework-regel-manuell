package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import java.net.http.HttpResponse;
import java.util.UUID;

import jakarta.enterprise.inject.Instance;
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
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;

@SuppressWarnings("unused")
public class RegelManuellController<T, Y> implements RegelManuellControllerApi
{
   @Inject
   RegelManuellServiceInterface<T, Y> regelService;

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
   public Response getData(UUID handlaggningId){
      var result = regelService.get(handlaggningId);
      return Response.ok(result).build();
   }

   @PATCH
   @Path("/{handlaggningId}")
   public Response patch(Y request){ 
      var result = regelService.patch(request);
      return Response.ok(result).build();
   }

   @POST
   @Path("/{handlaggningId}/done")
   @Override
   public void markDone(
   @PathParam("handlaggningId") UUID handlaggningId)
   {
      regelService.handleRegelDone(handlaggningId);
   }
}
