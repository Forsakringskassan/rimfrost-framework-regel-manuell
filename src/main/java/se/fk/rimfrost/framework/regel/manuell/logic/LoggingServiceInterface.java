package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

public interface LoggingServiceInterface<T, Y> {
    
   T read(UUID handlaggningId);

   void update(UUID handlaggningId, Y request);

   void done(UUID handlaggningId);

}
