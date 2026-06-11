package se.fk.rimfrost.framework.regel.manuell.storage;

import se.fk.rimfrost.framework.regel.manuell.storage.entity.ProcessTopicInfo;

import java.util.UUID;

public interface ProcessTopicInfoStorage
{
   ProcessTopicInfo getProcessTopicInfo(UUID handlaggningId);

   void setProcessTopicInfo(UUID handlaggningId, ProcessTopicInfo processTopicInfo);

   void deleteProcessTopicInfo(UUID handlaggningId);
}
