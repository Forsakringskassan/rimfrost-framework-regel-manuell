# rimfrost-framework-regel-manuell

Quarkus-baserat ramverk för manuella regler i Rimfrost. Tillhandahåller gemensam
infrastruktur för Kafka-hantering, OUL-integration, hantering av statusnotifieringar,
persistent mellanlagring och REST-gränssnitt mot handläggarportalen.

Manuella regler delar ett gemensamt integrationsmönster: ta emot regelförfrågan via Kafka,
skapa en uppgift i OUL, hantera statusuppdateringar och skicka ett regelsvar när handläggaren
är klar. Ramverket centraliserar denna logik så att regelimplementationer enbart behöver
bidra med regelspecifik affärslogik — all övrig orkestrering hanteras av ramverket.

## Aktörer

| Aktör | Roll |
|---|---|
| Regelimplementationer | Konsumerar ramverket genom att ärva basklasser och implementera definierade gränssnitt |
| Handläggarportalen | Anropar regelns REST-gränssnitt för att läsa underlag, registrera uppdateringar och markera uppgifter som klara |
| Kundbehovsflödet | Initierar regelkörningen via Kafka och tar emot regelsvaret |
| OUL (Operativt Uppgiftslager) | Tar emot uppgifter, hanterar tilldelning och publicerar statusnotifieringar |
| Handläggningstjänsten | Förvaltar ärendets livscykel och lagrar handläggningsunderlag |

```text
root
├── src/main/java       (ramverksimplementation)
└── src/test/java
    ├── (ramverkstester)
    ├── base/           (abstrakta testbasklasser — ingår i test-JAR)
    └── helpers/        (hjälpklasser för test — ingår i test-JAR)
```

---

## Implementera en manuell regel

En regelimplementation behöver tillhandahålla tre saker:

### 1. Serviceklass

Ärv `RegelManuellServiceBase` och implementera `RegelManuellServiceInterface<T, Y>` där
`T` är regelns GET-response-typ och `Y` är regelns PATCH-request-typ.

Klassen **måste** annoteras med `@ApplicationScoped` — ramverket tillhandahåller inte
denna annotering.

Eftersom `RegelManuellServiceInterface` utökar `KompletteringKontrollInterface` löser CDI
automatiskt upp ramverkets beroende av `KompletteringKontrollInterface` via serviceklassen.
Det förutsätter att exakt **en** `@ApplicationScoped`-bean implementerar
`RegelManuellServiceInterface` i applikationen. Standardimplementationen av
`checkKomplettering()` returnerar en tom lista — ingen komplettering initieras om inte
regelimplementationen åsidosätter metoden.

```java
@ApplicationScoped
@Startup
public class MinRegelService extends RegelManuellServiceBase
    implements RegelManuellServiceInterface<MinRegelResponse, MinRegelRequest> {

    @Override
    public MinRegelResponse readData(Handlaggning handlaggning) {
        // returnera regelspecifikt underlag baserat på ärendet
    }

    @Override
    public HandlaggningUpdate updateData(Handlaggning handlaggning, MinRegelRequest request) {
        // returnera HandlaggningUpdate med de ändringar som ska persisteras
    }

    @Override
    public void done(UUID handlaggningId) {
        sendRegelResponse(handlaggningId, Utfall.JA);
    }
}
```

### 2. REST-kontroller

Ärv `RegelManuellController<T, Y>` och annoteras med `@Path`. Ramverket tillhandahåller
automatiskt följande ändpunkter:

| Ändpunkt | Metod | Beskrivning |
|---|---|---|
| `/utokadUppgiftsbeskrivning` | GET | Utökad uppgiftsbeskrivning |
| `/{handlaggningId}` | GET | Hämta regelspecifikt underlag |
| `/{handlaggningId}` | PATCH | Uppdatera regelspecifikt underlag |
| `/{handlaggningId}/done` | POST | Markera uppgift som klar |

Vid `GET /{handlaggningId}` kontrollerar ramverket automatiskt om någon av ärendets individer
har skyddad identitet via SID-tjänsten. Om så är fallet returneras HTTP 403 och
`readData()` anropas inte. Konfigurationsegenskapen `sid.api.base-url` måste sättas i alla
regelimplementationer.

Ramverket hanterar även följande Kafka-kanaler:

| Kanal | Riktning | Beskrivning |
|---|---|---|
| `{regel-name}-in` | Inkommande | Regelförfrågan från kundbehovsflödet |
| `{subtopic}-status` | Inkommande | OUL-statusnotifieringar |
| `{replyTo}` | Utgående | Regelresultat till kundbehovsflödet |

Fullständiga API-specifikationer definieras i regelimplementationens OpenAPI- och AsyncAPI-repon.

```java
@Path("/min-regel")
public class MinRegelController extends RegelManuellController<MinRegelResponse, MinRegelRequest> {
}
```

### 3. Konfiguration

```properties
# Unikt prefix per regel — styr tabellnamn i databasen
regel.persistence.table-prefix=min_regel

# OUL-subtopic för statusnotifieringar till denna regel
kafka.subtopic=min-regel-reply

# Bas-URL till SID-tjänsten (krävs — används för skyddad identitet-kontroll vid GET)
sid.api.base-url=https://<sid-service-host>
```

---

## Test-JAR

Ramverket levererar en test-JAR med abstrakta basklasser och hjälpklasser för
regelimplementationernas tester.

### Abstrakta basklasser för test

Ärv och annoteras med `@QuarkusTest` och `@QuarkusTestResource` för att aktivera testerna:

| Klass                                               | Täcker                                    |
|-----------------------------------------------------|-------------------------------------------|
| `AbstractRegelManuellTest`                          | Grundkonfiguration med Kafka och WireMock |
| `AbstractRegelManuellHandlaggningTest`              | REST-gränssnittets grundflöden            |
| `AbstractRegelManuellOulTest`                       | OUL-integration och statusnotifieringar   |
| `AbstractRegelManuellResponseTest`                  | Regelsvar                                 |
| `AbstractRegelManuellUtokadUppgiftsbeskrivningTest` | Utökad uppgiftsbekrivning                 |

```java
@QuarkusTest
@QuarkusTestResource.List({
    @QuarkusTestResource(WireMockRegelManuell.class)
})
public class MinRegelHandlaggningTest extends AbstractRegelManuellHandlaggningTest {
}
```

### Hjälpklasser

| Klass                  | Användning                                        |
|------------------------|---------------------------------------------------|
| `RegelManuellTestData` | Metoder för testdata                              |
| `OulKafkaConnector`    | In-memory Kafka för OUL-kommunikation i tester    |
| `WireMockRegelManuell` | WireMock-setup för externa HTTP-beroenden         |
| `StorageTestCleaner`   | Rensar ramverkets lagrade tillstånd mellan tester |
