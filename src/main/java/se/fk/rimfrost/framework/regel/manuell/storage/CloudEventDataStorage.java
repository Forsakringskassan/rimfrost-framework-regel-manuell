package se.fk.rimfrost.framework.regel.manuell.storage;

import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import java.util.UUID;

public interface CloudEventDataStorage
{
   CloudEventData getCloudEventData(UUID handlaggningId);

   void setCloudEventData(UUID handlaggningId, CloudEventData cloudEventData);

   void deleteCloudEventData(UUID handlaggningId);
}
