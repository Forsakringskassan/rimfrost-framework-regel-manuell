package se.fk.rimfrost.framework.regel.manuell.storage;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
@ApplicationScoped
@DefaultBean
public class TestCloudEventDataStorage implements CloudEventDataStorage
{
   private final Map<UUID, CloudEventData> store = new HashMap<>();

   @Override
   public CloudEventData getCloudEventData(UUID handlaggningId)
   {
      return store.get(handlaggningId);
   }

   @Override
   public void setCloudEventData(UUID handlaggningId, CloudEventData cloudEventData)
   {
      store.put(handlaggningId, cloudEventData);
   }

   @Override
   public void deleteCloudEventData(UUID handlaggningId)
   {
      store.remove(handlaggningId);
   }
}
