# FKPOC-1134 — Spara utfall i handläggningsärendet vid POST /done

## Bakgrund

`RegelManuellRequestHandler.handleUppgiftDone()` skickar korrekt `utfall` i `RegelResponse` på
Kafka, men skriver inte tillbaka värdet till handläggningsärendet. Fixet persisterar `regelutfall`
i `Uppgift` vid `POST /done`, vilket kräver en ny snapshot av `rimfrost-framework-handlaggning-adapter`
som introducerar `Regelutfall`-domäntypen och en ny `HandlaggningUpdate`-struktur.

---

## Steg

- [ ] **1. Uppdatera beroenden i `pom.xml`**

  Uppdatera till nya snapshots:
  - `rimfrost-framework-regel` (inkl. test-classifier)
  - `rimfrost-framework-regel-oul`
  - `rimfrost-framework-oul-adapter` _(pinnas explicit, transitiv)_
  - `rimfrost-service-oul-management-regler-api-jaxrs-spec` _(pinnas explicit, transitiv)_

  `rimfrost-framework-handlaggning-adapter` kommer transitivt via `rimfrost-framework-regel`.

- [ ] **2. Åtgärda brytande ändringar i `RegelManuellRequestHandler`**

  - `utfordTs(` → `utfordTS(`
  - `erbjudandeId()` → `ingangtypId()` (×2 på `Yrkande` i `handleRegelRequest()`)
  - `.assignable(true)` läggs till på `ImmutableOulUppgiftSpec`-builder (nytt obligatoriskt fält)
  - `ImmutableHandlaggningUpdate`-byggaren förenklas till `{ handlaggning, uppgift }` (ny struktur i adapter)

- [ ] **3. Sätt `regelutfall` i `handleUppgiftDone()`**

  Lägg till i `ImmutableUppgift`-byggaren:
  ```java
  .regelutfall(ImmutableRegelutfall.builder().varde(utfall.getValue()).build())
  ```

- [ ] **4. Åtgärda brytande ändringar i `RegelManuellMiddlewareService`**

  - Ta bort imports: `ImmutableHandlaggningUpdate`, `Underlag`, `RegelUtils`
  - Ta bort dead-code-metoden `createHandlaggningUpdate(Handlaggning, Underlag)` (`Underlag` borttagen ur adapter)
  - `handlaggningUpdate.id()` → `handlaggningUpdate.handlaggning().id()` (×3)
  - `individYrkandeRoller()` → `rollerIYrkande()` i `extractIndivider()`

- [ ] **5. Åtgärda brytande ändringar i tester**

  **`AbstractRegelManuellTest`**
  - `skapadTs(` → `skapadTS(` i `ImmutableUppgift`-builder
  - Lägg till `@Inject protected RegelManuellTestService regelManuellTestService`
  - Anropa `regelManuellTestService.setNextUtfall(Utfall.JA)` i `@BeforeEach` för testisolering

  **`RegelManuellTestService`**
  - Lägg till `nextUtfall`-fält för konfigurerbart utfall per test
  - Lägg till `setNextUtfall(Utfall)`
  - Låt `done()` använda `nextUtfall` i stället för hårdkodat `Utfall.JA`
  - Förenkla `ImmutableHandlaggningUpdate`-builder till `{ handlaggning, uppgift }`

  **`RegelManuellMiddlewareServiceExceptionTest`**
  - Ersätt `.processInstansId(UUID.randomUUID())` med `.handlaggningIdTyp("TEST")` i `ImmutableHandlaggning`-builders
  - Ta bort `when(handlaggning.processInstansId()).thenReturn(...)` ur mock-setup

  **`RegelManuellReadDelegationTest`**
  - Ta bort `when(handlaggning.processInstansId()).thenReturn(...)`
  - `individYrkandeRoller()` → `rollerIYrkande()` i mock-setup

  **`RegelManuellSidCheckTest`**
  - `IndividYrkandeRoll` → `RollIYrkande` (import, typ, metodsignatur)
  - `individYrkandeRoller()` → `rollerIYrkande()`
  - Ta bort `when(handlaggning.processInstansId()).thenReturn(...)`

- [ ] **6. Uppdatera WireMock JSON-stubbar**

  - `"erbjudandeId"` → `"ingangtypId"`
  - `"individYrkandeRoller"` → `"rollerIYrkande"`
  - Lägg till obligatoriska fält `"id"` och `"avserYrkande"` på varje roll
  - Ta bort `"produceradeResultat"`, ersätt med `"sakfragorStallningstaganden": []` och `"beslut": []`
  - `"processinstansId"` → `"handlaggningIdTyp": "TEST"`
  - PUT-response: lyft ut `uppgift` ur `handlaggning` till toppnivå, `"skapadTs"` → `"skapadTS"`, lägg till `"utfordTS"` och `"resultat": []`

- [ ] **7. Lägg till testfall i `AbstractRegelManuellHandlaggningTest`**

  - `getUtfordTs()` → `getUtfordTS()` i befintligt test
  - Lägg till datadrivet test `should_put_handlaggning_with_regelutfall` med `@EnumSource(JA, NEJ)`

- [ ] **8. Bygg och testa**

  ```
  mvn spotless:apply
  mvn test
  ```
