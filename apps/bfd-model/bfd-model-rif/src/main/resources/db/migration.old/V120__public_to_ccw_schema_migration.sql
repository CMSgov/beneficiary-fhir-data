-- Create the schema
CREATE SCHEMA IF NOT EXISTS ccw;

-- Add the roles to the new schema
${logic.psql-only} DO $$
${logic.psql-only} BEGIN
${logic.psql-only} PERFORM add_reader_role_to_schema('bfd_reader_role', 'ccw');
${logic.psql-only} PERFORM add_writer_role_to_schema('bfd_writer_role', 'ccw');
${logic.psql-only} PERFORM add_migrator_role_to_schema('bfd_migrator_role', 'ccw');
${logic.psql-only} END
${logic.psql-only} $$ LANGUAGE plpgsql;

-- move the tables to the new schema
DROP TABLE IF EXISTS ccw.beneficiaries CASCADE;
ALTER TABLE public.beneficiaries
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.beneficiaries_history CASCADE;
ALTER TABLE public.beneficiaries_history
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.beneficiaries_history_invalid_beneficiaries CASCADE;
ALTER TABLE public.beneficiaries_history_invalid_beneficiaries
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.beneficiary_monthly CASCADE;
ALTER TABLE public.beneficiary_monthly
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.beneficiary_monthly_audit CASCADE;
ALTER TABLE public.beneficiary_monthly_audit
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.carrier_claim_lines CASCADE;
ALTER TABLE public.carrier_claim_lines
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.carrier_claims CASCADE;
ALTER TABLE public.carrier_claims
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.dme_claim_lines CASCADE;
ALTER TABLE public.dme_claim_lines
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.dme_claims CASCADE;
ALTER TABLE public.dme_claims
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.hha_claims CASCADE;
ALTER TABLE public.hha_claims
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.hha_claim_lines CASCADE;
ALTER TABLE public.hha_claim_lines
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.hospice_claim_lines CASCADE;
ALTER TABLE public.hospice_claim_lines
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.hospice_claims CASCADE;
ALTER TABLE public.hospice_claims
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.inpatient_claim_lines CASCADE;
ALTER TABLE public.inpatient_claim_lines
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.inpatient_claims CASCADE;
ALTER TABLE public.inpatient_claims
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.loaded_batches CASCADE;
ALTER TABLE public.loaded_batches
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.loaded_files CASCADE;
ALTER TABLE public.loaded_files
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.outpatient_claim_lines CASCADE;
ALTER TABLE public.outpatient_claim_lines
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.outpatient_claims CASCADE;
ALTER TABLE public.outpatient_claims
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.partd_events CASCADE;
ALTER TABLE public.partd_events
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.s3_data_files CASCADE;
ALTER TABLE public.s3_data_files
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.s3_manifest_files CASCADE;
ALTER TABLE public.s3_manifest_files
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.skipped_rif_records CASCADE;
ALTER TABLE public.skipped_rif_records
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.snf_claim_lines CASCADE;
ALTER TABLE public.snf_claim_lines
    SET SCHEMA ccw;
DROP TABLE IF EXISTS ccw.snf_claims CASCADE;
ALTER TABLE public.snf_claims
    SET SCHEMA ccw;

DROP FUNCTION IF EXISTS public.track_bene_monthly_change() CASCADE;
-- Some of the functions and triggers need to be rewritten to use the new schema
${logic.psql-only} CREATE OR REPLACE FUNCTION ccw.track_bene_monthly_change() RETURNS TRIGGER
${logic.psql-only} AS $beneficiary_monthly_audit$
${logic.psql-only}     BEGIN
${logic.psql-only}        IF (TG_OP = 'UPDATE') THEN
${logic.psql-only}            INSERT INTO ccw.beneficiary_monthly_audit VALUES (OLD.*, 'U', now(), nextval('ccw.bene_monthly_audit_seq'));
${logic.psql-only}            RETURN NEW;
${logic.psql-only}        END IF;
${logic.psql-only}        RETURN NULL; -- result is ignored since this is an AFTER trigger
${logic.psql-only}     END;
${logic.psql-only} $beneficiary_monthly_audit$ LANGUAGE plpgsql;

${logic.psql-only} DROP TRIGGER IF EXISTS audit_ccw_update ON ccw.beneficiary_monthly;

${logic.psql-only} CREATE TRIGGER audit_ccw_update
${logic.psql-only} AFTER UPDATE ON ccw.beneficiary_monthly
${logic.psql-only} FOR EACH ROW
${logic.psql-only}     WHEN ((
${logic.psql-only}         OLD.partd_contract_number_id <> '0' OR
${logic.psql-only}         OLD.partc_contract_number_id <> '0')
${logic.psql-only}     AND ((
${logic.psql-only}         OLD.partd_contract_number_id,
${logic.psql-only}         OLD.partc_contract_number_id,
${logic.psql-only}         OLD.medicare_status_code,
${logic.psql-only}         OLD.fips_state_cnty_code,
${logic.psql-only}         OLD.entitlement_buy_in_ind,
${logic.psql-only}         OLD.hmo_indicator_ind,
${logic.psql-only}         OLD.medicaid_dual_eligibility_code,
${logic.psql-only}         OLD.partd_pbp_number_id,
${logic.psql-only}         OLD.partd_segment_number_id,
${logic.psql-only}         OLD.partd_low_income_cost_share_group_code,
${logic.psql-only}         OLD.partc_pbp_number_id,
${logic.psql-only}         OLD.partc_plan_type_code)
${logic.psql-only}     IS DISTINCT FROM (
${logic.psql-only}         NEW.partd_contract_number_id,
${logic.psql-only}         NEW.partc_contract_number_id,
${logic.psql-only}         NEW.medicare_status_code,
${logic.psql-only}         NEW.fips_state_cnty_code,
${logic.psql-only}         NEW.entitlement_buy_in_ind,
${logic.psql-only}         NEW.hmo_indicator_ind,
${logic.psql-only}         NEW.medicaid_dual_eligibility_code,
${logic.psql-only}         NEW.partd_pbp_number_id,
${logic.psql-only}         NEW.partd_segment_number_id,
${logic.psql-only}         NEW.partd_low_income_cost_share_group_code,
${logic.psql-only}         NEW.partc_pbp_number_id,
${logic.psql-only}         NEW.partc_plan_type_code)) )
${logic.psql-only}     EXECUTE FUNCTION ccw.track_bene_monthly_change();

DROP FUNCTION IF EXISTS check_claims_mask(bigint);
${logic.psql-only} CREATE OR REPLACE FUNCTION ccw.check_claims_mask(v_bene_id bigint)
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
${logic.psql-only}   PERFORM 1 FROM ccw.carrier_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = V_CARRIER;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM ccw.inpatient_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_INPATIENT;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM ccw.outpatient_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_OUTPATIENT;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM ccw.snf_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_SNF;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM ccw.dme_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_DME;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM ccw.hha_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_HHA;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM ccw.hospice_claims WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_HOSPICE;
${logic.psql-only}   END IF;
${logic.psql-only}   PERFORM 1 FROM ccw.partd_events WHERE bene_id = v_bene_id limit 1;
${logic.psql-only}   IF FOUND THEN
${logic.psql-only}     v_rslt = v_rslt + V_PART_D;
${logic.psql-only}   END IF;
${logic.psql-only} RETURN v_rslt;
${logic.psql-only} END;
${logic.psql-only} $func$;

DROP FUNCTION IF EXISTS find_beneficiary(text, text);
CREATE OR REPLACE FUNCTION ccw.find_beneficiary(p_type text, p_value text)
RETURNS text
LANGUAGE plpgsql AS
$func$
DECLARE
v_rslt text;
	v_type text := lower(p_type);
BEGIN
	IF v_type = 'mbi' THEN
SELECT string_agg(distinct t1.bene_id::text, ',') INTO v_rslt
FROM
    (
        select distinct b.bene_id from ccw.beneficiaries_history b
        where b.mbi_num = p_value
        union
        select distinct a.bene_id from ccw.beneficiaries a
        where a.mbi_num = p_value
    ) t1;

ELSIF v_type = 'mbi-hash' THEN
SELECT string_agg(distinct t1.bene_id::text, ',') INTO v_rslt
FROM
    (
        select distinct b.bene_id from ccw.beneficiaries_history b
        where b.mbi_hash = p_value
        union
        select distinct a.bene_id from ccw.beneficiaries a
        where a.mbi_hash = p_value
    ) t1;

ELSIF v_type = 'hicn-hash' THEN
SELECT string_agg(distinct t1.bene_id::text, ',') INTO v_rslt
FROM
    (
        select distinct b.bene_id from ccw.beneficiaries_history b
        where b.bene_crnt_hic_num = p_value
        union
        select distinct a.bene_id from ccw.beneficiaries a
        where a.bene_crnt_hic_num = p_value
    ) t1;
END IF;
RETURN v_rslt;
END;
$func$;

-- Move the sequences to the new schema
DROP SEQUENCE IF EXISTS ccw.bene_monthly_audit_seq;
ALTER SEQUENCE public.bene_monthly_audit_seq
    SET SCHEMA ccw;
DROP SEQUENCE IF EXISTS ccw.beneficiaryhistory_beneficiaryhistoryid_seq;
ALTER SEQUENCE public.beneficiaryhistory_beneficiaryhistoryid_seq
    SET SCHEMA ccw;
DROP SEQUENCE IF EXISTS ccw.beneficiaryhistorytemp_beneficiaryhistoryid_seq;
ALTER SEQUENCE public.beneficiaryhistorytemp_beneficiaryhistoryid_seq
    SET SCHEMA ccw;
DROP SEQUENCE IF EXISTS ccw.loadedbatches_loadedbatchid_seq;
ALTER SEQUENCE public.loadedbatches_loadedbatchid_seq
    SET SCHEMA ccw;
DROP SEQUENCE IF EXISTS ccw.loadedfiles_loadedfileid_seq;
ALTER SEQUENCE public.loadedfiles_loadedfileid_seq
    SET SCHEMA ccw;
DROP SEQUENCE IF EXISTS ccw.s3_manifest_files_manifest_id_seq;
ALTER SEQUENCE public.s3_manifest_files_manifest_id_seq
    SET SCHEMA ccw;
DROP SEQUENCE IF EXISTS ccw.skipped_rif_records_record_id_seq;
ALTER SEQUENCE public.skipped_rif_records_record_id_seq
    SET SCHEMA ccw;
