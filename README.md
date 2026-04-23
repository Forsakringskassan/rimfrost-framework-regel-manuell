# rimfrost-framework-regel-manuell

Ramverkskomponent med definitioner gemensamma för alla manuella regler.
Innehåller både framework-logik och hjälpklasser vid test av regler.


```text
root
├── src/main
│   └── (framework implementation)
├── src/test
│   └── (tester av ramverket)
├── src/test/base
│   └── (abstrakta testklasser)
├── src/test/helpers
    └── (helpers för testklasser)
```

# src/main

## RegelManuellService

Implementerar en service som en basklass där den specifika regeln gör _extends_ på denna
 basservice med regel-specifik logik. <br>

Notera att basservicen INTE annoteras som @ApplicationScoped, <br>
det måste servicen som _extendar_ basservice göra. t.ex:

```
@ApplicationScoped
@Startup
public class BekraftaBeslutService extends RegelManuellService
{
```

### Interface implementation

Implementerar _RegelRequestHandlerInterface_ eftersom alla manuella regler kan implementera samma logik
för _handleRegelRequest_.<br>
Implementerar även _OulHandlerInterface_ och _OulUppgiftDoneHandler_ som även de har samma logik för alla manuella regler.<br>

# src/test

Innehåller tester av ramverket, men även abstrakta testklasser som är byggda för att kunna användas och köras i den färdiga regeln.<br>
Ramverkstestklasserna _Regel*Test.java_ extendar de abstrakta testklasserna i _src/test/base_ för att kunna köras även för verifiering av ramverket.

Note: Vid bygge av test-jar-filen (som sedan används av de färdiga reglerna) inkluderas endast /base och /helpers.<br>
Övriga testklasser används bara vid verifiering av ramverket.

## src/test/base

Abstrakta testklasser _Abstract*Test.java_ som innehåller tester som behöver extendas och annoteras med QuarkusTest för att kunna köras.
T.ex. genom:
```
@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellHandlaggningTest extends AbstractRegelManuellHandlaggningTest
{
}
```

### RegelManuellTestData

Utility-klass som skapar testdata.

## src/test/helpers

Innehåller hjälpkomponenter för OUL-kafka-kommunikation och Wiremock för manuella regler. 

### OulKafkaConnector

Extendar _KafkaConnector_ för att hantera kommunikation med OUL.

### WireMockRegelManuell

Utility-klass för hantering av manuella reglers Wiremock-setup.