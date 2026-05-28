# rimfrost-framework-regel-manuell changelog

Changelog of rimfrost-framework-regel-manuell.

## 1.0.12 (2026-05-28)

### Bug Fixes

-  bump rimfrost-framework-regel to 1.0.5 ([deeb0](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/deeb0d748caeec1) Ulf Slunga)  
-  Replace deprecated configuration property ([b0c0d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/b0c0d6c5f3d75e2) Lars Persson)  
-  Attempt to cleanup storage on non-interactive cases ([978e4](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/978e40e6713aff6) Lars Persson)  

## 1.0.11 (2026-05-25)

### Bug Fixes

-  Remove OUL Kafka messaging channels from application properties ([84edf](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/84edf866b119b40) Ulf Slunga)  

## 1.0.10 (2026-05-22)

### Bug Fixes

-  Send erbjudande information with OUL create request ([7ce95](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/7ce95c544c8ee51) Lars Persson)  
-  corrected test ([5ea4d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5ea4d2563e440db) Nils Elveros)  
-  test ([b36e0](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/b36e05b848f0d92) Nils Elveros)  
-  add more tests ([3abf9](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/3abf9eb96fb924a) Nils Elveros)  
-  Improve error handling robustness for non-interactive cases ([5fdda](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5fdda64e7bb9918) Lars Persson)  
-  add tests for endOperativUppgift ([968bb](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/968bbc456f89737) Nils Elveros)  

### Dependency updates

- update dependency org.immutables:value-processor to v2.12.2 ([8ce86](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/8ce866f0c4899e5) renovate[bot])  
## 1.0.9 (2026-05-22)

### Bug Fixes

-  add OUL REST WireMock stubs to WireMockRegelManuell ([c43c3](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c43c3833a990bc7) Ulf Slunga)  

## 1.0.8 (2026-05-21)

### Bug Fixes

-  switch to tryEndOperativUppgift and send uppgiftid ([162ad](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/162ad644de802e1) Nils Elveros)  
-  spotless ([25222](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2522220340a0acb) Nils Elveros)  
-  added fault handling when oul is down ([8699d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/8699d259b326f72) Nils Elveros)  
-  comments ([15cf8](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/15cf8eed79647e6) Nils Elveros)  
-  make null-safety tests properly reproduce bugs 1 and 2 ([43f29](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/43f29250c8ab9a1) Ulf Slunga)  
-  spotless ([ed1d9](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/ed1d93fa304f37d) Nils Elveros)  
-  fix final tests ([c0244](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c02448b9f67bd7d) Nils Elveros)  
-  update oul asyncapi spec ([2cf0c](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2cf0c6148224408) Nils Elveros)  
-  more test fixes ([2afc3](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2afc3d5c8195e14) Nils Elveros)  
-  update some tests ([cee51](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/cee51a68cfcb00a) Nils Elveros)  
-  add version to oulrequest ([2b75f](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2b75f7154dc990d) Nils Elveros)  
-  Use ouladapter ([26866](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/268664c370788ec) Nils Elveros)  

### Other changes

**Merge branch 'main' into fix/bump-oul**


[dd3c8](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/dd3c898ca44e2f7) Nils Elveros *2026-05-19 13:12:23*


## 1.0.7 (2026-05-19)

### Bug Fixes

-  Add waitForRegelManuellReady to AbstractRegelManuellTest ([b0238](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/b023856a9384e70) Ulf Slunga)  

### Other changes

**Add waitForRegelManuellReady to AbstractRegelManuellTest**

* Adds a shared waitForRegelManuellReady helper to the test base class, using the already-configured regel.manuell.base-path property to construct the polling URL. This allows regel implementations to drop their own local wait utilities and rely on the framework instead. 

[8de83](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/8de834037189e01) Ulf Slunga *2026-05-19 06:37:14*


## 1.0.6 (2026-05-18)

### Bug Fixes

-  public data entities ([29023](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/29023abd821c09e) Ulf Slunga)  
-  initial approach för persistent lagring av reglers data ([2be44](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2be44aa72ad0328) Ulf Slunga)  
-  **deps**  update dependency org.yaml:snakeyaml to v2.6 ([45bb2](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/45bb20562ee0e49) renovate[bot])  

### Other changes

**tabellnamn blir regel-specifika, specas i application.properties**


[4e05c](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/4e05c8d4c98bb73) Ulf Slunga *2026-05-18 11:31:16*


## 1.0.5 (2026-05-12)

### Bug Fixes

-  Use specific error codes for applicable error cases insted of RIMFROST_OTHER ([07413](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/074136fe791ca48) Lars Persson)  
-  **deps**  update dependency org.immutables:value to v2.12.1 ([06084](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/060849f1a985e6b) renovate[bot])  
-  returnerar 500 för uppläsningsfel på commondata. lagt till test av detta. ([e0178](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/e0178c50df5ecb7) Ulf Slunga)  

## 1.0.4 (2026-05-08)

### Bug Fixes

-  bumpar rimfrost-service-oul-asyncapi ([5e672](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5e67202cb80481f) Ulf Slunga)  
-  hanterar cloudevent attributes. avbryter uppgift vid exception i hantering av OUL response, men skickar regel respons. ([e6c47](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/e6c4732389b666e) Ulf Slunga)  

## 1.0.3 (2026-05-07)

### Bug Fixes

-  hanterar cloudevent attributes. avbryter uppgift vid exception men skickar regel respons. ([10fa5](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/10fa50d73ae0b2d) Ulf Slunga)  
-  hanterar cloudevent attributes ([3d270](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/3d2707a3fab6d09) Ulf Slunga)  

## 1.0.2 (2026-05-07)

### Bug Fixes

-  Fix minor issues with exception mappers ([d0c93](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/d0c930f760ad613) Lars Persson)  
-  Add exception mappers for consistent error response ([1b5c4](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/1b5c449c90e41df) Lars Persson)  
-  remove unused imports ([47241](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/47241e16b3abec5) Nils Elveros)  
-  add tests for middleware ([6169d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/6169d118d51fd54) Nils Elveros)  
-  full sökväg till CloudEventData ej nödvändigt ([5de32](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5de3244c612b196) Ulf Slunga)  
-  cloudevent data i separat storage interface ([64010](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/64010f6a9683572) Ulf Slunga)  

## 1.0.1 (2026-05-05)

### Bug Fixes

-  spotless ([19eb2](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/19eb27fe00ce5e9) Nils Elveros)  
-  small fix in RegelManuellMiddlewareService ([a03c6](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a03c6de0ce3db96) Nils Elveros)  
-  framework-regel bump depenedency ([64430](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/64430dff9556a8f) Nils Elveros)  
-  use RegelCancelledException ([c2ad8](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c2ad8b7c6ac717e) Nils Elveros)  
-  spotless ([744ac](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/744ac32277f1ede) Nils Elveros)  
-  removed unused import ([79e6f](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/79e6f8ff6981596) Nils Elveros)  
-  handle handlaggning errors ([a6d4a](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a6d4a31704454d8) Nils Elveros)  
-  Error handling for non-REST parts ([62b8c](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/62b8cfd666155ab) Lars Persson)  

### Dependency updates

- update dependency org.immutables:value-processor to v2.12.1 ([ae6c5](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/ae6c5848ef6fa19) renovate[bot])  
- update dependency org.awaitility:awaitility to v4.3.0 ([b37da](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/b37da07a874506a) renovate[bot])  
- update dependency org.apache.maven.plugins:maven-jar-plugin to v3.5.0 ([46999](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/4699927e694b46f) renovate[bot])  
- update dependency org.apache.maven.plugins:maven-compiler-plugin to v3.15.0 ([f3bb1](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/f3bb13fd067a52e) renovate[bot])  
### Other changes

**Merge branch 'main' into fix/handle-handlaggning-errors**


[813f2](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/813f2a151323500) Nils Elveros *2026-05-05 08:18:11*


## 1.0.0 (2026-04-28)

### Breaking changes

-  release 1.0 ([a61de](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a61de1b158ddd07) Ulf Slunga)  

### Features

-  release 1.0 ([a61de](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a61de1b158ddd07) Ulf Slunga)  

### Bug Fixes

-  released versions ([eb65a](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/eb65a97c2b175b8) Ulf Slunga)  

## 0.2.17 (2026-04-23)

### Bug Fixes

-  Uppdatering av README. Tar bort icke använd abstrakt testklass AbstractMockedRegelManuellTest. ([ff930](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/ff930a0ed457327) Ulf Slunga)  

## 0.2.16 (2026-04-23)

### Bug Fixes

-  Uppdatering av README. Tar bort icke använd abstrakt testklass AbstractMockedRegelManuellTest. ([816e7](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/816e72a365d5e73) Ulf Slunga)  

## 0.2.15 (2026-04-23)

### Bug Fixes

-  lägger till nytt OUL test ([cac29](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/cac29a0764807dc) Ulf Slunga)  

## 0.2.14 (2026-04-23)

### Bug Fixes

-  tar bort onödig inMemoryConnector null-check ([a5bbd](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a5bbd114cda5070) Ulf Slunga)  
-  tar bort annotation Inject för basePath ([5f26c](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5f26ca8ea3a0831) Ulf Slunga)  
-  minskar sleep i testfall ([70e4b](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/70e4b76b2dd5961) Ulf Slunga)  
-  tar bort onödig loggning ([25496](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/25496dcb072c09c) Ulf Slunga)  
-  single module. fixar för att kunna köra tester även av regler. ([2d99a](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2d99abc87dcfdf9) Ulf Slunga)  

## 0.2.13 (2026-04-22)

### Bug Fixes

-  bump framework-regel-version ([9cb8e](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/9cb8e843cbe5e4b) Nils Elveros)  
-  common manuell properties in in framework ([74b26](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/74b26f7a819e8e6) Nils Elveros)  

## 0.2.12 (2026-04-20)

### Bug Fixes

-  javadoc och readme ([c34cb](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c34cbfcc63a5682) Ulf Slunga)  

## 0.2.11 (2026-04-20)

### Bug Fixes

-  specar ej junit version i pom, bumpar version av quarkus-parent ([8584a](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/8584a9fcf722c1b) Ulf Slunga)  
-  flyttar regelKafkaConnector till basklass ([ab0d5](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/ab0d5c383e240f3) Ulf Slunga)  

### Dependency updates

- update dependency com.diffplug.spotless:spotless-maven-plugin to v3.4.0 ([b9abc](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/b9abc610961c14a) renovate[bot])  
## 0.2.10 (2026-04-17)

### Bug Fixes

-  Bump rimfrost-framework-regel version ([2b387](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2b387dab876a5eb) Lars Persson)  

## 0.2.9 (2026-04-15)

### Bug Fixes

-  Depend on UppgiftStatusProvider for upppgift status ids ([3bf2b](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/3bf2bb3ef9fa177) Lars Persson)  

## 0.2.8 (2026-04-14)

### Bug Fixes

-  uppgiftstatus hantering ([ab5d3](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/ab5d31e7d67447c) Ulf Slunga)  
-  conditional creation of kafka connectors ([4725a](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/4725a1d1de6fa29) Ulf Slunga)  
-  bumpar framework. tar bort onödiga wiremockmappings som hör hemma i resp regel. ([04d97](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/04d978b0dc8786b) Ulf Slunga)  
-  cleanup av imports ([85ecc](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/85ecc70d832fb0f) Ulf Slunga)  
-  test refactoring ([21a31](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/21a314ddbf71191) Ulf Slunga)  
-  tar bort setupRegelManuellTest eftersom loadTestProperties ärvs från basklass ([930f4](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/930f48ab713ef46) Ulf Slunga)  
-  flyttar befintlig src till /core ([0ff37](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/0ff370a67abb195) Ulf Slunga)  

## 0.2.7 (2026-04-02)

### Bug Fixes

-  Bump rimfrost-framework-regel version ([fb1a5](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/fb1a5ec65a47c77) Lars Persson)  

## 0.2.6 (2026-04-02)

### Bug Fixes

-  framework regel 0.3.29 ([5fcec](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5fcecf9da654ce7) Ulf Slunga)  

### Dependency updates

- add renovate.json ([6068e](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/6068ef4f52467d7) renovate[bot])  
## 0.2.5 (2026-04-01)

### Bug Fixes

-  update to new framework version ([c57fe](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c57fe5605a3dd3c) Nils Elveros)  

## 0.2.4 (2026-04-01)

### Bug Fixes

-  Bump rimfrost-framework-regel version ([70e5b](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/70e5bb2ac6379ac) Lars Persson)  

## 0.2.3 (2026-03-30)

### Bug Fixes

-  Step uppgift version on field change ([18927](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/18927fdb5d81081) Lars Persson)  

## 0.2.2 (2026-03-30)

### Bug Fixes

-  Remove handlaggning from ManuellRegelCommonData ([aa2e1](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/aa2e16e22f8c88c) Lars Persson)  

## 0.2.1 (2026-03-27)

### Bug Fixes

-  add uppgift in put in middleware ([fe9a1](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/fe9a16c00ba5452) Nils Elveros)  

## 0.2.0 (2026-03-26)

### Features

-  add a middleware service that logs handlaggning ([112f1](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/112f1898e533c06) Nils Elveros)  

### Bug Fixes

-  removed unused imports ([1093d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/1093d55b5119eaf) Nils Elveros)  
-  spotless ([a0364](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a0364e93c2cb5ed) Nils Elveros)  
-  remove mapper and use private method instead ([7bb97](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/7bb97cd714a2051) Nils Elveros)  
-  rename loggingservice to RegelManuellMiddlewareService ([ea45d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/ea45d2949d4b619) Nils Elveros)  
-  spotless ([d1597](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/d15978e297751ad) Nils Elveros)  
-  working on fixing tests ([bdbd1](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/bdbd14f89645701) Nils Elveros)  
-  refactoring av testerna ([e03f4](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/e03f48e931e760f) Ulf Slunga)  

### Other changes


## 0.1.29 (2026-03-25)

### Bug Fixes

-  createHandlaggning to createHandlaggningUpdate ([4a7a3](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/4a7a303a8ce77a3) Ulf Slunga)  
-  refactoring av testfall ([fd264](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/fd2641e95309dab) Ulf Slunga)  
-  lägger till notification vid status update ([6153b](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/6153bdf1763a26c) Ulf Slunga)  

## 0.1.28 (2026-03-24)

### Bug Fixes

-  Add support for sending individuals with OUL request message ([247f2](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/247f2a8de10b524) Lars Persson)  

## 0.1.27 (2026-03-19)

### Bug Fixes

-  Use kogitoprocInstanceId for handlaggning ([c1543](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c15437d70a3705a) Lars Persson)  

## 0.1.26 (2026-03-19)

### Bug Fixes

-  Bump rimfrost-framework-regel version ([2a603](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2a603249f1e8157) Lars Persson)  
-  Add support for aktivitetId ([051f0](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/051f0cef841fa42) Lars Persson)  

## 0.1.25 (2026-03-09)

### Bug Fixes

-  bump regel-framework version ([fa480](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/fa48092e6e19346) Nils Elveros)  

## 0.1.24 (2026-03-06)

### Bug Fixes

-  Use interface-based storage for common rule data ([356df](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/356df12d9c0b490) Lars Persson)  
-  Rename mapping files to handlaggning ([555fe](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/555fe8dd9fff38c) Lars Persson)  

## 0.1.23 (2026-03-04)

### Bug Fixes

-  Rename kundbehovsflode to handlaggning ([9a90d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/9a90d6db4590695) Lars Persson)  

## 0.1.22 (2026-02-26)

### Bug Fixes

-  Bump rimfrost-framework-regel version to include upstream bugfix ([421b1](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/421b17b68328996) Lars Persson)  
-  Add "NEJ" result test case ([a95c7](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a95c7d5d1eaec47) Lars Persson)  

## 0.1.21 (2026-02-25)

### Bug Fixes

-  Bump rimfrost-framework-regel version to fix upstream issue ([a53b0](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a53b06faa38eac9) Lars Persson)  
-  Add test for manuell framework ([3b7d0](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/3b7d00e05e4741e) Lars Persson)  

## 0.1.20 (2026-02-24)

### Bug Fixes

-  Bump rimfrost-framework-regel version ([4f583](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/4f5839dd6d0ad17) Lars Persson)  

## 0.1.19 (2026-02-24)

### Bug Fixes

-  Bump rimfrost-framework-regel version to include upstream bugfixes ([7fadf](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/7fadf7b93c79adf) Lars Persson)  

## 0.1.18 (2026-02-23)

### Bug Fixes

-  Bump rimfrost-framework-version ([244af](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/244af45a30a2a13) Lars Persson)  

## 0.1.17 (2026-02-23)

### Bug Fixes

-  Bump rimfrost-framework-regel version to include utforarId ([2105c](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2105ce94419fb62) Lars Persson)  

## 0.1.16 (2026-02-20)

### Bug Fixes

-  Use rule as a service instead of extending RegelManuellService ([3c9c8](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/3c9c80d62ab25b0) Lars Persson)  

## 0.1.15 (2026-02-20)

### Bug Fixes

-  Update rimfrost-framework-regel version ([88f74](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/88f74477bec51be) Lars Persson)  

## 0.1.14 (2026-02-18)

### Bug Fixes

-  Add getRegelData and getCloudEventData convenience methods ([c5c9c](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c5c9c710e2ff6e8) Lars Persson)  

## 0.1.13 (2026-02-17)

### Bug Fixes

-  Use rimfrost-framework-storage ([c523d](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/c523dd391888bce) Lars Persson)  

## 0.1.12 (2026-02-17)

### Bug Fixes

-  bump rimfrost-framework-regel ([7ec14](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/7ec14b9aead37ca) Ulf Slunga)  

## 0.1.11 (2026-02-16)

### Bug Fixes

-  uppdatering för framework regel ([d1825](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/d182524f449efe7) Ulf Slunga)  

## 0.1.10 (2026-02-13)

### Bug Fixes

-  beslutsutfall null ([0477e](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/0477ecdb61677fc) Ulf Slunga)  

## 0.1.9 (2026-02-12)

### Bug Fixes

-  bumping rimfrost-framework-oul version ([7a05c](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/7a05ccfecbef669) Ulf Slunga)  

## 0.1.8 (2026-02-12)

### Bug Fixes

-  use new framework-regel version ([487e7](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/487e7d89096da43) Nils Elveros)  

## 0.1.7 (2026-02-12)

### Bug Fixes

-  Bump rimfrost-framework-regel version ([5aed8](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5aed835952be889) Lars Persson)  

## 0.1.6 (2026-02-12)

### Bug Fixes

-  Bump rimfrost-framework-regel version ([10a47](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/10a471d94f965bc) Lars Persson)  

## 0.1.5 (2026-02-11)

### Bug Fixes

-  kundbehovsflode adapter ([3942f](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/3942fdb89cee830) Ulf Slunga)  

## 0.1.4 (2026-02-11)

### Bug Fixes

-  Add GET endpoint for extended task description ([799a0](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/799a075c1608d42) Lars Persson)  
-  Move RegelManuellController from rimfrost-framework-oul ([908df](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/908dfc49f5878ad) Lars Persson)  

## 0.1.3 (2026-02-10)

### Bug Fixes

-  kundbehovsflodeMapper ist för RegelManuellMapper ([a3bb5](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a3bb5c4fdb0501d) Ulf Slunga)  

## 0.1.2 (2026-02-10)

### Bug Fixes

-  regel provider från rimfrost-framework-regel ([915be](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/915bef9c34fed3b) Ulf Slunga)  

## 0.1.1 (2026-02-09)

### Bug Fixes

-  Decide Utfall value in a separate (abstract) method call ([4efdf](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/4efdf5bbfebc68a) Lars Persson)  

## 0.1.0 (2026-02-06)

### Features

-  send replyToTopic header in OUL message ([bb650](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/bb650cacd0f7bb1) Nils Elveros)  

### Bug Fixes

-  uppdaterar README ([a6815](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a68157ba05c64fe) Ulf Slunga)  
-  Update task status and utforarId on OUL status update ([611c8](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/611c8c477c9c881) Lars Persson)  

## 0.0.3 (2026-02-05)

### Bug Fixes

-  handleUppgiftDone ([875e7](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/875e72f583b5bf9) Ulf Slunga)  

## 0.0.2 (2026-02-05)

### Bug Fixes

-  lägger till handleOulResponse ([f9088](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/f90887c2c3202c0) Ulf Slunga)  

## 0.0.1 (2026-02-05)

### Bug Fixes

-  lägger till fler mappers, handlers och producers som är gemensamma för alla manuella regler ([5eb14](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/5eb14c522ef2272) Ulf Slunga)  
-  lägger till handleRegelRequest till framework för manuella regler ([a80dd](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/a80dd61a92fd8a0) Ulf Slunga)  

### Other changes

**Lägger till maven**


[43d2f](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/43d2f6bfb70646c) Ulf Slunga *2026-02-05 06:37:27*

**Lägger till gitignore**


[4f906](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/4f90688514e23e7) Ulf Slunga *2026-02-05 06:37:07*

**Create bundle-maven-lib-release.yaml**


[e4fa7](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/e4fa7e1ab7d48cb) Ulf Slunga *2026-02-04 15:05:12*

**Create bundle-maven-lib-ci.yaml**


[2cdc8](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2cdc8e117b13275) Ulf Slunga *2026-02-04 15:04:20*

**Create CODEOWNERS**


[1da46](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/1da4656dc76ba8d) Ulf Slunga *2026-02-04 15:02:54*

**Initial commit**


[2e551](https://github.com/Forsakringskassan/rimfrost-framework-regel-manuell/commit/2e551664872fd85) Ulf Slunga *2026-02-04 15:01:25*


