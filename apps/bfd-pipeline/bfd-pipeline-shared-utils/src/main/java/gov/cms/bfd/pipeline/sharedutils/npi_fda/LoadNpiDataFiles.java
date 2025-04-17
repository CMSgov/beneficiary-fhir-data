package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import static java.util.Map.entry;

import gov.cms.bfd.model.rif.npi_fda.NPIData;
import jakarta.persistence.EntityManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class to download NPI data and persist it into the database. */
public class LoadNpiDataFiles extends LoadDataFiles<NPIData> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadNpiDataFiles.class);
  private static final String TABLE_NAME = "npi_data";

  private final Map<String, String> taxonomyMap;

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

  /** CSV delimiter. */
  private static final String DELIMITER = ",";

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

  /**
   * Constructor.
   *
   * @param entityManager the EntityManager to use for database operations;
   * @param batchSize The number of records saved before committing a transaction.
   * @param runInterval How often to run the job, in days.
   */
  public LoadNpiDataFiles(EntityManager entityManager, int batchSize, int runInterval)
      throws IOException {
    super(TABLE_NAME, entityManager, batchSize, runInterval, LOGGER, DELIMITER);
    taxonomyMap = processTaxonomyDescriptions();
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

  /** {@inheritDoc} */
  @Override
  boolean resolveFileName(String name) {
    return (name.startsWith("npidata_pfile_") && !name.toLowerCase().endsWith("_fileheader.csv"));
  }

  /** {@inheritDoc} */
  @Override
  Integer persistResource() throws IOException {
    String fileUrl;
    try {
      fileUrl = getFileName(false);
      return downloadDataFile(fileUrl);
    } catch (IOException e) {
      fileUrl = getFileName(true);
      return downloadDataFile(fileUrl);
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
          CSVParser.builder()
              .setReader(reader)
              .setFormat(
                  CSVFormat.DEFAULT
                      .builder()
                      .setDelimiter(",")
                      .setHeader()
                      .setIgnoreHeaderCase(true)
                      .setTrim(true)
                      .get())
              .get();

      for (CSVRecord csvRecord : csvParser) {
        String code = csvRecord.get("code");
        String displayName = csvRecord.get("Display Name");
        taxonomyMapping.put(code, displayName);
      }
    }
    return taxonomyMapping;
  }

  /** {@inheritDoc} */
  @Override
  NPIData getDataFromCsv(CSVRecord csvRecord) {
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
    return NPIData.builder()
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
  }
}
