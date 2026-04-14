package se.fk.rimfrost.framework.regel.manuell.storage;

import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;
import java.util.UUID;

public interface ManuellRegelCommonDataStorage
{
   ManuellRegelCommonData getManuellRegelCommonData(UUID handlaggningId);

   void setManuellRegelCommonData(UUID handlaggningId, ManuellRegelCommonData manuellRegelCommonData);

   void deleteManuellRegelCommonData(UUID handlaggningId);
}
