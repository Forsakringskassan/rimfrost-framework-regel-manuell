# Plan: SID check in RegelManuell framework read

## Background

`RegelManuellMiddlewareService.read()` currently delegates directly to `regelService.readData()`
without checking whether any of the individer on the handläggning have a protected identity (SID).

The SID service (`rimfrost-service-sid`) exposes `POST /sid/status` and an adapter
(`rimfrost-framework-sid-adapter`) already wraps it with `SidAdapter.containsSid(List<Idtyp>)`.

Individer are extracted from `handlaggning.yrkande().individYrkandeRoller()`. The
`handlaggning.model.Idtyp` and `sid.model.Idtyp` share the same structure (`typId`, `varde`)
but live in different packages and must be mapped.

If any individ has SID the framework returns HTTP 403 and `readData()` is never called. All
rules get this behaviour automatically — no per-rule configuration required.

Requirement: FRMM-FR-08.

---

## Steps

### Step 1 — Add rimfrost-framework-sid-adapter dependency ✅

**`pom.xml`**
- Add `se.fk.rimfrost.framework.sid:rimfrost-framework-sid-adapter:0.0.1`.
- Bump `rimfrost-framework-oul-adapter` to `1.1.3-SNAPSHOT` (locally available).

---

### Step 2 — Implement SID check in RegelManuellMiddlewareService

**`RegelManuellMiddlewareService.java`**
- Inject `SidAdapter sidAdapter`.
- Add `checkSid(Handlaggning)` — calls `sidAdapter.containsSid(...)`, throws
  `RegelManuellException(FORBIDDEN)` if true, maps `SidException` to HTTP status otherwise.
- Add `extractIndivider(Handlaggning)` — maps `individYrkandeRoller` entries to
  `List<se.fk.rimfrost.framework.sid.model.Idtyp>`.
- Add `toHttpStatus(SidException)` overload mirroring the existing `toHttpStatus(HandlaggningException)`.
- Call `checkSid(handlaggning)` at the top of `read()`, before `regelService.readData()`.

---

### Step 3 — Write unit tests

Test cases:
- SID found → `RegelManuellException` with HTTP 403, `readData()` not called.
- SID not found → `readData()` is called normally.
- `SidException(SERVICE_UNAVAILABLE)` → `RegelManuellException` with HTTP 503.
- Empty `individYrkandeRoller` → no exception, `readData()` proceeds.

---

### Step 4 — Document sid.api.base-url configuration property

Document in README that consuming services must configure `sid.api.base-url`.
