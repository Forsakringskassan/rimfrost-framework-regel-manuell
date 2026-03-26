package se.fk.rimfrost.framework.regel.manuell.logic;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.Underlag;

@ApplicationScoped
public class HandlaggningMapper
{

   public HandlaggningUpdate toHandlaggningUpdate(Handlaggning handlaggning, Underlag underlag)
   {

      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .processInstansId(handlaggning.processInstansId())
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .version(handlaggning.version() + 1)
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .yrkande(handlaggning.yrkande())
            .addUnderlag(underlag)
            .build();

   }
}
