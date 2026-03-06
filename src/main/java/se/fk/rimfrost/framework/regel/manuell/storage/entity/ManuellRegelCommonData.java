package se.fk.rimfrost.framework.regel.manuell.storage.entity;

import org.immutables.value.Value;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.manuell.logic.entity.RegelData;

@Value.Immutable
public interface ManuellRegelCommonData
{
   CloudEventData cloudEventData();

   RegelData regelData();
}
