package se.fk.rimfrost.framework.regel.manuell.storage;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.storage.DataStorageProvider;

@ApplicationScoped
public class RegelManuellTestDataStorageProvider implements DataStorageProvider<RegelManuellTestDataStorage>
{
   private RegelManuellTestDataStorage regelManuellTestDataStorage;

   @PostConstruct
   public void init()
   {
      regelManuellTestDataStorage = new RegelManuellTestDataStorage();
   }

   @Override
   public RegelManuellTestDataStorage getDataStorage()
   {
      return regelManuellTestDataStorage;
   }
}
