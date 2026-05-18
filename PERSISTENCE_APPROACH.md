# Persistence approach — manuella regler

## Overview

`rimfrost-framework-regel-manuell` provides PostgreSQL-backed persistence as a transparent capability for all manuella regler.<br>
The framework implements `ManuellRegelCommonDataStorage` and `CloudEventDataStorage` using Panache repositories. Service code injects these interfaces without knowing about the underlying storage.


---

## Architectural layers

```
┌──────────────────────────────────────────────────────────┐
│ Service code                                             │
│   Uses storage interfaces (unchanged contract)           │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│ Framework persistence layer                              │
│   • Panache repositories (CDI beans)                     │
│   • JPA entities + Immutable↔entity mappers              │
│   • No migrations — each service owns its full sequence  │
└────────────────────────┬─────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────┐
│ PostgreSQL (schema per service)                          │
└──────────────────────────────────────────────────────────┘
```

---

## Domain ↔ entity separation

`ManuellRegelCommonData` is an Immutables-generated value type and cannot serve as a JPA entity (JPA requires a no-arg constructor, mutable fields, identity equality). Two distinct shapes exist with a mapper between them:

```
ManuellRegelCommonData          ManuellRegelCommonDataEntity
(Immutables, public API)   ←→   (@Entity, package-private)
```

A package-private mapper translates in both directions. The Immutable type is the only thing service code ever sees — JPA never leaks past the storage interface.

---

## Source structure

```
src/main/java/.../storage/
  ManuellRegelCommonDataStorage.java       (interface)
  CloudEventDataStorage.java               (interface)
  entity/
    ManuellRegelCommonData.java            (Immutable value type)
  internal/
    PanacheManuellRegelCommonDataStorage.java   (@ApplicationScoped impl)
    PanacheCloudEventDataStorage.java           (@ApplicationScoped impl)
    ManuellRegelCommonDataRepository.java       (PanacheRepository)
    CloudEventDataRepository.java               (PanacheRepository)
    ManuellRegelCommonDataEntity.java           (@Entity, package-private)
    CloudEventDataEntity.java                   (@Entity, package-private)
    ManuellRegelCommonDataMapper.java           (entity ↔ Immutable)
    CloudEventDataMapper.java                   (entity ↔ Immutable)
    RegelManuellPhysicalNamingStrategy.java     (custom Hibernate naming strategy)

# No db/migration directory — framework ships no SQL migrations
```

Each service provides its own `src/main/resources/db/migration/` with all migrations it needs, including the framework common data tables.

---

## Table naming

The framework uses `RegelManuellPhysicalNamingStrategy`, a custom Hibernate `PhysicalNamingStrategy` that prepends a configurable prefix to every table name. The prefix is read from:

```properties
regel.persistence.table-prefix=<value>
```

This property is mandatory — the application will fail at startup if it is not set. The logical entity names (`common_data`, `cloud_event_data`) are resolved to `<prefix>_common_data` and `<prefix>_cloud_event_data` at runtime.

Each service sets a prefix unique to that service (e.g. `rtf_manuell`), which avoids table name collisions when multiple services share a PostgreSQL schema.

---

## Entity conventions

Framework entities have:

| Field | Purpose |
|---|---|
| `handlaggningId` (UUID) | Natural primary key — system-wide correlation id |
| `version` (long, `@Version`) | Optimistic locking against concurrent writes from scaled instances |
| `createdAt` (Instant) | Audit |
| `updatedAt` (Instant, updated via `@PreUpdate`) | Audit |

Service entities are encouraged to follow the same convention but not required to.

---

## Repository pattern

Panache repositories (`PanacheRepository<EntityType>`) rather than active-record style. Entities are pure data; repositories are CDI beans. Storage interface implementations delegate to repositories.

Storage implementations are annotated `@Transactional`. The interface contract guarantees atomicity for single-method calls. Service code that needs wider transactions adds `@Transactional` on the calling method — Quarkus joins existing transactions automatically.

---

## Migration strategy

### Ownership

Each service owns its entire Flyway migration sequence. The framework ships no migration files. Each service creates all tables it needs — including the framework common data tables — in its own migrations, using the table names that correspond to its configured prefix:

```
Service (e.g. rimfrost-regel-rtf-manuell)
────────────────────────────────────────
V001__common_data.sql          creates rtf_manuell_common_data, rtf_manuell_cloud_event_data
V002__service_specific.sql     service-specific tables
...
```

### Execution

`migrate-at-start=true` in all environments. Flyway's DB-level lock makes concurrent pod startups safe — the first instance migrates, others wait then skip.

### Schema generation

`quarkus.hibernate-orm.database.generation=validate` — Hibernate never touches DDL, Flyway is the sole DDL owner. Mismatches between entities and schema fail fast at startup.

---

## Configuration

### Framework defaults (`rimfrost-framework-regel-manuell/.../application.properties`)

```properties
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.database.default-schema=${quarkus.flyway.default-schema}
quarkus.hibernate-orm.physical-naming-strategy=se.fk.rimfrost.framework.regel.manuell.storage.internal.RegelManuellPhysicalNamingStrategy
quarkus.flyway.migrate-at-start=true
quarkus.flyway.create-schemas=true
quarkus.flyway.schemas=${quarkus.flyway.default-schema}

%dev.quarkus.datasource.devservices.enabled=true
%test.quarkus.datasource.devservices.enabled=true
```

### Per-service (in each manuell regel)

```properties
# Mandatory — unique per service
quarkus.flyway.default-schema=regel_rtf_manuell
regel.persistence.table-prefix=rtf_manuell

# Dev & test — Dev Services starts PostgreSQL automatically, no config needed.

# Prod — injected from Kubernetes Secret
%prod.quarkus.datasource.username=${DB_USERNAME}
%prod.quarkus.datasource.password=${DB_PASSWORD}
%prod.quarkus.datasource.jdbc.url=${DB_URL}
```

---

## Environment summary

| Environment | Database | Datasource config | Migrations |
|---|---|---|---|
| Local (`quarkus:dev`) | Dev Services container | Automatic | At app start |
| CI / tests | Dev Services (Testcontainers) | Automatic | At app start |
| OpenShift | CloudNativePG | K8s Secret → env vars | At app start (Flyway DB lock handles pod races) |

---

## Scaling

Pods are stateless. All instances share the schema. Rows are keyed on `handlaggningId`; concurrent writes to the same row are caught by `@Version` optimistic locking. No partitioning or coordination between instances.
