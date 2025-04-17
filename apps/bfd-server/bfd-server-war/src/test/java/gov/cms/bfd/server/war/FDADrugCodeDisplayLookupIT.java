package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** Verifies it can get an actual fda drug code display. */
public class FDADrugCodeDisplayLookupIT {
  /** Verifies a real drug code display. */
  @Test
  public void verifiedRealDrugCode() throws IOException {
    FDADrugCodeDisplayLookup fdaDrugCodeDisplayLookup =
        RDATestUtils.createFdaDrugCodeDisplayLookup();
    String fdaCodeDisplay =
        fdaDrugCodeDisplayLookup.retrieveFDADrugCodeDisplay(null).get("80425-0039");
    assertEquals("Celecoxib - CELECOXIB", fdaCodeDisplay);
  }
}
