-- Function that takes in data values derived from the Synthea
-- end_state.properties file, and performs a validation check to
-- determine if the the pipeline can go ahead with processing
-- the associated RIF files or not.
--
-- Basically it performs range-checking on all the claims tables
-- and beneficiary-related tables. The function returns an integer
-- value that denotes an OK to proceed (value zero) or not OK to
-- proceed (value > 0). It does this mainly by getting counts of
-- rows for various tables and summing the counts; a value of zero
-- denotes no potential duplicate rows.
--
-- The Synthea manifest holds end-state information like the following:
-- ====================================================================
--  <endStateProperties>
--    <numGenerated>1000</numGenerated>
--    <generated>Tue Oct 18 15:24:11 EDT 2022</generated>
--    <clm_grp_id_start>-100001170</clm_grp_id_start>
--    <pde_id_start>-100000623</pde_id_start>
--    <carr_clm_cntl_num_start>-100000388</carr_clm_cntl_num_start>
--    <fi_doc_cntl_num_start>-100000159</fi_doc_cntl_num_start>
--    <hicn_start>T01000010A</hicn_start>
--    <bene_id_start>-1000010</bene_id_start>
--    <clm_id_start>-100000547</clm_id_start>
--    <mbi_start>1S00E00AA10</mbi_start>
--  </endStateProperties>
-- ====================================================================
-- so to test this function, it could be invoked using positional
-- notation like this:
--
-- SELECT synthea_load_pre_validate(
--    -1000010, -1000020, -100000547, -100001170, -100000623, -100000388,
--    -100000159, 'T01000010A', '1S00E00AA10');
--
-- or using named notation like this:
--
-- SELECT synthea_load_pre_validate(
--    p_beg_bene_id => -10000010050073,
--    p_beg_bene_id => -10000010050083,
--    p_clm_id => -10000920860670,
--    p_beg_clm_grp_id => -1617752175,
--    p_pde_id_start => -10596891505,
--    p_carr_clm_cntl_num_start => -694670632,
--    p_fi_doc_cntl_num_start => 423014560,
--    p_hicn_start => 'T11100073A',
--    p_mbi_start => '1S00EU2GA73');

CREATE OR REPLACE FUNCTION synthea_load_pre_validate (
    p_beg_bene_id               bigint,
    p_end_bene_id               bigint,
    p_clm_id                    bigint,
    p_beg_clm_grp_id            bigint,
    p_pde_id_start              bigint,
    p_carr_clm_cntl_num_start   bigint,
    p_fi_doc_cntl_num_start     bigint,
    p_hicn_start                text,
    p_mbi_start                 text )
returns INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    -- end of the batch 1 relocated ids; should be nothing between the generated start and this
    CLM_GRP_ID_END constant bigint := -99999831003;

    -- cast p_fi_doc_cntl_num_start to a varchar, which is how it will be
    -- defined in the various tables that include it.
    FI_DOC_CNTL_NUM varchar := p_fi_doc_cntl_num_start::varchar;
    
    RSLT integer := 0;

BEGIN    
    select sum(v2.cnt) into RSLT
    from
        (
        select '01_beneficiaries', count(v1.bene_id) as "cnt"
        from (
            select bene_id from beneficiaries
            where bene_id <= p_beg_bene_id and bene_id > p_end_bene_id
            limit 1
        ) v1
        union
        select '02_carrier_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from carrier_claims
            where clm_id <= p_clm_id
            and carr_clm_cntl_num::bigint <= p_carr_clm_cntl_num_start
            limit 1
        ) v1
        union
        select '03_carrier_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from carrier_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '04_inpatient_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from inpatient_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '05_outpatient_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from outpatient_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '06_snf_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from snf_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '07_dme_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from dme_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '08_hha_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from hha_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '09_hospice_claims', count(v1.clm_id) as "cnt"
        from (
            select clm_id from hospice_claims
            where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        select '10_partd_events', count(v1.pde_id) as "cnt"
        from (
            select pde_id from partd_events
            where pde_id <= p_pde_id_start
            and clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
            limit 1
        ) v1
        union
        -- look for hicn or mbi collisions...only need 1 collision to trigger a problem
        select '11_bene_hicn_mbi', count(v1.bene_id)
        from (
            select a.bene_id from beneficiaries a
            where (a.hicn_unhashed = p_hicn_start or a.mbi_num = p_mbi_start)
            limit 1    
        ) v1
        union
        --
        -- add support for fi_cntl_num here; not the most efficient step since
        -- fi_doc_clm_cntl_num is not an indexed field (therefore causing a
        -- full table scan). This could be refactored as its own function that
        -- allows a 'fail fast' operational model thereby bypassing any further
        -- checking of subsequent table(s). 
        --
        select '12a_fi_num_outpatient', count(v1.clm_id)
        from (
            select a.clm_id from outpatient_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12b_fi_num_inpatient', count(v1.clm_id)
        from (
            select a.clm_id from inpatient_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12c_fi_num_hha', count(v1.clm_id)
        from (
            select a.clm_id from hha_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12d_fi_num_snf', count(v1.clm_id)
        from (
            select a.clm_id from snf_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        select '12e_fi_num_hospice', count(v1.clm_id)
        from (
            select a.clm_id from hospice_claims a
            where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
            limit 1    
        ) v1
        union
        -- this also is a candidate for a 'fail fast' function!
        select '13_dupl_bene_id', v3.cnt
        from (
            select count(*) as "cnt" from
            (
              select count(*) bene_id_count from (
                select bene_id, mbi_num 
                from public.beneficiaries 
                where bene_id < 0 and mbi_num IS NOT NULL
               union 
                select distinct bene_id, mbi_num 
                from public.beneficiaries_history 
                where bene_id < 0 and mbi_num IS NOT NULL 
               union 
                select distinct bene_id, mbi_num 
                from public.medicare_beneficiaryid_history 
                where bene_id < 0 and mbi_num IS NOT NULL
              ) as foo 
              group by mbi_num 
              having count(*) > 1
            ) v1
        ) v3
        where v3.cnt > 1    -- account for duplicate in db (on purpose) for testing 
    ) v2;
    RETURN rslt;
END;
$$;