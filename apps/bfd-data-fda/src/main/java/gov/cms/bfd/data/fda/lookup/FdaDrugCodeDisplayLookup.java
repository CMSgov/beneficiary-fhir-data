package gov.cms.bfd.data.fda.lookup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an FDA Drug Code to FDA Drug Code Display lookup. */
public class FdaDrugCodeDisplayLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(FdaDrugCodeDisplayLookup.class);

  /**
   * Stores a map from Drug Code (PRODUCTNDC) to Drug Code Display (SUBSTANCENAME) derived from the
   * downloaded NDC file.
   */
  private Map<String, String> ndcProductHashMap = new HashMap<>();

  /** Tracks the national drug codes that have already had code lookup failures. */
  private final Set<String> drugCodeLookupMissingFailures = new HashSet<>();

  /** Product NDC column name in the fda_products_utf8.tsv file. */
  private static final String PRODUCT_NDC_COLUMN = "PRODUCTNDC";

  /** Proprietary Name column name in the fda_products_utf8.tsv file. */
  private static final String PROPRIETARY_NAME_COLUMN = "PROPRIETARYNAME";

  /** Substance Name column name in the fda_products_utf8.tsv file. */
  private static final String SUBSTANCE_NAME_COLUMN = "SUBSTANCENAME";

  /**
   * Constructs an {@link FdaDrugCodeDisplayLookup}.
   *
   * @param npiDataStream include the file for testing
   */
  public FdaDrugCodeDisplayLookup(InputStream npiDataStream) throws IOException {
    readFDADrugCodeFile(npiDataStream);
  }

  /**
   * Retrieves the Drug Code Display (SUBSTANCENAME) for the given Drug Code (PRODUCTNDC) using the
   * drug code file downloaded during the build.
   *
   * @param claimDrugCode - NDC value in claim records
   * @return the fda drug code display string
   */
  public String retrieveFDADrugCodeDisplay(Optional<String> claimDrugCode) {
    /*
     * Handle bad data (e.g. our random test data) if drug code is empty or length is less than 9
     * characters
     */
    if (!claimDrugCode.isPresent() || claimDrugCode.get().length() < 9) {
      return null;
    }

    String claimDrugCodeReformatted = null;

    claimDrugCodeReformatted =
        claimDrugCode.get().substring(0, 5) + "-" + claimDrugCode.get().substring(5, 9);

    if (ndcProductHashMap.containsKey(claimDrugCodeReformatted)) {
      String ndcSubstanceName = ndcProductHashMap.get(claimDrugCodeReformatted);
      return ndcSubstanceName;
    }

    // log which NDC codes we couldn't find a match for in our downloaded NDC file
    if (!drugCodeLookupMissingFailures.contains(claimDrugCode.get())) {
      drugCodeLookupMissingFailures.add(claimDrugCode.get());
      LOGGER.debug(
          "No national drug code value (PRODUCTNDC column) match found for drug code {} in"
              + " resource {}.",
          claimDrugCode.get(),
          "fda_products_utf8.tsv");
    }

    return null;
  }

  /**
   * Constructs a {@link FdaDrugCodeDisplayLookup} for testing purposes only.
   *
   * @param ndcProdMap Drug code lookup map to populate FdaDrugCodeDisplayLookup
   */
  public FdaDrugCodeDisplayLookup(Map<String, String> ndcProdMap) {
    ndcProductHashMap.putAll(ndcProdMap);
  }

  /**
   * Gets the processed FDA data
   *
   * <p>See {@link gov.cms.bfd.data.fda.utility.App} for details.
   *
   * @param inputStream is the inputStream that is passed in
   * @return a map with drug codes and fields.
   */
  public Map<String, String> readFDADrugCodeFile(InputStream inputStream) {

    ndcProductHashMap = getFdaProcessedData(inputStream);
    return ndcProductHashMap;
  }

  /**
   * Reads all the <code>PRODUCTNDC</code> and <code>SUBSTANCENAME</code> fields from the FDA NDC
   * Products file which was downloaded during the build process.
   *
   * <p>See {@link gov.cms.bfd.data.fda.utility.App} for details.
   *
   * @param inputStream is the inputStream that is passed in
   * @return a map with drug codes and fields.
   */
  protected Map<String, String> getFdaProcessedData(InputStream inputStream) {

    Map<String, String> ndcProcessedData = new HashMap<String, String>();

    try (final InputStream ndcProductStream = inputStream;
        final BufferedReader ndcProductsIn =
            new BufferedReader(new InputStreamReader(ndcProductStream))) {
      /*
       * We want to extract the PRODUCTNDC and PROPRIETARYNAME/SUBSTANCENAME from the FDA Products
       * file (fda_products_utf8.tsv is in /target/classes directory) and put in a Map for easy
       * retrieval to get the display value which is a combination of PROPRIETARYNAME &
       * SUBSTANCENAME
       */
      CSVParser csvParser =
          new CSVParser(
              ndcProductsIn,
              CSVFormat.DEFAULT
                  .builder()
                  .setDelimiter('\t')
                  .setHeader()
                  .setIgnoreHeaderCase(true)
                  .setTrim(true)
                  .build());
      for (CSVRecord csvRecord : csvParser) {
        String productNdc = csvRecord.get(PRODUCT_NDC_COLUMN);
        String proprietaryName = csvRecord.get(PROPRIETARY_NAME_COLUMN);
        String substanceName = csvRecord.get(SUBSTANCE_NAME_COLUMN);
        String nationalDrugCodeManufacturer =
            StringUtils.leftPad(productNdc.substring(0, productNdc.indexOf("-")), 5, '0');
        String nationalDrugCodeIngredient =
            StringUtils.leftPad(
                productNdc.substring(productNdc.indexOf("-") + 1, productNdc.length()), 4, '0');
        ndcProcessedData.put(
            String.format("%s-%s", nationalDrugCodeManufacturer, nationalDrugCodeIngredient),
            String.format("%s - %s", proprietaryName, substanceName));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read NDC code data.", e);
    }
    return ndcProcessedData;
  }

  /**
   * Returns a inputStream from file name passed in.
   *
   * @param fileName is the file name passed in
   * @return InputStream of file.
   */
  protected InputStream getFileInputStream(String fileName) {
    return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
  }
}
