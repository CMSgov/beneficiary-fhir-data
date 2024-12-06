package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import jakarta.persistence.EntityManager;
import java.util.List;

/** CCW implementation for AbstractSamhsaBackfill. */
public class CCWSamhsaBackfill extends AbstractSamhsaBackfill {

  /** The name of the claim id column. */
  private final String CLAIM_ID_COLUMN_NAME = "clm_id";

  /** The list of tables. */
  private final List<TableEntry> TABLES =
      List.of(
          new TableEntry(
              "ccw.carrier_claims", CarrierClaim.class, "ccw.carrier_tags", CLAIM_ID_COLUMN_NAME),
          new TableEntry("ccw.dme_claims", DMEClaim.class, "ccw.dme_tags", CLAIM_ID_COLUMN_NAME),
          new TableEntry("ccw.hha_claims", HHAClaim.class, "ccw.hha_tags", CLAIM_ID_COLUMN_NAME),
          new TableEntry(
              "ccw.hospice_claims", HospiceClaim.class, "ccw.hospice_tags", CLAIM_ID_COLUMN_NAME),
          new TableEntry(
              "ccw.inpatient_claims",
              InpatientClaim.class,
              "ccw.inpatient_tags",
              CLAIM_ID_COLUMN_NAME),
          new TableEntry(
              "ccw.outpatient_claims",
              OutpatientClaim.class,
              "ccw.outpatient_tags",
              CLAIM_ID_COLUMN_NAME),
          new TableEntry("ccw.snf_claims", SNFClaim.class, "ccw.snf_tags", CLAIM_ID_COLUMN_NAME));

  /**
   * Constructor.
   *
   * @param transactionManager Transaction manager.
   * @param batchSize the query batch size.
   */
  public CCWSamhsaBackfill(TransactionManager transactionManager, int batchSize) {
    super(transactionManager, batchSize);
  }

  /** {@inheritDoc} */
  @Override
  protected List<TableEntry> getTables() {
    return TABLES;
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
