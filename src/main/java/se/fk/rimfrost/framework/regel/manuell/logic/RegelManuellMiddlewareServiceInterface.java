package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

//Middleware that handles the read and update operations to handlaggningService.
//T is the GET operations response body and Y is the PATCH operations body.
public interface RegelManuellMiddlewareServiceInterface<T, Y>
{

   T read(UUID handlaggningId);

   void update(UUID handlaggningId, Y request);

   void done(UUID handlaggningId);

}
