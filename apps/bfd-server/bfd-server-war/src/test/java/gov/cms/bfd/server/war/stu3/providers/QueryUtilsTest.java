package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.server.war.commons.QueryUtils;
import java.time.Instant;
import org.junit.Assert;
import org.junit.Test;

public class QueryUtilsTest {
  @Test
  public void testInRange() {
    /*
     * Dev Note: There might be a bug in DateRangeParam and DateParam where it rounds a value that
     * is set a millisecond up or down. It makes it hard to test at the edges of a range.
     */
    Instant lowerDate = Instant.now();
    Instant middleDate = Instant.now().plusSeconds(500);
    Instant upperDate = Instant.now().plusSeconds(1000);

    Assert.assertTrue(
        QueryUtils.isInRange(upperDate, new DateRangeParam().setLowerBoundExclusive(middleDate)));
    Assert.assertFalse(
        QueryUtils.isInRange(lowerDate, new DateRangeParam().setLowerBoundInclusive(middleDate)));
    Assert.assertTrue(
        QueryUtils.isInRange(lowerDate, new DateRangeParam().setUpperBoundExclusive(middleDate)));
    Assert.assertFalse(
        QueryUtils.isInRange(upperDate, new DateRangeParam().setUpperBoundInclusive(middleDate)));

    Assert.assertTrue(QueryUtils.isInRange(middleDate, new DateRangeParam(lowerDate, upperDate)));
    Assert.assertFalse(QueryUtils.isInRange(lowerDate, new DateRangeParam(middleDate, upperDate)));
    Assert.assertFalse(QueryUtils.isInRange(upperDate, new DateRangeParam(lowerDate, middleDate)));
  }

  @Test
  public void testInRangeWithNullLastUpdate() {
    Instant lowerDate = Instant.now();
    Instant upperDate = Instant.now().plusSeconds(1000);

    Assert.assertFalse(
        QueryUtils.isInRange(null, new DateRangeParam().setLowerBoundExclusive(lowerDate)));
    Assert.assertFalse(
        QueryUtils.isInRange(null, new DateRangeParam().setLowerBoundInclusive(lowerDate)));
    Assert.assertTrue(
        QueryUtils.isInRange(null, new DateRangeParam().setUpperBoundExclusive(lowerDate)));
    Assert.assertTrue(
        QueryUtils.isInRange(null, new DateRangeParam().setUpperBoundInclusive(lowerDate)));
    Assert.assertFalse(QueryUtils.isInRange(null, new DateRangeParam(lowerDate, upperDate)));
  }
}
