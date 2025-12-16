ALTER TABLE idr.claim
ADD COLUMN prvdr_srvc_prvdr_npi_num VARCHAR(10);

ALTER TABLE idr.claim
ADD COLUMN prvdr_atndg_prvdr_npi_num VARCHAR(10);

ALTER TABLE idr.claim
ADD COLUMN prvdr_othr_prvdr_npi_num VARCHAR(10);

ALTER TABLE idr.claim
ADD COLUMN prvdr_rndrng_prvdr_npi_num VARCHAR(10);

ALTER TABLE idr.claim
ADD COLUMN prvdr_oprtg_prvdr_npi_num VARCHAR(10);

ALTER TABLE idr.claim_item
ADD COLUMN prvdr_rndrng_prvdr_npi_num VARCHAR(10);


