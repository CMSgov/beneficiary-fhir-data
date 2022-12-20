package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.server.war.commons.QueryUtils;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.Test;

public class QueryUtilsTest {
  @Test
  public void testInRange() {
    /*
     * Dev Note: There might be a bug in DateRangeParam and DateParam where it rounds a value that
     * is set a millisecond up or down. It makes it hard to test at the edges of a range.
     */
    Instant lowerInstant = Instant.now();
    Date lowerDate = new Date();
    Instant middleInstant = Instant.now().plusSeconds(500);
    Date middleDate = Date.from(Instant.now().plusSeconds(500));
    Instant upperInstant = Instant.now().plusSeconds(1000);
    Date upperDate = Date.from(Instant.now().plusSeconds(1000));

    assertTrue(
        QueryUtils.isInRange(
            upperInstant, new DateRangeParam().setLowerBoundExclusive(middleDate)));
    assertFalse(
        QueryUtils.isInRange(
            lowerInstant, new DateRangeParam().setLowerBoundInclusive(middleDate)));
    assertTrue(
        QueryUtils.isInRange(
            lowerInstant, new DateRangeParam().setUpperBoundExclusive(middleDate)));
    assertFalse(
        QueryUtils.isInRange(
            upperInstant, new DateRangeParam().setUpperBoundInclusive(middleDate)));

    assertTrue(QueryUtils.isInRange(middleInstant, new DateRangeParam(lowerDate, upperDate)));
    assertFalse(QueryUtils.isInRange(lowerInstant, new DateRangeParam(middleDate, upperDate)));
    assertFalse(QueryUtils.isInRange(upperInstant, new DateRangeParam(lowerDate, middleDate)));
  }

  @Test
  public void testInRangeWithNullLastUpdate() {
    Date lowerDate = new Date();
    Date upperDate = Date.from(Instant.now().plusSeconds(1000));

    assertFalse(QueryUtils.isInRange(null, new DateRangeParam().setLowerBoundExclusive(lowerDate)));
    assertFalse(QueryUtils.isInRange(null, new DateRangeParam().setLowerBoundInclusive(lowerDate)));
    assertTrue(QueryUtils.isInRange(null, new DateRangeParam().setUpperBoundExclusive(lowerDate)));
    assertTrue(QueryUtils.isInRange(null, new DateRangeParam().setUpperBoundInclusive(lowerDate)));
    assertFalse(QueryUtils.isInRange(null, new DateRangeParam(lowerDate, upperDate)));
  }
}
