package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import static gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.QueryConstants.*;

import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CCW implementation for AbstractSamhsaBackfill. */
public class CCWSamhsaBackfill extends AbstractSamhsaBackfill {
  /** The Logger. */
  static final Logger LOGGER = LoggerFactory.getLogger(CCWSamhsaBackfill.class);

  /** The table to process in this thread. */
  private final TableEntry tableEntry;

  /** The list of table entries for CCW claims. */
  public enum CCW_TABLES {
    /** Carrier Claim. */
    CARRIER_CLAIMS(
        new TableEntry(
            CARRIER_SAMHSA_QUERY, "ccw.carrier_tags", "cc.clm_id", "ccw.carrier_claims")),
    /** Carrier Claim Lines. */
    CARRIER_CLAIM_LINES(
        new TableEntry(
            CARRIER_CLAIM_LINES_SAMHSA_QUERY,
            "ccw.carrier_tags",
            "cc.clm_id",
            "ccw.carrier_claim_lines")),

    /** DME Claim. */
    DME_CLAIMS(new TableEntry(DME_SAMHSA_QUERY, "ccw.dme_tags", "dc.clm_id", "ccw.dme_claims")),
    /** DME Claim Lines. */
    DME_CLAIM_LINES(
        new TableEntry(
            DME_CLAIM_LINES_SAMHSA_QUERY, "ccw.dme_tags", "dc.clm_id", "ccw.dme_claim_lines")),
    /** HHA Claim. */
    HHA_CLAIMS(new TableEntry(HHA_SAMHSA_QUERY, "ccw.hha_tags", "hc.clm_id", "ccw.hha_claims")),
    /** HHA Claim Lines. */
    HHA_CLAIM_LINES(
        new TableEntry(
            HHA_CLAIM_LINES_SAMHSA_QUERY, "ccw.hha_tags", "hc.clm_id", "ccw.hha_claim_lines")),
    /** Hospice Claim. */
    HOSPICE_CLAIMS(
        new TableEntry(
            HOSPICE_SAMHSA_QUERY, "ccw.hospice_tags", "hc.clm_id", "ccw.hospice_claims")),
    /** Hospice Claim Lines. */
    HOSPICE_CLAIM_LINES(
        new TableEntry(
            HOSPICE_CLAIM_LINES_SAMHSA_QUERY,
            "ccw.hospice_tags",
            "hc.clm_id",
            "ccw.hospice_claim_lines")),
    /** Inpatient Claim. */
    INPATIENT_CLAIMS(
        new TableEntry(
            INPATIENT_SAMHSA_QUERY, "ccw.inpatient_tags", "ic.clm_id", "ccw.inpatient_claims")),
    /** Inpatient Claim Lines. */
    INPATIENT_CLAIM_LINES(
        new TableEntry(
            INPATIENT_CLAIM_LINES_SAMHSA_QUERY,
            "ccw.inpatient_tags",
            "ic.clm_id",
            "ccw.inpatient_claim_lines")),
    /** Outpatient Claim. */
    OUTPATIENT_CLAIMS(
        new TableEntry(
            OUTPATIENT_SAMHSA_QUERY, "ccw.outpatient_tags", "oc.clm_id", "ccw.outpatient_claims")),
    /** Outpatient Claim Lines. */
    OUTPATIENT_CLAIM_LINES(
        new TableEntry(
            OUTPATIENT_CLAIM_LINES_SAMHSA_QUERY,
            "ccw.outpatient_tags",
            "oc.clm_id",
            "ccw.outpatient_claim_lines")),
    /** SNF Claim. */
    SNF_CLAIMS(new TableEntry(SNF_SAMHSA_QUERY, "ccw.snf_tags", "sc.clm_id", "ccw.snf_claims")),
    /** SNF Claim Lines. */
    SNF_CLAIM_LINES(
        new TableEntry(
            SNF_CLAIM_LINES_SAMHSA_QUERY, "ccw.snf_tags", "sc.clm_id", "ccw.snf_claim_lines"));

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
   * @param logInterval The log interval.
   * @param tableEntry The table to use in this thread.
   */
  public CCWSamhsaBackfill(
      TransactionManager transactionManager,
      int batchSize,
      Long logInterval,
      CCW_TABLES tableEntry) {
    super(transactionManager, batchSize, LOGGER, logInterval, tableEntry.getEntry());
    this.tableEntry = tableEntry.getEntry();
  }

  /** {@inheritDoc} */
  @Override
  protected Long convertClaimId(String claim) {
    return Long.valueOf(claim);
  }
}
