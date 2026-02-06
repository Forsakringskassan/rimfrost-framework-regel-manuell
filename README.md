# rimfrost-framework-regel-manuell

Ramverkskomponent med definitioner gemensamma för alla manuella regler.

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

