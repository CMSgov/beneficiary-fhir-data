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

  /**
   * The list of table entries for CCW claims. This is mostly used in building the queries for the
   * tables.
   */
  public enum CCW_TABLES {
    /** Carrier Claim. */
    CARRIER_CLAIMS(
        new TableEntry(
            CARRIER_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.carrier_tags",
            "cc.clm_id",
            "ccw.carrier_claims",
            "ccw.carrier_claims", // Should not be used, since is already a parent table. Included
                                  // out of an abundance of caution.
            false)),
    /** Carrier Claim Lines. */
    CARRIER_CLAIM_LINES(
        new TableEntry(
            CARRIER_CLAIM_LINES_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.carrier_tags",
            "clm_id",
            "ccw.carrier_claim_lines",
            "ccw.carrier_claims",
            true)),

    /** DME Claim. */
    DME_CLAIMS(
        new TableEntry(
            DME_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.dme_tags",
            "dc.clm_id",
            "ccw.dme_claims",
            "ccw.dme_claims", // Should not be used, since is already a parent table. Included out
                              // of an abundance of caution.
            false)),
    /** DME Claim Lines. */
    DME_CLAIM_LINES(
        new TableEntry(
            DME_CLAIM_LINES_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.dme_tags",
            "clm_id",
            "ccw.dme_claim_lines",
            "ccw.dme_claims",
            true)),
    /** HHA Claim. */
    HHA_CLAIMS(
        new TableEntry(
            HHA_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.hha_tags",
            "hc.clm_id",
            "ccw.hha_claims",
            "ccw.hha_claims", // Should not be used, since is already a parent table. Included out
                              // of an abundance of caution.
            false)),
    /** HHA Claim Lines. */
    HHA_CLAIM_LINES(
        new TableEntry(
            HHA_CLAIM_LINES_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.hha_tags",
            "clm_id",
            "ccw.hha_claim_lines",
            "ccw.hha_claims",
            true)),
    /** Hospice Claim. */
    HOSPICE_CLAIMS(
        new TableEntry(
            HOSPICE_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.hospice_tags",
            "hc.clm_id",
            "ccw.hospice_claims",
            "ccw.hospice_claims", // Should not be used, since is already a parent table. Included
                                  // out of an abundance of caution.
            false)),
    /** Hospice Claim Lines. */
    HOSPICE_CLAIM_LINES(
        new TableEntry(
            HOSPICE_CLAIM_LINES_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.hospice_tags",
            "clm_id",
            "ccw.hospice_claim_lines",
            "ccw.hospice_claims",
            true)),
    /** Inpatient Claim. */
    INPATIENT_CLAIMS(
        new TableEntry(
            INPATIENT_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.inpatient_tags",
            "ic.clm_id",
            "ccw.inpatient_claims",
            "ccw.inpatient_claims", // Should not be used, since is already a parent table. Included
                                    // out of an abundance of caution.
            false)),
    /** Inpatient Claim Lines. */
    INPATIENT_CLAIM_LINES(
        new TableEntry(
            INPATIENT_CLAIM_LINES_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.inpatient_tags",
            "clm_id",
            "ccw.inpatient_claim_lines",
            "ccw.inpatient_claims",
            true)),
    /** Outpatient Claim. */
    OUTPATIENT_CLAIMS(
        new TableEntry(
            OUTPATIENT_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.outpatient_tags",
            "oc.clm_id",
            "ccw.outpatient_claims",
            "ccw.outpatient_claims", // Should not be used, since is already a parent table.
                                     // Included out of an abundance of caution.
            false)),
    /** Outpatient Claim Lines. */
    OUTPATIENT_CLAIM_LINES(
        new TableEntry(
            OUTPATIENT_CLAIM_LINES_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.outpatient_tags",
            "clm_id",
            "ccw.outpatient_claim_lines",
            "ccw.outpatient_claims",
            true)),
    /** SNF Claim. */
    SNF_CLAIMS(
        new TableEntry(
            SNF_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.snf_tags",
            "sc.clm_id",
            "ccw.snf_claims",
            "ccw.snf_claims", // Should not be used, since is already a parent table. Included out
                              // of an abundance of caution.
            false)),
    /** SNF Claim Lines. */
    SNF_CLAIM_LINES(
        new TableEntry(
            SNF_CLAIM_LINES_SAMHSA_QUERY,
            GET_CLAIM_DATES,
            "ccw.snf_tags",
            "clm_id",
            "ccw.snf_claim_lines",
            "ccw.snf_claims",
            true));

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
   * @param logInterval The log reporting interval, in seconds.
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
