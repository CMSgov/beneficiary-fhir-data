package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import java.time.Instant;
import java.util.Date;
import org.junit.Assert;
import org.junit.Test;

public class QueryUtilsTest {
  @Test
  public void testInRange() {
    /*
     * Dev Note: There might be a bug in DateRangeParam and DateParam where it rounds a value that
     * is set a millisecond up or down. It makes it hard to test at the edges of a range.
     */
    Date lowerDate = new Date();
    Date middleDate = Date.from(Instant.now().plusSeconds(500));
    Date upperDate = Date.from(Instant.now().plusSeconds(1000));

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

    DateParam equalParam = new DateParam(ParamPrefixEnum.EQUAL, middleDate);
    Assert.assertTrue(QueryUtils.isInRange(middleDate, new DateRangeParam(equalParam, equalParam)));
    Assert.assertFalse(QueryUtils.isInRange(lowerDate, new DateRangeParam(equalParam, equalParam)));
    Assert.assertFalse(QueryUtils.isInRange(upperDate, new DateRangeParam(equalParam, equalParam)));
  }

  @Test
  public void testInRangeWithNullLastUpdate() {
    Date lowerDate = new Date();
    Date upperDate = Date.from(Instant.now().plusSeconds(1000));

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
