package se.fk.rimfrost.framework.regel.manuell.storage.entity;

import org.immutables.value.Value;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;

import javax.annotation.Nullable;
import java.util.UUID;

@Value.Immutable
public interface ManuellRegelCommonData
{
   CloudEventData cloudEventData();

   Handlaggning handlaggning();

   @Nullable
   UUID oulUppgiftId();
}
