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
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CCW implementation for AbstractSamhsaBackfill. */
public class CCWSamhsaBackfill extends AbstractSamhsaBackfill {
  /** The Logger. */
  static final Logger LOGGER = LoggerFactory.getLogger(CCWSamhsaBackfill.class);

  /** The name of the claim id column. */
  private final String CLAIM_ID_COLUMN_NAME = "clm_id";

  /** List ot tables that we want to process in this instance. */
  private final String claimTable;

  /** The list of table entries for CCW claims. */
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
   * @param claimTable The table to use in this thread.
   */
  public CCWSamhsaBackfill(
      TransactionManager transactionManager, int batchSize, String claimTable) {
    super(transactionManager, batchSize, LOGGER);
    this.claimTable = claimTable;
  }

  /** {@inheritDoc} */
  @Override
  protected Optional<TableEntry> getTable() {
    // Get the entry for the table that we're using in this thread.
    return TABLES.stream().filter(table -> claimTable.equals(table.getClaimTable())).findFirst();
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
