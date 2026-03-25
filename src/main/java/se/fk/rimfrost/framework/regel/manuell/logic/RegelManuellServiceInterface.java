package se.fk.rimfrost.framework.regel.manuell.logic;

import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.regel.Utfall;

import java.util.UUID;

public interface RegelManuellServiceInterface
{
   Utfall decideUtfall(HandlaggningUpdate handlaggningUpdate);

   void handleRegelDone(UUID handlaggningId);
}
