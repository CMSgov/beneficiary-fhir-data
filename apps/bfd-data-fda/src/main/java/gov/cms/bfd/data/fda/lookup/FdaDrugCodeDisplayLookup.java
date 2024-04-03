package gov.cms.bfd.data.fda.lookup;

import gov.cms.bfd.data.fda.utility.App;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides an FDA Drug Code to FDA Drug Code Display lookup. */
public class FdaDrugCodeDisplayLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(FdaDrugCodeDisplayLookup.class);

  /** A fake drug code used for testing. */
  public static final String FAKE_DRUG_CODE = "00000-0000";

  /** A fake drug code display that is associated with the FAKE_DRUG_CODE. */
  public static final String FAKE_DRUG_CODE_DISPLAY = "Fake Diluent - WATER";

  /**
   * Stores a map from Drug Code (PRODUCTNDC) to Drug Code Display (SUBSTANCENAME) derived from the
   * downloaded NDC file.
   */
  private Map<String, String> ndcProductHashMap = new HashMap<>();

  /** Tracks the national drug codes that have already had code lookup failures. */
  private final Set<String> drugCodeLookupMissingFailures = new HashSet<>();

  /** Keeps track of the PRODUCTNDC column index in the fda_products_utf8.tsv file. */
  private static int PRODUCT_NDC_COLUMN_INDEX = 1;

  /** Keeps track of the PROPRIETARYNAME column index in the fda_products_utf8.tsv file. */
  private static int PROPRIETARY_NAME_COLUMN_INDEX = 3;

  /** Keeps track of the SUBSTANCENAME column index in the fda_products_utf8.tsv file. */
  private static int SUBSTANCE_NAME_COLUMN_INDEX = 13;

  /**
   * Cached copy of the testing version of the {@link FdaDrugCodeDisplayLookup} so that we don't
   * have to construct it over and over in the unit tests.
   */
  private static FdaDrugCodeDisplayLookup drugCodeLookupForTesting;

  /**
   * Cached copy of the production version of the {@link FdaDrugCodeDisplayLookup} so that we don't
   * have to construct it over and over in the unit tests.
   */
  private static FdaDrugCodeDisplayLookup drugCodeLookupForProduction;

  /**
   * Factory method for creating a {@link FdaDrugCodeDisplayLookup} for testing that includes the
   * fake drug code.
   *
   * @return the {@link FdaDrugCodeDisplayLookup}
   */
  public static FdaDrugCodeDisplayLookup createDrugCodeLookupForTesting() {
    if (drugCodeLookupForTesting == null) {
      drugCodeLookupForTesting = new FdaDrugCodeDisplayLookup(true);
    }

    return drugCodeLookupForTesting;
  }

  /**
   * Factory method for creating a {@link FdaDrugCodeDisplayLookup} for production that does not
   * include the fake drug code.
   *
   * @return the {@link FdaDrugCodeDisplayLookup}
   */
  public static FdaDrugCodeDisplayLookup createDrugCodeLookupForProduction() {
    if (drugCodeLookupForProduction == null) {
      drugCodeLookupForProduction = new FdaDrugCodeDisplayLookup(false);
    }

    return drugCodeLookupForProduction;
  }

  /**
   * Constructs a {@link FdaDrugCodeDisplayLookup} for testing purposes only.
   *
   * @param npiOrgMap Drug code lookup map to populate FdaDrugCodeDisplayLookup
   */
  public FdaDrugCodeDisplayLookup(Map<String, String> npiOrgMap) {
    ndcProductHashMap.putAll(npiOrgMap);
  }

  /**
   * Constructs an {@link FdaDrugCodeDisplayLookup}.
   *
   * @param includeFakeDrugCode whether to include the fake testing drug code or not
   */
  private FdaDrugCodeDisplayLookup(boolean includeFakeDrugCode) {
    readFDADrugCodeFile(includeFakeDrugCode, getFileInputStream(App.FDA_PRODUCTS_RESOURCE));
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
   * Gets the processed FDA data and checks whether the fake drug code needs to be inserted into the
   * map.
   *
   * <p>See {@link gov.cms.bfd.data.fda.utility.App} for details.
   *
   * @param includeFakeDrugCode is whether to incliude fake drug code
   * @param inputStream is the inputStream that is passed in
   * @return a map with drug codes and fields.
   */
  protected Map<String, String> readFDADrugCodeFile(
      boolean includeFakeDrugCode, InputStream inputStream) {

    ndcProductHashMap = getFdaProcessedData(includeFakeDrugCode, inputStream);

    if (includeFakeDrugCode) {
      ndcProductHashMap.put(FAKE_DRUG_CODE, FAKE_DRUG_CODE_DISPLAY);
    }
    return ndcProductHashMap;
  }

  /**
   * Reads all the <code>PRODUCTNDC</code> and <code>SUBSTANCENAME</code> fields from the FDA NDC
   * Products file which was downloaded during the build process.
   *
   * <p>See {@link gov.cms.bfd.data.fda.utility.App} for details.
   *
   * @param includeFakeDrugCode is whether to incliude fake drug code
   * @param inputStream is the inputStream that is passed in
   * @return a map with drug codes and fields.
   */
  protected Map<String, String> getFdaProcessedData(
      boolean includeFakeDrugCode, InputStream inputStream) {

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
      String line = "";
      ndcProductsIn.readLine();
      while ((line = ndcProductsIn.readLine()) != null) {
        String ndcProductColumns[] = line.split("\t");
        String nationalDrugCodeManufacturer =
            StringUtils.leftPad(
                ndcProductColumns[PRODUCT_NDC_COLUMN_INDEX].substring(
                    0, ndcProductColumns[PRODUCT_NDC_COLUMN_INDEX].indexOf("-")),
                5,
                '0');
        String nationalDrugCodeIngredient =
            StringUtils.leftPad(
                ndcProductColumns[PRODUCT_NDC_COLUMN_INDEX].substring(
                    ndcProductColumns[PRODUCT_NDC_COLUMN_INDEX].indexOf("-") + 1,
                    ndcProductColumns[PRODUCT_NDC_COLUMN_INDEX].length()),
                4,
                '0');
        ndcProcessedData.put(
            String.format("%s-%s", nationalDrugCodeManufacturer, nationalDrugCodeIngredient),
            ndcProductColumns[PROPRIETARY_NAME_COLUMN_INDEX].replace("\"", "")
                + " - "
                + ndcProductColumns[SUBSTANCE_NAME_COLUMN_INDEX].replace("\"", ""));
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
