-- BFD-2549
-- SETUP_CLAIMS_AVAILABILITY_FUNCTION.SQL
-- flyway migration to reate a db function that takes a bene_id as an input parameter,
-- and returns a bit mask denoting available claims data for that beneficiary. This
-- function will be invoked during ExplanationOfBenefitResourceProvider (STU3, R4) to
-- improve performance, especially for the case(s) when there are no claims wahtsoever
-- for a given Claim type for the Beneficiary.
--
-- NOTE: HSQL does not support mathematical 'bitwise &' functionality; thus when computing
--       the bitmask result, we'll simply perform additive functions to derive the same.
--       PSQL does support support mathematical 'bitwise &' and will therefore be used.
--
${logic.hsql-only} CREATE 
${logic.psql-only} CREATE OR REPLACE 
FUNCTION check_claims_mask(v_bene_id bigint)
RETURNS integer
${logic.psql-only} LANGUAGE plpgsql AS
${logic.psql-only} $func$
DECLARE
	v_rslt           integer  := 0;
	v_cnt            integer  := 0;         
                                       -- Java definitions
	V_CARRIER        integer  := 1;    -- public static final int V_CARRIER_HAS_DATA     = (1 << 0);
	V_INPATIENT      integer  := 2;    -- public static final int V_INPATIENT_HAS_DATA   = (1 << 1);
	V_OUTPATIENT     integer  := 4;    -- public static final int V_OUTPATIENT_HAS_DATA  = (1 << 2);
	V_SNF            integer  := 8;    -- public static final int V_SNF_HAS_DATA         = (1 << 3);
	V_DME            integer  := 16;   -- public static final int V_DME_HAS_DATA         = (1 << 4);
	V_HHA            integer  := 32;   -- public static final int V_HHA_HAS_DATA         = (1 << 5);
	V_HOSPICE        integer  := 64;   -- public static final int V_HOSPICE_HAS_DATA     = (1 << 6);
	V_PART_D         integer  := 128;  -- public static final int V_PART_D_HAS_DATA      = (1 << 7);
BEGIN
	SELECT COUNT(BENE_ID) INTO v_cnt FROM carrier_claims WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
		v_rslt = V_CARRIER;
	END IF;
	SELECT COUNT(BENE_ID) INTO v_cnt  FROM inpatient_claims WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
${logic.hsql-only} v_rslt = v_rslt + V_INPATIENT;
${logic.psql-only} v_rslt = v_rslt # V_INPATIENT;
	END IF;
	SELECT COUNT(BENE_ID) INTO v_cnt  FROM outpatient_claims WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
${logic.hsql-only} v_rslt = v_rslt + V_OUTPATIENT;
${logic.psql-only} v_rslt = v_rslt # V_OUTPATIENT;
	END IF;
	SELECT COUNT(BENE_ID) INTO v_cnt  FROM snf_claims WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
${logic.hsql-only} v_rslt = v_rslt + V_SNF;
${logic.psql-only} v_rslt = v_rslt # V_SNF;
	END IF;
	SELECT COUNT(BENE_ID) INTO v_cnt  FROM dme_claims WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
${logic.hsql-only} v_rslt = v_rslt + V_DME;
${logic.psql-only} v_rslt = v_rslt # V_DME;
	END IF;
	SELECT COUNT(BENE_ID) INTO v_cnt  FROM hha_claims WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
${logic.hsql-only} v_rslt = v_rslt + V_HHA;
${logic.psql-only} v_rslt = v_rslt # V_HHA;
	END IF;
	SELECT COUNT(BENE_ID) INTO v_cnt  FROM hospice_claims WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
		v_rslt = v_rslt # V_HOSPICE;
${logic.hsql-only} v_rslt = v_rslt + V_HOSPICE;
${logic.psql-only} v_rslt = v_rslt # V_HOSPICE;
	END IF;
	SELECT COUNT(BENE_ID) INTO v_cnt  FROM partd_events WHERE bene_id = v_bene_id limit 1;
	IF v_cnt > 0 THEN
${logic.hsql-only} v_rslt = v_rslt + V_PART_D;
${logic.psql-only} v_rslt = v_rslt # V_PART_D;
	END IF;
	RETURN v_rslt;
END
${logic.psql-only} $func$;
