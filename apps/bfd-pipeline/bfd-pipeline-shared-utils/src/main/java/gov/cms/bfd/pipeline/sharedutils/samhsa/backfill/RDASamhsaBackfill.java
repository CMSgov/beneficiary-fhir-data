package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import static gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.QueryConstants.*;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RDA implementation of AbstractSamhsaBackfill. */
public class RDASamhsaBackfill extends AbstractSamhsaBackfill {
  /** The Logger. */
  static final Logger LOGGER = LoggerFactory.getLogger(RDASamhsaBackfill.class);

  /** The column name for the fiss claim id. */
  private static final String FISS_CLAIM_ID_FIELD = "claim_id";

  private static final String RDA_POSITION = "rda_position";
  private static final String IDR_DTL_NUM = "idr_dtl_number";

  /** The column name for the mcs claim id. */
  private static final String MCS_CLAIM_ID_FIELD = "idr_clm_hd_icn";

  /** Columns for mcs_diagnosis. */
  private static String[] MCS_DIAGNOSIS_SAMHSA_COLUMNS =
      new String[] {RDA_POSITION, "idr_diag_code"};

  /** Columns for mcs_details. */
  private static String[] MCS_DETAILS_SAMHSA_COLUMNS =
      new String[] {IDR_DTL_NUM, "idr_dtl_primary_diag_code", "idr_proc_code"};

  /** Columns for fiss. */
  private static String[] FISS_SAMHSA_COLUMNS =
      new String[] {
        "stmt_cov_from_date", "stmt_cov_to_date", "admit_diag_code", "drg_cd", "principle_diag"
      };

  private static final String FISS_FROM_DATE = "stmt_cov_from_date";
  private static final String FISS_TO_DATE = "stmt_cov_to_date";

  /** Columns for fiss revenue lines. */
  private String[] FISS_REVENUE_LINES_SAMHSA_COLUMNS =
      new String[] {RDA_POSITION, "apc_hcpcs_apc", "hcpc_cd"};

  /** Columns for fiss_diagnosis_codes. */
  private String[] FISS_DIAGNOSIS_SAMHSA_COLUMNS = new String[] {RDA_POSITION, "diag_cd2"};

  /** Columns for fiss_proc_codes. */
  private String[] FISS_PROC_SAMHSA_COLUMNS = new String[] {RDA_POSITION, "proc_code"};

  @Override
  COLUMN_TYPE getEntryType(String entry) {
    switch (entry) {
      case RDA_POSITION:
      case IDR_DTL_NUM:
        return COLUMN_TYPE.LINE_NUM;
      case FISS_FROM_DATE:
        return COLUMN_TYPE.DATE_FROM;
      case FISS_TO_DATE:
        return COLUMN_TYPE.DATE_TO;
      case FISS_CLAIM_ID_FIELD:
      case MCS_CLAIM_ID_FIELD:
        return COLUMN_TYPE.CLAIM_ID;
      default:
        return COLUMN_TYPE.SAMHSA_CODE;
    }
  }

  /** The list of table entries for RDA claims. */
  public enum RDA_TABLES {
    /** Fiss Claims. */
    FISS_CLAIMS(
        new TableEntry(
            GET_CLAIM_DATES_FISS,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_claims",
            "rda.fiss_claims", // Should not be used, since is already a parent table. Included out
            // of an abundance of caution.
            false)),
    /** Fiss proc codes. */
    FISS_PROC_CODES(
        new TableEntry(
            GET_CLAIM_DATES_FISS,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_proc_codes",
            "rda.fiss_claims",
            true)),
    /** Fiss diagnosis codes. */
    FISS_DIAGNOSIS_CODES(
        new TableEntry(
            GET_CLAIM_DATES_FISS,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_diagnosis_codes",
            "rda.fiss_claims",
            true)),
    /** Fiss revenue lines. */
    FISS_REVENUE_LINES(
        new TableEntry(
            GET_CLAIM_DATES_FISS,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_revenue_lines",
            "rda.fiss_claims",
            true)),

    /** MCS diagnosis codes. */
    MCS_DIAGNOSIS_CODES(
        new TableEntry(
            GET_CLAIM_DATES_MCS,
            "rda.mcs_tags",
            MCS_CLAIM_ID_FIELD,
            "rda.mcs_diagnosis_codes",
            "rda.mcs_claims",
            true)),
    /** MCS details. */
    MCS_DETAILS(
        new TableEntry(
            GET_CLAIM_DATES_MCS,
            "rda.mcs_tags",
            MCS_CLAIM_ID_FIELD,
            "rda.mcs_details",
            "rda.mcs_claims",
            true));

    /** The tableEntry. */
    TableEntry entry;

    /**
     * Constructor.
     *
     * @param entry the tableEntry.
     */
    RDA_TABLES(TableEntry entry) {
      this.entry = entry;
    }

    /**
     * Returns the tableEntry.
     *
     * @return the tableEntry.
     */
    public TableEntry getEntry() {
      return entry;
    }
  }

  /**
   * Constructor.
   *
   * @param transactionManager The transaction manager.
   * @param batchSize the query batch size.
   * @param logInterval The log reporting interval, in seconds.
   * @param tableEntry The table to process in this thread.
   */
  public RDASamhsaBackfill(
      TransactionManager transactionManager,
      int batchSize,
      Long logInterval,
      RDA_TABLES tableEntry) {
    super(transactionManager, batchSize, LOGGER, logInterval, tableEntry.getEntry());
    query = getQueryByTableEntry(tableEntry);
  }

  /** {@inheritDoc} */
  @Override
  protected String convertClaimId(String claimId) {
    // Already a string, do nothing.
    return claimId;
  }

  private String getQueryByTableEntry(RDA_TABLES tableEntry) {
    switch (tableEntry) {
      case MCS_DETAILS:
        return buildQueryStringTemplate(
            "rda.mcs_details", MCS_CLAIM_ID_FIELD, MCS_DETAILS_SAMHSA_COLUMNS);
      case MCS_DIAGNOSIS_CODES:
        return buildQueryStringTemplate(
            "rda.mcs_diagnosis_codes", MCS_CLAIM_ID_FIELD, MCS_DIAGNOSIS_SAMHSA_COLUMNS);
      case FISS_CLAIMS:
        return buildQueryStringTemplate(
            "rda.fiss_claims", FISS_CLAIM_ID_FIELD, FISS_SAMHSA_COLUMNS);
      case FISS_DIAGNOSIS_CODES:
        return buildQueryStringTemplate(
            "rda.fiss_diagnosis_codes", FISS_CLAIM_ID_FIELD, FISS_DIAGNOSIS_SAMHSA_COLUMNS);
      case FISS_PROC_CODES:
        return buildQueryStringTemplate(
            "rda.fiss_proc_codes", FISS_CLAIM_ID_FIELD, FISS_PROC_SAMHSA_COLUMNS);
      case FISS_REVENUE_LINES:
        return buildQueryStringTemplate(
            "rda.fiss_revenue_lines", FISS_CLAIM_ID_FIELD, FISS_REVENUE_LINES_SAMHSA_COLUMNS);
      default:
        throw new IllegalArgumentException("Unknown RDA_TABLES type.");
    }
  }
}
