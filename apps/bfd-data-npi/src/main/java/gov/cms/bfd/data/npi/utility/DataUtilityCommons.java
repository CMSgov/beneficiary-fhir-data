package gov.cms.bfd.data.npi.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.data.npi.dto.NPIData;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Data Utility Commons class for npi. * */
public class DataUtilityCommons {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataUtilityCommons.class);

  /** The day of the month we should check to see if the file has posted. */
  private static final int DAYS_IN_EXPIRATION = 10;

  /** Field for taxonomy code in CSV. */
  private static final String TAXONOMY_CODE_FIELD = "Healthcare Provider Taxonomy Code_1";

  /** Field for provider organization name in CSV. */
  public static final String PROVIDER_ORGANIZATION_NAME_FIELD =
      "Provider Organization Name (Legal Business Name)";

  /** Field for NPI in CSV. */
  public static final String NPI_FIELD = "NPI";

  /** Field for Entity Type Code in CSV. */
  public static final String ENTITY_TYPE_CODE_FIELD = "Entity Type Code";

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

  /** Code for Provider entity type. */
  public static final String ENTITY_TYPE_CODE_PROVIDER = "1";

  /** Code for Organization entity type. */
  public static final String ENTITY_TYPE_CODE_ORGANIZATION = "2";

  /**
   * Gets the org names from the npi file.
   *
   * @param outputDir the output directory
   * @param downloadUrl the downloadUrl passed in
   * @param npiFile the npi file
   * @throws IOException if there is an issue reading file
   * @throws IllegalStateException if there is an issue with the output directory
   */
  @SuppressWarnings("java:S5443")
  public static void getNPIOrgNames(String outputDir, Optional<String> downloadUrl, String npiFile)
      throws IOException, IllegalStateException {
    Path outputPath = Paths.get(outputDir);
    if (!Files.isDirectory(outputPath)) {
      throw new IllegalStateException("OUTPUT_DIR does not exist for NPI download.");
    }

    // If the output file isn't already there, go build it.
    Path convertedNpiDataFile = outputPath.resolve(npiFile);
    try {
      buildNPIResource(convertedNpiDataFile, downloadUrl);
    } catch (IOException exception) {
      LOGGER.error("NPI data file could not be read.  Error:", exception);
      throw new IOException(exception);
    }
  }

  /**
   * Streams the zip file and calls a method to create the NPI csv file.
   *
   * @param convertedNpiDataFile Data file.
   * @param fileName the output file/resource to produce
   * @throws IOException (any errors encountered will be bubbled up)
   * @throws IllegalStateException if there is an issue with NPI File
   */
  public static void getOriginalNpiDataFile(Path convertedNpiDataFile, String fileName)
      throws IOException, IllegalStateException {
    // download NPI file
    URL ndctextZipUrl = new URL(fileName);
    HttpURLConnection connection = (HttpURLConnection) ndctextZipUrl.openConnection();
    boolean foundMatch = false;
    try (ZipInputStream zipInputStream = new ZipInputStream(connection.getInputStream())) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.getName().startsWith("npidata_pfile_")
            && !entry.getName().endsWith("_FileHeader.csv")
            && !entry.getName().endsWith("_fileheader.csv")) {
          InputStreamReader ir = new InputStreamReader(zipInputStream);
          convertNpiDataFile(convertedNpiDataFile, ir);
          foundMatch = true;
          break;
        }
        zipInputStream.closeEntry();
      }
    }
    if (!foundMatch) {
      throw new IllegalStateException("No NPI file found");
    }
  }

  /**
   * Creates the {@link #NPI_RESOURCE} file in the specified location.
   *
   * @param convertedNpiDataFile the output file/resource to produce
   * @param downloadUrl is the download url
   * @throws IOException (any errors encountered will be bubbled up)
   */
  private static void buildNPIResource(Path convertedNpiDataFile, Optional<String> downloadUrl)
      throws IOException {
    String fileUrl;
    // don't recreate the file if it already exists
    if (Files.isRegularFile(convertedNpiDataFile)) {
      LOGGER.info(
          "using existing data file {}, delete file if it is broken and re-run",
          convertedNpiDataFile);
      return;
    }

    if (downloadUrl.isPresent()) {
      try {
        getOriginalNpiDataFile(convertedNpiDataFile, downloadUrl.get());
      } catch (IOException e) {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_MONTH);
        if (day >= DAYS_IN_EXPIRATION) {
          throw new IOException(
              "NPI file is not available for the month and should have been made available by the NPI Files team.");
        } else {
          throw new IOException("NPI file is not for the month.");
        }
      }

    } else {
      try {
        fileUrl = FileNameCalculation.getFileName(false);
        getOriginalNpiDataFile(convertedNpiDataFile, fileUrl);
      } catch (IOException e) {
        fileUrl = FileNameCalculation.getFileName(true);
        getOriginalNpiDataFile(convertedNpiDataFile, fileUrl);
      }
    }
  }

  /**
   * Loads the taxonomy groupings into a map.
   *
   * @return map of taxonomy groupings.
   * @throws IOException exception thrown.
   */
  public static Map<String, String> processTaxonomyDescriptions() throws IOException {
    ClassLoader classLoader = DataUtilityCommons.class.getClassLoader();
    File taxonomyFile =
        new File(
            Objects.requireNonNull(classLoader.getResource("nucc_taxonomy_240.csv")).getFile());
    Map<String, String> taxonomyMapping = new HashMap<>();
    try (FileInputStream is = new FileInputStream(taxonomyFile);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
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
   * @param convertedNpiDataFile converted npi data file.
   * @param is the file input stream.
   * @throws IOException exception thrown.
   */
  private static void convertNpiDataFile(Path convertedNpiDataFile, InputStreamReader is)
      throws IOException {

    Map<String, String> taxonomyMap = DataUtilityCommons.processTaxonomyDescriptions();
    // convert file format from cp1252 to utf8
    CharsetDecoder inDec =
        Charset.forName("windows-1252")
            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    CharsetEncoder outEnc =
        StandardCharsets.UTF_8
            .newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT);
    try (BufferedReader reader = new BufferedReader(is);
        FileOutputStream fw =
            new FileOutputStream(convertedNpiDataFile.toFile().getAbsolutePath());
        DeflaterOutputStream dos = new DeflaterOutputStream(fw); ) {
      CSVParser csvParser =
          new CSVParser(
              reader,
              CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim());
      ObjectMapper objectMapper = new ObjectMapper();
      for (CSVRecord csvRecord : csvParser) {
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
        String json = objectMapper.writeValueAsString(npiData);
        dos.write(json.getBytes());
        dos.write("\n".getBytes());
      }
      dos.close();
    }
  }

  /**
   * sets the index number to their header value.
   *
   * @param fields the string array of header values.
   * @return map of indexes and field names
   */
  protected static Map<String, Integer> getIndexNumbers(String[] fields) {
    Map<String, Integer> indexNumbers = new HashMap<String, Integer>();
    Integer indexCounter = 0;
    for (String field : fields) {
      indexNumbers.put(field.trim().replace("\"", ""), indexCounter++);
    }

    return indexNumbers;
  }

  /**
   * gets the index number according to their field name.
   *
   * @param fieldName the header value to check.
   * @param indexNumbers the map of header values and indexes.
   * @return int of index
   */
  protected static int getIndexNumberForField(Map<String, Integer> indexNumbers, String fieldName) {
    if (indexNumbers.containsKey(fieldName)) {
      return indexNumbers.get(fieldName);
    }

    throw new IllegalStateException(
        "NPI Org File Processing Error: Cannot field fieldname " + fieldName);
  }
}
