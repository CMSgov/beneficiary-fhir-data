package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.model.rif.samhsa.DmeTag;
import gov.cms.bfd.model.rif.samhsa.HhaTag;
import gov.cms.bfd.model.rif.samhsa.HospiceTag;
import gov.cms.bfd.model.rif.samhsa.InpatientTag;
import gov.cms.bfd.model.rif.samhsa.OutpatientTag;
import gov.cms.bfd.model.rif.samhsa.SnfTag;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import jakarta.persistence.EntityManager;
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
    CARRIER_CLAIMS(
        new TableEntry("ccw.carrier_claims", CarrierClaim.class, CarrierTag.class, CLAIM_ID_FIELD)),
    /** DME Claim. */
    DME_CLAIMS(new TableEntry("ccw.dme_claims", DMEClaim.class, DmeTag.class, CLAIM_ID_FIELD)),
    /** HHA Claim. */
    HHA_CLAIMS(new TableEntry("ccw.hha_claims", HHAClaim.class, HhaTag.class, CLAIM_ID_FIELD)),
    /** Hospice Claim. */
    HOSPICE_CLAIMS(
        new TableEntry("ccw.hospice_claims", HospiceClaim.class, HospiceTag.class, CLAIM_ID_FIELD)),
    /** Inpatient Claim. */
    INPATIENT_CLAIMS(
        new TableEntry(
            "ccw.inpatient_claims", InpatientClaim.class, InpatientTag.class, CLAIM_ID_FIELD)),
    /** Outpatient Claim. */
    OUTPATIENT_CLAIMS(
        new TableEntry(
            "ccw.outpatient_claims", OutpatientClaim.class, OutpatientTag.class, CLAIM_ID_FIELD)),
    /** SNF Claim. */
    SNF_CLAIMS(new TableEntry("ccw.snf_claims", SNFClaim.class, SnfTag.class, CLAIM_ID_FIELD));

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
  protected <TClaim> boolean processClaim(TClaim claim, EntityManager entityManager) {
    return samhsaUtil.processCcwClaim(claim, entityManager);
  }

  /** {@inheritDoc} */
  @Override
  protected String getClaimId(Object claim) {
    return switch (claim) {
      case CarrierClaim carrierClaim -> String.valueOf(carrierClaim.getClaimId());
      case HHAClaim hhaClaim -> String.valueOf(hhaClaim.getClaimId());
      case DMEClaim dmeClaim -> String.valueOf(dmeClaim.getClaimId());
      case HospiceClaim hospiceClaim -> String.valueOf(hospiceClaim.getClaimId());
      case OutpatientClaim outpatientClaim -> String.valueOf(outpatientClaim.getClaimId());
      case InpatientClaim inpatientClaim -> String.valueOf(inpatientClaim.getClaimId());
      case SNFClaim snfClaim -> String.valueOf(snfClaim.getClaimId());
      default -> throw new RuntimeException("Unknown claim type.");
    };
  }
}
