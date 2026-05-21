package se.fk.rimfrost.framework.regel.manuell.logic;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.framework.regel.logic.entity.ImmutableCloudEventData;

class CloudEventAttributesMapperTest
{
   @Test
   void to_cloud_event_data_maps_all_fields_correctly()
   {
      var id = UUID.randomUUID();
      var rootProcIId = UUID.randomUUID();
      var parentProcIId = UUID.randomUUID();
      var procInstanseId = UUID.randomUUID();

      var attributes = Map.of(
            "id", id.toString(),
            "kogitorootprociid", rootProcIId.toString(),
            "kogitoparentprociid", parentProcIId.toString(),
            "kogitoprocinstanceid", procInstanseId.toString(),
            "kogitorootprocid", "root-proc",
            "kogitoprocid", "proc",
            "kogitoprocist", "proc-ist",
            "kogitoprocversion", "1.0",
            "type", "test-type",
            "source", "test-source");

      var result = CloudEventAttributesMapper.toCloudEventData(attributes);

      assertEquals(id, result.id());
      assertEquals(rootProcIId, result.kogitorootprociid());
      assertEquals(parentProcIId, result.kogitoparentprociid());
      assertEquals(procInstanseId, result.kogitoprocinstanceid());
      assertEquals("root-proc", result.kogitorootprocid());
      assertEquals("proc", result.kogitoprocid());
      assertEquals("proc-ist", result.kogitoprocist());
      assertEquals("1.0", result.kogitoprocversion());
      assertEquals("test-type", result.type());
      assertEquals("test-source", result.source());
   }

   @Test
   void to_attributes_roundtrip_through_to_cloud_event_data()
   {
      var original = ImmutableCloudEventData.builder()
            .id(UUID.randomUUID())
            .kogitorootprociid(UUID.randomUUID())
            .kogitoparentprociid(UUID.randomUUID())
            .kogitoprocinstanceid(UUID.randomUUID())
            .kogitorootprocid("root-proc")
            .kogitoprocid("proc")
            .kogitoprocist("proc-ist")
            .kogitoprocversion("1.0")
            .type("test-type")
            .source("test-source")
            .build();

      var attributes = CloudEventAttributesMapper.toAttributes(original);
      var result = CloudEventAttributesMapper.toCloudEventData(attributes);

      assertEquals(original, result);
   }

   @Test
   void to_cloud_event_data_throws_for_null_attributes()
   {
      assertThrows(IllegalArgumentException.class, () -> CloudEventAttributesMapper.toCloudEventData(null));
   }
}
