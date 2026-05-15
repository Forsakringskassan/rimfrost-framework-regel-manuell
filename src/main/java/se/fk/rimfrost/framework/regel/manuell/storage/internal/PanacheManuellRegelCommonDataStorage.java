package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;
import java.util.UUID;

@ApplicationScoped
@Transactional
public class PanacheManuellRegelCommonDataStorage implements ManuellRegelCommonDataStorage
{
   @Inject
   ManuellRegelCommonDataRepository repository;

   @Inject
   ManuellRegelCommonDataMapper mapper;

   @Override
   public ManuellRegelCommonData getManuellRegelCommonData(UUID handlaggningId)
   {
      return repository.findByIdOptional(handlaggningId)
            .map(mapper::toDomain)
            .orElse(null);
   }

   @Override
   public void setManuellRegelCommonData(UUID handlaggningId, ManuellRegelCommonData manuellRegelCommonData)
   {
      var existing = repository.findByIdOptional(handlaggningId);
      if (existing.isPresent())
      {
         var entity = existing.get();
         var updated = mapper.toEntity(handlaggningId, manuellRegelCommonData);
         updated.version = entity.version;
         updated.createdAt = entity.createdAt;
         repository.getEntityManager().merge(updated);
      }
      else
      {
         repository.persist(mapper.toEntity(handlaggningId, manuellRegelCommonData));
      }
   }

   @Override
   public void deleteManuellRegelCommonData(UUID handlaggningId)
   {
      repository.deleteById(handlaggningId);
   }
}
