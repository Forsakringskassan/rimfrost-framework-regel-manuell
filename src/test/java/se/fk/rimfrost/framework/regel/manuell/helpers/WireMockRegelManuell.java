package se.fk.rimfrost.framework.regel.manuell.helpers;

import com.github.tomakehurst.wiremock.WireMockServer;
import se.fk.rimfrost.framework.regel.WireMockHandlaggning;
import java.util.HashMap;
import java.util.Map;

public class WireMockRegelManuell extends WireMockHandlaggning
{

   /**
    * Defines wiremock mappings for manual rules.
    * <p>
    * Note: Introducing this class creates a placeholder for adding mappings to all manual rules
    * (even if initially no such mappings have been identified).</p>
    *
    * @param server active WireMock server
    * @return property mappings
    */
   @SuppressWarnings("UnnecessaryLocalVariable")
   @Override
   protected Map<String, String> wiremockMapping(WireMockServer server)
   {
      Map<String, String> map = new HashMap<>(super.wiremockMapping(server));
      // Add more mappings if manual rules need to define extra mappings
      // map.put("something", "value");
      return map;
   }
}
