# Persistence approach — manuella regler

## Overview

Persistence is delivered by `rimfrost-framework-regel-manuell` as a transparent capability for all manuella regler. The framework provides PostgreSQL implementations of the existing storage interfaces (`ManuellRegelCommonDataStorage`, `CloudEventDataStorage`); each service contributes its own entities, migrations, and a schema name.

Service code is unaffected — `@Inject ManuellRegelCommonDataStorage` continues to work; the implementation is now Panache-backed.

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

`ManuellRegelCommonData` is an Immutables-generated value type and cannot serve as a JPA entity (JPA requires a no-arg constructor, mutable fields, identity equality). Two distinct shapes with a mapper between them:

```
ManuellRegelCommonData          ManuellRegelCommonDataEntity
(Immutables, public API)   ←→   (@Entity, package-private)
```

A package-private mapper translates in both directions. The Immutable type stays the only thing service code ever sees — JPA never leaks past the storage interface.

---

## Source structure

```
src/main/java/.../storage/
  ManuellRegelCommonDataStorage.java       (existing interface — unchanged)
  CloudEventDataStorage.java               (existing interface — unchanged)
  entity/
    ManuellRegelCommonData.java            (existing Immutable — unchanged)
  internal/
    PanacheManuellRegelCommonDataStorage.java   (@ApplicationScoped impl)
    PanacheCloudEventDataStorage.java           (@ApplicationScoped impl)
    ManuellRegelCommonDataRepository.java       (PanacheRepository)
    CloudEventDataRepository.java               (PanacheRepository)
    ManuellRegelCommonDataEntity.java           (@Entity, package-private)
    CloudEventDataEntity.java                   (@Entity, package-private)
    ManuellRegelCommonDataMapper.java           (entity ↔ Immutable)
    CloudEventDataMapper.java                   (entity ↔ Immutable)

# No db/migration directory — framework ships no SQL migrations
```

Each service provides its own `src/main/resources/db/migration/` with all migrations it needs, including the framework common data tables:

```
src/main/resources/db/migration/       (in each service)
  V1__common_data.sql                  creates manuell_regel_common_data and cloud_event_data
  V2__service_specific_data.sql        service-specific tables
  ...
```

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

## Maven dependencies (added to framework `pom.xml`)

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-flyway</artifactId>
</dependency>
```

---

## Repository pattern

Panache repositories (`PanacheRepository<EntityType>`) rather than active-record style. Entities stay pure data; repositories are CDI beans, easy to inject and mock. The storage interface impls call into the repositories.

Storage implementations are annotated `@Transactional`. The interface contract guarantees atomicity for single-method calls. Service code that needs wider transactions adds `@Transactional` on the calling method — Quarkus joins existing transactions automatically.

---

## Migration strategy

### Ownership

Each service owns its entire Flyway migration sequence from V1. The framework ships no migration files. Each service is responsible for creating all tables it needs — including the framework common data tables (`manuell_regel_common_data`, `cloud_event_data`) — in its own migrations.

```
Service (e.g. rimfrost-regel-rtf-manuell)
────────────────────────────────────────
V1__common_data.sql          creates manuell_regel_common_data, cloud_event_data
V2__rtf_data.sql             service-specific tables
V3__add_column.sql
...
```

This gives each service full control over its schema lifecycle with no dependency on framework migration files.

### Execution

`migrate-at-start=true` in all environments. Flyway's DB-level lock makes concurrent pod startups safe — the first instance migrates, others wait then skip.

### Schema generation

`quarkus.hibernate-orm.database.generation=validate` — Hibernate never touches DDL, Flyway is sole owner. Mismatches between entities and schema fail fast at startup.

---

## Configuration

### Framework defaults (`rimfrost-framework-regel-manuell/.../application.properties`)

```properties
quarkus.datasource.db-kind=postgresql
quarkus.hibernate-orm.database.generation=validate
quarkus.hibernate-orm.database.default-schema=${quarkus.flyway.default-schema}
quarkus.hibernate-orm.physical-naming-strategy=org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy
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

---

## Deferred decisions

These are deliberately not addressed in v1; can be added later when measured need arises:

- **Connection pool tuning** — Agroal defaults are fine until load data exists.
- **Observability config** — Hibernate stats, slow query logging, datasource metrics. One-line additions when needed.
- **DB role-based schema isolation** (`CREATE ROLE ... AUTHORIZATION schema`) — currently handled at config level; can be hardened at the DB level if/when ops takes ownership of the pattern.
- **Soft delete / retention** — not needed by framework; services add locally if required.
- **Separate migration Job in prod** — `migrate-at-start` is sufficient given expected migration size and cadence; revisit if migrations grow heavy or HA constraints tighten.
