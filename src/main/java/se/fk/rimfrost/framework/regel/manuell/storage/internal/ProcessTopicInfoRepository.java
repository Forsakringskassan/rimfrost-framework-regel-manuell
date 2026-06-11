package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ProcessTopicInfoRepository implements PanacheRepositoryBase<ProcessTopicInfoEntity, UUID>
{
}
