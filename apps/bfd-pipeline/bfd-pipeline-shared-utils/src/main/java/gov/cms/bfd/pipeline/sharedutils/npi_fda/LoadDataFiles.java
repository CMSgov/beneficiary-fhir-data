package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;

/** Abstract class to download and persist datafiles, for NPI and FDA. */
public abstract class LoadDataFiles<TData> implements Callable<Integer> {

  /** The EntityManager to use for database operations. */
  final EntityManager entityManager;

  /** The number of records to save before committing a transaction. */
  int batchSize;

  /** How often to run the job, in days. */
  int runInterval;

  /** The table name; will be fda_data or npi_data. */
  private final String tableName;

  /** Query for checking lastUpdated of the table. */
  private static final String GET_LAST_UPDATED_QUERY =
      "SELECT last_updated FROM ccw.npi_fda_meta WHERE table_name = :tableName";

  /** Query for saving the lastUpdated of the table. */
  private static final String SAVE_LAST_UPDATED_QUERY =
      """
                INSERT INTO ccw.npi_fda_meta (table_name, last_updated)
                VALUES(:tableName, :lastUpdated)
                ON CONFLICT (table_name)
                DO UPDATE SET
                last_updated = :lastUpdated
             """;

  private final Logger LOGGER;
  private final String delimiter;

  /**
   * Builds the Resource and persists it.
   *
   * @return Total number of records saved.
   * @throws IOException (any errors encountered will be bubbled up)
   */
  abstract Integer persistResource() throws IOException;

  abstract boolean resolveFileName(String name);

  /**
   * Streams the zip file and calls a method to create the records.
   *
   * @param fileName the output file/resource to produce
   * @return Total number of records saved.
   * @throws IOException (any errors encountered will be bubbled up)
   * @throws IllegalStateException if there is an issue with the file
   */
  Integer downloadDataFile(String fileName) throws IOException, IllegalStateException {
    String version = getClass().getPackage().getImplementationVersion();
    // download the file
    URL ndctextZipUrl = new URL(fileName);
    HttpURLConnection connection = (HttpURLConnection) ndctextZipUrl.openConnection();
    connection.setRequestProperty(
        "User-Agent", String.format("BFD/%s (Beneficiary FHIR Data Server)", version));

    try (ZipInputStream zipInputStream = new ZipInputStream(connection.getInputStream())) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (resolveFileName(entry.getName())) {
          InputStreamReader ir = new InputStreamReader(zipInputStream);
          return saveDataFile(ir);
        }
        zipInputStream.closeEntry();
      }
    }
    throw new IllegalStateException("No DataFile file found");
  }

  /**
   * Converts a CSV record to an entity.
   *
   * @param csvRecord The CSV record with the data.
   * @return the NPIData entity.
   */
  abstract TData getDataFromCsv(CSVRecord csvRecord);

  /**
   * Constructor.
   *
   * @param tableName The table name to use.
   * @param entityManager The entityManager.
   * @param batchSize The Batch size to save at one time.
   * @param runInterval The time in days between runs.
   * @param LOGGER The logger.
   * @param delimiter The CSV delimiter.
   */
  LoadDataFiles(
      String tableName,
      EntityManager entityManager,
      int batchSize,
      int runInterval,
      Logger LOGGER,
      String delimiter) {
    this.tableName = tableName;
    this.entityManager = entityManager;
    this.runInterval = runInterval;
    this.batchSize = batchSize;
    this.LOGGER = LOGGER;
    this.delimiter = delimiter;
  }

  /** {@inheritDoc} */
  @Override
  public Integer call() throws Exception {
    if (shouldLoadData()) {
      Instant start = Instant.now();
      int totalRecords = persistResource();
      Instant finish = Instant.now();
      long totalTime = Duration.between(start, finish).toSeconds();
      LOGGER.info(
          "Finished saving to table {} in {} seconds. Processed {} records.",
          tableName,
          totalTime,
          totalRecords);
      return totalRecords;
    } else {
      LOGGER.info("Run interval has not passed. No data will be loaded into {}.", tableName);
      return -1;
    }
  }

  /**
   * Determines if the data should be loaded based on the lastUpdated date.
   *
   * @return true if the data should be loaded.
   */
  boolean shouldLoadData() {
    Date lastUpdated;
    Query query = entityManager.createNativeQuery(GET_LAST_UPDATED_QUERY);
    query.setParameter("tableName", tableName);
    try {
      lastUpdated = (Date) query.getSingleResult();
    } catch (NoResultException e) {
      return true;
    }
    return LocalDate.now().isAfter(lastUpdated.toLocalDate().plusDays(runInterval));
  }

  /** Updates the lastUpdated Date. */
  void setLastUpdated() {
    Date lastUpdated = new Date(System.currentTimeMillis());
    entityManager.getTransaction().begin();
    Query query = entityManager.createNativeQuery(SAVE_LAST_UPDATED_QUERY);
    query.setParameter("tableName", tableName);
    query.setParameter("lastUpdated", lastUpdated);
    query.executeUpdate();
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  /**
   * Converts the data file and saves it to the database.
   *
   * @param is the file input stream.
   * @return Total number of records saved.
   * @throws IOException exception thrown.
   */
  Integer saveDataFile(InputStreamReader is) throws IOException {

    int savedCount = 0;
    try (BufferedReader reader = new BufferedReader(is)) {
      CSVParser csvParser =
          CSVParser.builder()
              .setReader(reader)
              .setFormat(
                  CSVFormat.DEFAULT
                      .builder()
                      .setDelimiter(delimiter)
                      .setHeader()
                      .setIgnoreHeaderCase(true)
                      .setTrim(true)
                      .get())
              .get();

      LOGGER.info("Starting to save data to table {}", tableName);
      entityManager.getTransaction().begin();
      boolean interrupted = false;
      for (CSVRecord csvRecord : csvParser) {
        TData npiData = getDataFromCsv(csvRecord);
        entityManager.merge(npiData);
        savedCount++;

        // The commit frequency is determined by an SSM parameter.
        if (savedCount % batchSize == 0) {
          entityManager.getTransaction().commit();
          entityManager.clear();
          LOGGER.info("Progress: Saved {} records to table {}", savedCount, tableName);
          entityManager.getTransaction().begin();
        }
        if (Thread.currentThread().isInterrupted()) {
          interrupted = true;
          break;
        }
      }
      // final commit
      entityManager.getTransaction().commit();
      entityManager.clear();
      if (!interrupted) {
        setLastUpdated();
      }
    }
    return savedCount;
  }
}
