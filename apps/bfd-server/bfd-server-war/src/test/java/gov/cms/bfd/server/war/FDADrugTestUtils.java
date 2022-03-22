package gov.cms.bfd.server.war;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FDADrugTestUtils implements IDrugCodeProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(FDADrugUtils.class);
  /** Stores the PRODUCTNDC and SUBSTANCENAME from the downloaded NDC file. */
  private static Map<String, String> ndcProductMap = null;

  /** Tracks the national drug codes that have already had code lookup failures. */
  public static final Set<String> drugCodeLookupMissingFailures = new HashSet<>();

  /**
   * Retrieves the PRODUCTNDC and SUBSTANCENAME from the FDA NDC Products file which was downloaded
   * during the build process
   *
   * @param claimDrugCode - NDC value in claim records
   * @return the fda drug code display string
   */
  public String retrieveFDADrugCodeDisplay(String claimDrugCode) {

    /*
     * Handle bad data (e.g. our random test data) if drug code is empty or length is less than 9
     * characters
     */
    if (claimDrugCode.isEmpty() || claimDrugCode.length() < 9) {
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

    claimDrugCodeReformatted = claimDrugCode.substring(0, 5) + "-" + claimDrugCode.substring(5, 9);

    if (ndcProductMap.containsKey(claimDrugCodeReformatted)) {
      String ndcSubstanceName = ndcProductMap.get(claimDrugCodeReformatted);
      return ndcSubstanceName;
    }

    // log which NDC codes we couldn't find a match for in our downloaded NDC file
    if (!drugCodeLookupMissingFailures.contains(claimDrugCode)) {
      drugCodeLookupMissingFailures.add(claimDrugCode);
      LOGGER.info(
          "No national drug code value (PRODUCTNDC column) match found for drug code {} in"
              + " resource {}.",
          claimDrugCode,
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
    /*
     * We want to extract the PRODUCTNDC and PROPRIETARYNAME/SUBSTANCENAME from the FDA Products
     * file (fda_products_utf8.tsv is in /target/classes directory) and put in a Map for easy
     * retrieval to get the display value which is a combination of PROPRIETARYNAME &
     * SUBSTANCENAME
     */
    String line =
        "0000-0000_000000zz-0zz0-0z00-zzz0-0z00zzz00000\t0000-0000\tFAKE DRUG\tFake Diluent\t\tfake\tFAKE SOLUTION\tFake\t0\t\tFAK\tFAK000000\tFake Company\tWATER\t1\tmL/mL\t\t\tN\t00000000";
    String ndcProductColumns[] = line.split("\t");
    String nationalDrugCodeManufacturer =
        StringUtils.leftPad(
            ndcProductColumns[1].substring(0, ndcProductColumns[1].indexOf("-")), 5, '0');
    String nationalDrugCodeIngredient =
        StringUtils.leftPad(
            ndcProductColumns[1].substring(
                ndcProductColumns[1].indexOf("-") + 1, ndcProductColumns[1].length()),
            4,
            '0');
    ndcProductHashMap.put(
        String.format("%s-%s", nationalDrugCodeManufacturer, nationalDrugCodeIngredient),
        ndcProductColumns[3] + " - " + ndcProductColumns[13]);

    return ndcProductHashMap;
  }
}
