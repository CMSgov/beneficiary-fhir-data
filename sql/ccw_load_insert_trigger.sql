CREATE TRIGGER audit_ccw_insert
AFTER INSERT ON "BeneficiaryMonthly"
FOR EACH ROW			
	EXECUTE FUNCTION track_bene_monthly_change();