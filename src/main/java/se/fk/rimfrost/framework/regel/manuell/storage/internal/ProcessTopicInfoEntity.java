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
@Table(name = "process_topic_info")
public class ProcessTopicInfoEntity
{
   @Id
   UUID handlaggningId;

   @Column(nullable = false)
   String replyTopic;

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
