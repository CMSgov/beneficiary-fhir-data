-- BFD-3073
-- Function to derive a Beneficiary's BENE_ID from either:
-- mbi
-- mbi-hash
-- hicn-hash
--
-- Since MBI_NUM can change over time (i.e., a beneficiary is re-issued
-- a new MBI), this function searches both the current as well as
-- historical data.
--   p_type  - meta identifier for the type of data encapsulated in
--             the p_value. Valid values: mbi, mbi-hash, hicn-hash
--   p_value - value to use when performing the lookup.
--
-- Returns a text array of BENE_ID values; in theory this should only
-- contain a single value, but both mbi-hash and hicn-hash values have
-- hash collisions (duplicates) in our db.
--
-- When BFD sunsets support for mbi-hash and/or hicn-hash lookups, this
-- function should be changed to remove mbi-hash and hicn-hash support
-- and return a single bigint value for the mbi lookup. The necessity
-- to support an array of values is solely a function of handling any
-- hash collisions.

CREATE OR REPLACE FUNCTION find_beneficiary(p_type text, p_value text)
RETURNS text
LANGUAGE plpgsql AS
$func$
DECLARE
	v_rslt           text;
	v_type           text   := lower(p_type);
BEGIN
	IF v_type = 'mbi' THEN
        SELECT string_agg(distinct t1.bene_id::text, ',') INTO v_rslt
        FROM
        (
            select distinct b.bene_id from beneficiaries_history b
            where b.mbi_num = p_value
            union
            select distinct a.bene_id from beneficiaries a
            where a.mbi_num = p_value
        ) t1;
	
	ELSIF v_type = 'mbi-hash' THEN
        SELECT string_agg(distinct t1.bene_id::text, ',') INTO v_rslt
        FROM
        (
            select distinct b.bene_id from beneficiaries_history b
            where b.mbi_hash = p_value
            union
            select distinct a.bene_id from beneficiaries a
            where a.mbi_hash = p_value
        ) t1;
	
	ELSIF v_type = 'hicn-hash' THEN
        SELECT string_agg(distinct t1.bene_id::text, ',') INTO v_rslt
        FROM
        (
            select distinct b.bene_id from beneficiaries_history b
            where b.bene_crnt_hic_num = p_value
            union
            select distinct a.bene_id from beneficiaries a
            where a.bene_crnt_hic_num = p_value
        ) t1;
	END IF;
	RETURN v_rslt;
END;
$func$;
