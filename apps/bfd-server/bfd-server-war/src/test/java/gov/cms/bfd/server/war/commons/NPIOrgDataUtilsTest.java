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
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgDataLookup.FAKE_NPI_NUMBER));
    assertNotEquals(null, npiOrgDisplay.get());
  }

  /** Do Not Return Fake NPI Org Data when parameter is false */
  @Test
  public void shouldNotReturnFakeOrgWhenConstructorSetToFalse() {
    NPIOrgDataLookup npiOrgDataLookup = NPIOrgDataLookup.createNpiOrgLookupForProduction();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgDataLookup.FAKE_NPI_NUMBER));
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /** Return Fake NPI Org Name when parameter is true */
  @Test
  public void shouldReturnFakeNPIOrgNameWhenConstructorSetToTrue() {
    NPIOrgDataLookup npiOrgDataLookup = NPIOrgDataLookup.createNpiOrgLookupForTesting();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgDataLookup.FAKE_NPI_NUMBER));
    assertEquals(NPIOrgDataLookup.FAKE_NPI_ORG_NAME, npiOrgDisplay.get());
  }

  /** Return Fake NPI Org Name when parameter is true */
  @Test
  public void shouldNotReturnFakeNPIOrgNameWhenConstructorSetToFalse() {
    NPIOrgDataLookup npiOrgDataLookup = NPIOrgDataLookup.createNpiOrgLookupForProduction();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgDataLookup.FAKE_NPI_NUMBER));
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /** Return real NPI Org Data when parameter is false */
  @Test
  public void shouldReturnRealOrgDataWhenConstructorSetToTrue() {
    NPIOrgDataLookup npiOrgDataLookup = NPIOrgDataLookup.createNpiOrgLookupForProduction();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of("1497758544"));
    assertEquals("CUMBERLAND COUNTY HOSPITAL SYSTEM", npiOrgDisplay.get());
  }
}
