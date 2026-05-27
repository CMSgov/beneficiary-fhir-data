-- These are unused since we've combined the contract data with the claim_rx table now

ALTER TABLE idr.contract_pbp_number
DROP CONSTRAINT contract_pbp_number_cntrct_num_cntrct_pbp_num_bfd_contract__key;

ALTER TABLE idr.contract_pbp_number
DROP COLUMN bfd_contract_version_rank;
