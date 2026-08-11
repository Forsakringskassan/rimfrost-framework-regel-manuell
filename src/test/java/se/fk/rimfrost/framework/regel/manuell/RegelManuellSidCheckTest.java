package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import se.fk.rimfrost.framework.handlaggning.adapter.HandlaggningAdapter;
import se.fk.rimfrost.framework.handlaggning.exception.HandlaggningException;
import se.fk.rimfrost.framework.handlaggning.model.Handlaggning;
import se.fk.rimfrost.framework.handlaggning.model.IndividYrkandeRoll;
import se.fk.rimfrost.framework.handlaggning.model.Uppgift;
import se.fk.rimfrost.framework.handlaggning.model.Yrkande;
import se.fk.rimfrost.framework.oul.adapter.OulAdapter;
import se.fk.rimfrost.framework.oul.exception.OulException;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellException;
import se.fk.rimfrost.framework.regel.manuell.logic.RegelManuellMiddlewareServiceTest;
import se.fk.rimfrost.framework.regel.manuell.storage.ManuellRegelCommonDataStorage;
import se.fk.rimfrost.framework.regel.manuell.storage.entity.ManuellRegelCommonData;
import se.fk.rimfrost.framework.sid.adapter.SidAdapter;
import se.fk.rimfrost.framework.sid.exception.SidException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellSidCheckTest
{
   @Inject
   RegelManuellMiddlewareServiceTest service;

   @InjectMock
   HandlaggningAdapter handlaggningAdapter;

   @InjectMock
   SidAdapter sidAdapter;

   @InjectMock
   ManuellRegelCommonDataStorage dataStorage;

   @InjectMock
   OulAdapter oulAdapter;

   @Test
   void read_should_throw_forbidden_when_sid_detected() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenStoredOulUppgiftId(oulUppgiftId);

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
   }

   @Test
   void read_should_unassign_uppgift_when_sid_detected() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenStoredOulUppgiftId(oulUppgiftId);

      assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      verify(oulAdapter).unassignOperativUppgift(eq(oulUppgiftId));
   }

   @Test
   void read_should_not_unassign_when_no_sid() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      givenSuccessfulDataStorage();

      assertDoesNotThrow(() -> service.read(UUID.randomUUID()));

      verify(oulAdapter, never()).unassignOperativUppgift(any());
   }

   @Test
   void read_should_still_throw_forbidden_when_unassign_fails() throws Exception
   {
      var oulUppgiftId = UUID.randomUUID();
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenStoredOulUppgiftId(oulUppgiftId);
      doThrow(new OulException(OulException.ErrorType.SERVICE_UNAVAILABLE, "oul down"))
            .when(oulAdapter).unassignOperativUppgift(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
   }

   @Test
   void read_should_skip_unassign_when_oul_uppgift_id_is_null() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(true);
      givenStoredOulUppgiftId(null);

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(Response.Status.FORBIDDEN, ex.getStatus());
      verify(oulAdapter, never()).unassignOperativUppgift(any());
   }

   @Test
   void read_should_proceed_when_no_sid() throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      givenSuccessfulDataStorage();

      assertDoesNotThrow(() -> service.read(UUID.randomUUID()));
   }

   @ParameterizedTest
   @EnumSource(SidException.ErrorType.class)
   void read_should_throw_with_mapped_status_when_sid_throws(SidException.ErrorType errorType)
         throws Exception
   {
      var handlaggning = handlaggningWithIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      doThrow(new SidException(errorType, "sid error")).when(sidAdapter).containsSid(any());

      var ex = assertThrows(RegelManuellException.class, () -> service.read(UUID.randomUUID()));

      assertEquals(expectedStatus(errorType), ex.getStatus());
   }

   @Test
   void read_should_proceed_when_individYrkandeRoller_is_empty() throws Exception
   {
      var handlaggning = handlaggningWithoutIndivider();
      when(handlaggningAdapter.readHandlaggning(any())).thenReturn(handlaggning);
      when(sidAdapter.containsSid(any())).thenReturn(false);
      givenSuccessfulDataStorage();

      assertDoesNotThrow(() -> service.read(UUID.randomUUID()));
   }

   private Handlaggning handlaggningWithIndivider() throws HandlaggningException
   {
      var individ = mock(se.fk.rimfrost.framework.handlaggning.model.Idtyp.class);
      when(individ.typId()).thenReturn("some-type-id");
      when(individ.varde()).thenReturn("19901010-1234");
      var roll = mock(IndividYrkandeRoll.class);
      when(roll.individ()).thenReturn(individ);
      return buildHandlaggning(List.of(roll));
   }

   private Handlaggning handlaggningWithoutIndivider() throws HandlaggningException
   {
      return buildHandlaggning(Collections.emptyList());
   }

   private Handlaggning buildHandlaggning(List<IndividYrkandeRoll> roller)
   {
      var yrkande = mock(Yrkande.class);
      when(yrkande.individYrkandeRoller()).thenReturn(roller);
      var handlaggning = mock(Handlaggning.class);
      when(handlaggning.id()).thenReturn(UUID.randomUUID());
      when(handlaggning.version()).thenReturn(1);
      when(handlaggning.processInstansId()).thenReturn(UUID.randomUUID());
      when(handlaggning.skapadTS()).thenReturn(OffsetDateTime.now());
      when(handlaggning.handlaggningspecifikationId()).thenReturn(UUID.randomUUID());
      when(handlaggning.yrkande()).thenReturn(yrkande);
      return handlaggning;
   }

   private void givenSuccessfulDataStorage()
   {
      var data = mock(ManuellRegelCommonData.class);
      when(data.uppgift()).thenReturn(mock(Uppgift.class));
      when(dataStorage.getManuellRegelCommonData(any())).thenReturn(data);
   }

   private void givenStoredOulUppgiftId(UUID oulUppgiftId)
   {
      var data = mock(ManuellRegelCommonData.class);
      when(data.oulUppgiftId()).thenReturn(oulUppgiftId);
      when(dataStorage.getManuellRegelCommonData(any())).thenReturn(data);
   }

   private static Response.Status expectedStatus(SidException.ErrorType errorType)
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
