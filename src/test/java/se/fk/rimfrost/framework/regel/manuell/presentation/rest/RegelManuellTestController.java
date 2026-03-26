package se.fk.rimfrost.framework.regel.manuell.presentation.rest;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Path("/regel/manuell")
@DefaultBean
public class RegelManuellTestController extends RegelManuellController<String, String>
{
}
