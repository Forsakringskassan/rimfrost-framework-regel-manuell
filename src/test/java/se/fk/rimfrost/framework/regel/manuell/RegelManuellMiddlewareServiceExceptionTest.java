package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.ImmutableHandlaggning;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.oul.logic.OulUppgiftService;
import se.fk.rimfrost.framework.regel.oul.logic.entity.OulCorrelationData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import java.time.OffsetDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellMiddlewareServiceExceptionTest
{
   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   OulUppgiftService oulUppgiftService;

   @InjectMock
   SidAdapter sidAdapter;

   @ParameterizedTest
   @EnumSource(HandlaggningException.ErrorType.class)
   @DisplayName("FRMM-FR-06.4: HandlaggningException vid GET mappas till väldefinierad HTTP-statuskod")
   void read_should_throw_with_mapped_status_when_getHandlaggning_throws(HandlaggningException.ErrorType errorType)
         throws HandlaggningException
   {
      doThrow(new HandlaggningException(errorType, "error"))
            .when(handlaggningAdapter).readHandlaggning(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(expectedStatus(errorType), ex.getStatus());
   }

   @ParameterizedTest
   @EnumSource(HandlaggningException.ErrorType.class)
   @DisplayName("FRMM-FR-06.4: HandlaggningException vid PATCH mappas till väldefinierad HTTP-statuskod")
   void update_should_throw_with_mapped_status_when_getHandlaggning_throws(HandlaggningException.ErrorType errorType)
         throws HandlaggningException
   {
      doThrow(new HandlaggningException(errorType, "error"))
            .when(handlaggningAdapter).readHandlaggning(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.update(UUID.randomUUID(), "request"));

      assertEquals(expectedStatus(errorType), ex.getStatus());
   }

   @ParameterizedTest
   @EnumSource(HandlaggningException.ErrorType.class)
   @DisplayName("FRMM-FR-06.4: HandlaggningException vid uppdatering av handläggningsärende under GET mappas till väldefinierad HTTP-statuskod")
   void read_should_throw_with_mapped_status_when_updateHandlaggning_throws(HandlaggningException.ErrorType errorType)
         throws Exception
   {
      givenSuccessfulGetHandlaggning();
      givenSuccessfulCorrelationData();
      doThrow(new HandlaggningException(errorType, "error"))
            .when(handlaggningAdapter).updateHandlaggning(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(expectedStatus(errorType), ex.getStatus());
   }

   @ParameterizedTest
   @EnumSource(HandlaggningException.ErrorType.class)
   @DisplayName("FRMM-FR-06.4: HandlaggningException vid uppdatering av handläggningsärende under PATCH mappas till väldefinierad HTTP-statuskod")
   void update_should_throw_with_mapped_status_when_updateHandlaggning_throws(HandlaggningException.ErrorType errorType)
         throws HandlaggningException
   {

      var handlaggning = ImmutableHandlaggning.builder()
            .id(UUID.randomUUID())
            .version(1)
            .yrkande(mock(Yrkande.class))
            .processInstansId(UUID.randomUUID())
            .skapadTS(OffsetDateTime.now())
            .avslutadTS(OffsetDateTime.now())
            .handlaggningspecifikationId(UUID.randomUUID())
            .build();

      when(handlaggningAdapter.readHandlaggning(handlaggning.id())).thenReturn(handlaggning);
      givenSuccessfulCorrelationData();
      doThrow(new HandlaggningException(errorType, "error"))
            .when(handlaggningAdapter).updateHandlaggning(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.update(handlaggning.id(), "request"));

      assertEquals(expectedStatus(errorType), ex.getStatus());
   }

   @Test
   @DisplayName("FRMM-FR-06.4: HandlaggningException med CONFLICT feltyp vid uppdatering av handläggningsärende under PATCH mappas till väldefinierad HTTP-statuskod")
   void update_should_throw_with_internal_server_error_when_updateHandlaggning_throws_conflict_exception()
         throws HandlaggningException
   {
      var errorType = HandlaggningException.ErrorType.CONFLICT;
      var handlaggning = ImmutableHandlaggning.builder()
            .id(UUID.randomUUID())
            .version(1)
            .yrkande(mock(Yrkande.class))
            .processInstansId(UUID.randomUUID())
            .skapadTS(OffsetDateTime.now())
            .avslutadTS(OffsetDateTime.now())
            .handlaggningspecifikationId(UUID.randomUUID())
            .build();

      when(handlaggningAdapter.readHandlaggning(handlaggning.id())).thenReturn(handlaggning);
      givenSuccessfulCorrelationData();
      doThrow(new HandlaggningException(errorType, "error"))
            .when(handlaggningAdapter).updateHandlaggning(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.update(handlaggning.id(), "request"));

      assertEquals(expectedStatus(errorType), ex.getStatus());
      assertTrue(ex.getMessage().matches("(?i).*conflict.*"));
      assertTrue(ex.getMessage().matches("(?i).*programming fault.*"));
   }

   private void givenSuccessfulGetHandlaggning() throws HandlaggningException
   {
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(1);
      when(handlaggning.processInstansId()).thenReturn(UUID.randomUUID());
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(mock(Yrkande.class));
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
   }

   private void givenSuccessfulCorrelationData()
   {
      var correlationData = mock(OulCorrelationData.class);
      when(correlationData.uppgift()).thenReturn(mock(Uppgift.class));
      when(oulUppgiftService.getCorrelationData(any())).thenReturn(correlationData);
   }

   private static Response.Status expectedStatus(HandlaggningException.ErrorType errorType)
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
