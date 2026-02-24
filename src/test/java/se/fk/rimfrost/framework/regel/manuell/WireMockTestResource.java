package se.fk.rimfrost.framework.regel.manuell;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import se.fk.rimfrost.framework.regel.test.AbstractWireMockTestResource;

import java.util.Map;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

public class WireMockTestResource extends AbstractWireMockTestResource
{
   @Override
   protected Map<String, String> getProperties()
   {
      var server = getWireMockServer();

      return Map.of(
            "kundbehovsflode.api.base-url", server.baseUrl());
   }
}
