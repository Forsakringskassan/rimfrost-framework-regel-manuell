# Plan — FKPOC-1024: Använd unassign-wrapper från rimfrost-framework-regel-oul

## Mål

Ersätta det direkta anropet till `OulAdapter.unassignOperativUppgift()` i
`RegelManuellMiddlewareService` med `OulUppgiftService.tryUnassignOulUppgift()` från
`rimfrost-framework-regel-oul` (version 0.0.5). Det eliminerar behovet av att injicera
`OulAdapter` direkt i middleware-klassen och centraliserar all OUL-uppgiftslogik i wrappern.

### Notering: direkt beroende till rimfrost-framework-oul-adapter kvarstår

`rimfrost-framework-oul-adapter` tas **inte** bort som direkt Maven-dependency. Det beror på att
typer från adaptern fortfarande används direkt i detta ramverk:

- `OulException` — fångas och mappas till HTTP-statuskod i `RegelManuellRequestHandler`
- `Erbjudande` / `ImmutableErbjudande` — används för att bygga `OulUppgiftSpec`
- OUL-modelltyper (`ImmutableOperativUppgift`, m.fl.) — används i testbasklasserna

För att helt eliminera beroendet krävs bredare förändringar i `rimfrost-framework-regel-oul`:
ett eget undantagstyp som wrapprar `OulException`, samt modelltyper fristående från adaptern.
Det är utanför scopet för denna branch.

## Steg

- [x] **1. Uppdatera krav.md**
  Ändra FRMM-FR-08.6–08.8 så att de refererar till `OulUppgiftService.tryUnassignOulUppgift()`
  från `rimfrost-framework-regel-oul` i stället för `OulAdapter` direkt.

- [x] **2. Bumpa rimfrost-framework-regel-oul i pom.xml och rensa application.properties**
  Uppdatera `rimfrost-framework-regel-oul` från `0.0.4` till `0.0.5`.
  Behåll `rimfrost-framework-oul-adapter` i pom.xml — `OulException`, `Erbjudande` och
  `ImmutableErbjudande` används fortfarande direkt i `RegelManuellRequestHandler` och testkoden.
  Ta bort `quarkus.index-dependency`-raderna för oul-adapter i `src/main/resources/application.properties`
  — `rimfrost-framework-regel-oul` deklarerar redan samma index-dependency i sin egen
  `application.properties` och den laddas från JAR:en av Quarkus.

- [x] **3. Ersätt OulAdapter med OulUppgiftService i RegelManuellMiddlewareService**
  - Ta bort `@Inject OulAdapter oulAdapter`.
  - Injicera i stället `OulUppgiftService oulUppgiftService`.
  - Ersätt hela `unassignUppgift`-metodens try/catch med ett anrop till
    `oulUppgiftService.tryUnassignOulUppgift(oulUppgiftId)` — wrappern hanterar loggning och
    felsvälj internt.

- [x] **4. Verifiera tester — inga ändringar behövdes**
  `@InjectMock OulAdapter oulAdapter` i båda testklasserna fortsätter att fungera korrekt.
  `OulUppgiftService` injicerar fortfarande `OulAdapter` internt, så mocken fångas upp i
  anropskedjan middleware → `tryUnassignOulUppgift` → `oulAdapter.unassignOperativUppgift` (mockad).
  Verifieringarna i `RegelManuellSidCheckTest` passerar oförändrade.

- [x] **5. Kör tester och verifiera**
  `mvn test` — 54 tester, alla gröna.

## Filer som berörs

| Fil | Förändring |
|-----|------------|
| `docs/krav.md` | FRMM-FR-08.6–08.8 uppdateras |
| `pom.xml` | `rimfrost-framework-regel-oul` → 0.0.5 (behåll `rimfrost-framework-oul-adapter`) |
| `src/main/resources/application.properties` | Ta bort `quarkus.index-dependency` för oul-adapter |
| `src/main/java/.../logic/RegelManuellMiddlewareService.java` | Byt `OulAdapter` mot `OulUppgiftService` |
| `src/test/java/.../base/AbstractRegelManuellHandlaggningTest.java` | Uppdatera mock |
| `src/test/java/.../RegelManuellSidCheckTest.java` | Uppdatera mock och verifiering |
