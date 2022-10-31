-- This script is to alter the clm_drg_code datatype size from 3 to 4 characters for
-- the inpatient and snf claims tables, add the claim_query_code to the hha 
-- and hospice claims tables, and add the carr_clm_blg_npi_num to the carrier claims table.

-- Update DRG Code from 3 to 4 characters
${logic.psql-only} ALTER TABLE public.inpatient_claims ALTER COLUMN clm_drg_cd TYPE varying(4) COLLATE pg_catalog."default";
${logic.psql-only} ALTER TABLE public.snf_claims ALTER COLUMN clm_drg_cd TYPE varying(4) COLLATE pg_catalog."default";

-- Add claim query code to the HHA Claims and Hospice Claims table.
${logic.psql-only} ALTER TABLE public.hha_claims ADD claim_query_code character(1) COLLATE pg_catalog."default";
${logic.psql-only} ALTER TABLE public.hospice_claims ADD claim_query_code character(1) COLLATE pg_catalog."default";

-- Add carrier claim blg npi number to the Carrier Claims table.
${logic.psql-only} ALTER TABLE public.carrier_claims ADD carr_clm_blg_npi_num character varying(10) COLLATE pg_catalog."default";