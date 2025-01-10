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
  private static String FISS_CLAIM_ID_FIELD = "claim_id";

  /** The column name for the mcs claim id. */
  private static String MCS_CLAIM_ID_FIELD = "idr_clm_hd_icn";

  /** The list of table entries for RDA claims. */
  public enum RDA_TABLES {
    /** Fiss Claims. */
    FISS_CLAIMS(
        new TableEntry(
            FISS_SAMHSA_QUERY,
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
            FISS_PROC_SAMHSA_QUERY,
            GET_CLAIM_DATES_FISS,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_proc_codes",
            "rda.fiss_claims",
            true)),
    /** Fiss diagnosis codes. */
    FISS_DIAGNOSIS_CODES(
        new TableEntry(
            FISS_DIAGNOSIS_SAMHSA_QUERY,
            GET_CLAIM_DATES_FISS,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_diagnosis_codes",
            "rda.fiss_claims",
            true)),
    /** Fiss revenue lines. */
    FISS_REVENUE_LINES(
        new TableEntry(
            FISS_REVENUE_SAMHSA_QUERY,
            GET_CLAIM_DATES_FISS,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_revenue_lines",
            "rda.fiss_claims",
            true)),

    /** MCS diagnosis codes. */
    MCS_DIAGNOSIS_CODES(
        new TableEntry(
            MCS_DIAG_SAMHSA_QUERY,
            GET_CLAIM_DATES_MCS,
            "rda.mcs_tags",
            MCS_CLAIM_ID_FIELD,
            "rda.mcs_diagnosis_codes",
            "rda.mcs_claims",
            true)),
    /** MCS details. */
    MCS_DETAILS(
        new TableEntry(
            MCS_DETAILS_SAMHSA_QUERY,
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
  }

  /** {@inheritDoc} */
  @Override
  protected String convertClaimId(String claimId) {
    // Already a string, do nothing.
    return claimId;
  }
}
