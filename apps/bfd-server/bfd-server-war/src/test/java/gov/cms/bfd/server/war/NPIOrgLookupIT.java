package gov.cms.bfd.server.war;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Integration tests for NPIOrgLookup. */
public class NPIOrgLookupIT {
  /** Verifies that it returns a valid npi org. */
  @Test
  public void VerifyAValidNPIOrg() throws IOException {
    NPIOrgLookup npiOrgLookup = RDATestUtils.createTestNpiOrgLookup();
    Map<String, NPIData> orgDisplay = npiOrgLookup.retrieveNPIOrgDisplay(null);
    assertTrue(orgDisplay.containsKey("1497758544"));
    assertEquals(
        "CUMBERLAND COUNTY HOSPITAL SYSTEM, INC",
        orgDisplay.get("1497758544").getProviderOrganizationName());
  }

  /** Verifies that it returns a empty string for a non valid npiOrg Number. */
  @Test
  public void VerifyANonValidNPIOrgReturnsEmpty() throws IOException {
    NPIOrgLookup npiOrgLookup = RDATestUtils.createTestNpiOrgLookup();
    Map<String, NPIData> orgDisplay = npiOrgLookup.retrieveNPIOrgDisplay(null);
    assertFalse(orgDisplay.containsKey("-497758544"));
  }
}
