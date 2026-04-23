package se.fk.rimfrost.framework.regel.manuell;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import se.fk.rimfrost.framework.regel.manuell.base.AbstractRegelManuellHandlaggningTest;
import se.fk.rimfrost.framework.regel.manuell.helpers.WireMockRegelManuell;

@QuarkusTest
@QuarkusTestResource.List(
{
      @QuarkusTestResource(WireMockRegelManuell.class)
})
public class RegelManuellHandlaggningTest extends AbstractRegelManuellHandlaggningTest
{
}
