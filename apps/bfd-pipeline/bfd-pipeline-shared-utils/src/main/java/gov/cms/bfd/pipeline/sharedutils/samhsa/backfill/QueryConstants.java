package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

/** Contains the queries to use. */
public class QueryConstants {
  /** Upsert query for tags. */
  public static final String TAG_UPSERT_QUERY =
      """
      INSERT INTO ${tagTable} (code, clm_id)
      VALUES (:code, :claimId)
      ON CONFLICT (code, clm_id) DO NOTHING;
      """;

  /** Line fo Greater Than claimId. */
  public static final String GT_CLAIM_LINE = "WHERE ${claimField} >= :startingClaim";

  /** Gets the date ranges for a claim in a line item table from its parent table. */
  public static final String GET_CLAIM_DATES =
      "SELECT clm_from_dt, clm_thru_dt from ${claimTable} where ${claimField} = :claimId ";

  /** Gets the date ranges for a claim in a line item table from its parent table. */
  public static final String GET_CLAIM_DATES_FISS =
      "SELECT stmt_cov_from_date, stmt_cov_to_date from ${claimTable} where ${claimField} = :claimId ";

  /** Gets the date ranges for a claim in a line item table from its parent table. */
  public static final String GET_CLAIM_DATES_MCS =
      "SELECT idr_hdr_from_date_of_svc, idr_hdr_to_date_of_svc from ${claimTable} where ${claimField} = :claimId ";

  /** Query to perform upsert on the backfill progress table for a given claim table. */
  public static final String UPSERT_PROGRESS_QUERY =
      """
      INSERT INTO ccw.samhsa_backfill_progress
      (claim_table, last_processed_claim, total_processed, total_tags)
      VALUES (:tableName, :lastClaim, :totalProcessed, :totalTags)
      ON CONFLICT (claim_table)
      DO UPDATE SET
      last_processed_claim = :lastClaim, total_processed = :totalProcessed, total_tags = :totalTags
      """;

  /** Query to get the backfill progress from the database for a given claim table. */
  public static final String GET_PROGRESS_QUERY =
      " SELECT claim_table, last_processed_claim, total_processed, total_tags FROM ccw.samhsa_backfill_progress "
          + " WHERE claim_table = :tableName ";
}
