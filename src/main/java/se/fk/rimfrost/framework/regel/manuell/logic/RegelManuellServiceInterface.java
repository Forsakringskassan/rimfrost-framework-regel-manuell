package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

public interface RegelManuellServiceInterface<T, Y>
{
   T get(UUID handlaggningId);

   T patch(Y request);

   void handleRegelDone(UUID handlaggningId);
}
