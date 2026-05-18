package se.fk.rimfrost.framework.regel.manuell.base;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class StorageTestCleaner
{
   @Inject
   EntityManager em;

   @Transactional
   public void clearAll()
   {
      em.createQuery("DELETE FROM ManuellRegelCommonDataEntity").executeUpdate();
      em.createQuery("DELETE FROM CloudEventDataEntity").executeUpdate();
   }
}
