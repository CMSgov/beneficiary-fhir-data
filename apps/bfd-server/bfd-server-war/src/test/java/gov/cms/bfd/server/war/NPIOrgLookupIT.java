package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Integration tests for NPIOrgLookup. */
public class NPIOrgLookupIT {
  /** Verifies that it returns a valid npi org */
  @Test
  public void VerifyAValidNPIOrg() {
    NPIOrgLookup npiOrgLookup = NPIOrgLookup.createNpiOrgLookupForProduction();
    Optional<String> orgDisplay = npiOrgLookup.retrieveNPIOrgDisplay(Optional.of("1497758544"));
    assertEquals("CUMBERLAND COUNTY HOSPITAL SYSTEM", orgDisplay.get());
  }
}
