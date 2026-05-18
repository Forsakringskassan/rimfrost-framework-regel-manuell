package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class RegelManuellPhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy
{
   @Override
   public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment context)
   {
      Identifier physical = super.toPhysicalTableName(logicalName, context);
      String prefix = ConfigProvider.getConfig()
            .getOptionalValue("regel.persistence.table-prefix", String.class)
            .orElseThrow(() -> new IllegalStateException(
                  "regel.persistence.table-prefix must be configured — set it to a unique identifier for this regel service (e.g. rtf_manuell)"));
      return Identifier.toIdentifier(prefix + "_" + physical.getText());
   }
}
