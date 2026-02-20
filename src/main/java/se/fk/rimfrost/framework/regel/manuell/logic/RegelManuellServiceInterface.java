package se.fk.rimfrost.framework.regel.manuell.logic;

import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.RegelData;

import java.util.UUID;

public interface RegelManuellServiceInterface
{
   Utfall decideUtfall(RegelData regelData);

   void handleRegelDone(UUID kundbehovsflodeId);
}
