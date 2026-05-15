package se.fk.rimfrost.framework.regel.manuell.storage.internal;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
class ManuellRegelCommonDataRepository implements PanacheRepositoryBase<ManuellRegelCommonDataEntity, UUID>
{
}
