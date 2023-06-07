package gov.cms.bfd.data.npi.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** NPI lookup end to end test. */
public class NPIOrgLookupE2E {

  /** Global variable for NPILookup. */
  NPIOrgLookup npiOrgDataLookup;

  /** Global variable for npiOrgDisplay. */
  Optional<String> npiOrgDisplay;

  /** Global variable for valid npiOrgNumber. */
  private final String npiOrgNumber = "1992903249";

  /** Global variable for invalidNpiOrgNumber. */
  private final String invalidNpiOrgNumber = "000";

  /** Setup method. */
  @BeforeEach
  void setup() throws IOException {
    npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForProduction();
    npiOrgDisplay = Optional.empty();
  }

  /** End to End test for Npi Org Data with real npi number. */
  @Test
  public void shouldReturnRealOrgNPIDataReturnsRightOrganization() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(npiOrgNumber));
    assertEquals("CAMPBELL CLINIC", npiOrgDisplay.get());
  }

  /** End to End test for Npi Org Data with wrong npi number. */
  @Test
  public void shouldReturnEmptyOrganizationWithAWrongNpiNumber() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(invalidNpiOrgNumber));
    assertFalse(npiOrgDisplay.isPresent());
  }
}
