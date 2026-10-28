# Krav — rimfrost-framework-regel-manuell

Kraven här beskriver endast det som är implementerat i detta ramverk. Ärvda krav från
underliggande ramverk upprepas inte.

## Funktionella krav

### FRMM-FR-01 — Konsumera RegelDataRequest

- **FRMM-FR-01.1** Ramverket ska konsumera `RegelDataRequest`-meddelanden via Kafka och starta
  regelkörningens livscykel.
- **FRMM-FR-01.2** Vid `RegelDataRequest` ska ramverket hämta aktuellt handläggningsärende från
  handläggningstjänsten.
- **FRMM-FR-01.3** Ramverket ska slå upp erbjudandets namn från referensdata och använda det vid
  skapande av OUL-uppgiften.

### FRMM-FR-02 — Skapa OUL-uppgift

- **FRMM-FR-02.1** Ramverket ska skapa en regel-OUL-uppgift när en `RegelDataRequest` tas emot,
  via `createOperativUppgift` som tillhandahålls av `rimfrost-framework-regel-oul`.

### FRMM-FR-04 — REST-gränssnitt mot handläggarportalen

- **FRMM-FR-04.1** Ramverket ska exponera ett REST-gränssnitt med följande operationer:
  - `GET /utokadUppgiftsbeskrivning` — hämta regelns utökade uppgiftsbeskrivning
  - `GET /{handlaggningId}` — hämta regelspecifikt handläggningsunderlag
  - `PATCH /{handlaggningId}` — uppdatera handläggningsunderlag
  - `POST /{handlaggningId}/done` — markera uppgiften som utförd
- **FRMM-FR-04.2** `GET /{handlaggningId}` ska delegera datahämtning till regelimplementationens
  `readData()`-metod och returnera regelspecifik svarstyp `T`, inpackad tillsammans med
  informationsaccessdata enligt FRMM-FR-09.
- **FRMM-FR-04.4** `PATCH /{handlaggningId}` ska delegera uppdateringen till
  regelimplementationens `updateData()`-metod med regelspecifik förfråganstyp `Y`.
- **FRMM-FR-04.5** `PATCH`-förfrågan ska valideras med `@Valid @NotNull` innan delegering.
- **FRMM-FR-04.6** `POST /{handlaggningId}/done` ska delegera avslutningslogiken till
  regelimplementationens `done()`-metod.

### FRMM-FR-05 — Avsluta regelkörning

- **FRMM-FR-05.1** Regelimplementationen ska kunna signalera regelkörningens utfall via
  `sendRegelResponse(handlaggningId, utfall)`.
- **FRMM-FR-05.2** Ramverket ska avsluta OUL-uppgiften med orsak "Uppgift klar" vid
  avslutning, via `endOperativUppgift` som tillhandahålls av `rimfrost-framework-regel-oul`.
- **FRMM-FR-05.3** Ramverket ska skicka ett `RegelResponse` med regelns utfall till den
  `replyTo`-topic som angavs i den ursprungliga regelförfrågan.
- **FRMM-FR-05.4** Ramverket ska rensa samtliga lagrade korrelationsdata, processroutingdata och
  uppgiftsmetadata efter avslutad regelkörning, via rensningsoperationen som tillhandahålls av
  `rimfrost-framework-regel-oul`.
- **FRMM-FR-05.5** Ramverket ska skicka `RegelResponse` även om den sista uppdateringen av
  handläggningsärendet misslyckas, så att regelsvaret alltid når kundbehovsflödet.
- **FRMM-FR-05.6** Ramverket ska uppdatera handläggningsärendet med uppgiftens slutstatus och
  utföringstidsstämpel efter avslutning.

### FRMM-FR-06 — Felhantering och återhämtning

- **FRMM-FR-06.1** Om skapandet av OUL-uppgiften misslyckas ska ramverket skicka ett
  error-response via Kafka utan att uppdatera handläggningsärendet.
- **FRMM-FR-06.2** Om uppdatering av handläggningsärendet misslyckas efter att OUL-uppgift
  skapats ska ramverket avsluta OUL-uppgiften och skicka ett error-response.
- **FRMM-FR-06.3** Om lagring av korrelationsdata eller processroutingdata misslyckas ska
  ramverket avsluta OUL-uppgiften och skicka ett error-response.
- **FRMM-FR-06.4** REST-anrop mot externa tjänster ska resultera i väldefinierade
  HTTP-statuskoder: 404 vid `NOT_FOUND`, 400 vid `BAD_REQUEST`, 503 vid
  `SERVICE_UNAVAILABLE`, och 500 vid övriga fel.
- **FRMM-FR-06.5** Okategoriserade exceptions från REST-gränssnittet ska resultera i HTTP 500 med
  ett konfigurerbart generellt felmeddelande. Standardvärdet ska vara `Internal Server Error`.
- **FRMM-FR-06.6** Valideringsfel från REST-gränssnittet ska resultera i HTTP 400.
- **FRMM-FR-06.7** 404 fel vid REST-anrop för att avsluta OUL uppgift behandlas som lyckat anrop.  

### FRMM-FR-07 — Kontraktsdefinierade gränssnitt för regelimplementationer

- **FRMM-FR-07.1** Ramverket ska definiera `RegelManuellServiceInterface<T, Y>` som
  regelimplementationer implementerar för att tillhandahålla regelspecifik läs-, uppdaterings-
  och avslutningslogik.
- **FRMM-FR-07.2** Ramverket ska definiera `RegelManuellController<T, Y>` som
  regelimplementationer ärver för att exponera REST-gränssnittet under regelns sökväg.

### FRMM-FR-08 — Kontroll av skyddad identitet (SID)

- **FRMM-FR-08.1** Innan läsning, uppdatering och avslut av uppgift kopplad
  till handläggningsärende så ska ramverket kontrollera om någon av
  handläggningsärendets individer har skyddad identitet via SID-tjänsten.
- **FRMM-FR-08.2** Individerna hämtas från `handlaggning.yrkande().individYrkandeRoller()` och
  skickas i en `POST /sid/status`-förfrågan till SID-tjänsten.
- **FRMM-FR-08.3** Om en eller flera individer har skyddad identitet och handläggaren saknar
  SID-rättigheter ska ramverket returnera HTTP 403 och den efterfrågade operationen ska inte utföras.
- **FRMM-FR-08.5** SID-kontrollen ska ingå i ramverket och gälla automatiskt för alla
  regelimplementationer utan kodändringar.
- **FRMM-FR-08.6** Om en eller flera individer har skyddad identitet och handläggaren saknar
  SID-rättigheter ska ramverket ta bort tilldelningen av OUL-uppgiften innan HTTP 403 returneras,
  via `tryUnassignOulUppgift` som tillhandahålls av `rimfrost-framework-regel-oul`, så att
  uppgiften återgår till otilldelat läge och kan tilldelas handläggare med SID-rättigheter.
- **FRMM-FR-08.7** Unassign ska alltid försökas oavsett om uppgiften är tilldelad eller inte —
  OUL-tjänsten förväntas hantera anropet korrekt i båda fallen. Om inget uppgifts-ID finns lagrat
  för handläggningsärendet (t.ex. vid en oväntad timingrelaterad situation) ska unassign-försöket
  hoppas över utan fel — HTTP 403 ska ändå returneras.
- **FRMM-FR-08.8** Fel vid unassign av OUL-uppgiften ska loggas men ska inte påverka det
  returnerade HTTP 403-svaret. `tryUnassignOulUppgift` hanterar loggning och sväljer felet internt.
- **FRMM-FR-08.9** Om SID detekteras ska ramverket kontrollera om den inloggade handläggaren har
  SID-rättigheter via `PermissionsAdapter.hasSidPermission()`. Handläggarens identitet (`idTyp`
  och `idVarde`) ska hämtas via `UserIdentityHolder.getIdentity()`, som håller den identitet som sparats av `UserIdentityFilter` (FRMM-FR-11.2).
- **FRMM-FR-08.10** Om handläggaren har SID-rättigheter ska den efterfrågade operationen utföras utan begränsning.
- **FRMM-FR-08.11** *(ej testbar — konfigurationskrav)* Ramverket ska läsa adressen till SID-tjänsten från property `sid.api.base-url`.
- **FRMM-FR-08.12** *(ej testbar — konfigurationskrav)* Ramverket ska läsa adressen till behörighetstjänsten från property `permissions.api.base-url`.
- **FRMM-FR-08.13** *(ej testbar — konfigurationskrav)* Ramverket ska läsa adressen till identity-tjänsten från property `quarkus.rest-client.identity-api.url`.

### FRMM-FR-09 — Informationsaccessdata i GET-svaret

En BFF som visar regelns data ska kunna publicera ett informationsaccess-event enligt
`rimfrost-service-informationsaccess-asyncapi`. Eventet ska ange
handläggare, processinstans, uppgiftsinstans, uppgiftsspecifikation, tidpunkt och den data
som visas i GUI:t. Individerna listas inte separat, eftersom personuppgifterna finns i den
data som visas. Processinstansen anges med `handlaggningId`, som BFF:en redan har, tillsammans
med `uppgiftId`. Svaret bär därför den data som BFF:en inte själv har (FKPOC-1103).

- **FRMM-FR-09.1** `GET /{handlaggningId}` ska returnera ett objekt, en container, med två
  fält: `data` med regelns eget svar oförändrat, och `informationsaccess` med
  informationsaccessdata. Fältet `data` är det som BFF:en lägger i eventets
  `visadInformation`.
- **FRMM-FR-09.2** `informationsaccess.uppgiftId` ska vara OUL-uppgiftens id från lagrad
  korrelationsdata. Det anger uppgiftsinstansen. Om korrelationsdata saknas ska ramverket
  returnera HTTP 500, eftersom uppgiften inte kan identifieras utan den.
- **FRMM-FR-09.3** `informationsaccess.regelId` och `informationsaccess.regelVersion` ska hämtas
  från regelns `config.yaml`.
- **FRMM-FR-09.4** Containern ska bara returneras när SID-kontrollen har passerat.
- **FRMM-FR-09.5** `informationsaccess.anvandare` ska vara den inloggade handläggarens
  identitet (`typId` och `varde`), som ramverket hämtar ur anropets token. Då behöver
  BFF:en inte själv tolka token.

### FRMM-FR-10 — Utility för kontroll av access till uppgift

- **FRMM-FR-10.1** Ramverket ska definiera en utility som kan användas vid förfrågan till
  REST-endpoints för att kontrollera att handläggare har rätt att arbeta med en uppgift.
  Om handläggaren inte har rätt att jobba med uppgiften så ska HTTP 403 returneras som svar.
- **FRMM-FR-10.2** Kontrollen ska verifiera att handläggaren är tilldelad som ansvarig för uppgiften.
- **FRMM-FR-10.3** SID kontroll enligt FRMM-FR-08 ska utföras som del av kontrollen för att säkerställa att handläggaren har rätt att se uppgiften.
- **FRMM-FR-10.4** Kontrollen ska returnera så snabbt som möjligt. Om kontroll av tilldelning misslyckas ska inte kontroll av SID göras.
- **FRMM-FR-10.5** Om handläggarens identitet saknas i UserIdentityHolder eller om UserIdentityHolder inte är tillgänglig så ska HTTP 401 returneras.

### FRMM-FR-11 — Kontroll av identitet vid REST förfrågan

- **FRMM-FR-11.1** Användarens identitet ska verifieras vid REST förfrågan via `IdentityAdapter.getIdentity()`. Om identity-tjänsten svarar med 401 så ska HTTP 401 returneras. Övriga fel ska hanteras enligt FRMM-FR-06.4.
- **FRMM-FR-11.2** Om användarens identitet kunde verifieras så ska den sparas till UserIdentityHolder för att enkelt kunna återanvändas utan att en ny förfrågan till identity-tjänsten ska behövas.

---

## Icke-funktionella krav

### FRMM-NFR-01 — Tillförlitlighet

- **FRMM-NFR-01.1** Ramverket ska garantera att ett regelsvar alltid skickas för varje mottagen
  regelförfrågan — antingen med utfall eller med felinformation.

### FRMM-NFR-02 — Testbarhet

- **FRMM-NFR-02.1** Ramverket ska leverera ett test-JAR med abstrakta testbasklasser och
  hjälpklasser som regelimplementationer kan ärva och använda i sina egna tester.

### FRMM-NFR-03 — Observerbarhet

- **FRMM-NFR-03.1** Alla fel i integrationer mot OUL, handläggningstjänsten, SID-tjänsten,
  behörighetstjänsten och Kafka-lagringen ska loggas med tillräcklig information för felsökning.

### FRMM-NFR-04 — Underhållbarhet

- **FRMM-NFR-04.1** En ny regelimplementation ska kunna driftsättas utan förändringar i ramverket,
  enbart genom att ärva basklasser, implementera gränssnitt och konfigurera egenskaper.
