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
  `readData()`-metod och returnera regelspecifik svarstyp `T`.
- **FRMM-FR-04.3** Vid `GET /{handlaggningId}` ska ramverket skapa ett handläggningsunderlag av
  typen "GetResponse" och uppdatera handläggningsärendet.
- **FRMM-FR-04.4** `PATCH /{handlaggningId}` ska delegera uppdateringen till
  regelimplementationens `updateData()`-metod med regelspecifik förfråganstyp `Y`.
- **FRMM-FR-04.5** `PATCH`-förfrågan ska valideras med `@Valid @NotNull` innan delegering.
- **FRMM-FR-04.6** `POST /{handlaggningId}/done` ska delegera avslutningslogiken till
  regelimplementationens `done()`-metod.

### FRMM-FR-05 — Avsluta regelkörning

- **FRMM-FR-05.1** Regelimplementationen ska kunna signalera regelkörningens utfall via
  `sendRegelResponse(handlaggningId, utfall)`.
- **FRMM-FR-05.2** Ramverket ska avsluta OUL-uppgiften med orsak "Uppgift klar" vid
  avslutning, via `tryEndOperativUppgift` som tillhandahålls av `rimfrost-framework-regel-oul`.
- **FRMM-FR-05.3** Ramverket ska skicka ett `RegelResponse` med regelns utfall till den
  `replyTo`-topic som angavs i den ursprungliga regelförfrågan.
- **FRMM-FR-05.4** Ramverket ska rensa samtliga lagrade korrelationsdata, processroutingdata och
  uppgiftsmetadata efter avslutad regelkörning, via rensningsoperationen som tillhandahålls av
  `rimfrost-framework-regel-oul`.
- **FRMM-FR-05.6** Ramverket ska uppdatera handläggningsärendet med uppgiftens slutstatus och
  utföringstidsstämpel efter avslutning.

### FRMM-FR-06 — Felhantering och återhämtning

- **FRMM-FR-06.1** Om skapandet av OUL-uppgiften misslyckas ska ramverket skicka ett
  error-response via Kafka utan att uppdatera handläggningsärendet.
- **FRMM-FR-06.2** Om uppdatering av handläggningsärendet misslyckas efter att OUL-uppgift
  skapats ska ramverket avsluta OUL-uppgiften och skicka ett error-response.
- **FRMM-FR-06.3** Om lagring av korrelationsdata eller processroutingdata misslyckas ska
  ramverket avsluta OUL-uppgiften och skicka ett error-response.
- **FRMM-FR-06.4** REST-anrop mot handläggningstjänsten och OUL ska resultera i väldefinierade
  HTTP-statuskoder: 404 vid `NOT_FOUND`, 400 vid `BAD_REQUEST`, 503 vid
  `SERVICE_UNAVAILABLE`, och 500 vid övriga fel.
- **FRMM-FR-06.5** Okategoriserade exceptions från REST-gränssnittet ska resultera i HTTP 500 med
  ett konfigurerbart generellt felmeddelande. Standardvärdet ska vara `Internal Server Error`.
- **FRMM-FR-06.6** Valideringsfel från REST-gränssnittet ska resultera i HTTP 400.

### FRMM-FR-07 — Kontraktsdefinierade gränssnitt för regelimplementationer

- **FRMM-FR-07.1** Ramverket ska definiera `RegelManuellServiceInterface<T, Y>` som
  regelimplementationer implementerar för att tillhandahålla regelspecifik läs-, uppdaterings-
  och avslutningslogik.
- **FRMM-FR-07.2** Ramverket ska definiera `RegelManuellController<T, Y>` som
  regelimplementationer ärver för att exponera REST-gränssnittet under regelns sökväg.

### FRMM-FR-08 — Kontroll av skyddad identitet (SID)

- **FRMM-FR-08.1** Innan `readData()` anropas vid `GET /{handlaggningId}` ska ramverket kontrollera
  om någon av handläggningsärendets individer har skyddad identitet via SID-tjänsten.
- **FRMM-FR-08.2** Individerna hämtas från `handlaggning.yrkande().individYrkandeRoller()` och
  skickas i en `POST /sid/status`-förfrågan till SID-tjänsten.
- **FRMM-FR-08.3** Om en eller flera individer har skyddad identitet ska ramverket returnera
  HTTP 403 och `readData()` ska inte anropas.
- **FRMM-FR-08.4** Fel från SID-tjänsten ska resultera i väldefinierade HTTP-statuskoder på samma
  sätt som fel mot handläggningstjänsten: 404, 400, 503 respektive 500.
- **FRMM-FR-08.5** SID-kontrollen ska ingå i ramverket och gälla automatiskt för alla
  regelimplementationer utan kodändringar. Varje regelimplementation måste konfigurera `sid.api.base-url`
  med adressen till SID-tjänsten.
- **FRMM-FR-08.6** Om en eller flera individer har skyddad identitet ska ramverket ta bort tilldelningen av OUL-uppgiften
  (via `POST /uppgifter/{uppgiftId}/unassign`) innan HTTP 403 returneras, så att uppgiften
  återgår till otilldelat läge och kan tilldelas handläggare med SID-rättigheter.
- **FRMM-FR-08.7** Unassign ska alltid försökas oavsett om uppgiften är tilldelad eller inte —
  OUL-tjänsten förväntas hantera anropet korrekt i båda fallen. Om inget uppgifts-ID finns lagrat
  för handläggningsärendet (t.ex. vid en oväntad timingrelaterad situation) ska unassign-försöket
  hoppas över utan fel — HTTP 403 ska ändå returneras.
- **FRMM-FR-08.8** Fel vid unassign av OUL-uppgiften ska loggas men ska inte påverka det
  returnerade HTTP 403-svaret.

---

## Icke-funktionella krav

### FRMM-NFR-01 — Tillförlitlighet

- **FRMM-NFR-01.1** Ramverket ska garantera att ett regelsvar alltid skickas för varje mottagen
  regelförfrågan — antingen med utfall eller med felinformation.

### FRMM-NFR-02 — Testbarhet

- **FRMM-NFR-02.1** Ramverket ska leverera ett test-JAR med abstrakta testbasklasser och
  hjälpklasser som regelimplementationer kan ärva och använda i sina egna tester.

### FRMM-NFR-03 — Observerbarhet

- **FRMM-NFR-03.1** Alla fel i integrationer mot OUL, handläggningstjänsten och Kafka-lagringen
  ska loggas med tillräcklig information för felsökning.

### FRMM-NFR-04 — Underhållbarhet

- **FRMM-NFR-04.1** En ny regelimplementation ska kunna driftsättas utan förändringar i ramverket,
  enbart genom att ärva basklasser, implementera gränssnitt och konfigurera egenskaper.
