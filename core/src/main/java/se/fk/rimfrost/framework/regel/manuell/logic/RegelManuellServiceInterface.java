package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;

public interface RegelManuellServiceInterface<T, Y>
{
   T readData(Handlaggning handlaggning);

   HandlaggningUpdate updateData(Handlaggning handlaggning, Y request);

   void done(UUID handlaggningId);
}
