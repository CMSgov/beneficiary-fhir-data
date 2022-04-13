package gov.cms.bfd.server.war.commons;

import gov.cms.bfd.server.war.FDADrugDataUtilityApp;
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

/** Provides an FDA Drug Code to FDA Drug Code Display lookup */
public class FdaDrugCodeDisplayLookup {
  private static final Logger LOGGER = LoggerFactory.getLogger(FdaDrugCodeDisplayLookup.class);

  /**
   * Stores a map from Drug Code (PRODUCTNDC) to Drug Code Display (SUBSTANCENAME) derived from the
   * downloaded NDC file.
   */
  private final Map<String, String> ndcProductHashMap = new HashMap<>();

  /** Tracks the national drug codes that have already had code lookup failures. */
  private final Set<String> drugCodeLookupMissingFailures = new HashSet<>();

  /**
   * Cached copy of the testing version of the {@link FdaDrugCodeDisplayLookup} so that we don't have to construct it
   * over and over in the unit tests
   */
  private static FdaDrugCodeDisplayLookup drugCodeLookupForTesting;

  /**
   * Cached copy of the production version of the {@link FdaDrugCodeDisplayLookup} so that we don't have to construct it
   * over and over in the unit tests
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
   * Constructs an {@link FdaDrugCodeDisplayLookup}
   *
   * @param includeFakeDrugCode whether to include the fake testing drug code or not
   */
  private FdaDrugCodeDisplayLookup(boolean includeFakeDrugCode) {
    readFDADrugCodeFile(includeFakeDrugCode);
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
    if (!claimDrugCode.isPresent() || claimDrugCode.isEmpty() || claimDrugCode.get().length() < 9) {
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
      LOGGER.info(
          "No national drug code value (PRODUCTNDC column) match found for drug code {} in"
              + " resource {}.",
          claimDrugCode.get(),
          "fda_products_utf8.tsv");
    }

    return null;
  }

  /**
   * Reads all the <code>PRODUCTNDC</code> and <code>SUBSTANCENAME</code> fields from the FDA NDC
   * Products file which was downloaded during the build process.
   *
   * <p>See {@link gov.cms.bfd.server.war.FDADrugDataUtilityApp} for details.
   *
   * @return a map with drug codes and fields
   * @param includeFakeDrugCode
   */
  private Map<String, String> readFDADrugCodeFile(boolean includeFakeDrugCode) {
    try (final InputStream ndcProductStream =
            Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(FDADrugDataUtilityApp.FDA_PRODUCTS_RESOURCE);
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
        try {
          String nationalDrugCodeManufacturer =
              StringUtils.leftPad(
                  ndcProductColumns[1].substring(0, ndcProductColumns[1].indexOf("-")), 5, '0');
          String nationalDrugCodeIngredient =
              StringUtils.leftPad(
                  ndcProductColumns[1].substring(
                      ndcProductColumns[1].indexOf("-") + 1, ndcProductColumns[1].length()),
                  4,
                  '0');
          // ndcProductColumns[3] - Proprietary Name
          // ndcProductColumns[13] - Substance Name
          ndcProductHashMap.put(
              String.format("%s-%s", nationalDrugCodeManufacturer, nationalDrugCodeIngredient),
              ndcProductColumns[3] + " - " + ndcProductColumns[13]);
        } catch (StringIndexOutOfBoundsException e) {
          continue;
        }

        if (includeFakeDrugCode) {
          ndcProductHashMap.put("00000-0000", "Fake Diluent - WATER");
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read NDC code data.", e);
    }
    return ndcProductHashMap;
  }
}
