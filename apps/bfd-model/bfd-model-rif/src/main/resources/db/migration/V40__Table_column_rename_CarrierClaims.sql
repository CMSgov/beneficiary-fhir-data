--
-- NOTES:
--   1. when you rename a table, indexes/constraints will trickle down to contraint & index directives,
--      BUT do not modify constraint or index names themselves
--   2. don't try to rename a column that already has the name (i.e., "hicn" ${logic.rename-to} hicn)
--   3. optionally rename contraint and/or index names (i.e., remove camelCase)
--
-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file prior to performing table or column rename.
-- SCRIPT './bfd_schema_pre.txt';
--
-- Rename tables and table columns
--
--      psql: alter table public.beneficiaries rename column "beneficiaryId" to bene_id;
--      hsql: alter table public.beneficiaries alter column  "beneficiaryId" rename to bene_id;
--
--      ${logic.alter-rename-column}
--          psql: "rename column"
--          hsql: "alter column"
--
--      ${logic.rename-to}
--          psql: "to"
--          hsql: "rename to"
--
-- CarrierClaims to carrier_claims
--
alter table public."CarrierClaims" rename to carrier_claims;
alter table public.carrier_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.carrier_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.carrier_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.carrier_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.carrier_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.carrier_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.carrier_claims ${logic.alter-rename-column} "clinicalTrialNumber" ${logic.rename-to} clm_clncl_tril_num;
alter table public.carrier_claims ${logic.alter-rename-column} "claimDispositionCode" ${logic.rename-to} clm_disp_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "claimCarrierControlNumber" ${logic.rename-to} carr_clm_cntl_num;
alter table public.carrier_claims ${logic.alter-rename-column} "claimEntryCode" ${logic.rename-to} carr_clm_entry_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "hcpcsYearCode" ${logic.rename-to} carr_clm_hcpcs_yr_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "paymentDenialCode" ${logic.rename-to} carr_clm_pmt_dnl_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "providerAssignmentIndicator" ${logic.rename-to} carr_clm_prvdr_asgnmt_ind_sw;
alter table public.carrier_claims ${logic.alter-rename-column} "referringProviderIdNumber" ${logic.rename-to} carr_clm_rfrng_pin_num;
alter table public.carrier_claims ${logic.alter-rename-column} "carrierNumber" ${logic.rename-to} carr_num;
alter table public.carrier_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.carrier_claims ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} nch_carr_clm_alowd_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} nch_carr_clm_sbmtd_chrg_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} nch_clm_bene_pmt_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} nch_clm_prvdr_pmt_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} carr_clm_cash_ddctbl_apld_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} carr_clm_prmry_pyr_pd_amt;
alter table public.carrier_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.carrier_claims ${logic.alter-rename-column} "referringPhysicianNpi" ${logic.rename-to} rfr_physn_npi;
alter table public.carrier_claims ${logic.alter-rename-column} "referringPhysicianUpin" ${logic.rename-to} rfr_physn_upin;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.carrier_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
--
-- CarrierClaimLines to carrier_claim_lines
--
alter table public."CarrierClaimLines" rename to carrier_claim_lines;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} line_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} line_nch_pmt_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "firstExpenseDate" ${logic.rename-to} line_1st_expns_dt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} line_alowd_chrg_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} line_bene_pmt_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "primaryPayerCode" ${logic.rename-to} line_bene_prmry_pyr_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} line_bene_prmry_pyr_pd_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} line_bene_ptb_ddctbl_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "cmsServiceTypeCode" ${logic.rename-to} line_cms_type_srvc_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "coinsuranceAmount" ${logic.rename-to} line_coinsrnc_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hctHgbTestResult" ${logic.rename-to} line_hct_hgb_rslt_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hctHgbTestTypeCode" ${logic.rename-to} line_hct_hgb_type_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "diagnosisCode" ${logic.rename-to} line_icd_dgns_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "diagnosisCodeVersion" ${logic.rename-to} line_icd_dgns_vrsn_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "lastExpenseDate" ${logic.rename-to} line_last_expns_dt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "nationalDrugCode" ${logic.rename-to} line_ndc_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "placeOfServiceCode" ${logic.rename-to} line_place_of_srvc_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "paymentCode" ${logic.rename-to} line_pmt_80_100_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "processingIndicatorCode" ${logic.rename-to} line_prcsg_ind_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} line_sbmtd_chrg_amt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "serviceDeductibleCode" ${logic.rename-to} line_service_deductible;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "serviceCount" ${logic.rename-to} line_srvc_cnt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "mtusCode" ${logic.rename-to} carr_line_mtus_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "mtusCount" ${logic.rename-to} carr_line_mtus_cnt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "betosCode" ${logic.rename-to} betos_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "anesthesiaUnitCount" ${logic.rename-to} carr_line_ansthsa_unit_cnt;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "cliaLabNumber" ${logic.rename-to} carr_line_clia_lab_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "linePricingLocalityCode" ${logic.rename-to} carr_line_prcng_lclty_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerTypeCode" ${logic.rename-to} carr_line_prvdr_type_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "reducedPaymentPhysicianAsstCode" ${logic.rename-to} carr_line_rdcd_pmt_phys_astn_c;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "rxNumber" ${logic.rename-to} carr_line_rx_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "performingProviderIdNumber" ${logic.rename-to} carr_prfrng_pin_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hcpcsInitialModifierCode" ${logic.rename-to} hcpcs_1st_mdfr_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hcpcsSecondModifierCode" ${logic.rename-to} hcpcs_2nd_mdfr_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "hpsaScarcityCode" ${logic.rename-to} hpsa_scrcty_ind_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "performingPhysicianNpi" ${logic.rename-to} prf_physn_npi;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "performingPhysicianUpin" ${logic.rename-to} prf_physn_upin;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerParticipatingIndCode" ${logic.rename-to} prtcptng_ind_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerSpecialityCode" ${logic.rename-to} prvdr_spclty;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerZipCode" ${logic.rename-to} prvdr_zip;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerTaxNumber" ${logic.rename-to} tax_num;
alter table public.carrier_claim_lines ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} line_prvdr_pmt_amt;

-- psql only
${logic.psql-only-alter} index if exists public."CarrierClaimLines_pkey" rename to carrier_claim_lines_pkey;
${logic.psql-only-alter} index if exists public."CarrierClaims_pkey" rename to carrier_claims_pkey;

${logic.psql-only-alter} table public.carrier_claim_lines rename constraint "CarrierClaimLines_parentClaim_to_CarrierClaims" to carrier_claim_lines_clmid_to_carrier_claims;
${logic.psql-only-alter} table public.carrier_claims rename constraint "CarrierClaims_beneficiaryId_to_Beneficiaries" to carrier_claims_bene_id_to_beneficiaries;

-- hsql only      
${logic.hsql-only-alter} table public.carrier_claim_lines add constraint carrier_claim_lines_pkey primary key (clm_id, line_num);
${logic.hsql-only-alter} table public.carrier_claims add constraint carrier_claims_pkey primary key (clm_id);

${logic.hsql-only-alter} table public.carrier_claim_lines ADD CONSTRAINT carrier_claim_lines_clmid_to_carrier_claims FOREIGN KEY (clm_id) REFERENCES public.carrier_claims (clm_id);
${logic.hsql-only-alter} table public.carrier_claims ADD CONSTRAINT carrier_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

-- both psql and hsql
ALTER INDEX "CarrierClaims_beneficiaryId_idx" RENAME TO carrier_claims_beneid_idx;