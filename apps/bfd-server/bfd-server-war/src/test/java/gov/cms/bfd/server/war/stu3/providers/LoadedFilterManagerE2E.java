package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.PipelineTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFileRecords;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.commons.LoadedFileFilter;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Exceptions;

/** Integration tests for {@link LoadedFilterManager}. */
public final class LoadedFilterManagerE2E extends ServerRequiredTest {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadedFilterManagerE2E.class);

  /** The bene id to use for testing. */
  private static final long SAMPLE_BENE = 567834L;

  /** An invalid bene id. */
  private static final long INVALID_BENE = 1L;

  /**
   * Ensures there is no filter result found when searching the filter for an invalid bene, even
   * after a refresh.
   */
  @Test
  public void emptyFilters() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              final LoadedFilterManager filterManager = new LoadedFilterManager(dataSource);
              filterManager.setEntityManager(entityManager);
              filterManager.init();

              // After init manager should have an empty filter list but a valid transaction time
              assertTrue(
                  filterManager.getTransactionTime().isBefore(Instant.now()),
                  "Expect transactionTime be before now");
              final List<LoadedFileFilter> beforeFilters = filterManager.getFilters();
              assertEquals(0, beforeFilters.size());
              final DateRangeParam testBound =
                  new DateRangeParam()
                      .setLowerBoundExclusive(Date.from(Instant.now().plusSeconds(1)));
              assertFalse(filterManager.isInBounds(testBound));
              assertFalse(filterManager.isResultSetEmpty(INVALID_BENE, testBound));

              // Refresh the filter list
              filterManager.refreshFilters();

              // Should have zero filters
              assertTrue(
                  filterManager.getTransactionTime().isBefore(Instant.now()),
                  "Expect transactionTime be before now");
              final List<LoadedFileFilter> afterFilters = filterManager.getFilters();
              assertEquals(0, afterFilters.size());
              assertFalse(filterManager.isInBounds(testBound));
              assertFalse(filterManager.isResultSetEmpty(INVALID_BENE, testBound));
            });
  }

  /**
   * Ensures there is a filter result found when searching the filter for a valid bene after a
   * refresh.
   */
  @Test
  public void refreshFilters() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              final LoadedFilterManager filterManager = new LoadedFilterManager(dataSource);
              filterManager.setEntityManager(entityManager);
              filterManager.init();
              final Instant initialTransactionTime = filterManager.getTransactionTime();
              assertTrue(initialTransactionTime.isBefore(Instant.now().plusMillis(1)));
              loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
              Date afterLoad = new Date();

              // Without a refresh, the manager should have an empty filter list
              final List<LoadedFileFilter> beforeFilters = filterManager.getFilters();
              assertEquals(0, beforeFilters.size());

              // Refresh the filter list
              filterManager.refreshFilters();
              Date afterRefresh = new Date();

              // Should have many filters
              final List<LoadedFileFilter> afterFilters = filterManager.getFilters();
              assertTrue(
                  filterManager.getFirstBatchCreated().toEpochMilli() <= afterLoad.getTime());
              assertTrue(
                  filterManager.getLastBatchCreated().toEpochMilli() <= afterRefresh.getTime());
              assertTrue(
                  filterManager.getTransactionTime().toEpochMilli()
                      > initialTransactionTime.toEpochMilli());
              assertTrue(afterFilters.size() > 1);
            });
  }

  /** Test isResultSetEmpty with one filter. */
  @Test
  public void isResultSetEmpty() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              final LoadedFilterManager filterManager = new LoadedFilterManager(dataSource);
              filterManager.setEntityManager(entityManager);
              filterManager.init();

              // Establish a before load time
              final Date beforeLoad = new Date();
              final DateRangeParam beforeLoadUnbounded =
                  new DateRangeParam().setUpperBoundExclusive(beforeLoad);
              PipelineTestUtils.get().pauseMillis(10);

              loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

              // Establish a time after load but before refresh
              PipelineTestUtils.get().pauseMillis(10);
              final Date beforeRefresh = new Date();
              final DateRangeParam beforeRefreshRange =
                  new DateRangeParam(beforeRefresh, Date.from(Instant.now().plusMillis(5)));
              PipelineTestUtils.get().pauseMillis(10);

              filterManager.refreshFilters();

              // Establish a after refresh time
              final Date afterRefresh = Date.from(Instant.now().plusMillis(10));
              final DateRangeParam afterRefreshRange =
                  new DateRangeParam(afterRefresh, Date.from(Instant.now().plusMillis(15)));

              // Assert on isInKnownBounds
              assertTrue(
                  filterManager.isInBounds(beforeRefreshRange),
                  "Known bound should include a time range before refresh");
              assertTrue(
                  filterManager.isInBounds(afterRefreshRange),
                  "Known bound includes time after refresh");
              assertFalse(
                  filterManager.isInBounds(beforeLoadUnbounded),
                  "Unbounded lower range always match null lastUpdated which are not known");

              // Assert on isResultSetEmpty
              assertTrue(
                  filterManager.isResultSetEmpty(SAMPLE_BENE, beforeRefreshRange),
                  "Expected date range to not have a matching filter");
              assertTrue(
                  filterManager.isResultSetEmpty(INVALID_BENE, beforeRefreshRange),
                  "Expected date range to not have a matching filter");
            });
  }

  /** Test isResultSetEmpty with multiple refreshes. */
  @Test
  public void testWithMultipleRefreshes() {
    PipelineTestUtils.get()
        .doTestWithDb(
            (dataSource, entityManager) -> {
              final LoadedFilterManager filterManager = new LoadedFilterManager(dataSource);
              filterManager.setEntityManager(entityManager);
              filterManager.init();
              loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

              // Establish a couple of times
              PipelineTestUtils.get().pauseMillis(1000);
              final Date afterSampleA = new Date();
              PipelineTestUtils.get().pauseMillis(10);
              final Date afterSampleAPlus = new Date();
              final DateRangeParam afterSampleARange =
                  new DateRangeParam(afterSampleA, afterSampleAPlus);

              // Refresh the filter list
              PipelineTestUtils.get().pauseMillis(10);
              filterManager.refreshFilters();

              // Test after refresh
              int after1Count = filterManager.getFilters().size();
              assertTrue(filterManager.isInBounds(afterSampleARange));
              assertTrue(
                  filterManager.isResultSetEmpty(SAMPLE_BENE, afterSampleARange),
                  "Expected date range to not have a matching filter");
              assertTrue(
                  filterManager.isResultSetEmpty(INVALID_BENE, afterSampleARange),
                  "Expected date range to not have a matching filter");

              // Refresh again (should do nothing)
              filterManager.refreshFilters();
              int after2Count = filterManager.getFilters().size();
              assertEquals(after1Count, after2Count);

              // Load some more data
              loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
              PipelineTestUtils.get().pauseMillis(1000);
              final Date afterSampleU = new Date();
              final DateRangeParam aroundSampleU = new DateRangeParam(afterSampleA, afterSampleU);
              filterManager.refreshFilters();

              // Test after refresh
              assertFalse(
                  filterManager.isResultSetEmpty(SAMPLE_BENE, aroundSampleU),
                  "Expected date range to not have a matching filter");
              assertTrue(
                  filterManager.isResultSetEmpty(INVALID_BENE, aroundSampleU),
                  "Expected date range to not have a matching filter");
            });
  }

  /**
   * Loads data for the test.
   *
   * @param sampleResources the sample RIF resources to load
   */
  private static void loadData(List<StaticRifResource> sampleResources) {
    LoadAppOptions loadOptions = CcwRifLoadTestUtils.getLoadOptions();
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            false,
            sampleResources.stream()
                .map(StaticRifResource::toRifFile)
                .collect(Collectors.toList()));

    // Create the processors that will handle each stage of the pipeline.
    RifFilesProcessor processor = new RifFilesProcessor();
    RifLoader loader =
        new RifLoader(loadOptions, PipelineTestUtils.get().getPipelineApplicationState());

    // Link up the pipeline and run it.
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
      try {
        loader.processBlocking(rifFileRecords);
      } catch (Exception ex) {
        throw Exceptions.propagate(ex);
      }
    }
  }
}
