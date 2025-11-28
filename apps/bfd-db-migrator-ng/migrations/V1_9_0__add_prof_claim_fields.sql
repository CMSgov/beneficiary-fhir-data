ALTER TABLE idr.claim
ADD COLUMN clm_rfrg_prvdr_pin_num VARCHAR(10);

ALTER TABLE idr.claim_item
ADD COLUMN clm_rndrg_prvdr_npi_num VARCHAR(10);
