package gov.cms.bfd.server.war.stu3.providers;

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
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoadedFilterManagerTest {
  @SuppressWarnings("unused")
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadedFilterManagerIT.class);

  private static final String SAMPLE_BENE = "567834";
  private static final String INVALID_BENE = "1";
  private static final LoadedBatch[] preBatches = new LoadedBatch[8];
  private static final Instant[] preDates = new Instant[preBatches.length * 5];

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void beforeAll() {
    // Create a few time stamps to play with
    Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
    for (int i = 0; i < preDates.length; i++) {
      preDates[i] = (now.plusSeconds(i));
    }
    List<String> beneficiaries = Collections.singletonList(SAMPLE_BENE);
    for (int i = 0; i < preBatches.length; i++) {
      preBatches[i] = new LoadedBatch(i + 1, (i / 2) + 1, beneficiaries, preDates[i * 5 + 4]);
    }
  }

  @Test
  public void buildEmptyFilter() {
    final MockDb mockDb = new MockDb().insert(1, preDates[2]);
    final List<LoadedFileFilter> loadedFilter =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(0, loadedFilter.size());
  }

  @Test
  public void buildOneFilter() {
    final MockDb mockDb = new MockDb().insert(1, preDates[0]).insert(preBatches[0]);
    final List<LoadedFileFilter> filters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(1, filters.size());

    // Test the filter
    final DateRangeParam during1 =
        new DateRangeParam(Date.from(preDates[1]), Date.from(preDates[2]));
    Assert.assertTrue(filters.get(0).matchesDateRange(during1));
    Assert.assertEquals(1, filters.get(0).getBatchesCount());
    Assert.assertTrue(filters.get(0).mightContain(SAMPLE_BENE));
    Assert.assertFalse(filters.get(0).mightContain(INVALID_BENE));
  }

  @Test
  public void buildManyFilter() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(3, preDates[21])
            .insert(preBatches[0], preBatches[2], preBatches[4]);
    final List<LoadedFileFilter> filters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(3, filters.size());
    Assert.assertEquals(1, filters.get(2).getBatchesCount());
  }

  @Test
  public void updateManyFilters() {
    final MockDb mockDb1 =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(3, preDates[21])
            .insert(preBatches[0], preBatches[2], preBatches[4]);
    final List<LoadedFileFilter> filters1 =
        LoadedFilterManager.updateFilters(
            Collections.emptyList(), mockDb1.fetchAllTuples(), mockDb1::fetchById);
    Assert.assertEquals(3, filters1.size());
    Assert.assertEquals(1, filters1.get(2).getLoadedFileId());
    Assert.assertEquals(1, filters1.get(2).getBatchesCount());

    final MockDb mockDb2 = new MockDb().insert(1, preDates[1]).insert(preBatches[0], preBatches[1]);
    final List<LoadedFileFilter> filters2 =
        LoadedFilterManager.updateFilters(filters1, mockDb2.fetchAllTuples(), mockDb2::fetchById);
    Assert.assertEquals(3, filters2.size());
    Assert.assertEquals(1, filters2.get(2).getLoadedFileId());
    Assert.assertEquals(2, filters2.get(2).getBatchesCount());

    final MockDb mockDb3 = new MockDb().insert(4, preDates[31]).insert(preBatches[6]);
    final List<LoadedFileFilter> filters3 =
        LoadedFilterManager.updateFilters(filters1, mockDb3.fetchAllTuples(), mockDb3::fetchById);
    Assert.assertEquals(4, filters3.size());
    Assert.assertEquals(1, filters3.get(3).getLoadedFileId());
    Assert.assertEquals(4, filters3.get(0).getLoadedFileId());
  }

  @Test
  public void testIsResultSetEmpty() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFilterManager.LoadedTuple> tuples = mockDb.fetchAllTuples();
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildFilters(tuples, mockDb::fetchById);
    Assert.assertEquals(2, aFilters.size());

    // Setup the manager and test a few lastUpdated ranges
    final LoadedFilterManager filterManagerA = new LoadedFilterManager();
    filterManagerA.set(aFilters, preDates[1], preBatches[2].getCreated());
    final DateRangeParam beforeRange =
        new DateRangeParam(Date.from(preDates[0]), Date.from(preDates[1]));
    Assert.assertFalse(filterManagerA.isInBounds(beforeRange));
    Assert.assertFalse(filterManagerA.isResultSetEmpty(SAMPLE_BENE, beforeRange));
    final DateRangeParam duringRange1 =
        new DateRangeParam(Date.from(preDates[2]), Date.from(preDates[3]));
    Assert.assertTrue(filterManagerA.isInBounds(duringRange1));
    Assert.assertFalse(filterManagerA.isResultSetEmpty(SAMPLE_BENE, duringRange1));
    Assert.assertTrue(filterManagerA.isResultSetEmpty(INVALID_BENE, duringRange1));
    final DateRangeParam duringRange2 =
        new DateRangeParam()
            .setLowerBoundExclusive(Date.from(preDates[9]))
            .setUpperBoundExclusive(Date.from(preDates[10]));
    Assert.assertTrue(filterManagerA.isInBounds(duringRange2));
    Assert.assertTrue(filterManagerA.isResultSetEmpty(SAMPLE_BENE, duringRange2));
    Assert.assertTrue(filterManagerA.isResultSetEmpty(INVALID_BENE, duringRange2));
    final DateRangeParam afterRange =
        new DateRangeParam(Date.from(preDates[20]), Date.from(preDates[21]));
    Assert.assertTrue(filterManagerA.isInBounds(afterRange));
    Assert.assertTrue(filterManagerA.isResultSetEmpty(SAMPLE_BENE, afterRange));
  }

  @Test
  public void testTypicalSequence() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(2, aFilters.size());

    // Simulate starting a new file with no mockDb
    mockDb.insert(3, preDates[21]);
    final List<LoadedFileFilter> bFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(2, bFilters.size());

    // Simulate adding a new batch with the same fileId
    mockDb.insert(preBatches[4]);
    final List<LoadedFileFilter> cFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(3, cFilters.size());
    Assert.assertEquals(1, cFilters.get(1).getBatchesCount());
  }

  @Test
  public void testErrorSequence() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(2, aFilters.size());

    // Simulate starting a new file with no mockDb. Don't complete this batch
    mockDb.insert(3, preDates[21]);
    final List<LoadedFileFilter> bFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(2, bFilters.size());

    // Simulate adding a new batch not in the same file id
    mockDb.insert(4, preDates[28]).insert(preBatches[6]);
    final List<LoadedFileFilter> cFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(3, cFilters.size());
    Assert.assertEquals(1, cFilters.get(0).getBatchesCount());
  }

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

    // You would expect this to be true, but Timestamp splits the ms from the seconds, and this is
    // only comparing the seconds which are equal
    Assert.assertFalse(lastBatchCreated.isBefore(currentLastBatchCreated));
  }

  @Test
  public void testTrimFilters() {
    // Build a couple of filters
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(preBatches[0], preBatches[1], preBatches[2]);
    final List<LoadedFileFilter> aFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(2, aFilters.size());

    // Trim the loadedFiles
    List<LoadedFile> files = mockDb.fetchAllFiles();
    Assert.assertEquals(1L, files.get(0).getLoadedFileId());
    files.remove(0);

    // Trim the map and test results. Should have the filter for
    final List<LoadedFileFilter> bFilters = LoadedFilterManager.trimFilters(aFilters, files);
    Assert.assertEquals(1, bFilters.size());
    Assert.assertSame(bFilters.get(0), aFilters.get(0));
  }

  /** Helper class that mocks a DB for LoadedFilterManager testing */
  private static class MockDb {
    private final ArrayList<LoadedBatch> batches = new ArrayList<>();
    private final ArrayList<LoadedFile> files = new ArrayList<>();

    MockDb insert(LoadedBatch... batches) {
      Collections.addAll(this.batches, batches);
      return this;
    }

    MockDb insert(long loadedFileId, Instant firstUpdated) {
      files.add(new LoadedFile(loadedFileId, "BENEFICIARY", firstUpdated));
      return this;
    }

    List<LoadedBatch> fetchById(Long loadedFiledId) {
      return batches.stream()
          .filter(b -> b.getLoadedFileId() == loadedFiledId)
          .collect(Collectors.toList());
    }

    List<LoadedFile> fetchAllFiles() {
      return files;
    }

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
