package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class NPIOrgDataUtilsTest {

  /** Return Fake NPI Org Data when parameter is true */
  @Test
  public void shouldReturnFakeOrgDataWhenConstructorSetToTrue() {
    NPIOrgDataLookup npiOrgDataLookup = NPIOrgDataLookup.createNpiOrgLookupForTesting();
    String npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgDataLookup.FAKE_NPI_NUMBER));
    assertNotEquals(null, npiOrgDisplay);
  }

  /** Do Not Return Fake NPI Org Data when parameter is false */
  @Test
  public void shouldNotReturnFakeOrgWhenConstructorSetToFalse() {
    NPIOrgDataLookup npiOrgDataLookup = NPIOrgDataLookup.createNpiOrgLookupForProduction();
    String npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgDataLookup.FAKE_NPI_NUMBER));
    assertEquals(null, npiOrgDisplay);
  }
}
