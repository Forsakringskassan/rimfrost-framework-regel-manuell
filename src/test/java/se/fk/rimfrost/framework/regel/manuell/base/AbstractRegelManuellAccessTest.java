package se.fk.rimfrost.framework.regel.manuell.base;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Access checks over REST for manual rules.
 *
 * <p>The tests rely on the default stubs registered by {@link WireMockRegelManuell} (handläggning
 * {@link WireMockRegelManuell#DEFAULT_HANDLAGGNING_ID}, identity, SID and permissions) and on the
 * default correlation data of {@link AbstractRegelManuellTest}. Mappings defined by the rule
 * implementation override those defaults. Only the SID scenario stubs its own preconditions.</p>
 */
@Disabled("Base test class - not executable")
public abstract class AbstractRegelManuellAccessTest extends AbstractRegelManuellTest
{
   private static final String OTHER_AUTHORIZATION_HEADER_VALUE = "Bearer ANNAN_TYP:ANNAT_VARDE";

   private final List<StubMapping> stubs = new ArrayList<>();

   @AfterEach
   void removeStubs()
   {
      stubs.forEach(WireMockRegelManuell.getWireMockServer()::removeStub);
      stubs.clear();
   }

   @Test
   @DisplayName("FRMM-FR-10.2: GET returnerar 200 när inloggad handläggare är tilldelad uppgiften")
   void get_should_return_200_when_handlaggare_is_assigned()
   {
      given()
            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
            .when()
            .get(basePath() + "/{handlaggningId}", WireMockRegelManuell.DEFAULT_HANDLAGGNING_ID)
            .then()
            .statusCode(200);
   }

   @Test
   @DisplayName("FRMM-FR-10.1/10.2: GET returnerar 403 när inloggad handläggare inte är tilldelad uppgiften")
   void get_should_return_403_when_handlaggare_is_not_assigned()
   {
      given()
            .header(HttpHeaders.AUTHORIZATION, OTHER_AUTHORIZATION_HEADER_VALUE)
            .when()
            .get(basePath() + "/{handlaggningId}", WireMockRegelManuell.DEFAULT_HANDLAGGNING_ID)
            .then()
            .statusCode(403);
   }

   @Test
   @DisplayName("FRMM-FR-11.1: GET returnerar 401 när Authorization-header saknas")
   void get_should_return_401_when_authorization_header_missing()
   {
      given()
            .when()
            .get(basePath() + "/{handlaggningId}", WireMockRegelManuell.DEFAULT_HANDLAGGNING_ID)
            .then()
            .statusCode(401);
   }

   @Test
   @DisplayName("FRMM-FR-08.3/08.6: GET returnerar 403 och uppgiften unassignas när SID detekteras och handläggaren saknar SID-behörighet")
   void get_should_return_403_and_unassign_when_sid_detected_without_permission()
   {
      stub(WireMock.post(WireMock.urlPathEqualTo("/sid/status"))
            .willReturn(WireMock.okJson("{\"sid\": true}")));
      stub(WireMock.post(WireMock.urlPathEqualTo("/hasSidPermission"))
            .willReturn(WireMock.okJson("false")));

      given()
            .header(HttpHeaders.AUTHORIZATION, AUTHORIZATION_HEADER_VALUE)
            .when()
            .get(basePath() + "/{handlaggningId}", WireMockRegelManuell.DEFAULT_HANDLAGGNING_ID)
            .then()
            .statusCode(403);

      verify(oulUppgiftService).tryUnassignOulUppgift(any());
   }

   /**
    * Stubs a precondition that defines the scenario of a test. Uses the highest priority, so it
    * applies regardless of the defaults and of mappings defined by the rule implementation.
    */
   private void stub(MappingBuilder mappingBuilder)
   {
      stubs.add(WireMockRegelManuell.getWireMockServer().stubFor(mappingBuilder.atPriority(1)));
   }
}
