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
--
-- NOTE: The final SQL is not as performant as a version using only PSQL;
--       Unfortunately, HSQL constraints precluded aspects of the PSQL query
--       so the RSLT variable had to be incremented in a sequenctial manner.
--
${logic.psql-only}  CREATE OR REPLACE FUNCTION synthea_load_pre_validate (
${logic.hsql-only}  CREATE FUNCTION synthea_load_pre_validate (
        p_beg_bene_id               bigint,
        p_end_bene_id               bigint,
        p_clm_id                    bigint,
        p_beg_clm_grp_id            bigint,
        p_pde_id_start              bigint,
        p_carr_clm_cntl_num_start   bigint,
        p_fi_doc_cntl_num_start     bigint,
        p_hicn_start                varchar(20),
        p_mbi_start                 varchar(20) )
        returns INTEGER
${logic.psql-only}  LANGUAGE plpgsql
${logic.psql-only}  AS $$
${logic.hsql-only}  READS SQL DATA
${logic.hsql-only}  BEGIN ATOMIC
                    DECLARE
                        -- end of the batch 1 relocated ids; should be nothing between the generated start and this
${logic.psql-only}      CLM_GRP_ID_END constant bigint := -99999831003;
                        -- cast p_fi_doc_cntl_num_start to a varchar, which is how it will be
                        -- defined in the various tables that include it.
${logic.psql-only}      FI_DOC_CNTL_NUM varchar := p_fi_doc_cntl_num_start::varchar;
${logic.psql-only}      RSLT integer := 0;

${logic.hsql-only}      CLM_GRP_ID_END bigint;
${logic.hsql-only}      DECLARE
${logic.hsql-only}      FI_DOC_CNTL_NUM varchar(20); 
${logic.hsql-only}      DECLARE
${logic.hsql-only}      RSLT integer;
${logic.hsql-only}      set CLM_GRP_ID_END = -99999831003;
${logic.hsql-only}      set FI_DOC_CNTL_NUM = CONVERT(p_fi_doc_cntl_num_start, SQL_VARCHAR);
${logic.hsql-only}      set RSLT = 0;

${logic.psql-only}  BEGIN
${logic.psql-only}      RSLT := (
${logic.hsql-only}      set RSLT = (
                            select count(bene_id)
                            from beneficiaries
                            where bene_id <= p_beg_bene_id and bene_id > p_end_bene_id
                            limit 1
                        );
                        
                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from carrier_claims
                                where clm_id <= p_clm_id
${logic.psql-only}			    and carr_clm_cntl_num::bigint <= p_carr_clm_cntl_num_start
${logic.hsql-only}			    and CONVERT(carr_clm_cntl_num, bigint) <= p_carr_clm_cntl_num_start
							    limit 1
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from carrier_claims
                                where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from inpatient_claims
                                where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            ); 
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from outpatient_claims
                                 where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from snf_claims
                                where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from dme_claims
                                where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from hha_claims
                                where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from hospice_claims
                                where clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select pde_id from partd_events
                                where pde_id <= p_pde_id_start
                                and clm_grp_id <= p_beg_clm_grp_id and clm_grp_id > CLM_GRP_ID_END
                                limit 1
                            );
                        END IF;

    -- look for hicn or mbi collisions...only need 1 collision to trigger a problem
                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count (bene_id) from beneficiaries
                                where (hicn_unhashed = p_hicn_start or mbi_num = p_mbi_start)
                                limit 1    
                            );
                        END IF;

    -- add support for fi_cntl_num here; not the most efficient step since
    -- fi_doc_clm_cntl_num is not an indexed field (therefore causing a
    -- full table scan).
                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from outpatient_claims
                                where fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
                                limit 1    
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from inpatient_claims
                                where fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
                                limit 1    
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from hha_claims
                                where fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
                                limit 1    
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from snf_claims
                                where fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
                                limit 1    
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(clm_id) from hospice_claims
                                where fi_doc_clm_cntl_num = FI_DOC_CNTL_NUM
                                limit 1    
                            );
                        END IF;

                        IF RSLT < 1 THEN
${logic.psql-only}          RSLT :=
${logic.hsql-only}          set RSLT =
                            (
                                select count(*) from
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
                                )
                            );
                        END IF;
                        RETURN rslt;
                    END;
${logic.psql-only}  $$;