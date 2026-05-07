package se.fk.rimfrost.framework.regel.manuell.logic;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.HandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggningUpdate;
import se.fk.rimfrost.framework.handlaggning.model.ProduceratResultat;
import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.logic.RegelUtils;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;

import java.util.ArrayList;
import java.util.UUID;

import com.networknt.schema.OutputFormat.List;

@SuppressWarnings("unused")
@ApplicationScoped
@DefaultBean
public class RegelManuellTestService extends RegelManuellServiceBase
      implements RegelManuellServiceInterface<String, String>
{
   @Inject
   ManuellRegelCommonDataStorage dataStorage;

   @Override
   public String readData(Handlaggning handlaggning)
   {
      return "read";
   }

   @Override
   public void done(UUID handlaggningId)
   {
      sendRegelResponse(handlaggningId, Utfall.JA);
   }

   @Override
   public HandlaggningUpdate updateData(Handlaggning handlaggning, String request)
   {
      return ImmutableHandlaggningUpdate.builder()
            .id(handlaggning.id())
            .version(handlaggning.version())
            .yrkande(handlaggning.yrkande())
            .processInstansId(handlaggning.processInstansId())
            .skapadTS(handlaggning.skapadTS())
            .avslutadTS(handlaggning.avslutadTS())
            .handlaggningspecifikationId(handlaggning.handlaggningspecifikationId())
            .uppgift(dataStorage.getManuellRegelCommonData(handlaggning.id()).uppgift())
            .build();
   }

}
