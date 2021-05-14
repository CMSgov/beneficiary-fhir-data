CREATE TRIGGER audit_ccw_delete
AFTER DELETE ON "BeneficiaryMonthly"
FOR EACH ROW			
	EXECUTE FUNCTION track_bene_monthly_change();