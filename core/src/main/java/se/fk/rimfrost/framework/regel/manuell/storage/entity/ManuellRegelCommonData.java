package se.fk.rimfrost.framework.regel.manuell.storage.entity;

import org.immutables.value.Value;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import javax.annotation.Nullable;
import java.util.UUID;

@Value.Immutable
public interface ManuellRegelCommonData
{
   CloudEventData cloudEventData();

   Uppgift uppgift();

   @Nullable
   UUID oulUppgiftId();
}
