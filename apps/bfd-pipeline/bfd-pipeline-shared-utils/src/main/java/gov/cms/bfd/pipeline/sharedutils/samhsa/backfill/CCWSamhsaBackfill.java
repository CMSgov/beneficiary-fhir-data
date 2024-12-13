package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CCW implementation for AbstractSamhsaBackfill. */
public class CCWSamhsaBackfill extends AbstractSamhsaBackfill {
  /** The Logger. */
  static final Logger LOGGER = LoggerFactory.getLogger(CCWSamhsaBackfill.class);

  /** The name of the claim id column. */
  private static final String CLAIM_ID_FIELD = "claimId";

  /** The table to process in this thread. */
  private final TableEntry tableEntry;

  /** The list of table entries for CCW claims. */
  public enum CCW_TABLES {
    /** Carrier Claim. */
    CARRIER_CLAIMS(new TableEntry("", "", "", "")),
    /** DME Claim. */
    DME_CLAIMS(new TableEntry("", "", "", "")),
    /** HHA Claim. */
    HHA_CLAIMS(new TableEntry("", "", "", "")),
    /** Hospice Claim. */
    HOSPICE_CLAIMS(new TableEntry("", "", "", "")),
    /** Inpatient Claim. */
    INPATIENT_CLAIMS(new TableEntry("", "", "", "")),
    /** Outpatient Claim. */
    OUTPATIENT_CLAIMS(new TableEntry("", "", "", "")),
    /** SNF Claim. */
    SNF_CLAIMS(new TableEntry("", "", "", ""));

    /** The table entry. */
    private TableEntry entry;

    /**
     * Constructor.
     *
     * @param entry The tableEntry.
     */
    CCW_TABLES(TableEntry entry) {
      this.entry = entry;
    }

    /**
     * Returns the table entry.
     *
     * @return The tableEntry.
     */
    TableEntry getEntry() {
      return entry;
    }
  }

  /**
   * Constructor.
   *
   * @param transactionManager Transaction manager.
   * @param batchSize the query batch size.
   * @param tableEntry The table to use in this thread.
   */
  public CCWSamhsaBackfill(
      TransactionManager transactionManager, int batchSize, CCW_TABLES tableEntry) {
    super(transactionManager, batchSize, LOGGER, tableEntry.getEntry());
    this.tableEntry = tableEntry.getEntry();
  }

  /** {@inheritDoc} */
  @Override
  protected Long convertClaimId(String claim) {
    return Long.valueOf(claim);
  }
}
