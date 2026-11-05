ALTER TABLE idr.claim_item_institutional_nch
ADD COLUMN clm_fac_prvdr_oscar_num VARCHAR(20) NOT NULL DEFAULT '';

ALTER TABLE idr.claim_item_institutional_ss
ADD COLUMN clm_fac_prvdr_oscar_num VARCHAR(20) NOT NULL DEFAULT '';

ALTER TABLE idr.claim_item_professional_nch
ADD COLUMN clm_fac_prvdr_oscar_num VARCHAR(20) NOT NULL DEFAULT '';

ALTER TABLE idr.claim_item_professional_ss
ADD COLUMN clm_fac_prvdr_oscar_num VARCHAR(20) NOT NULL DEFAULT '';
