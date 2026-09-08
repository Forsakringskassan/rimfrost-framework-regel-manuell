# Testgap — rimfrost-framework-regel-manuell

## Infrastruktur- och konfigurationsnivå — rimligt att lämna utan test

| Krav          | Kommentar                                                                                                                                                                                                                                                                   |
|---------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| FRMM-NFR-04.1 | Utbyggbarhet utan ramverksändringar demonstreras av befintliga regelimplementationer                                                                                                                                                                                        |
| FRMM-FR-07.1  | Kontraktsdefinition — verifieras vid kompilering av implementationer                                                                                                                                                                                                        |
| FRMM-FR-07.2  | Kontraktsdefinition — verifieras vid kompilering av implementationer                                                                                                                                                                                                        |
| FRMM-FR-04.5  | `@Valid @NotNull` på generisk typparameter `Y` i abstrakt klass aktiveras inte av Quarkus valideringsinterceptorn via den raderade brygmetoden — kan ej testas via HTTP-lagret med nuvarande testarkitektur; verifieras via kodinspektion att annotationerna finns på plats |
| FRMM-FR-06.6  | Samma tekniska begränsning som FR-04.5 — se ovan                                                                                                                                                                                                                            |
| FRMM-FR-06.2  | Avslutning av OUL-uppgift och error-response vid handläggningsuppdateringsfel efter OUL-skapande testas i `rimfrost-framework-regel-oul`; detta ramverk verifierar enbart att delegering sker via `createOulUppgift` |
| FRMM-FR-06.3  | Lagring av korrelationsdata och processroutingdata är helt delegerat till `rimfrost-framework-regel-oul`; felscenarier testas där; detta ramverk har inga lagringsoperationer att testa |
