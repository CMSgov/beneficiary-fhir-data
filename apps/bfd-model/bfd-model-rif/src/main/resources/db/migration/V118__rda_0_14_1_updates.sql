-- As part of the 0.14.1 RDA API update, new fields added

/*
* fiss_claims
*/
ALTER TABLE rda.fiss_claims ADD adm_typ_cd varchar(2);

/*
* fiss_revenue_lines
*/
ALTER TABLE rda.fiss_revenue_lines ADD ndc varchar(11);
ALTER TABLE rda.fiss_revenue_lines ADD ndc_qty varchar(11);
ALTER TABLE rda.fiss_revenue_lines ADD ndc_qty_qual varchar(2);

/*
* mcs_details
*/
ALTER TABLE rda.mcs_details ADD idr_dtl_ndc varchar(48);
ALTER TABLE rda.mcs_details ADD idr_dtl_ndc_unit_count varchar(15);
