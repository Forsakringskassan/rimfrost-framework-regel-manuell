# Plan — FKPOC-1111: SID-kontroll med handläggarbehörighet

## Syfte

Utöka SID-flödet i ramverket så att en handläggare med SID-rättigheter får tillgång till ärenden
med skyddad identitet. Tidigare blockerades alla handläggare vid skyddad identitet (HTTP 403).
Nu ska ramverket även kontrollera handläggarens behörighet via `PermissionsAdapter` och bara
blockera de som saknar SID-rättigheter.

## Kravändringar (docs/krav.md)

- **FRMM-FR-08.3** — Uppdaterad: HTTP 403 returneras nu bara om handläggaren *saknar*
  SID-rättigheter.
- **FRMM-FR-08.4** — Borttagen (ersatt av FRMM-FR-06.4 som nu täcker alla externa tjänster).
- **FRMM-FR-08.5** — Utökad: ramverket ska även läsa `permissions.api.base-url` för
  behörighetstjänsten.
- **FRMM-FR-08.6** — Uppdaterad: unassign sker bara vid HTTP 403 (dvs. handläggare saknar
  SID-rättigheter).
- **FRMM-FR-08.9** (ny) — Om SID detekteras ska ramverket kontrollera handläggarens SID-rättigheter
  via `PermissionsAdapter.hasSidPermission()`.
- **FRMM-FR-08.10** (ny) — Om handläggaren har SID-rättigheter ska `readData()` anropas normalt.
- **FRMM-FR-06.4** — Generaliserad från "handläggningstjänsten och OUL" till "externa tjänster".
- **FRMM-NFR-03.1** — Utökad: loggning ska även täcka fel mot SID-tjänsten och
  behörighetstjänsten.

## Steg

- [x] **1. Lägg till beroende på `rimfrost-adapter-permissions`**
  - `PermissionsAdapter` finns redan i `rimfrost-adapter-permissions` — lägg till
    maven-beroendet i `pom.xml`.
  - Lägg till property `permissions.api.base-url` i ramverkets konfiguration.

- [x] **2. Uppdatera SID-kontrollen i ramverket**
  - Efter att SID detekteras: anropa `PermissionsAdapter.hasSidPermission()`.
  - Om handläggaren har SID-rättigheter: anropa `readData()` normalt (FRMM-FR-08.10).
  - Om handläggaren saknar SID-rättigheter: unassign + returnera HTTP 403 (FRMM-FR-08.6).

- [x] **3. Byt identitetskälla till `IdentityAdapter`**
  - `RegelManuellMiddlewareService` extraherar `Authorization`-headern via `RoutingContext` och
    skickar den till `IdentityAdapter.getIdentity(authHeader)`, som ansvarar för parsningen.
  - Lägg till `rimfrost-adapter-identity` som maven-beroende i `pom.xml`.
  - Konfigurera `quarkus.rest-client.identity-api.url` i `application.properties`.

- [x] **4. Tester**
  - Enhetstester för `PermissionsAdapter` (happy path + felfall).
  - Integrationstester för SID-flödet:
    - SID detekterat, handläggare har rättigheter → `readData()` anropas.
    - SID detekterat, handläggare saknar rättigheter → unassign + HTTP 403.
    - Fel mot behörighetstjänsten → korrekt HTTP-statuskod.
