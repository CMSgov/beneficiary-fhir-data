package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.server.war.commons.LoadedFileFilter;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Unit tests for the {@link LoadedFilterManager}. */
public final class LoadedFilterManagerTest {

  /** Sample valid bene for the test. */
  private static final long SAMPLE_BENE = 567834L;

  /** Sample invalid bene for the test. */
  private static final long INVALID_BENE = 1L;

  /** Array of batches for the test. */
  private static final LoadedBatch[] preBatches = new LoadedBatch[8];

  /** Array of dates for the test. */
  private static final Instant[] preDates = new Instant[preBatches.length * 5];

  /** Sets up required test data. */
  @BeforeAll
  public static void beforeAll() {
    // Create a few time stamps to play with
    Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
    for (int i = 0; i < preDates.length; i++) {
      preDates[i] = now.plusSeconds(i);
    }
    List<Long> beneficiaries = Collections.singletonList(SAMPLE_BENE);
    for (int i = 0; i < preBatches.length; i++) {
      preBatches[i] = new LoadedBatch(i + 1, (i / 2) + 1, beneficiaries, preDates[i * 5 + 4]);
    }
  }

  /** Validates that the filter is empty when no batches are added to the db. */
  @Test
  public void buildEmptyFilter() {
    final MockDb mockDb = new MockDb().insert(1, preDates[2]);
    final List<LoadedFileFilter> loadedFilter =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(0, loadedFilter.size());
  }

  /**
   * Validates that the filter exists and has the expected matches when a batch is added to the db.
   */
  @Test
  public void buildOneFilter() {
    final MockDb mockDb = new MockDb().insert(1, preDates[0]).insert(preBatches[0]);
    final List<LoadedFileFilter> filters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(1, filters.size());

    // Test the filter
    final DateRangeParam during1 =
        new DateRangeParam(Date.from(preDates[1]), Date.from(preDates[2]));
    assertTrue(filters.get(0).matchesDateRange(during1));
    assertEquals(1, filters.get(0).getBatchesCount());
    assertTrue(filters.get(0).mightContain(SAMPLE_BENE));
    assertFalse(filters.get(0).mightContain(INVALID_BENE));
  }

  /**
   * Validates that the filters exist and have the expected matches when many batches are added to
   * the db.
   */
  @Test
  public void buildManyFilter() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(3, preDates[21])
            .insert(preBatches[0], preBatches[2], preBatches[4]);
    final List<LoadedFileFilter> filters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(3, filters.size());
    assertEquals(1, filters.get(2).getBatchesCount());
  }

  /**
   * Validates that the filters exist and have the expected matches when many batches are added to
   * the db piecemeal.
   */
  @Test
  public void updateManyFilters() {
    final MockDb mockDb1 =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(3, preDates[21])
            .insert(preBatches[0], preBatches[2], preBatches[4]);
    final List<LoadedFileFilter> filters1 =
        LoadedFilterManager.buildMergedFilters(
                Collections.emptyList(),
                mockDb1.fetchAllTuples(),
                mockDb1::fetchById,
                mockDb1::fetchBatchSizeById,
                mockDb1::fetchEstimatedBeneSize)
            .toList();
    assertEquals(3, filters1.size());
    assertEquals(1, filters1.get(2).getLoadedFileId());
    assertEquals(1, filters1.get(2).getBatchesCount());

    final MockDb mockDb2 = new MockDb().insert(1, preDates[1]).insert(preBatches[0], preBatches[1]);
    final List<LoadedFileFilter> filters2 =
        LoadedFilterManager.buildMergedFilters(
                filters1,
                mockDb2.fetchAllTuples(),
                mockDb2::fetchById,
                mockDb2::fetchBatchSizeById,
                mockDb2::fetchEstimatedBeneSize)
            .toList();
    assertEquals(3, filters2.size());
    assertEquals(1, filters2.get(2).getLoadedFileId());
    assertEquals(2, filters2.get(2).getBatchesCount());

    final MockDb mockDb3 = new MockDb().insert(4, preDates[31]).insert(preBatches[6]);
    final List<LoadedFileFilter> filters3 =
        LoadedFilterManager.buildMergedFilters(
                filters1,
                mockDb3.fetchAllTuples(),
                mockDb3::fetchById,
                mockDb3::fetchBatchSizeById,
                mockDb3::fetchEstimatedBeneSize)
            .toList();
    assertEquals(4, filters3.size());
    assertEquals(1, filters3.get(3).getLoadedFileId());
    assertEquals(4, filters3.get(0).getLoadedFileId());
  }

  /** Tests the {@link LoadedFilterManager#isResultSetEmpty} works for various ranges. */
  @Test
  public void testIsResultSetEmpty() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFilterManager.LoadedTuple> tuples = mockDb.fetchAllTuples();
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildNewFilters(
                tuples,
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(2, aFilters.size());

    // Setup the manager and test a few lastUpdated ranges
    final LoadedFilterManager filterManagerA = new LoadedFilterManager(mock(DataSource.class));
    filterManagerA.set(aFilters, preDates[1], preBatches[2].getCreated());
    final DateRangeParam beforeRange =
        new DateRangeParam(Date.from(preDates[0]), Date.from(preDates[1]));
    assertFalse(filterManagerA.isInBounds(beforeRange));
    assertFalse(filterManagerA.isResultSetEmpty(SAMPLE_BENE, beforeRange));
    final DateRangeParam duringRange1 =
        new DateRangeParam(Date.from(preDates[2]), Date.from(preDates[3]));
    assertTrue(filterManagerA.isInBounds(duringRange1));
    assertFalse(filterManagerA.isResultSetEmpty(SAMPLE_BENE, duringRange1));
    assertTrue(filterManagerA.isResultSetEmpty(INVALID_BENE, duringRange1));
    final DateRangeParam duringRange2 =
        new DateRangeParam()
            .setLowerBoundExclusive(Date.from(preDates[9]))
            .setUpperBoundExclusive(Date.from(preDates[10]));
    assertTrue(filterManagerA.isInBounds(duringRange2));
    assertTrue(filterManagerA.isResultSetEmpty(SAMPLE_BENE, duringRange2));
    assertTrue(filterManagerA.isResultSetEmpty(INVALID_BENE, duringRange2));
    final DateRangeParam afterRange =
        new DateRangeParam(Date.from(preDates[20]), Date.from(preDates[21]));
    assertTrue(filterManagerA.isInBounds(afterRange));
    assertTrue(filterManagerA.isResultSetEmpty(SAMPLE_BENE, afterRange));
  }

  /** Tests a typical flow for using the filter. */
  @Test
  public void testTypicalSequence() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(2, aFilters.size());

    // Simulate starting a new file with no mockDb
    mockDb.insert(3, preDates[21]);
    final List<LoadedFileFilter> bFilters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(2, bFilters.size());

    // Simulate adding a new batch with the same fileId
    mockDb.insert(preBatches[4]);
    final List<LoadedFileFilter> cFilters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(3, cFilters.size());
    assertEquals(1, cFilters.get(1).getBatchesCount());
  }

  /** Tests an error scenario: adding a new batch not in the same file id. */
  @Test
  public void testErrorSequence() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(2, aFilters.size());

    // Simulate starting a new file with no mockDb. Don't complete this batch
    mockDb.insert(3, preDates[21]);
    final List<LoadedFileFilter> bFilters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(2, bFilters.size());

    // Simulate adding a new batch not in the same file id
    mockDb.insert(4, preDates[28]).insert(preBatches[6]);
    final List<LoadedFileFilter> cFilters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(3, cFilters.size());
    assertEquals(1, cFilters.get(0).getBatchesCount());
  }

  /**
   * Tests that required date comparison assumptions needed for the {@link LoadedFilterManager} to
   * work remain.
   */
  @Test
  public void testDateComparisonAssumptions() throws ParseException {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    // Root cause of BFD-713.
    // Adding this here in case a new version of Java changes behavior.  The Date objects used in
    // LoadedFilterManager are actually java.sql.Timestamps, which split milliseconds from the
    // seconds
    Instant lastBatchCreated =
        (LocalDateTime.parse("2021-03-27 21:14:52.316", formatter))
            .atZone(ZoneId.of("UTC"))
            .toInstant();
    Instant currentLastBatchCreated =
        (LocalDateTime.parse("2021-03-27 21:14:52.557", formatter))
            .atZone(ZoneId.of("UTC"))
            .toInstant();

    assertTrue(lastBatchCreated.isBefore(currentLastBatchCreated));
  }

  /**
   * Tests that removing a loaded file still has a remaining filter for a file that was not removed.
   */
  @Test
  public void testBuildTrimmedFilters() {
    // Build a couple of filters
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildNewFilters(
                mockDb.fetchAllTuples(),
                mockDb::fetchById,
                mockDb::fetchBatchSizeById,
                mockDb::fetchEstimatedBeneSize)
            .toList();
    assertEquals(2, aFilters.size());

    // Trim the loadedFiles
    List<LoadedFile> files = mockDb.fetchAllFiles();
    assertEquals(1L, files.get(0).getLoadedFileId());
    files.remove(0);

    // Trim the map and test results. Should have the filter for
    final List<LoadedFileFilter> bFilters =
        LoadedFilterManager.buildTrimmedFilters(aFilters.stream(), files).toList();
    assertEquals(1, bFilters.size());
    assertSame(bFilters.get(0), aFilters.get(0));
  }

  /** Helper class that mocks a DB for LoadedFilterManager testing. */
  private static class MockDb {
    /** Batches for loading. */
    private final ArrayList<LoadedBatch> batches = new ArrayList<>();

    /** Files for loading. */
    private final ArrayList<LoadedFile> files = new ArrayList<>();

    /**
     * Inserts a batch into the mock db.
     *
     * @param batches the batches to insert
     * @return the mock db
     */
    MockDb insert(LoadedBatch... batches) {
      Collections.addAll(this.batches, batches);
      return this;
    }

    /**
     * Inserts the file id and firstUpdated date to the mock db.
     *
     * @param loadedFileId the file id
     * @param firstUpdated the first updated date
     * @return the mock db
     */
    MockDb insert(long loadedFileId, Instant firstUpdated) {
      files.add(new LoadedFile(loadedFileId, "BENEFICIARY", firstUpdated));
      return this;
    }

    /**
     * Fetches from the database by id.
     *
     * @param loadedFiledId the loaded filed id
     * @param beneCount the bene count
     * @return the list of results
     */
    Stream<LoadedBatch> fetchById(Long loadedFiledId, Integer beneCount) {
      return batches.stream().filter(b -> b.getLoadedFileId() == loadedFiledId);
    }

    Long fetchBatchSizeById(Long loadedFileId) {
      return (long) fetchById(loadedFileId, 0).toList().size();
    }

    Long fetchEstimatedBeneSize(Long loadedFileId) {
      return (long)
          fetchById(loadedFileId, 0)
              .map(b -> b.getBeneficiariesList().size())
              .findFirst()
              .orElse(1);
    }

    /**
     * Fetches all files from the db.
     *
     * @return the list of results
     */
    List<LoadedFile> fetchAllFiles() {
      return files;
    }

    /**
     * Fetches all tuples from the database.
     *
     * @return the list of tuples
     */
    ArrayList<LoadedFilterManager.LoadedTuple> fetchAllTuples() {
      if (batches.size() + files.size() == 0) {
        return new ArrayList<>();
      }
      ArrayList<LoadedFilterManager.LoadedTuple> tuples = new ArrayList<>();
      files.forEach(
          file -> {
            Optional<Instant> lastUpdated =
                batches.stream()
                    .filter(b -> b.getLoadedFileId() == file.getLoadedFileId())
                    .map(LoadedBatch::getCreated)
                    .reduce((a, b) -> a.isAfter(b) ? a : b);
            lastUpdated.ifPresent(
                updated ->
                    tuples.add(
                        new LoadedFilterManager.LoadedTuple(
                            file.getLoadedFileId(), file.getCreated(), updated)));
          });
      tuples.sort((a, b) -> b.getFirstUpdated().compareTo(a.getFirstUpdated()));
      return tuples;
    }
  }
}
