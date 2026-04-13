package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import org.mockito.Mockito;
import se.fk.rimfrost.framework.regel.*;
import se.fk.rimfrost.framework.regel.manuell.jaxrsspec.controllers.generatedsource.model.GetUtokadUppgiftsbeskrivningResponse;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellServiceInterface;
import java.util.UUID;
import static io.restassured.RestAssured.given;

public class RegelManuellTest extends RegelManuellTestBase
{

   @InjectMock
   RegelManuellServiceInterface<String, String> regelManuellService;

   //
   // Rest assured helpers
   //

   protected void sendPostRegelManuellHandlaggningDone(String handlaggningId)
   {
      given().when().post("/regel/manuell/{handlaggningId}/done", handlaggningId).then().statusCode(204);
   }

   protected GetUtokadUppgiftsbeskrivningResponse sendGetUtokadUppgiftsbeskrivning()
   {
      return given().when().get("/regel/manuell/utokadUppgiftsbeskrivning").then().statusCode(200).extract()
            .as(GetUtokadUppgiftsbeskrivningResponse.class);
   }

   //
   // Mocking
   //

   protected void mockRegelService(Utfall utfall, String handlaggningId)
   {
      Mockito.doNothing().when(regelManuellService)
            .done(UUID.fromString(handlaggningId));
   }

}
