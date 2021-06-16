package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoaderTestUtils;
import gov.cms.bfd.server.war.commons.LoadedFileFilter;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
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
  public void emptyFilters() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          final LoadedFilterManager filterManager = new LoadedFilterManager();
          filterManager.setEntityManager(entityManager);
          filterManager.init();

          // After init manager should have an empty filter list but a valid transaction time
          Assert.assertTrue(
              "Expect transactionTime be before now",
              filterManager.getTransactionTime().before(new Date()));
          final List<LoadedFileFilter> beforeFilters = filterManager.getFilters();
          Assert.assertEquals(0, beforeFilters.size());
          final DateRangeParam testBound =
              new DateRangeParam().setLowerBoundExclusive(Date.from(Instant.now().plusSeconds(1)));
          Assert.assertFalse(filterManager.isInBounds(testBound));
          Assert.assertFalse(filterManager.isResultSetEmpty(INVALID_BENE, testBound));

          // Refresh the filter list
          filterManager.refreshFilters();

          // Should have zero filters
          Assert.assertTrue(
              "Expect transactionTime be before now",
              filterManager.getTransactionTime().before(new Date()));
          final List<LoadedFileFilter> afterFilters = filterManager.getFilters();
          Assert.assertEquals(0, afterFilters.size());
          Assert.assertFalse(filterManager.isInBounds(testBound));
          Assert.assertFalse(filterManager.isResultSetEmpty(INVALID_BENE, testBound));
        });
  }

  @Test
  public void refreshFilters() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          final LoadedFilterManager filterManager = new LoadedFilterManager();
          filterManager.setEntityManager(entityManager);
          filterManager.init();
          final Date initialTransactionTime = filterManager.getTransactionTime();
          Assert.assertTrue(initialTransactionTime.before(Date.from(Instant.now().plusMillis(1))));
          loadData(dataSource, Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
          Date afterLoad = new Date();

          // Without a refresh, the manager should have an empty filter list
          final List<LoadedFileFilter> beforeFilters = filterManager.getFilters();
          Assert.assertEquals(0, beforeFilters.size());

          // Refresh the filter list
          filterManager.refreshFilters();
          Date afterRefresh = new Date();

          // Should have many filters
          final List<LoadedFileFilter> afterFilters = filterManager.getFilters();
          Assert.assertTrue(filterManager.getFirstBatchCreated().getTime() <= afterLoad.getTime());
          Assert.assertTrue(
              filterManager.getLastBatchCreated().getTime() <= afterRefresh.getTime());
          Assert.assertTrue(
              filterManager.getTransactionTime().getTime() > initialTransactionTime.getTime());
          Assert.assertTrue(afterFilters.size() > 1);
        });
  }

  /** Test isResultSetEmpty with one filter */
  @Test
  public void isResultSetEmpty() {
    RifLoaderTestUtils.doTestWithDb(
        (dataSource, entityManager) -> {
          final LoadedFilterManager filterManager = new LoadedFilterManager();
          filterManager.setEntityManager(entityManager);
          filterManager.init();

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
              filterManager.isInBounds(beforeRefreshRange));
          Assert.assertTrue(
              "Known bound includes time after refresh",
              filterManager.isInBounds(afterRefreshRange));
          Assert.assertFalse(
              "Unbounded lower range always match null lastUpdated which are not known",
              filterManager.isInBounds(beforeLoadUnbounded));

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
          final LoadedFilterManager filterManager = new LoadedFilterManager();
          filterManager.setEntityManager(entityManager);
          filterManager.init();
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
          Assert.assertTrue(filterManager.isInBounds(afterSampleARange));
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
    try (final RifLoader loader = new RifLoader(loadAppMetrics, loadOptions, dataSource)) {
      // Link up the pipeline and run it.
      for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
        RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
        loader.process(rifFileRecords, error -> {}, result -> {});
      }
    }
  }
}
