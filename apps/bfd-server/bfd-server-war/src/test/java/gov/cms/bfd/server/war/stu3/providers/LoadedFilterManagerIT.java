package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.rif.load.RifLoader;
import gov.cms.bfd.pipeline.rif.load.RifLoaderTestUtils;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Integration tests for {@link gov.cms.bfd.server.war.stu3.providers.LoadedFilterManager}. */
public final class LoadedFilterManagerIT {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadedFilterManagerIT.class);

  private static final String SAMPLE_BENE = "567834";
  private static final String INVALID_BENE = "1";

  @Test
  public void refreshFilters() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          final LoadedFilterManager filterManager = new LoadedFilterManager(0);
          filterManager.setEntityManager(entityManager);
          loadData(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
          Date afterLoad = new Date();

          // Without a refresh, the manager should have an empty filter list
          final List<LoadedFileFilter> beforeFilters = filterManager.getFilters();
          Assert.assertEquals(0, beforeFilters.size());
          Assert.assertEquals(0, filterManager.getMaxBatchId());
          Assert.assertEquals(0, filterManager.getMinBatchId());
          Assert.assertEquals(0, filterManager.getReplicaDelay());

          // Refresh the filter list
          filterManager.refreshFilters();
          Date afterRefresh = new Date();

          // Should have many filters
          final List<LoadedFileFilter> afterFilters = filterManager.getFilters();
          Assert.assertTrue(filterManager.getKnownLowerBound().getTime() <= afterLoad.getTime());
          Assert.assertTrue(filterManager.getKnownUpperBound().getTime() <= afterRefresh.getTime());
          Assert.assertTrue(filterManager.getMaxBatchId() > 0);
          Assert.assertTrue(filterManager.getMinBatchId() > 0);
          Assert.assertTrue(filterManager.getMinBatchId() <= filterManager.getMaxBatchId());
          Assert.assertTrue(afterFilters.size() > 1);
        });
  }

  /** Test isResultSetEmpty with one filter */
  @Test
  public void isResultSetEmpty() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          final LoadedFilterManager filterManager = new LoadedFilterManager(0);
          filterManager.setEntityManager(entityManager);

          // Establish a before load time
          final Date beforeLoad = new Date();
          final DateRangeParam beforeLoadUnbounded =
              new DateRangeParam().setUpperBoundExclusive(beforeLoad);
          RifLoaderTestUtils.pauseMillis(10);

          loadData(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

          // Establish a time after load but before refresh
          RifLoaderTestUtils.pauseMillis(10);
          final Date beforeRefresh = new Date();
          final DateRangeParam beforeRefreshRange =
              new DateRangeParam(beforeRefresh, Date.from(Instant.now().plusMillis(5)));
          RifLoaderTestUtils.pauseMillis(10);

          filterManager.refreshFilters();

          // Establish a after refresh time
          final Date afterRefresh = Date.from(Instant.now().plusMillis(10));
          final DateRangeParam afterRefreshRange =
              new DateRangeParam(afterRefresh, Date.from(Instant.now().plusMillis(15)));

          // Assert on isInKnownBounds
          Assert.assertTrue(
              "Known bound should include a time range before refresh",
              filterManager.isInKnownBounds(beforeRefreshRange));
          Assert.assertFalse(
              "Known bound should not include a time range after refresh",
              filterManager.isInKnownBounds(afterRefreshRange));
          Assert.assertFalse(
              "Unbounded lower range always match null lastUpdated which are not known",
              filterManager.isInKnownBounds(beforeLoadUnbounded));

          // Assert on isResultSetEmpty
          Assert.assertTrue(
              "Expected date range to not have a matching filter",
              filterManager.isResultSetEmpty(SAMPLE_BENE, beforeRefreshRange));
          Assert.assertTrue(
              "Expected date range to not have a matching filter",
              filterManager.isResultSetEmpty(INVALID_BENE, beforeRefreshRange));
        });
  }

  /** Test isResultSetEmpty with multiple refreshes */
  @Test
  public void testWithMultipleRefreshes() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          final LoadedFilterManager filterManager = new LoadedFilterManager(0);
          filterManager.setEntityManager(entityManager);
          loadData(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

          // Establish a couple of times
          RifLoaderTestUtils.pauseMillis(1000);
          final Date afterSampleA = new Date();
          RifLoaderTestUtils.pauseMillis(10);
          final Date afterSampleAPlus = new Date();
          final DateRangeParam afterSampleARange =
              new DateRangeParam(afterSampleA, afterSampleAPlus);

          // Refresh the filter list
          RifLoaderTestUtils.pauseMillis(10);
          filterManager.refreshFilters();

          // Test after refresh
          int after1Count = filterManager.getFilters().size();
          Assert.assertTrue(filterManager.isInKnownBounds(afterSampleARange));
          Assert.assertTrue(
              "Expected date range to not have a matching filter",
              filterManager.isResultSetEmpty(SAMPLE_BENE, afterSampleARange));
          Assert.assertTrue(
              "Expected date range to not have a matching filter",
              filterManager.isResultSetEmpty(INVALID_BENE, afterSampleARange));

          // Refresh again (should do nothing)
          filterManager.refreshFilters();
          int after2Count = filterManager.getFilters().size();
          Assert.assertEquals(after1Count, after2Count);

          // Load some more data
          loadData(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
          RifLoaderTestUtils.pauseMillis(1000);
          final Date afterSampleU = new Date();
          final DateRangeParam aroundSampleU = new DateRangeParam(afterSampleA, afterSampleU);
          filterManager.refreshFilters();

          // Test after refresh
          Assert.assertFalse(
              "Expected date range to not have a matching filter",
              filterManager.isResultSetEmpty(SAMPLE_BENE, aroundSampleU));
          Assert.assertTrue(
              "Expected date range to not have a matching filter",
              filterManager.isResultSetEmpty(INVALID_BENE, aroundSampleU));
        });
  }

  /** @param sampleResources the sample RIF resources to load */
  private static void loadData(DataSource dataSource, List<StaticRifResource> sampleResources) {
    LoadAppOptions loadOptions = RifLoaderTestUtils.getLoadOptions(dataSource);
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            sampleResources.stream()
                .map(StaticRifResource::toRifFile)
                .collect(Collectors.toList()));

    // Create the processors that will handle each stage of the pipeline.
    MetricRegistry loadAppMetrics = new MetricRegistry();
    RifFilesProcessor processor = new RifFilesProcessor();
    try (final RifLoader loader = new RifLoader(loadAppMetrics, loadOptions)) {
      // Link up the pipeline and run it.
      for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
        RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
        loader.process(rifFileRecords, error -> {}, result -> {});
      }
    }
  }
}
