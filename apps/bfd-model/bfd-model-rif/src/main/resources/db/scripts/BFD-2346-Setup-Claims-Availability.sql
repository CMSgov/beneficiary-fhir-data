CREATE OR REPLACE FUNCTION check_claims_mask(v_bene_id bigint)
  RETURNS integer
  LANGUAGE plpgsql AS
$func$
DECLARE
	v_rslt		     integer  := 0;
/*
000000000 :  0    : nada
000000001 :  1    : beneficiaries
000000010 :  2    : carrier claims
000000100 :  4    : inpatient
000001000 :  8    : outpatient
000010000 :  16   : SNF
000100000 :  32   : DME
001000000 :  64   : HHA
010000000 : 128   : Hospice
100000000 : 256   : Part D
*/
                                       -- Java definitions
	V_BENEFICIARY    integer  := 1;    -- public static final int V_BENEFICIARY = (1 << 1);
	V_CARRIER        integer  := 2;    -- public static final int V_CARRIER     = (1 << 2);
	V_INPATIENT      integer  := 4;    -- public static final int V_INPATIENT   = (1 << 3);
	V_OUTPATIENT     integer  := 8;    -- public static final int V_OUTPATIENT  = (1 << 4);
	V_SNF            integer  := 16;   -- public static final int V_SNF         = (1 << 5);
	V_DME            integer  := 32;   -- public static final int V_DME         = (1 << 6);
	V_HHA            integer  := 64;   -- public static final int V_HHA         = (1 << 7);
	V_HOSPICE        integer  := 128;  -- public static final int V_HOSPICE     = (1 << 8);
	V_PART_D         integer  := 256;  -- public static final int V_PART_D      = (1 << 9);
BEGIN
	PERFORM 1 FROM beneficiaries WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = V_BENEFICIARY;
	ELSE
		return v_rslt;
	END IF;
	PERFORM 1 FROM carrier_claims WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_CARRIER;
	END IF;
	PERFORM 1 FROM inpatient_claims WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_INPATIENT;
	END IF;
	PERFORM 1 FROM outpatient_claims WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_OUTPATIENT;
	END IF;
	PERFORM 1 FROM snf_claims WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_SNF;
	END IF;
	PERFORM 1 FROM dme_claims WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_DME;
	END IF;
	PERFORM 1 FROM hha_claims WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_HHA;
	END IF;
	PERFORM 1 FROM hospice_claims WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_HOSPICE;
	END IF;
	PERFORM 1 FROM partd_events WHERE bene_id = v_bene_id limit 1;
	IF FOUND THEN
		v_rslt = v_rslt # V_PART_D;
	END IF;
	RETURN v_rslt;
END
$func$;
