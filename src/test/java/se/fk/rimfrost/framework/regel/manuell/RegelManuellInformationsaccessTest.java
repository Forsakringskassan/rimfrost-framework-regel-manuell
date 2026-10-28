package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import se.fk.rimfrost.adapter.identity.adapter.IdentityAdapter;
import se.fk.rimfrost.adapter.identity.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelAccessGuard;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellTestService;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies FRMM-FR-09: GET /{handlaggningId} returns informationsaccess data alongside the
 * rule-specific response.
 */
@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellInformationsaccessTest
{
   // Values from src/test/resources/config-test.yaml
   private static final UUID REGEL_ID = UUID.fromString("a11f3429-3a6b-4389-ba1c-c2747b0fb45b");
   private static final String REGEL_VERSION = "1";
   private static final String ID_TYP = "PERSONNR";
   private static final String ID_VARDE = "19901010-1234";

   @ConfigProperty(name = "regel.manuell.base-path")
   String basePath;

   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   RegelAccessGuard regelAccessGuard;

   @InjectMock
   IdentityAdapter identityAdapter;

   @InjectMock
   OulUppgiftService oulUppgiftService;

   @InjectMock
   RegelManuellTestService regelService;

   @Test
   @DisplayName("FRMM-FR-09.1: read() returnerar regelimplementationens svar i data")
   void read_should_return_rule_response_as_data() throws Exception
   {
      var handlaggning = givenHandlaggning();
      givenCorrelationData(UUID.randomUUID());
      when(regelService.readData(handlaggning)).thenReturn("expectedResult");

      var result = service.read(handlaggning.id());

      assertEquals("expectedResult", result.data());
   }

   @Test
   @DisplayName("FRMM-FR-09.2: uppgiftId är OUL-uppgiftens id från korrelationsdatan")
   void read_should_return_oul_uppgift_id() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = givenHandlaggning();
      givenCorrelationData(oulUppgiftId);

      var result = service.read(handlaggning.id());

      assertEquals(oulUppgiftId, result.informationsaccess().uppgiftId());
   }

   @Test
   @DisplayName("FRMM-FR-09.2: HTTP 500 returneras och readData() anropas inte när korrelationsdata saknas")
   void read_should_throw_500_without_correlation_data() throws Exception
   {
      var handlaggning = givenHandlaggning();
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(null);

      var ex = assertThrows(RegelManuellException.class, () -> service.read(handlaggning.id()));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, ex.getStatus());
      verify(regelService, never()).readData(any());
   }

   @Test
   @DisplayName("FRMM-FR-09.1: GET /{handlaggningId} returnerar data och informationsaccess som JSON")
   void get_should_return_container_as_json() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = givenHandlaggning();
      givenCorrelationData(oulUppgiftId);
      when(regelService.readData(handlaggning)).thenReturn("expectedResult");

      given().when().get(basePath + "/{handlaggningId}", handlaggning.id())
            .then()
            .statusCode(200)
            .body("data", equalTo("expectedResult"))
            .body("informationsaccess.uppgiftId", equalTo(oulUppgiftId.toString()))
            .body("informationsaccess.regelId", equalTo(REGEL_ID.toString()))
            .body("informationsaccess.regelVersion", equalTo(REGEL_VERSION));
   }

   @Test
   @DisplayName("FRMM-FR-09.3: regelId och regelVersion hämtas från regelns config.yaml")
   void read_should_return_regel_id_and_version_from_config() throws Exception
   {
      var handlaggning = givenHandlaggning();
      givenCorrelationData(UUID.randomUUID());

      var result = service.read(handlaggning.id());

      assertEquals(REGEL_ID, result.informationsaccess().regelId());
      assertEquals(REGEL_VERSION, result.informationsaccess().regelVersion());
   }

   private Handlaggning givenHandlaggning() throws Exception
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.individYrkandeRoller()).thenReturn(Collections.emptyList());
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(1);
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(identityAdapter.getIdentity(any())).thenReturn(ImmutableIdtyp.builder().typId(ID_TYP).varde(ID_VARDE).build());
      return handlaggning;
   }

   private void givenCorrelationData(UUID oulUppgiftId)
   {
      var correlationData = mock(OulCorrelationData.class);
      when(correlationData.uppgift()).thenReturn(mock(Uppgift.class));
      when(correlationData.oulUppgiftId()).thenReturn(oulUppgiftId);
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(correlationData);
   }
}
