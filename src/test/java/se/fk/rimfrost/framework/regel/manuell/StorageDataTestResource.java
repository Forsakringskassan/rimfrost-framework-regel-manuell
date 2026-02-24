package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class StorageDataTestResource implements QuarkusTestResourceLifecycleManager
{
   private void cleanup()
   {
      var testdataPath = Path.of("testdata");

      if (Files.isDirectory(testdataPath))
      {
         try (Stream<Path> pathStream = Files.walk(testdataPath))
         {
            pathStream.sorted(Comparator.reverseOrder())
                  .forEach(p -> {
                     try
                     {
                        Files.delete(p);
                     }
                     catch (IOException e)
                     {
                        throw new RuntimeException(e);
                     }
                  });
         }
         catch (IOException e)
         {
            throw new RuntimeException(e);
         }
      }
   }

   @Override
   public Map<String, String> start()
   {
      // Cleanup any existing testdata storage directory prior to quarkus starting
      cleanup();

      return new HashMap<>();
   }

   @Override
   public void stop()
   {
      // Cleanup testdata directory after quarkus has shut down
      cleanup();
   }
}
