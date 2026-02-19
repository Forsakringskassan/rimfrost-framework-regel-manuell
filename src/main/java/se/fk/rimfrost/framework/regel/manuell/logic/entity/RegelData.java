package se.fk.rimfrost.framework.regel.manuell.logic.entity;

import jakarta.validation.constraints.NotNull;
import org.immutables.value.Value;
import se.fk.rimfrost.framework.regel.logic.entity.ErsattningData;
import se.fk.rimfrost.framework.regel.logic.entity.Underlag;
import se.fk.rimfrost.framework.regel.logic.entity.UppgiftData;

import java.util.List;

@Value.Immutable
public interface RegelData
{
   @NotNull
   UppgiftData uppgiftData();

   @NotNull
   List<ErsattningData> ersattningar();

   @NotNull
   List<Underlag> underlag();
}
