package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.WireMockServer;
import se.fk.rimfrost.framework.regel.test.WireMockHandlaggning;
import java.util.HashMap;
import java.util.Map;

public class WireMockRegelManuell extends WireMockHandlaggning
{

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
