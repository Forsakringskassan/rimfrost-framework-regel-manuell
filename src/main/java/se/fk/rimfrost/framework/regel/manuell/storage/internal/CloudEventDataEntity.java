package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cloud_event_data")
public class CloudEventDataEntity
{
   @Id
   UUID handlaggningId;

   @Column(name = "event_id", nullable = false)
   UUID eventId;

   UUID kogitorootprociid;
   UUID kogitoparentprociid;
   UUID kogitoprocinstanceid;
   String kogitorootprocid;
   String kogitoprocid;
   String kogitoprocist;
   String kogitoprocversion;
   String type;
   String source;

   @Version
   long version;

   @Column(nullable = false, updatable = false)
   Instant createdAt;

   @Column(nullable = false)
   Instant updatedAt;

   @PrePersist
   void onCreate()
   {
      createdAt = Instant.now();
      updatedAt = createdAt;
   }

   @PreUpdate
   void onUpdate()
   {
      updatedAt = Instant.now();
   }
}
