DROP PROCEDURE IF EXISTS public.update_bene_monthly(
	character varying, date, character varying, character varying, character, character,
	character varying, character varying, character varying, character varying,
	character varying, character varying, character, character varying, character varying);
	
DROP PROCEDURE IF EXISTS public.load_from_ccw();

DROP TRIGGER IF EXISTS audit_ccw_insert ON public."BeneficiaryMonthly";
DROP TRIGGER IF EXISTS audit_ccw_update ON public."BeneficiaryMonthly";
DROP TRIGGER IF EXISTS audit_ccw_delete ON public."BeneficiaryMonthly";
DROP FUNCTION IF EXISTS public.track_bene_monthly_change();

-- ********************************************************
-- Don't drop the public.beneficiary_monthly_audit table
-- It will be needed to perform any analysis / remediation
-- ********************************************************
-- DROP TABLE IF EXISTS public.beneficiary_monthly_audit;