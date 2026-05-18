package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.manuell.storage.CloudEventDataStorage;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class PanacheCloudEventDataStorage implements CloudEventDataStorage
{
   @Inject
   CloudEventDataRepository repository;

   @Inject
   CloudEventDataMapper mapper;

   @Override
   public CloudEventData getCloudEventData(UUID handlaggningId)
   {
      return repository.findByIdOptional(handlaggningId)
            .map(mapper::toDomain)
            .orElse(null);
   }

   @Override
   public void setCloudEventData(UUID handlaggningId, CloudEventData cloudEventData)
   {
      var existing = repository.findByIdOptional(handlaggningId);
      if (existing.isPresent())
      {
         var entity = existing.get();
         var updated = mapper.toEntity(handlaggningId, cloudEventData);
         updated.version = entity.version;
         updated.createdAt = entity.createdAt;
         repository.getEntityManager().merge(updated);
      }
      else
      {
         repository.persist(mapper.toEntity(handlaggningId, cloudEventData));
      }
   }

   @Override
   public void deleteCloudEventData(UUID handlaggningId)
   {
      repository.deleteById(handlaggningId);
   }
}
