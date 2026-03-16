package se.fk.rimfrost.framework.regel.manuell.logic.entity;

import jakarta.validation.constraints.NotNull;
import org.immutables.value.Value;
import se.fk.rimfrost.framework.regel.logic.entity.ErsattningData;
import se.fk.rimfrost.framework.regel.logic.entity.Underlag;
import se.fk.rimfrost.framework.regel.logic.entity.UppgiftData;

import java.util.List;
import java.util.UUID;

@Value.Immutable
public interface RegelData
{
   UUID aktivitetId();

   @NotNull
   UppgiftData uppgiftData();

   @NotNull
   List<ErsattningData> ersattningar();

   @NotNull
   List<Underlag> underlag();
}
