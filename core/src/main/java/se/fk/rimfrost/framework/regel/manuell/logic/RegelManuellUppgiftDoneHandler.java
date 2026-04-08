package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.UUID;

import se.fk.rimfrost.framework.regel.Utfall;

public interface RegelManuellUppgiftDoneHandler
{
   void handleUppgiftDone(UUID handlaggningId, Utfall utfall);
}
