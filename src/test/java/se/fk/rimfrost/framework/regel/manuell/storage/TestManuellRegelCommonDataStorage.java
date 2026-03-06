package se.fk.rimfrost.framework.regel.manuell.storage;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;

import java.util.UUID;

@ApplicationScoped
public class TestManuellRegelCommonDataStorage implements ManuellRegelCommonDataStorage
{
   private ManuellRegelCommonData manuellRegelCommonData = null;

   @Override
   public ManuellRegelCommonData getManuellRegelCommonData(UUID handlaggningId)
   {
      return manuellRegelCommonData;
   }

   @Override
   public void setManuellRegelCommonData(UUID handlaggningId, ManuellRegelCommonData manuellRegelCommonData)
   {
      this.manuellRegelCommonData = manuellRegelCommonData;
   }

   @Override
   public void deleteManuellRegelCommonData(UUID handlaggningId)
   {
      manuellRegelCommonData = null;
   }
}
