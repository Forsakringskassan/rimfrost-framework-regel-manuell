package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.Utfall;

@SuppressWarnings("unused")
@ApplicationScoped
@DefaultBean
public class RegelManuellMiddlewareServiceTest extends RegelManuellMiddlewareService<String, String>
{

}
