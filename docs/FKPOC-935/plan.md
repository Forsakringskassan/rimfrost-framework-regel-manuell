# FKPOC-935 — Wire komplettering pre-check into manuell regel request handler

## Steps

### Step 1 — Extend `RegelManuellServiceInterface` with `KompletteringKontrollInterface`

**File:** `src/main/java/se/fk/rimfrost/framework/regel/manuell/logic/RegelManuellServiceInterface.java`

```java
public interface RegelManuellServiceInterface<T, Y> extends KompletteringKontrollInterface
```

The `default` implementation in `KompletteringKontrollInterface` returns `List.of()`,
so existing regel implementations compile and behave identically without change.

### Step 2 — Wire komplettering check in `RegelManuellRequestHandler`

**File:** `src/main/java/se/fk/rimfrost/framework/regel/manuell/logic/RegelManuellRequestHandler.java`

Inject `KompletteringKontrollInterface` and `KompletteringOulHandler`. After fetching
handlaggning and looking up `erbjudandeNamn`, before creating the regel-OUL-uppgift:

```java
var gaps = kompletteringKontroll.checkKomplettering(handlaggning);
if (!gaps.isEmpty())
{
    var erbjudande = createErbjudande(handlaggning.yrkande().erbjudandeId(), erbjudandeNamn);
    try
    {
        kompletteringOulHandler.initiate(
            request,
            CloudEventAttributesMapper.toAttributes(cloudevent),
            regelConfig,
            erbjudande
        );
    }
    catch (OulException e)
    {
        var message = String.format(
            "Failed to initiate komplettering. handlaggningId: %s", request.handlaggningId());
        var regelErrorInformation = createRegelErrorInformation(RegelFelkod.RIMFROST_OTHER, message);
        throw new RegelCancelledException(regelErrorInformation, message, e);
    }
    return;
}
```

`createErbjudande()` is inherited from `RegelRequestHandlerBase`.

### Step 3 — Tests

| Scenario | Expected behaviour |
|----------|--------------------|
| `checkKomplettering()` returns non-empty list | `kompletteringOulHandler.initiate()` called; regel-OUL-uppgift NOT created; method returns |
| `checkKomplettering()` returns empty list | existing flow unchanged; regel-OUL-uppgift created as before |
| `checkKomplettering()` returns non-empty, `initiate()` throws `OulException` | error response sent; no OUL task left open |

Use `@InjectMock` for `KompletteringKontrollInterface` and `KompletteringOulHandler`.
Ensure mocks for base-class injections (`ErbjudandeReferensdataInterface`, `OulAdapter`,
storage interfaces) are present in the test setup.

### Step 4 — Release

Bump to the next minor version of `rimfrost-framework-regel-manuell`.
Record the new version in `CHANGELOG.md`.

## Definition of done

- `RegelManuellServiceInterface` extends `KompletteringKontrollInterface`
- `RegelManuellRequestHandler` calls `checkKomplettering()` before creating the regel-OUL-uppgift
- If gaps found: komplettering OUL task initiated, early return, no regel-OUL-uppgift created
- All three test scenarios pass
- `mvn test` green
- New minor version released
