# rimfrost-framework-regel-manuell

Quarkus-baserat ramverk för manuella regler i Rimfrost. Tillhandahåller gemensam
infrastruktur för Kafka-hantering, OUL-integration och REST-gränssnitt mot handläggarportalen.

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
| `/{handlaggningId}` | GET | Hämta regelspecifikt underlag tillsammans med informationsaccessdata |
| `/{handlaggningId}` | PATCH | Uppdatera regelspecifikt underlag |
| `/{handlaggningId}/done` | POST | Markera uppgift som klar |

Vid REST anrop utför ramverket automatiskt en kontroll av det identitets-token som skickas med förfrågan i `Authorization` 
headern mot Identity-tjänsten. Om ingen identitet kan fastställas från det givna header-värdet så returneras HTTP 401.
Identitet som returneras från kontroll av Identity-tjänsten sparas i UserIdentityHolder för enkel åtkomst vid
senare tillfällen.

Vid anrop till någon av de ändpunkter som ramverket tillhandahåller så appliceras access kontroll via `RegelAccessGuard`
automatiskt.

### 3. RegelAccessGuard

Ramverket tillhandahåller en utility klass `RegelAccessGuard` som kan kontrollera vid REST anrop att
användaren är tilldelad till uppgiften operationen utförs mot. HTTP 401 returneras om
användarens identitet inte kan avgöras och HTTP 403 returneras ifall användaren inte är tilldelad till uppgiften.

`RegelAccessGuard` verifierar även att användaren har rättighet att se ärendet om någon av ärendets individer har
skyddad identitet. Kontrollen av detta görs via anrop till
Permission och SID-tjänsterna. HTTP 403 returneras om rättigheter att utföra operationen saknas.

Svaret från `GET /{handlaggningId}` är en container: `data` innehåller regelns eget svar
oförändrat, och `informationsaccess` innehåller `uppgiftId`, `regelId`, `regelVersion` och
`anvandare` (den inloggade handläggarens identitet), som BFF:en behöver för att publicera ett
informationsaccess-event (FRMM-FR-09). Saknas lagrad korrelationsdata returneras HTTP 500.

### 3. Kafka

Ramverket hanterar även följande Kafka-kanaler:

| Kanal | Riktning | Beskrivning |
|---|---|---|
| `{regel-name}-in` | Inkommande | Regelförfrågan från kundbehovsflödet |
| `{replyTo}` | Utgående | Regelresultat till kundbehovsflödet |

Fullständiga API-specifikationer definieras i regelimplementationens OpenAPI- och AsyncAPI-repon.

```java
@Path("/min-regel")
public class MinRegelController extends RegelManuellController<MinRegelResponse, MinRegelRequest> {
}
```

### 4. Konfiguration

```properties
# Bas-URL till handläggnings-tjänsten (krävs - används för att läsa och skriva handläggningsinformation)
handlaggning.api.base-url=https://<handlaggning-service-host>
# Bas-URL till operativa uppgiftslagret (krävs - används för att kommunicera med operativa uppgiftslagret)
oul.api.base-url=https://<oul-service-host>
# Bas-URL till SID-tjänsten (krävs — används för skyddad identitet-kontroll vid GET)
sid.api.base-url=https://<sid-service-host>
# Bas-URL till Permission-tjänsten (krävs - används för kontroll av rättigheter)
permission.api.base-url=https://<permission-service-host>
# Bas-URL till Identity-tjänsten (krävs - används för kontroll av identitet)
quarkus.rest-client.identity-api.url=https://<identity-service-host>
```

---

## Test-JAR

Ramverket levererar en test-JAR med abstrakta basklasser och hjälpklasser för
regelimplementationernas tester.

### Abstrakta basklasser för test

Ärv och annoteras med `@QuarkusTest` och `@QuarkusTestResource` för att aktivera testerna:

| Klass                                               | Täcker                                          |
|-----------------------------------------------------|-------------------------------------------------|
| `AbstractRegelManuellTest`                          | Grundkonfiguration med Kafka och WireMock       |
| `AbstractRegelManuellHandlaggningTest`              | REST-gränssnittets grundflöden                  |
| `AbstractRegelManuellOulTest`                       | OUL-integration                                 |
| `AbstractRegelManuellResponseTest`                  | Regelsvar                                       |
| `AbstractRegelManuellUtokadUppgiftsbeskrivningTest` | Utökad uppgiftsbekrivning                       |
| `AbstractRegelManuellAccessTest`                    | Access kontroller för tillhandhållna ändpunkter |
```java
@QuarkusTest
@QuarkusTestResource.List({
    @QuarkusTestResource(WireMockRegelManuell.class)
})
public class MinRegelHandlaggningTest extends AbstractRegelManuellHandlaggningTest {
}
```

### Hjälpklasser

| Klass                  | Användning                                     |
|------------------------|------------------------------------------------|
| `RegelManuellTestData` | Metoder för testdata                           |
| `OulKafkaConnector`    | In-memory Kafka för OUL-kommunikation i tester |
| `WireMockRegelManuell` | WireMock-setup för externa HTTP-beroenden      |

`AbstractRegelManuellTest` mockar `OulUppgiftService` via `@InjectMock` — den publika
API-gränsen mot `rimfrost-framework-regel-oul`. Se [`docs/test-strategy.md`](docs/test-strategy.md)
för bakgrund och designbeslut.
