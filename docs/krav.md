# Krav — rimfrost-framework-regel-manuell

## Funktionella krav

### FRMM-FR-01 — Konsumera RegelDataRequest

- **FRMM-FR-01.1** Ramverket ska konsumera `RegelDataRequest`-meddelanden via Kafka och starta
  regelkörningens livscykel.
- **FRMM-FR-01.2** Vid `RegelDataRequest` ska ramverket hämta aktuellt handläggningsärende från
  handläggningstjänsten.
- **FRMM-FR-01.3** Ramverket ska slå upp erbjudandets namn från referensdata och använda det vid
  skapande av OUL-uppgiften.
- **FRMM-FR-01.4** CloudEvent-attribut från inkommande meddelande ska bevaras och lagras
  persistent för korrelation under hela livscykeln.
- **FRMM-FR-01.5** `replyTo` från inkommande Kafka-meddelande ska lagras persistent för
  återkoppling vid regelkörningens avslut.

### FRMM-FR-02 — Skapa OUL-uppgift

- **FRMM-FR-02.1** Ramverket ska skapa en ny operativ uppgift i OUL när en `RegelDataRequest` tas emot.
- **FRMM-FR-02.2** OUL-uppgiften ska innehålla regelns namn, beskrivning, affärslogiktyp och roll.
- **FRMM-FR-02.3** OUL-uppgiften ska innehålla URL till regelns REST-gränssnitt för användning av
  handläggarportalen.
- **FRMM-FR-02.4** OUL-uppgiften ska innehålla CloudEvent-attributen från den inkommande
  regelförfrågan, för att möjliggöra korrelation.
- **FRMM-FR-02.5** OUL-uppgiften ska ange ett reply-subtopic som OUL använder för
  statusnotifieringar till ramverket.
- **FRMM-FR-02.6** Ramverket ska efter skapandet lagra uppgiftens metadata (uppgifts-ID, OUL:s
  uppgifts-ID) persistent.
- **FRMM-FR-02.7** Ramverket ska uppdatera handläggningsärendet med uppgiftsreferens och
  uppgiftsspecifikation efter skapandet.

### FRMM-FR-03 — Hantera OUL-statusnotifieringar

- **FRMM-FR-03.1** Ramverket ska prenumerera på OUL's statusnotifieringar via Kafka.
- **FRMM-FR-03.2** Vid statusnotifiering ska ramverket uppdatera uppgiftens version, status,
  utförar-ID och planerat tidsstämpel i den lagrade uppgiften.
- **FRMM-FR-03.3** Ramverket ska synkronisera uppdaterad uppgiftsstatus till handläggningstjänsten.
- **FRMM-FR-03.4** Statusuppdatering ska ske utan att handläggningsärendets egen version
  inkrementeras.

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
  avslutning.
- **FRMM-FR-05.3** Ramverket ska skicka ett `RegelResponse` med regelns utfall till det `replyTo`-ämne
  som angavs i den ursprungliga regelförfrågan.
- **FRMM-FR-05.4** Ramverket ska rensa samtliga lagrade korrelationsdata, processroutingdata och
  uppgiftsmetadata efter avslutad regelkörning.
- **FRMM-FR-05.5** Rensningsoperationerna ska genomföras med bästa möjliga ansträngning —
  fel i enskilda rensningar ska inte hindra övriga rensningar eller avsändande av regelsvaret.
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

---

## Persistenskrav

### FRMM-PR-01 — Tabeller och namngivning

- **FRMM-PR-01.1** Ramverket ska skapa tre tabeller i databasen med konfigurerbart prefix:
  `{prefix}_common_data`, `{prefix}_cloud_event_data` och `{prefix}_process_topic_info`.
- **FRMM-PR-01.2** Tabellprefixet ska vara konfigurerbart och unikt per regelimplementation
  för att möjliggöra deployment av flera regler i samma databas.
- **FRMM-PR-01.3** Ramverket ska rejecta uppstart om tabellprefixet inte är konfigurerat.
- **FRMM-PR-01.4** Databasmigrationer ska hanteras via Flyway och köras automatiskt vid uppstart.

---

## Icke-funktionella krav

### FRMM-NFR-01 — Tillförlitlighet

- **FRMM-NFR-01.1** Ramverket ska garantera att ett regelsvar alltid skickas för varje mottagen
  regelförfrågan — antingen med utfall eller med felinformation.
- **FRMM-NFR-01.2** Optimistisk låsning ska förhindra att samtida anrop mot samma
  handläggningsärende skriver över varandra.

### FRMM-NFR-02 — Testbarhet

- **FRMM-NFR-02.1** Ramverket ska leverera ett test-JAR med abstrakta testbasklasser och
  hjälpklasser som regelimplementationer kan ärva och använda i sina egna tester.

### FRMM-NFR-03 — Observerbarhet

- **FRMM-NFR-03.1** Alla fel i integrationer mot OUL, handläggningstjänsten och Kafka-lagringen
  ska loggas med tillräcklig information för felsökning.
- **FRMM-NFR-03.2** Misslyckade rensningsoperationer vid regelkörningens avslut ska loggas
  samlat utan att hindra regelsvaret från att skickas.

### FRMM-NFR-04 — Underhållbarhet

- **FRMM-NFR-04.1** En ny regelimplementation ska kunna driftsättas utan förändringar i ramverket,
  enbart genom att ärva basklasser, implementera gränssnitt och konfigurera egenskaper.


