package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import java.util.UUID;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.RegelManuellControllerApi;

@SuppressWarnings("unused")
public class RegelManuellController implements RegelManuellControllerApi
{
   @Inject
   Instance<RegelManuellUppgiftDoneHandler> regelManuellUppgiftDoneHandler;

   @POST
   @Path("/{kundbehovsflodeId}/done")
   @Override
   public void markDone(
         @PathParam("kundbehovsflodeId") UUID kundbehovsflodeId)
   {
      regelManuellUppgiftDoneHandler.get().handleUppgiftDone(kundbehovsflodeId);
   }
}
