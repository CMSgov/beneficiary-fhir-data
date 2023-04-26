-- BFD-2549
-- SETUP_CLAIMS_AVAILABILITY_FUNCTION.SQL
-- flyway migration to reate a db function that takes a bene_id as an input parameter,
-- and returns a bit mask denoting available claims data for that beneficiary. This
-- function will be invoked during ExplanationOfBenefitResourceProvider (STU3, R4) to
-- improve performance, especially for the case(s) when there are no claims wahtsoever
-- for a given Claim type for the Beneficiary.
--
${logic.psql-only} CREATE OR REPLACE FUNCTION check_claims_mask(v_bene_id bigint)
${logic.psql-only} RETURNS integer
${logic.psql-only} LANGUAGE plpgsql AS
${logic.psql-only} $func$
${logic.psql-only} DECLARE
${logic.psql-only}    v_rslt           integer  := 0;       
${logic.psql-only}                                     -- Java definitions
${logic.psql-only}    V_CARRIER        integer  := 1;    -- public static final int V_CARRIER_HAS_DATA     = (1 << 0);
${logic.psql-only}    V_INPATIENT      integer  := 2;    -- public static final int V_INPATIENT_HAS_DATA   = (1 << 1);
${logic.psql-only}    V_OUTPATIENT     integer  := 4;    -- public static final int V_OUTPATIENT_HAS_DATA  = (1 << 2);
${logic.psql-only}    V_SNF            integer  := 8;    -- public static final int V_SNF_HAS_DATA         = (1 << 3);
${logic.psql-only}    V_DME            integer  := 16;   -- public static final int V_DME_HAS_DATA         = (1 << 4);
${logic.psql-only}    V_HHA            integer  := 32;   -- public static final int V_HHA_HAS_DATA         = (1 << 5);
${logic.psql-only}    V_HOSPICE        integer  := 64;   -- public static final int V_HOSPICE_HAS_DATA     = (1 << 6);
${logic.psql-only}    V_PART_D         integer  := 128;  -- public static final int V_PART_D_HAS_DATA      = (1 << 7);
${logic.psql-only} BEGIN
${logic.psql-only}   PERFORM 1 FROM carrier_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = V_CARRIER;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM inpatient_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_INPATIENT;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM outpatient_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_OUTPATIENT;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM snf_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_SNF;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM dme_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_DME;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM hha_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_HHA;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM hospice_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_HOSPICE;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM partd_events WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_PART_D;
${logic.psql-only}   END IF;                 
 
-- Uncomment following line to enable HSQL detail logging during a migration
-- ${logic.hsql-only} set database event log level 4 
 
${logic.hsql-only} CREATE FUNCTION check_claims_mask(IN v_bene_id bigint)   
${logic.hsql-only} RETURNS integer              
${logic.hsql-only} READS SQL DATA
${logic.hsql-only} BEGIN ATOMIC
${logic.hsql-only}     DECLARE v_rslt           integer DEFAULT 0;
${logic.hsql-only}     DECLARE v_cnt            integer DEFAULT 0;         
${logic.hsql-only}     DECLARE V_CARRIER        integer DEFAULT 1;
${logic.hsql-only}     DECLARE V_INPATIENT      integer DEFAULT 2;
${logic.hsql-only}     DECLARE V_OUTPATIENT     integer DEFAULT 4;
${logic.hsql-only}     DECLARE V_SNF            integer DEFAULT 8;
${logic.hsql-only}     DECLARE V_DME            integer DEFAULT 16;
${logic.hsql-only}     DECLARE V_HHA            integer DEFAULT 32;
${logic.hsql-only}     DECLARE V_HOSPICE        integer DEFAULT 64;
${logic.hsql-only}     DECLARE V_PART_D         integer DEFAULT 128;

${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM carrier_claims WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = V_CARRIER;
${logic.hsql-only}   END IF;
${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM inpatient_claims WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = v_rslt + V_INPATIENT;
${logic.hsql-only}   END IF;
${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM outpatient_claims WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = v_rslt + V_OUTPATIENT;
${logic.hsql-only}   END IF;
${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM snf_claims WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = v_rslt + V_SNF;
${logic.hsql-only}   END IF;
${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM dme_claims WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = v_rslt + V_DME;
${logic.hsql-only}   END IF;
${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM hha_claims WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = v_rslt + V_HHA;
${logic.hsql-only}   END IF;
${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM hospice_claims WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = v_rslt + V_HOSPICE;
${logic.hsql-only}   END IF;
${logic.hsql-only}   SELECT COUNT(BENE_ID) INTO v_cnt FROM partd_events WHERE bene_id = v_bene_id;
${logic.hsql-only}   IF v_cnt > 0 THEN
${logic.hsql-only}     SET v_rslt = v_rslt + V_PART_D;
${logic.hsql-only}   END IF;

                     RETURN v_rslt;
END;
${logic.psql-only} $func$;
