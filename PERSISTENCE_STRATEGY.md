# Persistence strategy — manuella regler

## Strategy

1. **All manuella regler need persistence** — not opt-in; every service will use it.

2. **Each service has its own data shape** — the framework tables are not enough; each regel stores its own domain-specific fields on top.

3. **Single framework module** — persistence support goes into `rimfrost-framework-regel-manuell`, not a new separate artifact.

4. **Common data stays in the framework** — existing `ManuellRegelCommonDataStorage` and `CloudEventDataStorage` interfaces define what the framework owns; PostgreSQL implementations of these belong in the framework.

5. **Each service owns its entire migration sequence** — there is no framework-owned migration range. Each regel service manages all Flyway migrations from V1, including creation of the framework common data tables (`manuell_regel_common_data`, `cloud_event_data`) and its own service-specific tables. The framework ships no migration files.

6. **Schema-per-service isolation** — each service uses its own PostgreSQL schema. Services may share a database instance but must never share a schema. Flyway and the datasource must be configured per service to target the correct schema.

7. **No developer setup friction** — Quarkus Dev Services handles local PostgreSQL automatically, zero config per developer.

8. **OpenShift/Kubernetes production** — CloudNativePG operator manages database instances in production.

9. **Environment configuration via Quarkus profiles** — three environments are supported:
   - `dev`: Dev Services auto-starts a PostgreSQL container, no datasource config needed.
   - `test`: Dev Services via Testcontainers, also zero config.
   - `prod`: datasource credentials and URL injected from a Kubernetes Secret as environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`).

10. **Each service sets its own schema name** — the only mandatory per-service config is `quarkus.flyway.default-schema`. Everything else is inherited from the framework or injected at runtime.

11. **Flyway migration location is per service** — each service provides its own `classpath:db/migration` with all migrations it needs, including those for framework-owned tables. The framework does not contribute migration files to the classpath.

12. **Scaled instances share the schema** — all instances of a service read and write to the same schema. No instance column or partitioning needed. Each row is keyed on `handlaggningId` (UUID); pods are stateless and interchangeable. Concurrent writes are handled by database transactions, concurrent migrations by Flyway's built-in DB lock.
