ALTER TABLE idr.claim_professional_ss
ADD COLUMN bfd_blg_prvdr_npi_type integer,
ADD COLUMN bfd_prvdr_othr_npi_type integer,
ADD COLUMN bfd_prvdr_rfrg_npi_type integer;

ALTER TABLE idr.claim_professional_nch
ADD COLUMN bfd_blg_prvdr_npi_type integer,
ADD COLUMN bfd_prvdr_rfrg_npi_type integer,
ADD COLUMN bfd_prvdr_srvc_npi_type integer;

ALTER TABLE idr.claim_item_institutional_nch
ADD COLUMN bfd_prvdr_rndrng_npi_type integer;

ALTER TABLE idr.claim_institutional_nch
ADD COLUMN bfd_blg_prvdr_npi_type integer,
ADD COLUMN bfd_prvdr_atndg_npi_type integer,
ADD COLUMN bfd_prvdr_oprtg_npi_type integer,
ADD COLUMN bfd_prvdr_othr_npi_type integer,
ADD COLUMN bfd_prvdr_rndrg_npi_type integer,
ADD COLUMN bfd_prvdr_rfrg_npi_type integer,
ADD COLUMN bfd_prvdr_srvc_npi_type integer;

ALTER TABLE idr.claim_item_institutional_ss
ADD COLUMN bfd_prvdr_rndrng_npi_type integer;

ALTER TABLE idr.claim_institutional_ss
ADD COLUMN bfd_blg_prvdr_npi_type integer,
ADD COLUMN bfd_prvdr_atndg_npi_type integer,
ADD COLUMN bfd_prvdr_oprtg_npi_type integer,
ADD COLUMN bfd_prvdr_othr_npi_type integer,
ADD COLUMN bfd_prvdr_rndrg_npi_type integer,
ADD COLUMN bfd_prvdr_rfrg_npi_type integer;

ALTER TABLE idr.claim_item_professional_nch
ADD COLUMN bfd_prvdr_rndrng_npi_type integer;

ALTER TABLE idr.claim_item_professional_ss
ADD COLUMN bfd_prvdr_rndrng_npi_type integer;

ALTER TABLE idr.claim_rx
ADD COLUMN bfd_prvdr_prscrbng_npi_type integer,
ADD COLUMN bfd_srvc_prvdr_gnrc_id_npi_type integer;
