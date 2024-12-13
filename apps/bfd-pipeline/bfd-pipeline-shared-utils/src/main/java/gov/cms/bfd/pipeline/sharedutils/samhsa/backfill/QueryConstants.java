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

  /** Query for MCS Diagnosis codes. */
  public static final String MCS_DIAG_SAMHSA_QUERY =
      """
        SELECT mc.idr_clm_hd_icn, mc.idr_hdr_from_date_of_svc, mc.idr_hdr_to_date_of_svc, dc.idr_diag_code
            FROM rda.mcs_diagnosis_codes dc
            JOIN rda.mcs_claims mc ON mc.idr_clm_hd_icn = dc.idr_clm_hd_icn
            ${gtClaimLine}
            ORDER BY idr_clm_hd_icn ASC
            limit :limit
        """;

  /** Query for MCS Details. */
  public static final String MCS_DETAILS_SAMHSA_QUERY =
      """
        SELECT mc.idr_clm_hd_icn, mc.idr_hdr_from_date_of_svc, mc.idr_hdr_to_date_of_svc, dt.idr_dtl_primary_diag_code, dt.idr_proc_code
            FROM rda.mcs_details dt
            JOIN rda.mcs_claims mc ON mc.idr_clm_hd_icn = dt.idr_clm_hd_icn
            ${gtClaimLine}
            ORDER BY idr_clm_hd_icn ASC
            limit :limit
        """;

  /** Query for Fiss claims. */
  public static final String FISS_SAMHSA_QUERY =
      """
        SELECT fc.claim_id, fc.stmt_cov_from_date, fc.stmt_cov_to_date, fc.admit_diag_code, fc.drg_cd, fc.principle_diag
        FROM rda.fiss_claims fc
        ${gtClaimLine}
        ORDER BY claim_id ASC
        limit :limit
    """;

  /** Query for Fiss revenue lines. */
  public static final String FISS_REVENUE_SAMHSA_QUERY =
      """
            SELECT fc.claim_id, fc.stmt_cov_from_date, fc.stmt_cov_to_date, fr.apc_hcpcs_apc, fr.hcpc_cd
            FROM rda.fiss_revenue_lines fr
            JOIN rda.fiss_claims fc on fc.claim_id = fr.claim_id
            ${gtClaimLine}
            ORDER BY claim_id ASC
            limit :limit
        """;

  /** Query for Fiss diagnosis codes. */
  public static final String FISS_DIAGNOSIS_SAMHSA_QUERY =
      """
            SELECT fc.claim_id, fc.stmt_cov_from_date, fc.stmt_cov_to_date, fd.diag_cd2
            FROM rda.fiss_diagnosis_codes fd
            JOIN rda.fiss_claims fc on fc.claim_id = fd.claim_id
            ${gtClaimLine}
            ORDER BY claim_id ASC
            limit :limit
        """;

  /** Query for Fiss proc codes. */
  public static final String FISS_PROC_SAMHSA_QUERY =
      """
            SELECT fc.claim_id, fc.stmt_cov_from_date, fc.stmt_cov_to_date, fp.proc_code
            FROM rda.fiss_proc_codes fp
            JOIN rda.fiss_claims fc on fc.claim_id = fp.claim_id
            ${gtClaimLine}
            ORDER BY fc.claim_id ASC
            limit :limit
        """;

  /** Line fo Greater Than claimId. */
  public static final String GT_CLAIM_LINE = "WHERE ${claimField} >= :startingClaim";
}
