package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import gov.cms.bfd.server.war.commons.LoadedFileFilter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.apache.spark.util.sketch.BloomFilter;
import org.junit.jupiter.api.Test;

/** Tests for {@link gov.cms.bfd.server.war.stu3.providers.LoadedFileFilter}. */
public final class LoadedFilterTest {

  @Test
  public void testMatchesDateRange() {
    final BloomFilter emptyFilter = LoadedFileFilter.createFilter(10);
    final LoadedFileFilter filter1 =
        new LoadedFileFilter(
            1, 0, Instant.now().minusSeconds(10), Instant.now().minusSeconds(5), emptyFilter);

    assertTrue(
        filter1.matchesDateRange(null), "Expected null range to be treated as an infinite range");
    assertTrue(
        filter1.matchesDateRange(new DateRangeParam()),
        "Expected empty range to be treated as an infinite range");

    final DateRangeParam sinceYesterday =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.GREATERTHAN)
                .setValue(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))));
    assertTrue(
        filter1.matchesDateRange(sinceYesterday), "Expected since yesterday period to cover");

    final DateRangeParam beforeNow =
        new DateRangeParam(
            new DateParam().setPrefix(ParamPrefixEnum.LESSTHAN_OR_EQUALS).setValue(new Date()));
    assertTrue(filter1.matchesDateRange(beforeNow), "Expected since yesterday period to cover");

    final DateRangeParam beforeYesterday =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.LESSTHAN)
                .setValue(Date.from(Instant.now().minus(1, ChronoUnit.DAYS))));
    assertFalse(
        filter1.matchesDateRange(beforeYesterday), "Expected before yesterday period to not match");

    final DateRangeParam afterNow =
        new DateRangeParam(
            new DateParam().setPrefix(ParamPrefixEnum.GREATERTHAN_OR_EQUALS).setValue(new Date()));
    assertFalse(filter1.matchesDateRange(afterNow), "Expected after now period to not match");

    final DateRangeParam beforeSevenSeconds =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.LESSTHAN)
                .setValue(Date.from(Instant.now().minus(7, ChronoUnit.SECONDS))));
    assertTrue(filter1.matchesDateRange(beforeSevenSeconds), "Expected partial match to match");

    final DateRangeParam afterSevenSeconds =
        new DateRangeParam(
            new DateParam()
                .setPrefix(ParamPrefixEnum.GREATERTHAN)
                .setValue(Date.from(Instant.now().minus(7, ChronoUnit.SECONDS))));
    assertTrue(filter1.matchesDateRange(afterSevenSeconds), "Expected partial match to match");

    final DateRangeParam sevenSeconds =
        new DateRangeParam(
            Date.from(Instant.now().minusSeconds(8)), Date.from(Instant.now().minusSeconds(7)));
    assertTrue(filter1.matchesDateRange(sevenSeconds), "Expected partial match to match");
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
            1, 1, Instant.now().minusSeconds(10), Instant.now().minusSeconds(5), smallFilter);

    assertTrue(filter1.mightContain("1"));
    assertFalse(filter1.mightContain("888"));
    assertFalse(filter1.mightContain("BAD"));
  }
}
