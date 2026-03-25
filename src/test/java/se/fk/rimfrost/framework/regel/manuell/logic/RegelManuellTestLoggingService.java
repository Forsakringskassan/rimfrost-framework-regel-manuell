package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@DefaultBean
public class RegelManuellTestLoggingService implements LoggingServiceInterface<String, String> {

    @Override
    public String read(UUID handlaggningId) {
        return "read";
    }

    @Override
    public void update(UUID handlaggningId, String request) {
      //update
    }

    @Override
    public void done(UUID handlaggningId) {
       //done
    }


}
