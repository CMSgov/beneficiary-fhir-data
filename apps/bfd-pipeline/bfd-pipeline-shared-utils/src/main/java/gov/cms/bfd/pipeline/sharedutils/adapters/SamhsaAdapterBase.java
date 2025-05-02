package gov.cms.bfd.pipeline.sharedutils.adapters;

import static gov.cms.bfd.pipeline.sharedutils.SamhsaUtil.isDateOutsideOfRange;

import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaEntry;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import gov.cms.bfd.pipeline.sharedutils.model.TagDetails;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.logging.log4j.util.Strings;

/**
 * Base class for the SAMHSA Adapters.
 *
 * @param <TClaim> Claim type
 * @param <TClaimLine> ClaimLine type.
 */
public abstract class SamhsaAdapterBase<TClaim, TClaimLine> {
  /** The Claim to process. */
  TClaim claim;

  /** The claimLines to process. */
  List<TClaimLine> claimLines;

  /** The claim's table. */
  String table;

  /** The claimLines' table. */
  String linesTable;

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_1 = "icd_dgns_cd1";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_2 = "icd_dgns_cd2";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_3 = "icd_dgns_cd3";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_4 = "icd_dgns_cd4";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_5 = "icd_dgns_cd5";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_6 = "icd_dgns_cd6";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_7 = "icd_dgns_cd7";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_8 = "icd_dgns_cd8";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_9 = "icd_dgns_cd9";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_10 = "icd_dgns_cd10";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_11 = "icd_dgns_cd11";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_12 = "icd_dgns_cd12";

  /** Claim SAMHSA Column. */
  public static final String LINE_ICD_DGNS_CD = "line_icd_dgns_cd";

  /** Claim SAMHSA Column. */
  public static final String PRNCPAL_DGNS_CD = "prncpal_dgns_cd";

  /** Claim SAMHSA Column. */
  public static final String HCPCS_CD = "hcpcs_cd";

  /** Claim SAMHSA Column. */
  public static final String REV_CNTR_APC_HIPPS_CD = "rev_cntr_apc_hipps_cd";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_13 = "icd_dgns_cd13";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_14 = "icd_dgns_cd14";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_15 = "icd_dgns_cd15";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_16 = "icd_dgns_cd16";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_17 = "icd_dgns_cd17";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_18 = "icd_dgns_cd18";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_19 = "icd_dgns_cd19";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_20 = "icd_dgns_cd20";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_21 = "icd_dgns_cd21";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_22 = "icd_dgns_cd22";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_23 = "icd_dgns_cd23";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_24 = "icd_dgns_cd24";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_CD_25 = "icd_dgns_cd25";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_1 = "icd_dgns_e_cd1";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_2 = "icd_dgns_e_cd2";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_3 = "icd_dgns_e_cd3";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_4 = "icd_dgns_e_cd4";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_5 = "icd_dgns_e_cd5";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_6 = "icd_dgns_e_cd6";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_7 = "icd_dgns_e_cd7";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_8 = "icd_dgns_e_cd8";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_9 = "icd_dgns_e_cd9";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_10 = "icd_dgns_e_cd10";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_11 = "icd_dgns_e_cd11";

  /** Claim SAMHSA Column. */
  public static final String ICD_DGNS_E_CD_12 = "icd_dgns_e_cd12";

  /** Claim SAMHSA Column. */
  public static final String FST_DGNS_E_CD = "fst_dgns_e_cd";

  /** Claim SAMHSA Column. */
  public static final String CLM_DRG_CD = "clm_drg_cd";

  /** Claim SAMHSA Column. */
  public static final String ADMTG_DGNS_CD = "admtg_dgns_cd";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_1 = "icd_prcdr_cd1";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_2 = "icd_prcdr_cd2";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_3 = "icd_prcdr_cd3";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_4 = "icd_prcdr_cd4";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_5 = "icd_prcdr_cd5";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_6 = "icd_prcdr_cd6";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_7 = "icd_prcdr_cd7";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_8 = "icd_prcdr_cd8";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_9 = "icd_prcdr_cd9";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_10 = "icd_prcdr_cd10";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_11 = "icd_prcdr_cd11";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_12 = "icd_prcdr_cd12";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_13 = "icd_prcdr_cd13";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_14 = "icd_prcdr_cd14";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_15 = "icd_prcdr_cd15";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_16 = "icd_prcdr_cd16";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_17 = "icd_prcdr_cd17";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_18 = "icd_prcdr_cd18";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_19 = "icd_prcdr_cd19";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_20 = "icd_prcdr_cd20";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_21 = "icd_prcdr_cd21";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_22 = "icd_prcdr_cd22";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_23 = "icd_prcdr_cd23";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_24 = "icd_prcdr_cd24";

  /** Claim SAMHSA Column. */
  public static final String ICD_PRCDR_CD_25 = "icd_prcdr_cd25";

  /** Claim SAMHSA Column. */
  public static final String RSN_VISIT_CD_1 = "rsn_visit_cd1";

  /** Claim SAMHSA Column. */
  public static final String RSN_VISIT_CD_2 = "rsn_visit_cd2";

  /** Claim SAMHSA Column. */
  public static final String RSN_VISIT_CD_3 = "rsn_visit_cd3";

  /**
   * Retrieves a list of SAMHSA fields.
   *
   * @return {@link SamhsaFields}
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  public abstract List<SamhsaFields> getFields()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

  /** The list of samhsa fields for this claims. */
  List<SamhsaFields> samhsaFields = new ArrayList<>();

  /**
   * Returns the SAMHSA methods for a given claim line.
   *
   * @param claimLine The claim line to use.
   * @return a map of samhsa methods along with the columns.
   */
  abstract Map<Supplier<Optional<String>>, String> getClaimLineMethods(TClaimLine claimLine);

  /**
   * Returns the SAMHSA methods for the current claim.
   *
   * @return a map of samhsa methods along with the columns.
   */
  abstract Map<Supplier<Optional<String>>, String> getClaimMethods();

  /**
   * Returns the line number for a claim line.
   *
   * @param line the claim line to sue.
   * @return the line number.
   */
  abstract Short getLineNum(TClaimLine line);

  /**
   * Processes a claim.
   *
   * @param entityManager The entity manager
   * @return true if a SamhsaTag was merged.
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  public abstract boolean checkAndProcessClaim(EntityManager entityManager)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException;

  /** The class for the claim. */
  Class claimClass;

  /** The class for the claimLines. */
  Class claimLineClass;

  /**
   * Constructor.
   *
   * @param claim The claim.
   * @param claimLines The claimLines.
   */
  public SamhsaAdapterBase(TClaim claim, List<TClaimLine> claimLines) {
    this.claimLines = claimLines;
    this.claim = claim;
    this.claimClass = claim.getClass();
    if (claimLines != null && !claimLines.isEmpty()) {
      claimLineClass = claimLines.getFirst().getClass();
    }
  }

  /** Gets the SAMHSA codes for a claim and its claim lines. */
  void getCodes() {
    iterateMethods(getClaimMethods(), table, null);
    for (TClaimLine line : claimLines) {
      iterateMethods(getClaimLineMethods(line), linesTable, getLineNum(line));
    }
  }

  /**
   * Iterates over a set of methods and sets a SamhsaFields objects with all of the SAMHSA codes.
   *
   * @param methods the methods to loop over.
   * @param table the table belonging to this set of methods.
   * @param lineNum the line number for the claim line. Will be null if this is not a claim line.
   */
  void iterateMethods(
      Map<Supplier<Optional<String>>, String> methods, String table, Short lineNum) {
    for (Map.Entry<Supplier<Optional<String>>, String> method : methods.entrySet()) {
      Optional<String> code = method.getKey().get();
      if (code.isPresent()) {
        SamhsaFields field =
            SamhsaFields.builder()
                .column(method.getValue())
                .code(code.get())
                .table(table)
                .lineNum(lineNum)
                .build();
        samhsaFields.add(field);
      }
    }
  }

  /**
   * Builds the list of TagDetails.
   *
   * @return list of tag details
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   */
  Optional<List<TagDetails>> buildDetails()
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
    Optional<SamhsaEntry> entry;
    List<TagDetails> detailsList = new ArrayList<>();
    LocalDate serviceDate = getFromDate();
    LocalDate throughDate = getThroughDate();

    List<SamhsaFields> fields = getFields();
    for (SamhsaFields field : fields) {
      entry =
          SamhsaUtil.getSamhsaCode(
              Optional.ofNullable(field.getCode()), Optional.ofNullable(field.getColumn()));
      if (entry.isPresent()) {
        try {
          LocalDate startDate = LocalDate.parse(entry.get().getStartDate());
          LocalDate endDate =
              entry.get().getEndDate().equalsIgnoreCase("Active")
                  ? LocalDate.MAX
                  : LocalDate.parse(entry.get().getEndDate());

          // if the throughDate is not between the start and end date,
          // and the serviceDate is not between the start and end date,
          // then the claim falls outside the date range of the SAMHSA code.
          if (isDateOutsideOfRange(startDate, endDate, throughDate)
              && isDateOutsideOfRange(startDate, endDate, serviceDate)) {
            continue;
          }
        } catch (DateTimeParseException ignore) {
          // Parsing the date from the SamhsaEntry failed, so the tag should be created by default.
        }
        // Use the last part of the system path as the type
        String type =
            Arrays.stream(entry.get().getSystem().split("/"))
                .reduce((first, second) -> second)
                .orElse(Strings.EMPTY);
        TagDetails detail =
            TagDetails.builder()
                .table(field.getTable())
                .column(field.getColumn())
                .clmLineNum(field.getLineNum() != null ? (int) field.getLineNum() : null)
                .type(type)
                .build();
        detailsList.add(detail);
      }
    }
    return detailsList.isEmpty() ? Optional.empty() : Optional.of(detailsList);
  }

  /**
   * Gets the from date of a claim.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   * @return a LocalDate object of the claim's from date.
   */
  public LocalDate getFromDate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    LocalDate date = (LocalDate) claimClass.getMethod("getDateFrom").invoke(claim);
    return date == null ? LocalDate.MAX : date;
  }

  /**
   * Gets the to date of a claim.
   *
   * @throws InvocationTargetException Thrown when the one of the claim's methods cannot be invoked.
   * @throws NoSuchMethodException Thrown when one of the claim's method does not exist.
   * @throws IllegalAccessException Thrown on illegal access invoking the claim's method.
   * @return A LocalDate object of the claim's through date.
   */
  public LocalDate getThroughDate()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    LocalDate date = (LocalDate) claimClass.getMethod("getDateThrough").invoke(claim);
    return date == null ? LocalDate.MAX : date;
  }
}
