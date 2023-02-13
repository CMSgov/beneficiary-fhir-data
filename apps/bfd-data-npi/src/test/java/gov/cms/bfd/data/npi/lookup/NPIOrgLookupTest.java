package gov.cms.bfd.data.npi.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** NPI org lookup test. */
public class NPIOrgLookupTest {

  /** Return Fake NPI Org Data when the parameter bfdServer.include.fake.drug.code is true. */
  @Test
  public void shouldReturnFakeOrgDataWhenConstructorSetToTrue() throws IOException {
    NPIOrgLookup npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForTesting();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertNotEquals(null, npiOrgDisplay.get());
  }

  /**
   * Do Not Return Fake NPI Org Data when the parameter bfdServer.include.fake.drug.code is false.
   */
  @Test
  public void shouldNotReturnFakeOrgWhenConstructorSetToFalse() throws IOException {
    NPIOrgLookup npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForProduction();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /** Return Fake NPI Org Name when the parameter bfdServer.include.fake.drug.code is true. */
  @Test
  public void shouldReturnFakeNPIOrgNameWhenConstructorSetToTrue() throws IOException {
    NPIOrgLookup npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForTesting();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertEquals(NPIOrgLookup.FAKE_NPI_ORG_NAME, npiOrgDisplay.get());
  }

  /** Return Fake NPI Org Name when the parameter bfdServer.include.fake.drug.code is true. */
  @Test
  public void shouldNotReturnFakeNPIOrgNameWhenConstructorSetToFalse() throws IOException {
    NPIOrgLookup npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForProduction();
    Optional<String> npiOrgDisplay =
        npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(NPIOrgLookup.FAKE_NPI_NUMBER));
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /**
   * Should not return Org Name when the parameter bfdServer.include.fake.drug.code is true and NPI
   * Number is empty.
   */
  @Test
  public void shouldNotReturnWhenNPINumberIsEmptyAndWhenConstructorSetToTrue() throws IOException {
    NPIOrgLookup npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForTesting();
    Optional<String> npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.empty());
    assertEquals(false, npiOrgDisplay.isPresent());
  }

  /**
   * Should not return Org Name when the parameter bfdServer.include.fake.drug.code is true and NPI
   * Number is empty string.
   */
  @Test
  public void shouldNotReturnWhenNPINumberIEmptyStringAndWhenConstructorSetToTrue()
      throws IOException {
    NPIOrgLookup npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForTesting();
    Optional<String> npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(""));
    assertEquals(false, npiOrgDisplay.isPresent());
  }
}
