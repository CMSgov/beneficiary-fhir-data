-- BFD-3073
-- Support BENE_ID lookup using one of a supported way to search for a BENE_ID; the search
-- will look in both the BENEFICIARIES and BENEFICIARIES_HISTROY tables. The caller provides
-- both a search type and value.
--
-- Supported search types are: 'mbi', 'mbi-hash', 'hicn-hash'.
--
${logic.psql-only}  CREATE OR REPLACE FUNCTION find_beneficiary(p_type text, p_value text)
${logic.psql-only}  RETURNS text
${logic.psql-only}  LANGUAGE plpgsql AS
${logic.psql-only}  $func$
${logic.psql-only}  DECLARE
${logic.psql-only}  	v_rslt           text;
${logic.psql-only}  	v_type           text   := lower(p_type);
${logic.psql-only}  BEGIN
${logic.psql-only}  	IF v_type = 'mbi' THEN
${logic.psql-only}  		SELECT string_agg(a.bene_id::text, ',') INTO v_rslt
${logic.psql-only}  		FROM beneficiaries a,
${logic.psql-only}  		(
${logic.psql-only}  			select distinct b.bene_id from beneficiaries_history b
${logic.psql-only}  			where b.mbi_num = p_value
${logic.psql-only}  		) t1
${logic.psql-only}  		where a.mbi_num = p_value
${logic.psql-only}  		or a.bene_id = t1.bene_id;
${logic.psql-only}  	
${logic.psql-only}  	ELSIF v_type = 'mbi-hash' THEN
${logic.psql-only}  		SELECT string_agg(a.bene_id::text, ',') INTO v_rslt
${logic.psql-only}  		FROM beneficiaries a,
${logic.psql-only}  		(
${logic.psql-only}  			select distinct b.bene_id from beneficiaries_history b
${logic.psql-only}  			where b.mbi_hash = p_value
${logic.psql-only}  		) t1
${logic.psql-only}  		where a.mbi_hash = p_value
${logic.psql-only}  		or a.bene_id = t1.bene_id;
${logic.psql-only}  	
${logic.psql-only}  	ELSIF v_type = 'hicn-hash' THEN
${logic.psql-only}  		SELECT string_agg(a.bene_id::text, ',') INTO v_rslt
${logic.psql-only}  		FROM beneficiaries a,
${logic.psql-only}  		(
${logic.psql-only}  			select distinct b.bene_id from beneficiaries_history b
${logic.psql-only}  			where b.bene_crnt_hic_num = p_value
${logic.psql-only}  		) t1
${logic.psql-only}  		where a.bene_crnt_hic_num = p_value
${logic.psql-only}  		or a.bene_id = t1.bene_id;
${logic.psql-only}  	END IF;
${logic.psql-only}  	RETURN v_rslt;
${logic.psql-only}  END;
${logic.psql-only}  $func$;