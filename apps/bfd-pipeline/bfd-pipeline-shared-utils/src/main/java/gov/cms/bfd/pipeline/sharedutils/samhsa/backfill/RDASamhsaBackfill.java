package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.util.List;

/** RDA implementation of AbstractSamhsaBackfill. */
public class RDASamhsaBackfill extends AbstractSamhsaBackfill {
  /** The tables. */
  private final List<String> TABLES = List.of("rda.fiss_claims", "rda.mcs_claims");

  /** The column name for the fiss claim id. */
  private String FISS_CLAIM_ID_COLUMN = "claim_id";

  /** The column name for the mcs claim id. */
  private String MCS_CLAIM_ID_COLMN = "idr_clm_hd_icn";

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
  @Override
  protected List<String> getTables() {
    return TABLES;
  }

  /** {@inheritDoc} */
  @Override
  protected String getClaimIdColumnName(String table) {
    switch (table) {
      case "rda.fiss_claims" -> {
        return FISS_CLAIM_ID_COLUMN;
      }
      case "rda.mcs_claims" -> {
        return MCS_CLAIM_ID_COLMN;
      }
      default -> throw new RuntimeException("Unknown RDA table name.");
    }
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

  /** {@inheritDoc} */
  @Override
  protected Class getTableClass(String table) {
    switch (table) {
      case "rda.mcs_claims":
        return RdaMcsClaim.class;
      case "rda.fiss_claims":
        return RdaFissClaim.class;
      default:
        throw new RuntimeException("Error: cannot get class from unknown table.");
    }
  }
}
