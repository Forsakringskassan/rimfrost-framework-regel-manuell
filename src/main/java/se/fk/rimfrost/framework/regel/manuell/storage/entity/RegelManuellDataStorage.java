package se.fk.rimfrost.framework.regel.manuell.storage.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class RegelManuellDataStorage
{
   private final CommonRegelData commonRegelData = new CommonRegelData();

   public CommonRegelData getCommonRegelData()
   {
      return commonRegelData;
   }
}
