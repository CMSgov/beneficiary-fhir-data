package gov.cms.bfd.data.npi.lookup;

import static gov.cms.bfd.data.npi.utility.DataUtilityCommons.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import gov.cms.bfd.data.npi.dto.NPIData;
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
  Optional<NPIData> npiOrgDisplay;

  /** Global variable for NPI taxonomy. */
  Optional<String> npiTaxonomyDisplay;

  /** Global variable for valid npiOrgNumber. */
  private final String npiOrgNumber = "1992903249";

  /** Global variable for valid Practitioner NPI. */
  private final String practitionerNPI = "1679576722";

  /** Global variable for valid taxonomy code. */
  private final String taxonomyCode = "207X00000X";

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
    initialString
        .append(PROVIDER_ORGANIZATION_NAME_FIELD)
        .append(",")
        .append(ENTITY_TYPE_CODE_FIELD)
        .append(",")
        .append(NPI_FIELD)
        .append(",")
        .append(TAXONOMY_CODE_FIELD)
        .append(",")
        .append(TAXONOMY_DISPLAY_FIELD)
        .append(",")
        .append(PROVIDER_FIRST_NAME_FIELD)
        .append(",")
        .append(PROVIDER_MIDDLE_NAME_FIELD)
        .append(",")
        .append(PROVIDER_LAST_NAME_FIELD)
        .append(",")
        .append(PROVIDER_PREFIX_FIELD)
        .append(",")
        .append(PROVIDER_SUFFIX_FIELD)
        .append(",")
        .append(PROVIDER_CREDENTIAL_FIELD)
        .append("\n")
        .append(String.format("%s,2,%s,,,,,,,,", npiOrgName, npiOrgNumber))
        .append("\n")
        .append(
            String.format(
                ",1,%s,%s,%s,Stephen,J.,Smith,Dr.,Sr.,MD",
                practitionerNPI, taxonomyCode, taxonomyDisplay));

    InputStream npiDataStream = new ByteArrayInputStream(initialString.toString().getBytes());
    npiOrgDataLookup = new NPIOrgLookup(npiDataStream);
    npiOrgDisplay = Optional.empty();
    npiTaxonomyDisplay = Optional.empty();
  }

  /** End to End test for Npi Org Data with npi number. */
  @Test
  public void shouldCorrectlyReturnNpiNameObtainedFromFileStream() {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(npiOrgNumber));
    assertEquals(npiOrgName, npiOrgDisplay.get().getProviderOrganizationName());
  }

  /** End to End test for NPI Taxononomy Dislplay. */
  @Test
  public void shouldCorrectlyReturnTaxonomyForNPI() {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(practitionerNPI));
    assertEquals(taxonomyCode, npiOrgDisplay.get().getTaxonomyCode());
    assertEquals(taxonomyDisplay, npiOrgDisplay.get().getTaxonomyDisplay());
  }

  /** End to End test for Npi Org Data with wrong npi number. */
  @Test
  public void shouldReturnEmptyOrganizationWithAWrongNpiNumber() {
    npiOrgDisplay = npiOrgDataLookup.retrieveNPIOrgDisplay(Optional.of(invalidNpiOrgNumber));
    assertFalse(npiOrgDisplay.isPresent());
  }
}
