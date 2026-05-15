package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;
import java.util.UUID;

@ApplicationScoped
class CloudEventDataMapper
{
   CloudEventDataEntity toEntity(UUID handlaggningId, CloudEventData data)
   {
      var entity = new CloudEventDataEntity();
      entity.handlaggningId = handlaggningId;
      entity.eventId = data.id();
      entity.kogitorootprociid = data.kogitorootprociid();
      entity.kogitoparentprociid = data.kogitoparentprociid();
      entity.kogitoprocinstanceid = data.kogitoprocinstanceid();
      entity.kogitorootprocid = data.kogitorootprocid();
      entity.kogitoprocid = data.kogitoprocid();
      entity.kogitoprocist = data.kogitoprocist();
      entity.kogitoprocversion = data.kogitoprocversion();
      entity.type = data.type();
      entity.source = data.source();
      return entity;
   }

   CloudEventData toDomain(CloudEventDataEntity entity)
   {
      return ImmutableCloudEventData.builder()
            .id(entity.eventId)
            .kogitorootprociid(entity.kogitorootprociid)
            .kogitoparentprociid(entity.kogitoparentprociid)
            .kogitoprocinstanceid(entity.kogitoprocinstanceid)
            .kogitorootprocid(entity.kogitorootprocid)
            .kogitoprocid(entity.kogitoprocid)
            .kogitoprocist(entity.kogitoprocist)
            .kogitoprocversion(entity.kogitoprocversion)
            .type(entity.type)
            .source(entity.source)
            .build();
   }
}
