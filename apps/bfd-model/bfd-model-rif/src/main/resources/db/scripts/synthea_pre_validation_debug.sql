-- This SQL mimics the function SYNTHEA_LOAD_PRE_VALIDATE, but does so
-- by breaking apart the large 'union' of values into individual named
-- values so one can see when/why a Synthea pre-valiation may have failed.
--
SET max_parallel_workers = 24;
SET max_parallel_workers_per_gather = 20;
SET parallel_leader_participation = off;
SET parallel_tuple_cost = 0;
SET parallel_setup_cost = 0;
SET min_parallel_table_scan_size = 0;
--
-- EXAMPLE end_state.properties from Synthea run
-- =============================================
-- #BFD Properties End State
-- #Fri Oct 14 12:28:32 EDT 2022
-- exporter.bfd.clm_grp_id_start=-1617752175
-- exporter.bfd.pde_id_start=-10596891505
-- exporter.bfd.carr_clm_cntl_num_start=-694670632
-- exporter.bfd.fi_doc_cntl_num_start=-423014560
-- exporter.bfd.hicn_start=T11100073A
-- exporter.bfd.bene_id_start=-10000010050073
-- exporter.bfd.clm_id_start=-10000920860670
-- exporter.bfd.mbi_start=1S00EU2GA73
--
DO
$$
DECLARE
--
-- fill these in with values from the end_state.properties file
-- for p_num_benes, enter a value for # ob ene(s) that were to be created
--
    p_num_benes                 int         := 10;
    p_beg_bene_id               bigint      := -33333333333333;
    p_clm_id                    bigint      := -33333333333333;
    p_beg_clm_grp_id            bigint      := -33333333333333;
    p_pde_id_start              bigint      := -33333333333333;
    p_carr_clm_cntl_num_start   bigint      := -33333333333333;
    p_fi_doc_cntl_num_start     bigint      := -33333333333333;
    p_hicn_start                text        := 'replace this';
    p_mbi_start                 text        := 'replace this';
    
    -- we'll calculate then end bene_id by subtracting the p_num_benes
    -- from the p_beg_bene_id to derive its value.
    p_end_bene_id           bigint   := p_beg_bene_id - p_num_benes;
    
    -- end of the batch 1 relocated ids; should be nothing between the generated start and this
    CLM_GRP_ID_END constant bigint   := -99999831003;

    -- cast p_fi_doc_cntl_num_start to a varchar, which is how it will be
    -- defined in the various tables that include it.
    FI_DOC_CNTL_NUM         varchar  := p_fi_doc_cntl_num_start::varchar;
    
    -- some variables we'll
    v_ix int  := 1;
    v_cnt int := 0;
    
    QUERY_ID TEXT [] := '{
        "01_beneficiaries     ", "02_carrier_claims    ", "03_carrier_claims    ", "04_inpatient_claims  ",
        "05_outpatient_claims ", "06_snf_claims        ", "07_dme_claims        ", "08_hha_claims        ",
        "09_hospice_claims    ", "10_partd_events      ", "11_bene_hicn_mbi     ", "12a_fi_num_outpatient",
        "12b_fi_num_inpatient ", "12c_fi_num_hha       ", "12d_fi_num_snf       ", "12e_fi_num_hospice   ",
        "13_dupl_bene_id      "
    }';
    
    QUERY_CNT INT[] := '{ 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }';
    
BEGIN
   select count(v1.bene_id) into v_cnt
   from (
        select bene_id from beneficiaries
        where bene_id <= p_beg_bene_id and bene_id > p_end_bene_id
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--    
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from carrier_claims
        where clm_id <= p_clm_id
        and carr_clm_cntl_num::bigint <= p_carr_clm_cntl_num_start
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--    
--    
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from carrier_claims
        where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--    
--
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from inpatient_claims
        where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--    
--
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from outpatient_claims
        where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--    
--
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from snf_claims
        where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from dme_claims
        where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from hha_claims
        where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    select count(v1.clm_id) into v_cnt
    from (
        select clm_id from hospice_claims
        where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    select count(v1.pde_id) into v_cnt
    from (
        select pde_id from partd_events
        where pde_id <= p_pde_id_start
        and clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
        limit 1
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    -- look for hicn or mbi collisions...only need 1 collision to trigger a problem
    select count(v1.bene_id) into v_cnt
    from (
        select a.bene_id from beneficiaries a
        where (a.hicn_unhashed = p_hicn_start or a.mbi_num = p_mbi_start)
        limit 1    
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    -- add support for fi_cntl_num here; not the most efficient step since
    -- fi_doc_clm_cntl_num is not an indexed field (therefore causing a
    -- full table scan). This could be refactored as its own function that
    -- allows a 'fail fast' operational model thereby bypassing any further
    -- checking of subsequent table(s). 
    --
    select count(v1.clm_id) into v_cnt
    from (
        select a.clm_id from outpatient_claims a
        where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
        limit 1    
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--    
--
    select count(v1.clm_id) into v_cnt
    from (
        select a.clm_id from inpatient_claims a
        where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
        limit 1    
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    select count(v1.clm_id) into v_cnt
    from (
        select a.clm_id from hha_claims a
        where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
        limit 1    
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    select count(v1.clm_id) into v_cnt
    from (
        select a.clm_id from snf_claims a
        where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
        limit 1    
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    select count(v1.clm_id) into v_cnt
    from (
        select a.clm_id from hospice_claims a
        where a.fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
        limit 1    
    ) v1;
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
--
    -- this also is a candidate for a 'fail fast' function!
    select v3.cnt into v_cnt
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
    where v3.cnt > 1; 
    QUERY_CNT[v_ix] = v_cnt; v_ix := v_ix + 1;
--
-- Now show our results
--
  v_ix := 0;
  FOREACH v_cnt IN ARRAY QUERY_CNT
   LOOP
      v_ix := v_ix + 1;
      RAISE NOTICE '% : %', QUERY_ID[v_ix], v_cnt;
   END LOOP;

END;
$$ LANGUAGE plpgsql;