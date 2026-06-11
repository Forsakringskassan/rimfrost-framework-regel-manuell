package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import jakarta.enterprise.context.ApplicationScoped;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ImmutableProcessTopicInfo;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ProcessTopicInfo;

import java.util.UUID;

@ApplicationScoped
public class ProcessTopicInfoMapper
{
   public ProcessTopicInfoEntity toEntity(UUID handlaggningId, ProcessTopicInfo processTopicInfo)
   {
      ProcessTopicInfoEntity processTopicInfoEntity = new ProcessTopicInfoEntity();
      processTopicInfoEntity.handlaggningId = handlaggningId;
      processTopicInfoEntity.replyTopic = processTopicInfo.replyTopic();
      return processTopicInfoEntity;
   }

   public ProcessTopicInfo toDomain(ProcessTopicInfoEntity processTopicInfoEntity)
   {
      return ImmutableProcessTopicInfo.builder().replyTopic(processTopicInfoEntity.replyTopic).build();
   }
}
