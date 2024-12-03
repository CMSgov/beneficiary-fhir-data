package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.util.List;

/** CCW implementation for AbstractSamhsaBackfill. */
public class CCWSamhsaBackfill extends AbstractSamhsaBackfill {

  /** The list of tables. */
  private final List<String> TABLES =
      List.of(
          "ccw.carrier_claims",
          "ccw.dme_claims",
          "ccw.hha_claims",
          "ccw.hospice_claims",
          "ccw.inpatient_claims",
          "ccw.outpatient_claims",
          "ccw.snf_claims");

  /** The name of the claim id column. */
  private final String CLAIM_ID_COLUMN_NAME = "clm_id";

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
  protected List<String> getTables() {
    return TABLES;
  }

  /** {@inheritDoc} */
  @Override
  protected Class getTableClass(String table) {
    switch (table) {
      case "ccw.carrier_claims":
        return CarrierClaim.class;
      case "ccw.dme_claims":
        return DMEClaim.class;
      case "ccw.hha_claims":
        return HHAClaim.class;
      case "ccw.hospice_claims":
        return HospiceClaim.class;
      case "ccw.inpatient_claims":
        return InpatientClaim.class;
      case "ccw.outpatient_claims":
        return OutpatientClaim.class;
      case "ccw.snf_claims":
        return SNFClaim.class;
      default:
        throw new RuntimeException("Error: cannot get class from unknown table.");
    }
  }

  /** {@inheritDoc} */
  @Override
  protected String getClaimIdColumnName(String table) {
    return CLAIM_ID_COLUMN_NAME;
  }

  /** {@inheritDoc} */
  @Override
  protected String getClaimId(Object claim) {
    switch (claim) {
      case CarrierClaim carrierClaim -> {
        return String.valueOf(carrierClaim.getClaimId());
      }
      case HHAClaim hhaClaim -> {
        return String.valueOf(hhaClaim.getClaimId());
      }
      case DMEClaim dmeClaim -> {
        return String.valueOf(dmeClaim.getClaimId());
      }
      case HospiceClaim hospiceClaim -> {
        return String.valueOf(hospiceClaim.getClaimId());
      }
      case OutpatientClaim outpatientClaim -> {
        return String.valueOf(outpatientClaim.getClaimId());
      }
      case InpatientClaim inpatientClaim -> {
        return String.valueOf(inpatientClaim.getClaimId());
      }
      case SNFClaim snfClaim -> {
        return String.valueOf(snfClaim.getClaimId());
      }
      default -> throw new RuntimeException("Unknown claim type.");
    }
  }
}
