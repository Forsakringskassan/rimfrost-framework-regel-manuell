package se.fk.rimfrost.framework.regel.manuell.storage.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import se.fk.rimfrost.framework.regel.logic.entity.CloudEventData;
import se.fk.rimfrost.framework.regel.logic.entity.RegelData;

public class CommonRegelData
{
   private final Map<UUID, CloudEventData> cloudevents = new HashMap<>();
   private final Map<UUID, RegelData> regelDatas = new HashMap<>();
   private final Object lock = new Object();

   @SuppressFBWarnings("EI_EXPOSE_REP")
   public Map<UUID, CloudEventData> getCloudEvents()
   {
      return cloudevents;
   }

   @SuppressFBWarnings("EI_EXPOSE_REP")
   public Map<UUID, RegelData> getRegelDatas()
   {
      return regelDatas;
   }

   @SuppressWarnings("EI_EXPOSE_REP")
   public Object getLock()
   {
      return lock;
   }
}
