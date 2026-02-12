package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import java.util.UUID;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import se.fk.rimfrost.framework.regel.integration.config.RegelConfigProvider;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.RegelManuellControllerApi;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;

@SuppressWarnings("unused")
public class RegelManuellController implements RegelManuellControllerApi
{
   @Inject
   Instance<RegelManuellUppgiftDoneHandler> regelManuellUppgiftDoneHandler;

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

   @POST
   @Path("/{kundbehovsflodeId}/done")
   @Override
   public void markDone(
         @PathParam("kundbehovsflodeId") UUID kundbehovsflodeId)
   {
      regelManuellUppgiftDoneHandler.get().handleUppgiftDone(kundbehovsflodeId);
   }
}
