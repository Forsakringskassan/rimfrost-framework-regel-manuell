package se.fk.rimfrost.framework.regel.manuell.logic;

import java.util.Map;
import java.util.UUID;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;

public class CloudEventAttributesMapper
{
   private CloudEventAttributesMapper()
   {
   }

   public static Map<String, String> toAttributes(CloudEventData cloudEventData)
   {
      return Map.of(
            "id", cloudEventData.id().toString(),
            "kogitorootprociid", cloudEventData.kogitorootprociid().toString(),
            "kogitoparentprociid", cloudEventData.kogitoparentprociid().toString(),
            "kogitoprocinstanceid", cloudEventData.kogitoprocinstanceid().toString(),
            "kogitorootprocid", cloudEventData.kogitorootprocid(),
            "kogitoprocid", cloudEventData.kogitoprocid(),
            "kogitoprocist", cloudEventData.kogitoprocist(),
            "kogitoprocversion", cloudEventData.kogitoprocversion(),
            "type", cloudEventData.type(),
            "source", cloudEventData.source());
   }

   public static CloudEventData toCloudEventData(Map<String, String> attributes)
   {
      return ImmutableCloudEventData.builder()
            .id(UUID.fromString(attributes.get("id")))
            .kogitorootprociid(UUID.fromString(attributes.get("kogitorootprociid")))
            .kogitoparentprociid(UUID.fromString(attributes.get("kogitoparentprociid")))
            .kogitoprocinstanceid(UUID.fromString(attributes.get("kogitoprocinstanceid")))
            .kogitorootprocid(attributes.get("kogitorootprocid"))
            .kogitoprocid(attributes.get("kogitoprocid"))
            .kogitoprocist(attributes.get("kogitoprocist"))
            .kogitoprocversion(attributes.get("kogitoprocversion"))
            .type(attributes.get("type"))
            .source(attributes.get("source"))
            .build();
   }
}
