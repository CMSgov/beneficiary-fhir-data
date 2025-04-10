package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import static java.util.Map.entry;

import gov.cms.bfd.model.rif.npi_fda.NPIData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to download NPI data and persist it into the database. */
public class LoadNpiDataFiles implements Callable<Integer> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadNpiDataFiles.class);

  /** Field for taxonomy code in CSV. */
  private static final String TAXONOMY_CODE_FIELD = "Healthcare Provider Taxonomy Code_1";

  /** Field for provider organization name in CSV. */
  private static final String PROVIDER_ORGANIZATION_NAME_FIELD =
      "Provider Organization Name (Legal Business Name)";

  /** Field for NPI in CSV. */
  private static final String NPI_FIELD = "NPI";

  /** Field for Entity Type Code in CSV. */
  private static final String ENTITY_TYPE_CODE_FIELD = "Entity Type Code";

  /** Field for Provider Credential code in CSV. */
  private static final String PROVIDER_CREDENTIAL_FIELD = "Provider Credential Text";

  /** Field for Provider first name in CSV. */
  private static final String PROVIDER_FIRST_NAME_FIELD = "Provider First Name";

  /** Field for Provider middle name in CSV. */
  private static final String PROVIDER_MIDDLE_NAME_FIELD = "Provider Middle Name";

  /** Field for Provider last name in CSV. */
  private static final String PROVIDER_LAST_NAME_FIELD = "Provider Last Name (Legal Name)";

  /** Field for Provider prefix in CSV. */
  private static final String PROVIDER_PREFIX_FIELD = "Provider Name Prefix Text";

  /** Field for Provider Suffix in CSV. */
  private static final String PROVIDER_SUFFIX_FIELD = "Provider Name Suffix Text";

  /** Base url for the nppes download. */
  private static final String BASE_URL = "https://download.cms.gov/nppes/NPPES_Data_Dissemination_";

  /** Map of months. */
  private static final Map<Integer, String> months =
      Map.ofEntries(
          entry(0, "January"),
          entry(1, "February"),
          entry(2, "March"),
          entry(3, "April"),
          entry(4, "May"),
          entry(5, "June"),
          entry(6, "July"),
          entry(7, "August"),
          entry(8, "September"),
          entry(9, "October"),
          entry(10, "November"),
          entry(11, "December"));

  /** The EntityManager to use for database operations. */
  private final EntityManager entityManager;

  /** The number of records to save before committing a transaction. */
  int batchSize;

  /** How often to run the job, in days. */
  int runInterval;

  /** Query for checking lastUpdated of the table. */
  private static final String GET_LAST_UPDATED_QUERY =
      "SELECT last_updated FROM ccw.npi_fda_meta WHERE table_name = 'npi_data'";

  private static final String SAVE_LAST_UPDATED_QUERY =
      """
          INSERT INTO ccw.npi_fda_meta (table_name, last_updated)
          VALUES('npi_data', :lastUpdated)
          ON CONFLICT (table_name)
          DO UPDATE SET
          last_updated = :lastUpdated
       """;

  /**
   * Constructor.
   *
   * @param entityManager the EntityManager to use for database operations;
   * @param batchSize The number of records saved before committing a transaction.
   * @param runInterval How often to run the job, in days.
   */
  public LoadNpiDataFiles(EntityManager entityManager, int batchSize, int runInterval) {
    this.entityManager = entityManager;
    this.batchSize = batchSize;
    this.runInterval = runInterval;
  }

  /**
   * Extracts a file name.
   *
   * @param getMonthBefore gets the month before
   * @return a file name string
   */
  static String getFileName(boolean getMonthBefore) {
    Calendar cal = Calendar.getInstance();
    int currentMonth = cal.get(Calendar.MONTH);
    int currentYear = cal.get(Calendar.YEAR);

    return getMonthAndYearForFile(getMonthBefore, currentMonth, currentYear);
  }

  /**
   * Formats the file name with Month and Year.
   *
   * @param getMonthBefore whether to get the previous month or not
   * @param currentMonth is the integer for month
   * @param currentYear is the integer for year
   * @return a file name string
   */
  private static String getMonthAndYearForFile(
      boolean getMonthBefore, int currentMonth, int currentYear) {
    String month;
    int year;
    if (getMonthBefore) {
      if (currentMonth == 0) {
        month = months.get(11);
        year = currentYear - 1;
      } else {
        month = months.get(currentMonth - 1);
        year = currentYear;
      }

    } else {
      month = months.get(currentMonth);
      year = currentYear;
    }
    return String.format("%s%s_%s.zip", BASE_URL, month, year);
  }

  /**
   * Streams the zip file and calls a method to create the NPI records.
   *
   * @param fileName the output file/resource to produce
   * @return Total number of records saved.
   * @throws IOException (any errors encountered will be bubbled up)
   * @throws IllegalStateException if there is an issue with NPI File
   */
  private Integer getOriginalNpiDataFile(String fileName)
      throws IOException, IllegalStateException {
    // download NPI file
    URL ndctextZipUrl = new URL(fileName);
    HttpURLConnection connection = (HttpURLConnection) ndctextZipUrl.openConnection();
    try (ZipInputStream zipInputStream = new ZipInputStream(connection.getInputStream())) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().startsWith("npidata_pfile_")
            && !entry.getName().toLowerCase().endsWith("_fileheader.csv")) {
          InputStreamReader ir = new InputStreamReader(zipInputStream);
          return saveNpiDataFile(ir);
        }
        zipInputStream.closeEntry();
      }
    }
    throw new IllegalStateException("No NPI file found");
  }

  /**
   * Builds NPI Resource.
   *
   * @return Total number of records saved.
   * @throws IOException (any errors encountered will be bubbled up)
   */
  private Integer persistNPIResource() throws IOException {
    String fileUrl;
    try {
      fileUrl = getFileName(false);
      return getOriginalNpiDataFile(fileUrl);
    } catch (IOException e) {
      fileUrl = getFileName(true);
      return getOriginalNpiDataFile(fileUrl);
    }
  }

  /**
   * Loads the taxonomy groupings into a map.
   *
   * @return map of taxonomy groupings.
   * @throws IOException exception thrown.
   */
  static Map<String, String> processTaxonomyDescriptions() throws IOException {
    ClassLoader classLoader = LoadNpiDataFiles.class.getClassLoader();
    Map<String, String> taxonomyMapping = new HashMap<>();
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(
                Objects.requireNonNull(
                    classLoader.getResourceAsStream("nucc_taxonomy_240.csv"))))) {
      CSVParser csvParser =
          new CSVParser(
              reader,
              CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
      for (CSVRecord csvRecord : csvParser) {
        String code = csvRecord.get("code");
        String displayName = csvRecord.get("Display Name");
        taxonomyMapping.put(code, displayName);
      }
    }
    return taxonomyMapping;
  }

  /**
   * Converts the npi data file.
   *
   * @param is the file input stream.
   * @return Total number of records saved.
   * @throws IOException exception thrown.
   */
  Integer saveNpiDataFile(InputStreamReader is) throws IOException {

    Map<String, String> taxonomyMap = processTaxonomyDescriptions();

    int savedCount = 0;
    try (BufferedReader reader = new BufferedReader(is)) {
      CSVParser csvParser =
          new CSVParser(
              reader,
              CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());

      LOGGER.info("Starting to save records to the database.");
      entityManager.getTransaction().begin();
      boolean interrupted = false;
      for (CSVRecord csvRecord : csvParser) {
        NPIData npiData = getNpiDataFromCsv(csvRecord, taxonomyMap);
        entityManager.merge(npiData);
        savedCount++;

        // The commit frequency is determined by an SSM parameter.
        if (savedCount % batchSize == 0) {
          entityManager.getTransaction().commit();
          entityManager.clear();
          LOGGER.info("Progress: Saved {}", savedCount);
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

  /** Updates the lastUpdated Date. */
  void setLastUpdated() {
    Date lastUpdated = new Date(System.currentTimeMillis());
    entityManager.getTransaction().begin();
    Query query = entityManager.createNativeQuery(SAVE_LAST_UPDATED_QUERY);
    query.setParameter("lastUpdated", lastUpdated);
    query.executeUpdate();
    entityManager.getTransaction().commit();
    entityManager.clear();
  }

  /**
   * Converts a CSV record to a NpiData entity.
   *
   * @param csvRecord The CSV record with the NPI data.
   * @param taxonomyMap a Map of taxonomy codes to display values.
   * @return the NPIData entity.
   */
  private static NPIData getNpiDataFromCsv(CSVRecord csvRecord, Map<String, String> taxonomyMap) {
    String orgName = csvRecord.get(PROVIDER_ORGANIZATION_NAME_FIELD);
    String npi = csvRecord.get(NPI_FIELD);
    String entityTypeCode = csvRecord.get(ENTITY_TYPE_CODE_FIELD);
    String taxonomyCode = csvRecord.get(TAXONOMY_CODE_FIELD);
    String providerFirstName = csvRecord.get(PROVIDER_FIRST_NAME_FIELD);
    String providerMiddleName = csvRecord.get(PROVIDER_MIDDLE_NAME_FIELD);
    String providerLastName = csvRecord.get(PROVIDER_LAST_NAME_FIELD);
    String providerPrefix = csvRecord.get(PROVIDER_PREFIX_FIELD);
    String providerSuffix = csvRecord.get(PROVIDER_SUFFIX_FIELD);
    String providerCredential = csvRecord.get(PROVIDER_CREDENTIAL_FIELD);
    NPIData npiData =
        NPIData.builder()
            .npi(npi)
            .providerOrganizationName(orgName)
            .entityTypeCode(entityTypeCode)
            .taxonomyCode(taxonomyCode)
            .taxonomyDisplay(taxonomyMap.get(taxonomyCode))
            .providerFirstName(providerFirstName)
            .providerMiddleName(providerMiddleName)
            .providerLastName(providerLastName)
            .providerNamePrefix(providerPrefix)
            .providerNameSuffix(providerSuffix)
            .providerCredential(providerCredential)
            .build();
    return npiData;
  }

  /**
   * Determines if the data should be loaded based on the lastUpdated date.
   *
   * @return true if the data should be loaded.
   */
  boolean shouldLoadData() {
    Date lastUpdated;
    Query query = entityManager.createNativeQuery(GET_LAST_UPDATED_QUERY);
    try {
      lastUpdated = (Date) query.getSingleResult();
    } catch (NoResultException e) {
      return true;
    }
    return LocalDate.now().isAfter(lastUpdated.toLocalDate().plusDays(runInterval));
  }

  @Override
  public Integer call() throws Exception {
    if (shouldLoadData()) {
      return persistNPIResource();
    } else {
      return -1;
    }
  }
}
