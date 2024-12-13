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
  private static String FISS_CLAIM_ID_FIELD = "fc.claim_id";

  /** The column name for the mcs claim id. */
  private static String MCS_CLAIM_ID_FIELD = "mc.idr_clm_hd_icn";

  /** The list of table entries for RDA claims. */
  public enum RDA_TABLES {
    /** Fiss Claims. */
    FISS_CLAIMS(
        new TableEntry(FISS_SAMHSA_QUERY, "rda.fiss_tags", FISS_CLAIM_ID_FIELD, "rda.fiss_claims")),
    /** Fiss proc codes. */
    FISS_PROC_CODES(
        new TableEntry(
            FISS_PROC_SAMHSA_QUERY, "rda.fiss_tags", FISS_CLAIM_ID_FIELD, "rda.fiss_proc_codes")),
    /** Fiss diagnosis codes. */
    FISS_DIAGNOSIS_CODES(
        new TableEntry(
            FISS_DIAGNOSIS_SAMHSA_QUERY,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_diagnosis_codes")),
    /** Fiss revenue lines. */
    FISS_REVENUE_LINES(
        new TableEntry(
            FISS_REVENUE_SAMHSA_QUERY,
            "rda.fiss_tags",
            FISS_CLAIM_ID_FIELD,
            "rda.fiss_revenue_lines")),

    /** MCS diagnosis codes. */
    MCS_DIAGNOSIS_CODES(
        new TableEntry(
            MCS_DIAG_SAMHSA_QUERY, "rda.mcs_tags", MCS_CLAIM_ID_FIELD, "rda.mcs_diagnosis_codes")),
    /** MCS details. */
    MCS_DETAILS(
        new TableEntry(
            MCS_DETAILS_SAMHSA_QUERY, "rda.mcs_tags", MCS_CLAIM_ID_FIELD, "rda.mcs_details"));

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
   * @param tableEntry The table to process in this thread.
   */
  public RDASamhsaBackfill(
      TransactionManager transactionManager, int batchSize, RDA_TABLES tableEntry) {
    super(transactionManager, batchSize, LOGGER, tableEntry.getEntry());
  }

  /** {@inheritDoc} */
  @Override
  protected String convertClaimId(String claimId) {
    // Already a string, do nothing.
    return claimId;
  }
}
