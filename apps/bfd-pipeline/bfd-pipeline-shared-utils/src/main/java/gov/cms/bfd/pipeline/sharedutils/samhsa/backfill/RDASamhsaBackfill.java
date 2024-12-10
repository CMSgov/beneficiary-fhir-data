package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** RDA implementation of AbstractSamhsaBackfill. */
public class RDASamhsaBackfill extends AbstractSamhsaBackfill {
  /** The Logger. */
  static final Logger LOGGER = LoggerFactory.getLogger(RDASamhsaBackfill.class);

  /** The column name for the fiss claim id. */
  private String FISS_CLAIM_ID_COLUMN = "claim_id";

  /** The column name for the mcs claim id. */
  private String MCS_CLAIM_ID_COLUMN = "idr_clm_hd_icn";

  /** The list of table entries for RDA claims. */
  private final List<TableEntry> TABLES =
      List.of(
          new TableEntry(
              "rda.fiss_claims", RdaFissClaim.class, "rda.fiss_Tags", FISS_CLAIM_ID_COLUMN),
          new TableEntry("rda.mcs_claims", RdaMcsClaim.class, "rda.mcs_tags", MCS_CLAIM_ID_COLUMN));

  /** The table to process for this thread. */
  private final String claimTable;

  /**
   * Constructor.
   *
   * @param transactionManager The transaction manager.
   * @param batchSize the query batch size.
   * @param claimTable The list of tables to use.
   */
  public RDASamhsaBackfill(
      TransactionManager transactionManager, int batchSize, String claimTable) {
    super(transactionManager, batchSize, LOGGER);
    this.claimTable = claimTable;
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
  protected Optional<TableEntry> getTable() {
    // Get the entry for the table that we're using in this thread.
    return TABLES.stream().filter(table -> claimTable.equals(table.getClaimTable())).findFirst();
  }

  /** {@inheritDoc} */
  @Override
  protected <TClaim> boolean processClaim(TClaim claim, EntityManager entityManager) {
    return samhsaUtil.processRdaClaim(claim, entityManager);
  }
}
