# Test strategy — rimfrost-framework-regel-manuell

## OUL integration: mock `OulUppgiftService`

### Decision

Mock `OulUppgiftService` at the `@InjectMock` level. This is the public API boundary
between `rimfrost-framework-regel-manuell` and `rimfrost-framework-regel-oul` — it is
the correct place to draw the test double line.

`OulUppgiftService` is tested against a real PostgreSQL devservices container in
`rimfrost-framework-regel-oul`. There is no value in re-testing its internals here.

Prerequisite: `OulUppgiftService.getCorrelationData(UUID)` must be available in the
consumed version of regel-oul. This method was added in FKPOC-1020 and is locally
implemented; it unblocks the migration once released and consumed (steps 14–19 of
`docs/FKPOC-970/plan.md`).

### Implementation

`AbstractRegelManuellTest` holds a single `@InjectMock OulUppgiftService` and
configures default stubs before each test:

- `createOulUppgift(any())` → returns a pre-built `OperativUppgift`
- `getCorrelationData(any())` → returns a pre-built `OulCorrelationData`
- `endOulUppgift` and `cleanupCorrelation` → Mockito void defaults (do nothing)

Tests that need to simulate OUL failures override these defaults with
`thenThrow(OulException)` for the relevant method.

### Synchronisation

After `sendRegelRequest`, the Kafka consumer thread processes the message
asynchronously. Tests that call `/done` must wait for the consumer thread to finish.
The synchronisation signal is the WireMock handlaggning `GET` request, which is made
synchronously in `handleRegelRequest` before `createOulUppgift` is called:

```java
protected void waitForRegelRequestProcessed(String handlaggningId)
{
   WireMockRegelManuell.waitForHandlaggningRequests(handlaggningId, RequestMethod.GET, 1);
}
```

Because `getCorrelationData` is stubbed to return a fixed value for any UUID,
`handleUppgiftDone` succeeds regardless of whether `createOulUppgift` has been
called — the only requirement is that the Kafka message has been consumed.
