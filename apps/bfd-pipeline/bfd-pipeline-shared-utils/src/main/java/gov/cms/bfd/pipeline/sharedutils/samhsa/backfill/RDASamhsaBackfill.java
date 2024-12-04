package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import jakarta.persistence.EntityManager;
import java.util.List;

/** RDA implementation of AbstractSamhsaBackfill. */
public class RDASamhsaBackfill extends AbstractSamhsaBackfill {

  /** The column name for the fiss claim id. */
  private String FISS_CLAIM_ID_COLUMN = "claim_id";

  /** The column name for the mcs claim id. */
  private String MCS_CLAIM_ID_COLUMN = "idr_clm_hd_icn";

  /** The tables. */
  private final List<TableEntry> TABLES =
      List.of(
          new TableEntry(
              "rda.fiss_claims", RdaFissClaim.class, "rda.fiss_Tags", FISS_CLAIM_ID_COLUMN),
          new TableEntry("rda.mcs_claims", RdaMcsClaim.class, "rda.mcs_tags", MCS_CLAIM_ID_COLUMN));

  /**
   * Constructor.
   *
   * @param transactionManager The transaction manager.
   * @param batchSize the query batch size.
   */
  public RDASamhsaBackfill(TransactionManager transactionManager, int batchSize) {
    super(transactionManager, batchSize);
  }

  /** {@inheritDoc} */
  protected String getClaimId(Object claim) {
    switch (claim) {
      case RdaMcsClaim mcsClaim -> {
        return String.format("'%s'", mcsClaim.getIdrClmHdIcn());
      }
      case RdaFissClaim fissClaim -> {
        return String.format("'%s'", fissClaim.getClaimId());
      }
      default -> throw new RuntimeException("Unknown claim type.");
    }
  }

  @Override
  protected List<TableEntry> getTables() {
    return TABLES;
  }

  @Override
  <TClaim> boolean processClaim(TClaim claim, EntityManager entityManager) {
    return samhsaUtil.processRdaClaim(claim, transactionManager.getEntityManager());
  }
}
