package se.fk.rimfrost.framework.regel.manuell.helpers;

import com.github.tomakehurst.wiremock.WireMockServer;
import se.fk.rimfrost.framework.regel.WireMockHandlaggning;
import java.util.HashMap;
import java.util.Map;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WireMockRegelManuell extends WireMockHandlaggning
{

   public static final String DEFAULT_UPPGIFT_ID = "11e53b18-e9ac-4707-825b-a1cb80689c29";

   @Override
   public Map<String, String> start()
   {
      var map = super.start();
      server.stubFor(post(urlPathEqualTo("/uppgifter"))
            .willReturn(okJson("{\"uppgiftId\":\"" + DEFAULT_UPPGIFT_ID + "\",\"status\":\"NY\"}")));
      server.stubFor(post(urlPathMatching("/uppgifter/.+/end"))
            .willReturn(okJson("{\"uppgiftId\":\"" + DEFAULT_UPPGIFT_ID + "\",\"status\":\"AVSLUTAD\"}")));
      return map;
   }

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
