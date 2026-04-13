package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.WireMockServer;
import se.fk.rimfrost.framework.regel.test.WireMockHandlaggning;
import java.util.Map;

public class WireMockRegelManuell extends WireMockHandlaggning
{
   @Override
   protected Map<String, String> customMapping(WireMockServer server)
   {
      return Map.of(
            "folkbokford.api.base-url", server.baseUrl(),
            "arbetsgivare.api.base-url", server.baseUrl(),
            "individ.api.base-url", server.baseUrl());
   }
}
