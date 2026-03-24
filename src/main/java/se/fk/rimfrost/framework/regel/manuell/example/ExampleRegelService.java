package se.fk.rimfrost.framework.regel.manuell.example;

import java.util.UUID;

import se.fk.rimfrost.framework.regel.Utfall;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceBase;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;

public class ExampleRegelService extends RegelManuellServiceBase implements RegelManuellServiceInterface<RegelResponse, PatchRegelRequest>  {

    @Override
    public RegelResponse get(UUID handlaggningId) {
      return new RegelResponse();
    }

    @Override
    public RegelResponse patch(PatchRegelRequest request) {
        return new RegelResponse();
    }

    @Override
    public void handleRegelDone(UUID handlaggningId) {
        sendRegelResponse(handlaggningId, Utfall.JA);
    }
    
}
