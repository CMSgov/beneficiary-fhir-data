package gov.cms.bfd.data.npi.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** NPI lookup end to end test. */
public class NPIOrgLookupE2E {

  /** Global variable for NPILookup. */
  NPIOrgLookup npiOrgDataLookup;

  /** Global variable for npiOrgDisplay. */
  Optional<String> npiOrgDisplay;

  /** Global variable for NPI taxonomy. */
  Optional<String> npiTaxonomyDisplay;

  /** Global variable for valid npiOrgNumber. */
  private final String npiOrgNumber = "1992903249";

  /** Global variable for valid Practitioner NPI. */
  private final String practitionerNPI = "1679576722";

  /** Global variable for valid taxonomy code. */
  private final String taxononomyCode = "207X00000X";

  /** Global variable for valid taxonomy display. */
  private final String taxonomyDisplay = "Allopathic & Osteopathic Physicians";

  /** Global variable for valid npiOrg. */
  private final String npiOrgName = "CAMPBELL CLINICS";

  /** Global variable for invalidNpiOrgNumber. */
  private final String invalidNpiOrgNumber = "000";

  /** Setup method. */
  @BeforeEach
  void setup() throws IOException {
    StringBuilder initialString = new StringBuilder();
    initialString.append(npiOrgNumber);
    initialString.append("\t");
    initialString.append(npiOrgName);
    initialString.append("\n");
    initialString.append(practitionerNPI);
    initialString.append("\t");
    initialString.append(taxononomyCode);
    initialString.append("\t");
    initialString.append(taxonomyDisplay);
    InputStream npiDataStream = new ByteArrayInputStream(initialString.toString().getBytes());
    npiOrgDataLookup = new NPIOrgLookup(npiDataStream);
    npiOrgDisplay = Optional.empty();
    npiTaxonomyDisplay = Optional.empty();
  }

  /** End to End test for Npi Org Data with npi number. */
  @Test
  public void shouldCorrectlyReturnNpiNameObtainedFromFileStream() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(npiOrgNumber));
    assertEquals(npiOrgName, npiOrgDisplay.get());
  }

  /** End to End test for NPI Taxononomy Dislplay. */
  @Test
  public void shouldCorrectlyReturnTaxonomyForNPI() throws IOException {
    npiTaxonomyDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(practitionerNPI));
    assertEquals(taxononomyCode + "\t" + taxonomyDisplay, npiTaxonomyDisplay.get());
  }

  /** End to End test for Npi Org Data with wrong npi number. */
  @Test
  public void shouldReturnEmptyOrganizationWithAWrongNpiNumber() throws IOException {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(invalidNpiOrgNumber));
    assertFalse(npiOrgDisplay.isPresent());
  }
}
