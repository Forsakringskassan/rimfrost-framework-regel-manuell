# Plan — FKPOC-883: Unassign uppgift on SID detection

## Background

When a handläggare opens a manual-rule case (`GET /{handlaggningId}`), the framework already
checks for protected identity (SID) and returns HTTP 403 if detected. The new requirement
(FRMM-FR-08.6–08.8) adds that the framework must also unassign the OUL uppgift before returning
the 403, so the task returns to an unassigned state and can be picked up by a handläggare with
SID clearance.

## Approach

Split detection and action in `RegelManuellMiddlewareService.read()`:

- `checkSid(Handlaggning)` — refactored to return `boolean`; maps `SidException` to
  `RegelManuellException` as before.
- `unassignUppgift(UUID handlaggningId)` — new method; best-effort, never throws; looks up
  `oulUppgiftId` from storage and calls `oulAdapter.unassignOperativUppgift(oulUppgiftId)`.
- `read()` — orchestrates: `if (checkSid(...)) { unassignUppgift(...); throw 403; }`.

`UppgifterApi.unassignUppgift(UUID)` already exists in
`rimfrost-service-oul-management-api-jaxrs-spec:1.3.4`, so no spec change is needed.

---

## Steps

### Step 1 — Add `unassignOperativUppgift` to `OulAdapter` (`rimfrost-framework-oul-adapter`)

**Files:** `pom.xml`, `OulAdapter.java`

#### Design

Add `rimfrost-service-oul-management-api-jaxrs-spec:1.3.4` as a dependency. This brings in
`UppgifterApi` with `unassignUppgift(UUID)`.

In `OulAdapter`:
- Add a `UppgifterApi uppgifterClient` field alongside the existing `ReglerApi oulClient`.
- Initialize it in `@PostConstruct` using the same `oulBaseUrl` and JAX-RS client.
- Destroy it in `@PreDestroy` (set to null; the shared `client` is already closed there).
- Add method:

```java
public void unassignOperativUppgift(UUID uppgiftId) throws OulException
```

Exception mapping (mirrors `endOperativUppgift`):

| JAX-RS exception         | OulException.ErrorType  |
|--------------------------|-------------------------|
| `NotFoundException`      | `NOT_FOUND`             |
| `BadRequestException`    | `BAD_REQUEST`           |
| `ProcessingException`    | `SERVICE_UNAVAILABLE`   |
| `WebApplicationException`| `UNEXPECTED_ERROR`      |

Return type: `void` (the caller discards the response; no need to map `OperativUppgift`).

After implementation, release a new version of `rimfrost-framework-oul-adapter`
(`1.1.3-SNAPSHOT` → `1.1.3`).

---

### Step 2 — Wire `OulAdapter` into `RegelManuellMiddlewareService` and refactor SID handling

**Files:** `pom.xml` (regel-manuell), `RegelManuellMiddlewareService.java`

#### Design

1. Update `rimfrost-framework-oul-adapter` dependency to the released version.

2. Inject `OulAdapter` in `RegelManuellMiddlewareService`:
   ```java
   @Inject
   OulAdapter oulAdapter;
   ```

3. Refactor `checkSid` — return `boolean` instead of `void`:
   ```java
   private boolean checkSid(Handlaggning handlaggning)
   ```
   - Returns `true` when SID detected (no longer throws FORBIDDEN internally).
   - Still maps `SidException` → `RegelManuellException` (unchanged).

4. Add `unassignUppgift`:
   ```java
   private void unassignUppgift(UUID handlaggningId)
   ```
   - Reads `oulUppgiftId` from `dataStorage.getManuellRegelCommonData(handlaggningId)`.
   - If `oulUppgiftId` is `null`: logs a warning and returns (FRMM-FR-08.7 corner case).
   - Calls `oulAdapter.unassignOperativUppgift(oulUppgiftId)`.
   - Catches all exceptions, logs them, never rethrows (FRMM-FR-08.8).

5. Update `read()`:
   ```java
   if (checkSid(handlaggning)) {
      unassignUppgift(handlaggning.id());
      throw new RegelManuellException(Response.Status.FORBIDDEN, "Skyddad identitet");
   }
   ```

---

### Step 3 — Tests

**File:** `RegelManuellSidCheckTest.java`

Add `@InjectMock OulAdapter oulAdapter` to the test class (needed because OulAdapter is now
injected into the middleware service).

New test cases:

| Test | Verifies |
|------|----------|
| `read_should_unassign_uppgift_when_sid_detected` | `oulAdapter.unassignOperativUppgift` is called with the stored `oulUppgiftId` when SID is detected |
| `read_should_not_unassign_when_no_sid` | `unassignOperativUppgift` is NOT called when SID check returns false |
| `read_should_still_throw_forbidden_when_unassign_fails` | 403 is still thrown even if `unassignOperativUppgift` throws `OulException` |
| `read_should_skip_unassign_when_oulUppgiftId_is_null` | Unassign is skipped gracefully when `ManuellRegelCommonData.oulUppgiftId()` returns null |

Add a WireMock stub for `POST /uppgifter/.+/unassign` in `WireMockRegelManuell` for any
broader integration tests that drive through the full stack.