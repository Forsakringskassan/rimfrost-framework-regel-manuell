package se.fk.rimfrost.framework.regel.manuell.base;

import se.fk.rimfrost.framework.oul.logic.dto.ImmutableIdtyp;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Idtyp;

public class RegelManuellTestData
{

   public static Idtyp newHandlaggningApiIdtyp()
   {
      var idtyp = new Idtyp();
      idtyp.setTypId("Idtyp_typId");
      idtyp.setVarde("Idtyp_varde");
      return idtyp;
   }

   public static se.fk.rimfrost.framework.oul.logic.dto.Idtyp newHandlaggningIdtyp()
   {
      return ImmutableIdtyp.builder()
            .typId("Idtyp_typId")
            .varde("Idtyp_varde")
            .build();
   }

}
