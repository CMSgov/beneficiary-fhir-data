package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rif.npi_fda.NPIData;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Integration tests for NPIOrgLookup. */
public class NPIOrgLookupIT {
  /** Verifies that it returns a valid npi org. */
  @Test
  public void VerifyAValidNPIOrg() throws IOException {
    NPIOrgLookup npiOrgLookup = NPIOrgLookup.createTestNpiOrgLookup();
    Optional<NPIData> orgDisplay = npiOrgLookup.retrieveNPIOrgDisplay(Optional.of("1497758544"));
    assertEquals(
        "CUMBERLAND COUNTY HOSPITAL SYSTEM, INC", orgDisplay.get().getProviderOrganizationName());
  }

  /** Verifies that it returns a empty string for a non valid npiOrg Number. */
  @Test
  public void VerifyANonValidNPIOrgReturnsEmpty() throws IOException {
    NPIOrgLookup npiOrgLookup = NPIOrgLookup.createTestNpiOrgLookup();
    Optional<NPIData> orgDisplay = npiOrgLookup.retrieveNPIOrgDisplay(Optional.of("-497758544"));
    assertTrue(orgDisplay.isEmpty());
  }
}
