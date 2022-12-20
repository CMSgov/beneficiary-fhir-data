package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Verifies it can get an actual fda drug code display */
public class FDADrugCodeDisplayLookupIT {
  /** Verifies a real drug code display. */
  @Test
  public void verifiedRealDrugCode() {
    FdaDrugCodeDisplayLookup fdaDrugCodeDisplayLookup =
        FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    String fdaCodeDisplay =
        fdaDrugCodeDisplayLookup.retrieveFDADrugCodeDisplay(Optional.of("804250039"));
    assertEquals("Celecoxib - CELECOXIB", fdaCodeDisplay);
  }
}
