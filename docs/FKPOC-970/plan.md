# FKPOC-970 — Komplettering-refactoring: implementationsplan

## Bakgrund

`docs/krav.md` har skalats ner: alla krav som rör komplettering,
persistens och OUL-statusmottagning har tagits bort. Ansvaret för att skapa
OUL-uppgifter, ta emot OUL-statusnotifieringar, lagra korrelationsdata och
rensa flyttas till `rimfrost-framework-regel-oul`, som exponerar
följande publika API:er:

- `OulUppgiftService.createOulUppgift(OulUppgiftSpec)` — skapar OUL-uppgift
  och persisterar korrelationsdata (FR-02.1, FR-01.2, FR-01.3).
- `OulUppgiftService.endOulUppgift(UUID, String)` — kastar `OulException` vid
  fel; används från REST-`done`-flödet så att fel bubblar upp som HTTP 5xx (FR-05.2).
- `OulUppgiftService.cleanupCorrelation(UUID)` — rensar korrelationsdata (FR-05.4).
- `OulUppgiftService.getCorrelationData(UUID)` — returnerar ett samlat
  `OulCorrelationData`-värde med uppgifts-ID, reply-topic och CloudEvent-attribut;
  returnerar `null` om korrelationsdata saknas (FRMM-FR-06.5).
- `OulUppgiftService.handleOulStatus(OulStatus)` — implementerar
  `OulHandlerInterface` och äger hela FR-03-flödet.

Detta ramverk ska bli en tunn komposition ovanpå regel-oul + regel-basramverket.
Det ska inte längre äga persistens eller Kafka-lyssnare för OUL-status.

---

## Steg

- [x] **1. Ta bort kompletterings-hooken från kontraktet**
  - `RegelManuellServiceInterface<T,Y>` ska inte längre extenda
    `KompletteringKontrollInterface`.
  - Ta bort importen.

- [x] **2. Ta bort komplettering från `RegelManuellRequestHandler`**
  - Ta bort fälten `kompletteringKontroll` och `kompletteringOulHandler`
    samt tillhörande imports.
  - Ta bort blocket i `handleRegelRequest()` som anropar
    `checkKomplettering(...)` och `kompletteringOulHandler.initiate(...)`.

- [x] **3. Lägg till beroende till `rimfrost-framework-regel-oul`**
  - Dependency på `rimfrost-framework-regel-oul` tillagd i `pom.xml`.
  - Uppdatera `quarkus.index-dependency.*` i `application.properties`.

- [x] **4. Delegera OUL-skapande i `handleRegelRequest`**
  - Injecta `OulUppgiftService` (från regel-oul).
  - Bygg en `OulUppgiftSpec` från `RegelDataRequest` +
    `Handlaggning` + `erbjudandeNamn` + `regelConfig` + cloudevent-attribut.
  - Anropa `oulUppgiftService.createOulUppgift(spec)`.
  - Ta bort all lokal duplikerad logik: `createOperativUppgift`,
    `writeCloudEventData`, `writeProcessTopicInfo`,
    `writeRegelCommonData`, `updateHandlaggning`, `createUppgift`,
    `createHandlaggningUpdate`.

- [x] **5. Sluta hantera OUL-status här**
  - Ta bort metoden `handleOulStatus(...)`.
  - Ta bort `implements OulHandlerInterface`.

- [x] **6. Refaktorera `handleUppgiftDone`**
  - Anropa `oulUppgiftService.getCorrelationData(handlaggningId)` för att
    hämta `oulUppgiftId`, `replyTopic` och `cloudEventData` i ett steg.
  - `null`-retur kastar `RegelManuellException` med HTTP 500.
  - Anropa `oulUppgiftService.endOulUppgift(...)` och
    `oulUppgiftService.cleanupCorrelation(...)`.
  - Skicka `RegelResponse` och uppdatera handläggning.

- [x] **7. Sluta ärva `RegelRequestHandlerBase`**
  - Ta bort `extends RegelRequestHandlerBase` från `RegelManuellRequestHandler`.

- [x] **8. Ta bort persistens-infrastruktur från ramverket**
  - Ur `pom.xml`: `quarkus-hibernate-orm-panache`,
    `quarkus-jdbc-postgresql`, `quarkus-flyway`.
  - Ur `application.properties`: alla `quarkus.datasource.*`,
    `quarkus.hibernate-orm.*`, `quarkus.flyway.*`.
  - Ta bort Kafka-listener-konfiguration för
    `operativt-uppgiftslager-status-notification`.

- [x] **9. Rensa testbaser och tester**
  - Ta bort tester för kompletteringsflödet och `handleOulStatus`.
  - `@InjectMock OulUppgiftService` med standardstubs i `AbstractRegelManuellTest`
    (se `docs/test-strategy.md`).
  - Synkronisering via `waitForRegelRequestProcessed` (WireMock GET-signal).
  - Ta bort SQL-migrationsfiler och obsolet ORM-konfiguration från testresurser.

- [x] **10. Krav-fix i `docs/krav.md`**
  - FR-05.2: korrigera referens från `tryEndOperativUppgift` →
    `endOperativUppgift`.

- [x] **11. Bygg och testa**
  - `mvn spotless:apply` + `mvn test`

- [x] **12. Uppdatera `docs/testgap.md`**
  - Ta bort rader kopplade till borttagna krav.

- [x] **13. Uppdatera `rimfrost-framework-regel-oul`-versionen i `pom.xml`**
  - Byt versionsnummer till den release som innehåller `getCorrelationData` och
    `OulCorrelationData`.

- [x] **14. Verifiera `handleUppgiftDone` mot ny version**
  - Bekräfta att `getCorrelationData` kompilerar och beter sig korrekt mot
    den releasade artefakten.
  - Ta bort eventuella `@Inject`-fält för storage-gränssnitt om de kvarstår.

- [x] **15. Verifiera `AbstractRegelManuellTest` mot ny version**
  - Bekräfta att `@InjectMock OulUppgiftService` kompilerar med den releasade
    artefakten och att `OulCorrelationData` är tillgänglig på classpath.

- [x] **16. Uppdatera OUL-felhanteringstester**
  - `RegelManuellOulFaultHandlingTest`: bekräfta att `OulAdapter`-mocken är
    borttagen och att OUL-fel simuleras via `oulUppgiftService`.

- [x] **17. Uppdatera storage-felhanteringstester**
  - `RegelManuellStorageFaultHandlingTest`: bekräfta att ett enda
    `getCorrelationData(...) → null`-scenario täcker tidigare tre separata fall.

- [x] **18. Bygg och verifiera mot relesat beroende**
  - `mvn spotless:apply`
  - `mvn test`
  - Ta bort `docs/option2-mock-oul-service.md` (nu inaktuell).

- [x] **19. Uppdatera till `rimfrost-framework-regel-oul` 0.0.4**
  *(förutsätter att FKPOC-1021 är relesat som 0.0.4)*
  - Byt version i `pom.xml` från `0.0.3` till `0.0.4`.
  - `CloudEventData` i `rimfrost-framework-regel-oul` är nu borttagen — ersatt av typen
    från `rimfrost-framework-regel`.

- [x] **20. Förenkla `handleRegelRequest` till en enda `CloudEventData`-variabel**
  - Slå ihop `baseCloudEvent` och `oulCloudEventData` till en enda `cloudEvent`
    byggd med `.from(buildBaseCloudEvent(request)).type(responseTopic).source(kafkaSource)`.
  - Används för `cloudEventData`, `cloudEventAttributes` och felresponsen.

- [x] **21. Bygg och testa**
  - `mvn spotless:apply`
  - `mvn test`

---

## Design

### Klassomorganisation

`RegelManuellRequestHandler` — före → efter:

```java
// Före
public class RegelManuellRequestHandler
      extends RegelRequestHandlerBase
      implements OulHandlerInterface,
                 RegelRequestHandlerInterface,
                 RegelManuellUppgiftDoneHandler
{
   @Inject ErbjudandeReferensdataInterface erbjudandeReferensdata;
   @Inject KompletteringKontrollInterface  kompletteringKontroll;
   @Inject KompletteringOulHandler         kompletteringOulHandler;
   // ... ärver oulAdapter, handlaggningAdapter, storages, regelConfig, ...

   public void handleRegelRequest(RegelDataRequest r) { /* 100 rader */ }
   public void handleUppgiftDone(UUID h, Utfall u)     { /* 100 rader */ }
   public void handleOulStatus(OulStatus s)            { /* 60 rader */ }
}

// Efter
public class RegelManuellRequestHandler
      implements RegelRequestHandlerInterface,
                 RegelManuellUppgiftDoneHandler
{
   @Inject ErbjudandeReferensdataInterface erbjudandeReferensdata;
   @Inject OulUppgiftService               oulUppgiftService;
   @Inject HandlaggningAdapter             handlaggningAdapter;
   @Inject RegelKafkaProducer              regelKafkaProducer;
   @Inject RegelConfig                     regelConfig;

   public void handleRegelRequest(RegelDataRequest r) {
      CloudEventData ce = CloudEventAttributesMapper.toCloudEventData(r);
      Handlaggning h    = handlaggningAdapter.readHandlaggning(r.handlaggningId());
      String namn       = erbjudandeReferensdata.getErbjudandeNamn(h.yrkande().erbjudandeId());

      OulUppgiftSpec spec = ImmutableOulUppgiftSpec.builder()
            /* ... */
            .build();

      try {
         oulUppgiftService.createOulUppgift(spec);
      } catch (OulException | RegelCancelledException e) {
         sendErrorResponse(r, ce, e);
      }
   }

   public void handleUppgiftDone(UUID handlaggningId, Utfall utfall) {
      var correlation = oulUppgiftService.getCorrelationData(handlaggningId);
      if (correlation == null)
      {
         throw new RegelManuellException(Response.Status.INTERNAL_SERVER_ERROR,
               "Failed to read correlation data");
      }
      Handlaggning h = handlaggningAdapter.readHandlaggning(handlaggningId);

      try {
         oulUppgiftService.endOulUppgift(correlation.oulUppgiftId(), "Uppgift klar");
      } catch (OulException e) {
         throw new RegelManuellException(toHttpStatus(e), e.getMessage(), e);
      }
      regelKafkaProducer.sendRegelResponse(
            buildResponse(handlaggningId, correlation.cloudEventData(), utfall),
            correlation.replyTopic());
      oulUppgiftService.cleanupCorrelation(handlaggningId);
      handlaggningAdapter.updateHandlaggning(buildFinalUpdate(h, correlation.uppgift(), utfall));
   }
}
```

### Avgränsning: FR-08.6–8 (skyddad identitet, unassign)

Logiken för tilldelningsavbokning vid SID-träff finns i
`RegelManuellMiddlewareService` och anropar `oulAdapter.unassignOperativUppgift(...)`
direkt. Den klassen berörs inte av denna refactoring och lämnas oförändrad.
