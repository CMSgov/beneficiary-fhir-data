package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import gov.cms.bfd.server.war.commons.LoadedFileFilter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.apache.spark.util.sketch.BloomFilter;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link gov.cms.bfd.server.war.stu3.providers.LoadedFileFilter}. */
public final class LoadedFilterTest {

  @Test
  public void testMatchesDateRange() {
    final BloomFilter emptyFilter = LoadedFileFilter.createFilter(10);
    final LoadedFileFilter filter1 =
        new LoadedFileFilter(
            1,
            0,
            Date.from(Instant.now().minusSeconds(10)),
            Date.from(Instant.now().minusSeconds(5)),
            emptyFilter);

    Assert.assertTrue(
        "Expected null range to be treated as an infinite range", filter1.matchesDateRange(null));
    Assert.assertTrue(
        "Expected empty range to be treated as an infinite range",
        filter1.matchesDateRange(new DateRangeParam()));

    final DateRangeParam sinceYesterday =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.GREATERTHAN)
                .setValue(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))));
    Assert.assertTrue(
        "Expected since yesterday period to cover", filter1.matchesDateRange(sinceYesterday));

    final DateRangeParam beforeNow =
        new DateRangeParam(
            new DateParam().setPrefix(ParamPrefixEnum.LESSTHAN_OR_EQUALS).setValue(new Date()));
    Assert.assertTrue(
        "Expected since yesterday period to cover", filter1.matchesDateRange(beforeNow));

    final DateRangeParam beforeYesterday =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.LESSTHAN)
                .setValue(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))));
    Assert.assertFalse(
        "Expected before yesterday period to not match", filter1.matchesDateRange(beforeYesterday));

    final DateRangeParam afterNow =
        new DateRangeParam(
            new DateParam().setPrefix(ParamPrefixEnum.GREATERTHAN_OR_EQUALS).setValue(new Date()));
    Assert.assertFalse(
        "Expected after now period to not match", filter1.matchesDateRange(afterNow));

    final DateRangeParam beforeSevenSeconds =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.LESSTHAN)
                .setValue(Date.from(Instant.now().minus(7, ChronoUnit.SECONDS))));
    Assert.assertTrue(
        "Expected partial match to match", filter1.matchesDateRange(beforeSevenSeconds));

    final DateRangeParam afterSevenSeconds =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.GREATERTHAN)
                .setValue(Date.from(Instant.now().minus(7, ChronoUnit.SECONDS))));
    Assert.assertTrue(
        "Expected partial match to match", filter1.matchesDateRange(afterSevenSeconds));

    final DateRangeParam sevenSeconds =
        new DateRangeParam(
            Date.from(Instant.now().minusSeconds(8)), Date.from(Instant.now().minusSeconds(7)));
    Assert.assertTrue("Expected partial match to match", filter1.matchesDateRange(sevenSeconds));
  }

  @Test
  public void testMightContain() {
    // Very small test on the Guava implementation of BloomFilters. Assume this package works.
    final BloomFilter smallFilter = LoadedFileFilter.createFilter(10);
    smallFilter.putString("1");
    smallFilter.putString("100");
    smallFilter.putString("100");

    final LoadedFileFilter filter1 =
        new LoadedFileFilter(
            1,
            1,
            Date.from(Instant.now().minusSeconds(10)),
            Date.from(Instant.now().minusSeconds(5)),
            smallFilter);

    Assert.assertTrue("Expected to contain this", filter1.mightContain("1"));
    Assert.assertFalse("Expected to not contain this", filter1.mightContain("888"));
    Assert.assertFalse("Expected to not contain this", filter1.mightContain("BAD"));
  }
}
