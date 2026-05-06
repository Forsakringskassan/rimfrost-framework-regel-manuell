package se.fk.rimfrost.framework.regel.manuell.storage.entity;

import org.immutables.value.Value;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import javax.annotation.Nullable;
import java.util.UUID;

@Value.Immutable
public interface ManuellRegelCommonData
{
   Uppgift uppgift();

   @Nullable
   UUID oulUppgiftId();
}
