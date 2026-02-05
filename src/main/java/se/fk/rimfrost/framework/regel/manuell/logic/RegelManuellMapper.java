package se.fk.rimfrost.framework.regel.manuell.logic;

import se.fk.rimfrost.framework.regel.integration.kundbehovsflode.dto.*;
import se.fk.rimfrost.framework.regel.logic.config.RegelConfig;
import se.fk.rimfrost.framework.regel.logic.dto.Beslutsutfall;
import se.fk.rimfrost.framework.regel.logic.entity.ErsattningData;
import se.fk.rimfrost.framework.regel.logic.entity.RegelData;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Ersattning;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Roll;
import se.fk.rimfrost.jaxrsspec.controllers.generatedsource.model.Verksamhetslogik;
import java.time.ZoneOffset;
import java.util.ArrayList;

public class RegelManuellMapper
{

   public UpdateKundbehovsflodeRequest toUpdateKundbehovsflodeRequest(RegelData regelData,
         RegelConfig regelConfig)
   {

      var lagrum = ImmutableUpdateKundbehovsflodeLagrum.builder()
            .id(regelConfig.getLagrum().getId())
            .version(regelConfig.getLagrum().getVersion())
            .forfattning(regelConfig.getLagrum().getForfattning())
            .giltigFrom(regelConfig.getLagrum().getGiltigFom().toInstant().atOffset(ZoneOffset.UTC))
            .kapitel(regelConfig.getLagrum().getKapitel())
            .paragraf(regelConfig.getLagrum().getParagraf())
            .stycke(regelConfig.getLagrum().getStycke())
            .punkt(regelConfig.getLagrum().getPunkt())
            .build();

      var regel = ImmutableUpdateKundbehovsflodeRegel.builder()
            .id(regelConfig.getRegel().getId())
            .beskrivning(regelConfig.getRegel().getBeskrivning())
            .namn(regelConfig.getRegel().getNamn())
            .version(regelConfig.getRegel().getVersion())
            .lagrum(lagrum)
            .build();

      var specifikation = ImmutableUpdateKundbehovsflodeSpecifikation.builder()
            .id(regelConfig.getSpecifikation().getId())
            .version(regelConfig.getSpecifikation().getVersion())
            .namn(regelConfig.getSpecifikation().getNamn())
            .uppgiftsbeskrivning(regelConfig.getSpecifikation().getUppgiftbeskrivning())
            .verksamhetslogik(Verksamhetslogik.fromString(regelConfig.getSpecifikation().getVerksamhetslogik()))
            .roll(Roll.fromString(regelConfig.getSpecifikation().getRoll()))
            .applikationsId(regelConfig.getSpecifikation().getApplikationsId())
            .applikationsversion(regelConfig.getSpecifikation().getApplikationsversion())
            .url(regelConfig.getUppgift().getPath())
            .regel(regel)
            .build();

      var uppgift = ImmutableUpdateKundbehovsflodeUppgift.builder()
            .id(regelData.uppgiftId())
            .version(regelConfig.getUppgift().getVersion())
            .skapadTs(regelData.skapadTs())
            .utfordTs(regelData.utfordTs())
            .planeradTs(regelData.planeradTs())
            .utforarId(regelData.utforarId())
            .uppgiftStatus(regelData.uppgiftStatus())
            .aktivitet(regelConfig.getUppgift().getAktivitet())
            .fsSAinformation(regelData.fssaInformation())
            .specifikation(specifikation)
            .build();

      var requestBuilder = ImmutableUpdateKundbehovsflodeRequest.builder()
            .kundbehovsflodeId(regelData.kundbehovsflodeId())
            .uppgift(uppgift)
            .underlag(new ArrayList<>());

      for (ErsattningData rtfErsattning : regelData.ersattningar())
      {
         var ersattning = ImmutableUpdateKundbehovsflodeErsattning.builder()
               .beslutsutfall(mapBeslutsutfall(rtfErsattning.beslutsutfall()))
               .id(rtfErsattning.id())
               .avslagsanledning(rtfErsattning.avslagsanledning())
               .build();
         requestBuilder.addErsattningar(ersattning);
      }

      for (var rtfUnderlag : regelData.underlag())
      {
         var underlag = ImmutableUpdateKundbehovsflodeUnderlag.builder()
               .typ(rtfUnderlag.typ())
               .version(rtfUnderlag.version())
               .data(rtfUnderlag.data())
               .build();
         requestBuilder.addUnderlag(underlag);
      }

      return requestBuilder.build();
   }

   private Ersattning.BeslutsutfallEnum mapBeslutsutfall(Beslutsutfall beslutsutfall)
    {
        if (beslutsutfall == null)
        {
            return Ersattning.BeslutsutfallEnum.FU;
        }

        return switch (beslutsutfall) {
            case JA -> Ersattning.BeslutsutfallEnum.JA;
            case NEJ -> Ersattning.BeslutsutfallEnum.NEJ;
            default -> Ersattning.BeslutsutfallEnum.FU;
        };
    }

}
