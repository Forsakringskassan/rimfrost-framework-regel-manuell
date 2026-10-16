package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.component.QuarkusComponentTest;
import io.quarkus.test.component.SkipInject;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.fk.rimfrost.adapter.identity.adapter.IdentityAdapter;
import se.fk.rimfrost.adapter.identity.exception.IdentityException;
import se.fk.rimfrost.adapter.identity.model.ImmutableIdtyp;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.identity.UserIdentityFilter;
import se.fk.rimfrost.framework.regel.manuell.logic.identity.UserIdentityHolder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@QuarkusComponentTest(
{
      UserIdentityFilter.class, UserIdentityHolder.class
})
public class RegelManuellUserIdentityFilterTest
{
   private static final String AUTH_HEADER = "Bearer DUMMY:dummy";
   private static final String ID_TYP = "PERSONNR";
   private static final String ID_VARDE = "19901010-1234";

   @Inject
   UserIdentityFilter filter;

   @Inject
   UserIdentityHolder userIdentityHolder;

   @InjectMock
   IdentityAdapter identityAdapter;

   private ContainerRequestContext requestContext;

   @BeforeEach
   void setupRequestContext()
   {
      requestContext = mock(ContainerRequestContext.class);
      when(requestContext.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(AUTH_HEADER);
   }

   @Test
   @DisplayName("FRMM-FR-11.1/11.2: Identiteten verifieras med requestens bearer token och sparas i UserIdentityHolder")
   void filter_should_store_identity_resolved_from_authorization_header() throws Exception
   {
      when(identityAdapter.getIdentity(AUTH_HEADER))
            .thenReturn(ImmutableIdtyp.builder().typId(ID_TYP).varde(ID_VARDE).build());

      filter.filter(requestContext);

      var identity = userIdentityHolder.getIdentity();
      assertEquals(ID_TYP, identity.typId());
      assertEquals(ID_VARDE, identity.varde());
   }

   @Test
   @DisplayName("FRMM-FR-11.1: HTTP 401 returneras när identitetstjänsten svarar UNAUTHORIZED")
   void filter_should_throw_unauthorized_when_identity_is_unauthorized() throws Exception
   {
      var cause = new IdentityException(IdentityException.ErrorType.UNAUTHORIZED, "identity error");
      when(identityAdapter.getIdentity(any())).thenThrow(cause);

      var ex = assertThrows(RegelManuellException.class, () -> filter.filter(requestContext));

      assertEquals(Response.Status.UNAUTHORIZED, ex.getStatus());
      assertSame(cause, ex.getCause());
      assertNull(userIdentityHolder.getIdentity());
   }

   @ParameterizedTest
   @EnumSource(value = IdentityException.ErrorType.class, names = "UNAUTHORIZED", mode = EnumSource.Mode.EXCLUDE)
   @DisplayName("FRMM-FR-06.4, FRMM-FR-11.1: Fel från identitetstjänsten mappas till väldefinierade HTTP-statuskoder")
   void filter_should_throw_with_mapped_status_when_identity_throws(@SkipInject IdentityException.ErrorType errorType)
         throws Exception
   {
      var cause = new IdentityException(errorType, "identity error");
      when(identityAdapter.getIdentity(any())).thenThrow(cause);

      var ex = assertThrows(RegelManuellException.class, () -> filter.filter(requestContext));

      assertEquals(expectedStatus(errorType), ex.getStatus());
      assertSame(cause, ex.getCause());
      assertNull(userIdentityHolder.getIdentity());
   }

   @Test
   @DisplayName("HTTP 500 returneras när identitetstjänsten inte returnerar någon identitet")
   void filter_should_throw_internal_server_error_when_identity_is_null() throws Exception
   {
      when(identityAdapter.getIdentity(any())).thenReturn(null);

      var ex = assertThrows(RegelManuellException.class, () -> filter.filter(requestContext));

      assertEquals(Response.Status.INTERNAL_SERVER_ERROR, ex.getStatus());
      assertNull(userIdentityHolder.getIdentity());
   }

   private static Response.Status expectedStatus(IdentityException.ErrorType errorType)
   {
      return switch (errorType)
      {
         case NOT_FOUND -> Response.Status.NOT_FOUND;
         case BAD_REQUEST -> Response.Status.BAD_REQUEST;
         case SERVICE_UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE;
         default -> Response.Status.INTERNAL_SERVER_ERROR;
      };
   }

}
