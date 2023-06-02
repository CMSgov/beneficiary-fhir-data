package gov.cms.bfd.data.npi.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** NPI lookup end to end test. */
public class NPIOrgLookupE2ETest {

  /** Global variable for NPILookup. */
  NPIOrgLookup npiOrgDataLookup;

  /** Global variable for npiOrgDisplay. */
  Optional<String> npiOrgDisplay;

  /** Setup method. */
  @BeforeEach
  void setup() throws IOException {
    npiOrgDataLookup = NPIOrgLookup.createNpiOrgLookupForProduction();
    npiOrgDisplay = Optional.empty();
  }

  /** End to End test for Npi Org Data with real npi number. */
  @Test
  public void shouldReturnRealOrgNPIDataReturnsRightOrganization() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of("1992903249"));
    assertEquals("CAMPBELL CLINIC", npiOrgDisplay.get());
  }

  /** End to End test for Npi Org Data with wrong npi number. */
  @Test
  public void shouldReturnEmptyOrganizationWithAWrongNpiNumber() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of("000"));
    assertFalse(npiOrgDisplay.isPresent());
  }
}
