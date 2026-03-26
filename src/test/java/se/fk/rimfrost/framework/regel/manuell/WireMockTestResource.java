package se.fk.rimfrost.framework.regel.manuell;

import se.fk.rimfrost.framework.regel.test.AbstractWireMockTestResource;

import java.util.Map;

public class WireMockTestResource extends AbstractWireMockTestResource
{
   @Override
   protected Map<String, String> getProperties()
   {
      var server = getWireMockServer();

      return Map.of(
            "handlaggning.api.base-url", server.baseUrl());
   }
}
