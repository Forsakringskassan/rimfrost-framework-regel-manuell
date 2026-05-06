package se.fk.rimfrost.framework.regel.manuell.storage;

import java.util.UUID;

public interface CloudEventDataStorage
{
   se.fk.rimfrost.framework.regel.logic.entity.CloudEventData getCloudEventData(UUID handlaggningId);

   void setCloudEventData(UUID handlaggningId, se.fk.rimfrost.framework.regel.logic.entity.CloudEventData cloudEventData);

   void deleteCloudEventData(UUID handlaggningId);
}
