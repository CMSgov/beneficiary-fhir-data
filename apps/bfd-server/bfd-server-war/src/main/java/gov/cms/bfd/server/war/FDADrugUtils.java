package gov.cms.bfd.server.war;

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

public class FDADrugUtils {

  private static final Logger LOGGER = LoggerFactory.getLogger(FDADrugUtils.class);
  /** Stores the PRODUCTNDC and SUBSTANCENAME from the downloaded NDC file. */
  private static Map<String, String> ndcProductMap = null;

  /** Tracks the national drug codes that have already had code lookup failures. */
  public static final Set<String> drugCodeLookupMissingFailures = new HashSet<>();

  private final boolean includeFakeDrugCode;

  public FDADrugUtils(boolean includeFakeDrugCode) {
    this.includeFakeDrugCode = includeFakeDrugCode;
  }

  /**
   * Retrieves the PRODUCTNDC and SUBSTANCENAME from the FDA NDC Products file which was downloaded
   * during the build process
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

    /*
     * There's a race condition here: we may initialize this static field more than once if multiple
     * requests come in at the same time. However, the assignment is atomic, so the race and
     * reinitialization is harmless other than maybe wasting a bit of time.
     */
    // read the entire NDC file the first time and put in a Map
    if (ndcProductMap == null) {
      ndcProductMap = readFDADrugCodeFile();
    }

    String claimDrugCodeReformatted = null;

    claimDrugCodeReformatted =
        claimDrugCode.get().substring(0, 5) + "-" + claimDrugCode.get().substring(5, 9);

    if (ndcProductMap.containsKey(claimDrugCodeReformatted)) {
      String ndcSubstanceName = ndcProductMap.get(claimDrugCodeReformatted);
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
   */
  public Map<String, String> readFDADrugCodeFile() {
    Map<String, String> ndcProductHashMap = new HashMap<String, String>();
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
          appendFDATestCode(ndcProductHashMap);
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Unable to read NDC code data.", e);
    }
    return ndcProductHashMap;
  }

  private void appendFDATestCode(Map<String, String> ndcProductHashMap) {
    ndcProductHashMap.put("00000-0000", "Fake Diluent - WATER");
  }
}
