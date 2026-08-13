ALTER TABLE idr.claim_institutional_nch
ADD COLUMN prvdr_atndg_prvdr_npi_type integer,
ADD COLUMN prvdr_rfrg_prvdr_npi_type integer,
ADD COLUMN prvdr_othr_prvdr_npi_type integer,
ADD COLUMN prvdr_oprtg_prvdr_npi_type integer,
ADD COLUMN prvdr_rndrg_prvdr_npi_type integer,
ADD COLUMN prvdr_srvc_prvdr_npi_type integer,
ADD COLUMN prvdr_blg_prvdr_npi_type integer;

ALTER TABLE idr.claim_institutional_ss
ADD COLUMN prvdr_atndg_prvdr_npi_type integer,
ADD COLUMN prvdr_rfrg_prvdr_npi_type integer,
ADD COLUMN prvdr_othr_prvdr_npi_type integer,
ADD COLUMN prvdr_oprtg_prvdr_npi_type integer,
ADD COLUMN prvdr_rndrg_prvdr_npi_type integer,
ADD COLUMN prvdr_blg_prvdr_npi_type integer;

ALTER TABLE idr.claim_professional_nch
ADD COLUMN prvdr_blg_prvdr_npi_type integer,
ADD COLUMN prvdr_rfrg_prvdr_npi_type integer,
ADD COLUMN prvdr_srvc_prvdr_npi_type integer;

ALTER TABLE idr.claim_professional_ss
ADD COLUMN prvdr_blg_prvdr_npi_type integer,
ADD COLUMN prvdr_rfrg_prvdr_npi_type integer,
ADD COLUMN prvdr_othr_prvdr_npi_type integer;

ALTER TABLE idr.claim_item_institutional_nch
ADD COLUMN prvdr_rndrg_prvdr_npi_type integer;

ALTER TABLE idr.claim_item_institutional_ss
ADD COLUMN prvdr_rndrg_prvdr_npi_type integer;

ALTER TABLE idr.claim_item_professional_nch
ADD COLUMN prvdr_rndrg_prvdr_npi_type integer;

ALTER TABLE idr.claim_item_professional_ss
ADD COLUMN prvdr_rndrg_prvdr_npi_type integer;

ALTER TABLE idr.claim_rx
ADD COLUMN prvdr_srvc_prvdr_npi_type integer,
ADD COLUMN prvdr_prscrbng_prvdr_npi_type integer;