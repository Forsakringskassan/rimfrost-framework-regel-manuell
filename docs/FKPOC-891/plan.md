# Plan: FKPOC-891 — Remove individer from OUL create request

## Background

The framework currently includes `individer` when creating an operative task in OUL
(`CreateOperativUppgiftRequest`). The individer are extracted from
`handlaggning.yrkande().individYrkandeRoller()` and mapped via a `toIdtyp()` helper.

Per FRMM-FR-02.9, the framework shall not include individer in the OUL create request. OUL
fetches individinformation internally when needed via the `handlaggningId` that the task is
linked to. This aligns with OUL-FR-01.8.

No tests currently assert that individer is included in the OUL create request, so no test
changes are needed.

Requirement: FRMM-FR-02.9.

---

## Dependencies

`individer` has already been removed from `CreateOperativUppgiftRequest` in the local
`rimfrost-framework-oul-adapter` repo (currently at `1.0.0-SNAPSHOT`). A new release of
that adapter must be published before Step 2 can be completed.

---

## Steps

### Step 1 — Bump rimfrost-framework-oul-adapter dependency

**`pom.xml`**
- Update `rimfrost-framework-oul-adapter` to the version that removes `individer` from
  `CreateOperativUppgiftRequest`.

### Step 2 — Remove individer from the OUL create request

**`RegelManuellRequestHandler.java`**
- Remove `.individer(...)` (lines 83–86) from `ImmutableCreateOperativUppgiftRequest.builder()`.
- Remove the `toIdtyp(Idtyp idtyp)` private helper method (lines 150–156) — it is only used
  by the individer mapping.