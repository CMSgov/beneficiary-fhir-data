package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RDA implementation of AbstractSamhsaBackfill. */
public class RDASamhsaBackfill extends AbstractSamhsaBackfill {
  /** The Logger. */
  static final Logger LOGGER = LoggerFactory.getLogger(RDASamhsaBackfill.class);

  /** The column name for the fiss claim id. */
  private static String FISS_CLAIM_ID_COLUMN = "claim_id";

  /** The column name for the mcs claim id. */
  private static String MCS_CLAIM_ID_COLUMN = "idr_clm_hd_icn";

  /** The list of table entries for RDA claims. */
  public enum RDA_TABLES {
    /** Fiss Claim. */
    FISS_CLAIMS(
        new TableEntry(
            "rda.fiss_claims", RdaFissClaim.class, "rda.fiss_Tags", FISS_CLAIM_ID_COLUMN)),
    /** MCS Claim. */
    MCS_CLAIMS(
        new TableEntry("rda.mcs_claims", RdaMcsClaim.class, "rda.mcs_tags", MCS_CLAIM_ID_COLUMN));

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
  protected String getClaimId(Object claim) {
    return switch (claim) {
      case RdaMcsClaim mcsClaim -> String.format("'%s'", mcsClaim.getIdrClmHdIcn());
      case RdaFissClaim fissClaim -> String.format("'%s'", fissClaim.getClaimId());
      default -> throw new RuntimeException("Unknown claim type.");
    };
  }

  /** {@inheritDoc} */
  @Override
  protected <TClaim> boolean processClaim(TClaim claim, EntityManager entityManager) {
    return samhsaUtil.processRdaClaim(claim, entityManager);
  }
}
