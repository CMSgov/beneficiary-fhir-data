package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import java.time.Instant;
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
  private static final Date[] preDates = new Date[preBatches.length * 5];

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void beforeAll() {
    // Create a few time stamps to play with
    Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS);
    for (int i = 0; i < preDates.length; i++) {
      preDates[i] = Date.from(now.plusSeconds(i));
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
    final DateRangeParam during1 = new DateRangeParam(preDates[1], preDates[2]);
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
  public void calcBoundsWithInitial() {
    final Date upper = LoadedFilterManager.calcUpperBound(Collections.emptyList(), preDates[7]);
    final Date lower = LoadedFilterManager.calcLowerBound(Collections.emptyList(), upper);

    Assert.assertEquals(preDates[7], upper);
    Assert.assertEquals(preDates[7], lower);
  }

  @Test
  public void calcBoundsWithEmpty() {
    final MockDb mockDb = new MockDb().insert(1, preDates[1]);
    final Date upper = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[7]);
    final Date lower = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upper);

    Assert.assertEquals(preDates[7], upper);
    Assert.assertEquals(preDates[1], lower);
  }

  @Test
  public void calcBoundsWithOne() {
    final MockDb mockDb = new MockDb().insert(1, preDates[1]).insert(preBatches[0]);
    final Date upper = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[7]);
    final Date lower = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upper);

    Assert.assertEquals(preDates[7], upper);
    Assert.assertEquals(preDates[1], lower);
  }

  @Test
  public void calcBoundsWithMany() {
    final MockDb mockDb =
        new MockDb()
            .insert(1, preDates[1])
            .insert(2, preDates[11])
            .insert(3, preDates[21])
            .insert(preBatches[0], preBatches[2], preBatches[4]);
    final Date upper = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[28]);
    final Date lower = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upper);

    Assert.assertEquals(preDates[28], upper);
    Assert.assertEquals(preDates[1], lower);
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
    final Date upperA = LoadedFilterManager.calcUpperBound(tuples, preDates[19]);
    final Date lowerA = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upperA);
    Assert.assertEquals(preDates[19], upperA);
    Assert.assertEquals(preDates[1], lowerA);

    // Setup the manager and test a few lastUpdated ranges
    final LoadedFilterManager filterManagerA = new LoadedFilterManager(0);
    filterManagerA.set(aFilters, lowerA, upperA, 1, 2);
    final DateRangeParam beforeRange = new DateRangeParam(preDates[0], preDates[1]);
    Assert.assertFalse(filterManagerA.isInKnownBounds(beforeRange));
    Assert.assertFalse(filterManagerA.isResultSetEmpty(SAMPLE_BENE, beforeRange));
    final DateRangeParam duringRange1 = new DateRangeParam(preDates[2], preDates[3]);
    Assert.assertTrue(filterManagerA.isInKnownBounds(duringRange1));
    Assert.assertFalse(filterManagerA.isResultSetEmpty(SAMPLE_BENE, duringRange1));
    Assert.assertTrue(filterManagerA.isResultSetEmpty(INVALID_BENE, duringRange1));
    final DateRangeParam duringRange2 =
        new DateRangeParam()
            .setLowerBoundExclusive(preDates[9])
            .setUpperBoundExclusive(preDates[10]);
    Assert.assertTrue(filterManagerA.isInKnownBounds(duringRange2));
    Assert.assertTrue(filterManagerA.isResultSetEmpty(SAMPLE_BENE, duringRange2));
    Assert.assertTrue(filterManagerA.isResultSetEmpty(INVALID_BENE, duringRange2));
    final DateRangeParam afterRange = new DateRangeParam(preDates[20], preDates[21]);
    Assert.assertFalse(filterManagerA.isInKnownBounds(afterRange));
    Assert.assertFalse(filterManagerA.isResultSetEmpty(SAMPLE_BENE, afterRange));
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
    final Date upperA = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[19]);
    final Date lowerA = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upperA);
    Assert.assertEquals(preDates[19], upperA);
    Assert.assertEquals(preDates[1], lowerA);

    // Simulate starting a new file with no mockDb
    mockDb.insert(3, preDates[21]);
    final List<LoadedFileFilter> bFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    Assert.assertEquals(2, bFilters.size());
    final Date upperB = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[22]);
    final Date lowerB = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upperB);
    Assert.assertEquals(preDates[22], upperB);
    Assert.assertEquals(preDates[1], lowerB);

    // Simulate adding a new batch with the same fileId
    mockDb.insert(preBatches[4]);
    final List<LoadedFileFilter> cFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    final Date upperC = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[25]);
    final Date lowerC = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upperC);
    Assert.assertEquals(3, cFilters.size());
    Assert.assertEquals(1, cFilters.get(1).getBatchesCount());
    Assert.assertEquals(preDates[25], upperC);
    Assert.assertEquals(preDates[1], lowerC);
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
    final Date upperB = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[22]);
    final Date lowerB = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upperB);
    Assert.assertEquals(2, bFilters.size());
    Assert.assertEquals(preDates[22], upperB);
    Assert.assertEquals(preDates[1], lowerB);

    // Simulate adding a new batch not in the same file id
    mockDb.insert(4, preDates[28]).insert(preBatches[6]);
    final List<LoadedFileFilter> cFilters =
        LoadedFilterManager.buildFilters(mockDb.fetchAllTuples(), mockDb::fetchById);
    final Date upperC = LoadedFilterManager.calcUpperBound(mockDb.fetchAllTuples(), preDates[33]);
    final Date lowerC = LoadedFilterManager.calcLowerBound(mockDb.fetchAllFiles(), upperC);
    Assert.assertEquals(3, cFilters.size());
    Assert.assertEquals(1, cFilters.get(0).getBatchesCount());
    Assert.assertEquals(preDates[34], upperC);
    Assert.assertEquals(preDates[1], lowerC);
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
    private final ArrayList<LoadedBatch> mockDb = new ArrayList<>();
    private final ArrayList<LoadedFile> files = new ArrayList<>();

    MockDb insert(LoadedBatch... mockDb) {
      Collections.addAll(this.mockDb, mockDb);
      return this;
    }

    MockDb insert(long loadedFileId, Date firstUpdated) {
      files.add(new LoadedFile(loadedFileId, "BENFICIARY", firstUpdated));
      return this;
    }

    List<LoadedBatch> fetchById(Long loadedFiledId) {
      return mockDb.stream()
          .filter(b -> b.getLoadedFileId() == loadedFiledId)
          .collect(Collectors.toList());
    }

    List<LoadedFile> fetchAllFiles() {
      return files;
    }

    ArrayList<LoadedFilterManager.LoadedTuple> fetchAllTuples() {
      if (mockDb.size() + files.size() == 0) {
        return new ArrayList<>();
      }
      ArrayList<LoadedFilterManager.LoadedTuple> tuples = new ArrayList<>();
      files.forEach(
          file -> {
            Optional<Date> lastUpdated =
                mockDb.stream()
                    .filter(b -> b.getLoadedFileId() == file.getLoadedFileId())
                    .map(LoadedBatch::getCreated)
                    .reduce((a, b) -> a.after(b) ? a : b);
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
