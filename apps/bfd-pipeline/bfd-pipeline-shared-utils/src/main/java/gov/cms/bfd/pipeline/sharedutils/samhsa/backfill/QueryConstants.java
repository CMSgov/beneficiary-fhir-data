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
        SELECT dc.idr_clm_hd_icn, dc.idr_diag_code
            FROM rda.mcs_diagnosis_codes dc
            ${gtClaimLine}
            ORDER BY idr_clm_hd_icn ASC
            limit :limit
        """;

  /** Query for MCS Details. */
  public static final String MCS_DETAILS_SAMHSA_QUERY =
      """
        SELECT dt.idr_clm_hd_icn, dt.idr_dtl_primary_diag_code, dt.idr_proc_code
            FROM rda.mcs_details dt
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
            SELECT fr.claim_id, fr.apc_hcpcs_apc, fr.hcpc_cd
            FROM rda.fiss_revenue_lines fr
            ${gtClaimLine}
            ORDER BY claim_id ASC
            limit :limit
        """;

  /** Query for Fiss diagnosis codes. */
  public static final String FISS_DIAGNOSIS_SAMHSA_QUERY =
      """
            SELECT fd.claim_id, fd.diag_cd2
            FROM rda.fiss_diagnosis_codes fd
            ${gtClaimLine}
            ORDER BY claim_id ASC
            limit :limit
        """;

  /** Query for Fiss proc codes. */
  public static final String FISS_PROC_SAMHSA_QUERY =
      """
            SELECT fp.claim_id, fp.proc_code
            FROM rda.fiss_proc_codes fp
            ${gtClaimLine}
            ORDER BY fc.claim_id ASC
            limit :limit
        """;

  /** Query for Carrier claims. */
  public static final String CARRIER_SAMHSA_QUERY =
      """
            SELECT cc.clm_id, cc.clm_from_dt, cc.clm_thru_dt,
              cc.prncpal_dgns_cd, cc.icd_dgns_cd1, cc.icd_dgns_cd2, cc.icd_dgns_cd3,
              cc.icd_dgns_cd4, cc.icd_dgns_cd5, cc.icd_dgns_cd6, cc.icd_dgns_cd7,
              cc.icd_dgns_cd8, cc.icd_dgns_cd9, cc.icd_dgns_cd10, cc.icd_dgns_cd11, cc.icd_dgns_cd12
            FROM ccw.carrier_claims cc
            ${gtClaimLine}
            ORDER BY cc.clm_id ASC
            limit :limit
      """;

  /** Query for Carrier Claim Lines. */
  public static final String CARRIER_CLAIM_LINES_SAMHSA_QUERY =
      """
            SELECT ccl.clm_id,
              ccl.line_icd_dgns_cd, ccl.hcpcs_cd
            FROM ccw.carrier_claim_lines ccl
            ${gtClaimLine}
            ORDER BY ccl.clm_id ASC
            limit :limit
      """;

  /** Query for DME claims. */
  public static final String DME_SAMHSA_QUERY =
      """
           SELECT dc.clm_id, dc.clm_from_dt, dc.clm_thru_dt,
             dc.prncpal_dgns_cd, dc.icd_dgns_cd1, dc.icd_dgns_cd2, dc.icd_dgns_cd3,
             dc.icd_dgns_cd4, dc.icd_dgns_cd5, dc.icd_dgns_cd6, dc.icd_dgns_cd7, dc.icd_dgns_cd8,
             dc.icd_dgns_cd9, dc.icd_dgns_cd10, dc.icd_dgns_cd11, dc.icd_dgns_cd12
           FROM ccw.dme_claims dc
           ${gtClaimLine}
           ORDER BY dc.clm_id ASC
           limit :limit
      """;

  /** Query for DME Claim Lines. */
  public static final String DME_CLAIM_LINES_SAMHSA_QUERY =
      """
                SELECT dcl.clm_id,
                  dcl.line_icd_dgns_cd, dcl.hcpcs_cd
                FROM ccw.dme_claim_lines dcl
                ${gtClaimLine}
                ORDER BY dcl.clm_id ASC
                limit :limit
          """;

  /** Query for Hospice claims. */
  public static final String HOSPICE_SAMHSA_QUERY =
      """
          SELECT hc.clm_id, hc.clm_from_dt, hc.clm_thru_dt,
            hc.prncpal_dgns_cd, hc.icd_dgns_cd1, hc.icd_dgns_cd2, hc.icd_dgns_cd3,
            hc.icd_dgns_cd4, hc.icd_dgns_cd5, hc.icd_dgns_cd6, hc.icd_dgns_cd7, hc.icd_dgns_cd8,
            hc.icd_dgns_cd9, hc.icd_dgns_cd10, hc.icd_dgns_cd11, hc.icd_dgns_cd12, hc.icd_dgns_cd13,
            hc.icd_dgns_cd14, hc.icd_dgns_cd15, hc.icd_dgns_cd16, hc.icd_dgns_cd17, hc.icd_dgns_cd18,
            hc.icd_dgns_cd19, hc.icd_dgns_cd20, hc.icd_dgns_cd21, hc.icd_dgns_cd22, hc.icd_dgns_cd23,
            hc.icd_dgns_cd24, hc.icd_dgns_cd25, hc.icd_dgns_e_cd1, hc.icd_dgns_e_cd2, hc.icd_dgns_e_cd3,
            hc.icd_dgns_e_cd4, hc.icd_dgns_e_cd5, hc.icd_dgns_e_cd6, hc.icd_dgns_e_cd7, hc.icd_dgns_e_cd8,
            hc.icd_dgns_e_cd9, hc.icd_dgns_e_cd10, hc.icd_dgns_e_cd11, hc.icd_dgns_e_cd12, hc.fst_dgns_e_cd
          FROM ccw.hospice_claims hc
          ${gtClaimLine}
          ORDER BY hc.clm_id ASC
          limit :limit
      """;

  /** Query for Hospice Claim Lines. */
  public static final String HOSPICE_CLAIM_LINES_SAMHSA_QUERY =
      """
                SELECT hcl.clm_id,
                  hcl.hcpcs_cd
                FROM ccw.hospice_claim_lines hcl
                ${gtClaimLine}
                ORDER BY hcl.clm_id ASC
                limit :limit
          """;

  /** Query for HHA claims. */
  public static final String HHA_SAMHSA_QUERY =
      """
        SELECT hc.clm_id, hc.clm_from_dt, hc.clm_thru_dt,
            hc.prncpal_dgns_cd, hc.icd_dgns_cd1, hc.icd_dgns_cd2, hc.icd_dgns_cd3,
            hc.icd_dgns_cd4, hc.icd_dgns_cd5, hc.icd_dgns_cd6, hc.icd_dgns_cd7, hc.icd_dgns_cd8,
            hc.icd_dgns_cd9, hc.icd_dgns_cd10, hc.icd_dgns_cd11, hc.icd_dgns_cd12, hc.icd_dgns_cd13,
            hc.icd_dgns_cd14, hc.icd_dgns_cd15, hc.icd_dgns_cd16, hc.icd_dgns_cd17, hc.icd_dgns_cd18,
            hc.icd_dgns_cd19, hc.icd_dgns_cd20, hc.icd_dgns_cd21, hc.icd_dgns_cd22, hc.icd_dgns_cd23,
            hc.icd_dgns_cd24, hc.icd_dgns_cd25, hc.icd_dgns_e_cd1, hc.icd_dgns_e_cd2, hc.icd_dgns_e_cd3,
            hc.icd_dgns_e_cd4, hc.icd_dgns_e_cd5, hc.icd_dgns_e_cd6, hc.icd_dgns_e_cd7, hc.icd_dgns_e_cd8,
            hc.icd_dgns_e_cd9, hc.icd_dgns_e_cd10, hc.icd_dgns_e_cd11, hc.icd_dgns_e_cd12, hc.fst_dgns_e_cd
        FROM ccw.hha_claims hc
        ${gtClaimLine}
        ORDER BY hc.clm_id ASC
        limit :limit
     """;

  /** Query for HHA Claim Lines. */
  public static final String HHA_CLAIM_LINES_SAMHSA_QUERY =
      """
                SELECT hcl.clm_id,
                  hcl.hcpcs_cd, hcl.rev_cntr_apc_hipps_cd
                FROM ccw.hha_claim_lines hcl
                ${gtClaimLine}
                ORDER BY hcl.clm_id ASC
                limit :limit
          """;

  /** Query for SNF claims. */
  public static final String SNF_SAMHSA_QUERY =
      """
        SELECT sc.clm_id, sc.clm_from_dt, sc.clm_thru_dt,
            sc.clm_drg_cd, sc.icd_dgns_cd1, sc.icd_dgns_cd2, sc.icd_dgns_cd3,
            sc.icd_dgns_cd4, sc.icd_dgns_cd5, sc.icd_dgns_cd6, sc.icd_dgns_cd7, sc.icd_dgns_cd8,
            sc.icd_dgns_cd9, sc.icd_dgns_cd10, sc.icd_dgns_cd11, sc.icd_dgns_cd12, sc.icd_dgns_cd13,
            sc.icd_dgns_cd14, sc.icd_dgns_cd15, sc.icd_dgns_cd16, sc.icd_dgns_cd17, sc.icd_dgns_cd18,
            sc.icd_dgns_cd19, sc.icd_dgns_cd20, sc.icd_dgns_cd21, sc.icd_dgns_cd22, sc.icd_dgns_cd23,
            sc.icd_dgns_cd24, sc.icd_dgns_cd25, sc.icd_dgns_e_cd1, sc.icd_dgns_e_cd2, sc.icd_dgns_e_cd3,
            sc.icd_dgns_e_cd4, sc.icd_dgns_e_cd5, sc.icd_dgns_e_cd6, sc.icd_dgns_e_cd7, sc.icd_dgns_e_cd8,
            sc.icd_dgns_e_cd9, sc.icd_dgns_e_cd10, sc.icd_dgns_e_cd11, sc.icd_dgns_e_cd12, sc.fst_dgns_e_cd,
            sc.admtg_dgns_cd, sc.prncpal_dgns_cd, sc.icd_prcdr_cd1, sc.icd_prcdr_cd2, sc.icd_prcdr_cd3,
            sc.icd_prcdr_cd4, sc.icd_prcdr_cd5, sc.icd_prcdr_cd6, sc.icd_prcdr_cd7, sc.icd_prcdr_cd8,
            sc.icd_prcdr_cd9, sc.icd_prcdr_cd10, sc.icd_prcdr_cd11, sc.icd_prcdr_cd12, sc.icd_prcdr_cd13,
            sc.icd_prcdr_cd14, sc.icd_prcdr_cd15, sc.icd_prcdr_cd16, sc.icd_prcdr_cd17, sc.icd_prcdr_cd18,
            sc.icd_prcdr_cd19, sc.icd_prcdr_cd20, sc.icd_prcdr_cd21, sc.icd_prcdr_cd22, sc.icd_prcdr_cd23,
            sc.icd_prcdr_cd24, sc.icd_prcdr_cd25
        FROM ccw.snf_claims sc
        ${gtClaimLine}
        ORDER BY sc.clm_id ASC
        limit :limit
    """;

  /** Query for SNF Claim Lines. */
  public static final String SNF_CLAIM_LINES_SAMHSA_QUERY =
      """
                SELECT scl.clm_id,
                  scl.hcpcs_cd
                FROM ccw.snf_claim_lines scl
                ${gtClaimLine}
                ORDER BY scl.clm_id ASC
                limit :limit
          """;

  /** Query for Inpatient claims. */
  public static final String INPATIENT_SAMHSA_QUERY =
      """
       SELECT ic.clm_id, ic.clm_from_dt, ic.clm_thru_dt,
           ic.clm_drg_cd, ic.icd_dgns_cd1, ic.icd_dgns_cd2, ic.icd_dgns_cd3, ic.icd_dgns_cd4, ic.icd_dgns_cd5,
           ic.icd_dgns_cd6, ic.icd_dgns_cd7, ic.icd_dgns_cd8, ic.icd_dgns_cd9, ic.icd_dgns_cd10, ic.icd_dgns_cd11,
           ic.icd_dgns_cd12, ic.icd_dgns_cd13, ic.icd_dgns_cd14, ic.icd_dgns_cd15, ic.icd_dgns_cd16, ic.icd_dgns_cd17,
           ic.icd_dgns_cd18, ic.icd_dgns_cd19, ic.icd_dgns_cd20, ic.icd_dgns_cd21, ic.icd_dgns_cd22, ic.icd_dgns_cd23,
           ic.icd_dgns_cd24, ic.icd_dgns_cd25, ic.icd_dgns_e_cd1, ic.icd_dgns_e_cd2, ic.icd_dgns_e_cd3, ic.icd_dgns_e_cd4,
           ic.icd_dgns_e_cd5, ic.icd_dgns_e_cd6, ic.icd_dgns_e_cd7, ic.icd_dgns_e_cd8, ic.icd_dgns_e_cd9,
           ic.icd_dgns_e_cd10, ic.icd_dgns_e_cd11, ic.icd_dgns_e_cd12, ic.admtg_dgns_cd, ic.prncpal_dgns_cd,
           ic.fst_dgns_e_cd, ic.icd_prcdr_cd1, ic.icd_prcdr_cd2, ic.icd_prcdr_cd3, ic.icd_prcdr_cd4, ic.icd_prcdr_cd5,
           ic.icd_prcdr_cd6, ic.icd_prcdr_cd7, ic.icd_prcdr_cd8, ic.icd_prcdr_cd9, ic.icd_prcdr_cd10, ic.icd_prcdr_cd11,
           ic.icd_prcdr_cd12, ic.icd_prcdr_cd13, ic.icd_prcdr_cd14, ic.icd_prcdr_cd15, ic.icd_prcdr_cd16, ic.icd_prcdr_cd17,
           ic.icd_prcdr_cd18, ic.icd_prcdr_cd19, ic.icd_prcdr_cd20, ic.icd_prcdr_cd21, ic.icd_prcdr_cd22, ic.icd_prcdr_cd23,
           ic.icd_prcdr_cd24, ic.icd_prcdr_cd25
       FROM ccw.inpatient_claims ic
       ${gtClaimLine}
       ORDER BY ic.clm_id ASC
       limit :limit
    """;

  /** Query for Inpatient Claim Lines. */
  public static final String INPATIENT_CLAIM_LINES_SAMHSA_QUERY =
      """
                SELECT icl.clm_id,
                  icl.hcpcs_cd
                FROM ccw.inpatient_claim_lines icl
                ${gtClaimLine}
                ORDER BY icl.clm_id ASC
                limit :limit
          """;

  /** Query for Outpatient claims. */
  public static final String OUTPATIENT_SAMHSA_QUERY =
      """
       SELECT oc.clm_id, oc.clm_from_dt, oc.clm_thru_dt,
       oc.icd_dgns_cd1, oc.icd_dgns_cd2, oc.icd_dgns_cd3, oc.icd_dgns_cd4, oc.icd_dgns_cd5, oc.icd_dgns_cd6,
       oc.icd_dgns_cd7, oc.icd_dgns_cd8, oc.icd_dgns_cd9, oc.icd_dgns_cd10, oc.icd_dgns_cd11, oc.icd_dgns_cd12,
       oc.icd_dgns_cd13, oc.icd_dgns_cd14, oc.icd_dgns_cd15, oc.icd_dgns_cd16, oc.icd_dgns_cd17, oc.icd_dgns_cd18,
       oc.icd_dgns_cd19, oc.icd_dgns_cd20, oc.icd_dgns_cd21, oc.icd_dgns_cd22, oc.icd_dgns_cd23, oc.icd_dgns_cd24,
       oc.icd_dgns_cd25, oc.icd_dgns_e_cd1, oc.icd_dgns_e_cd2, oc.icd_dgns_e_cd3, oc.icd_dgns_e_cd4, oc.icd_dgns_e_cd5,
       oc.icd_dgns_e_cd6, oc.icd_dgns_e_cd7, oc.icd_dgns_e_cd8, oc.icd_dgns_e_cd9, oc.icd_dgns_e_cd10, oc.icd_dgns_e_cd11,
       oc.icd_dgns_e_cd12, oc.prncpal_dgns_cd, oc.fst_dgns_e_cd, oc.rsn_visit_cd1, oc.rsn_visit_cd2, oc.rsn_visit_cd3,
       oc.icd_prcdr_cd1, oc.icd_prcdr_cd2, oc.icd_prcdr_cd3, oc.icd_prcdr_cd4, oc.icd_prcdr_cd5, oc.icd_prcdr_cd6,
       oc.icd_prcdr_cd7, oc.icd_prcdr_cd8, oc.icd_prcdr_cd9, oc.icd_prcdr_cd10, oc.icd_prcdr_cd11, oc.icd_prcdr_cd12,
       oc.icd_prcdr_cd13, oc.icd_prcdr_cd14, oc.icd_prcdr_cd15, oc.icd_prcdr_cd16, oc.icd_prcdr_cd17, oc.icd_prcdr_cd18,
       oc.icd_prcdr_cd19, oc.icd_prcdr_cd20, oc.icd_prcdr_cd21, oc.icd_prcdr_cd22, oc.icd_prcdr_cd23, oc.icd_prcdr_cd24,
       oc.icd_prcdr_cd25
       FROM ccw.outpatient_claims oc
       ${gtClaimLine}
       ORDER BY oc.clm_id ASC
       limit :limit
    """;

  /** Query for Outpatient Claim Lines. */
  public static final String OUTPATIENT_CLAIM_LINES_SAMHSA_QUERY =
      """
                SELECT ocl.clm_id,
                  ocl.hcpcs_cd, ocl.rev_cntr_apc_hipps_cd
                FROM ccw.outpatient_claim_lines ocl
                ${gtClaimLine}
                ORDER BY ocl.clm_id ASC
                limit :limit
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
      "SELECT mc.idr_hdr_from_date_of_svc, mc.idr_hdr_to_date_of_svc from ${claimTable} where ${claimField} = :claimId ";
}
