--
-- DMEClaims to dme_claims
--
alter table public."DMEClaims" rename to dme_claims;
alter table public.dme_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.dme_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.dme_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.dme_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.dme_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.dme_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.dme_claims ${logic.alter-rename-column} "claimDispositionCode" ${logic.rename-to} clm_disp_cd;
alter table public.dme_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.dme_claims ${logic.alter-rename-column} "clinicalTrialNumber" ${logic.rename-to} clm_clncl_tril_num;
alter table public.dme_claims ${logic.alter-rename-column} "carrierNumber" ${logic.rename-to} carr_num;
alter table public.dme_claims ${logic.alter-rename-column} "claimCarrierControlNumber" ${logic.rename-to} carr_clm_cntl_num;
alter table public.dme_claims ${logic.alter-rename-column} "claimEntryCode" ${logic.rename-to} carr_clm_entry_cd;
alter table public.dme_claims ${logic.alter-rename-column} "providerAssignmentIndicator" ${logic.rename-to} carr_clm_prvdr_asgnmt_ind_sw;
alter table public.dme_claims ${logic.alter-rename-column} "hcpcsYearCode" ${logic.rename-to} carr_clm_hcpcs_yr_cd;
alter table public.dme_claims ${logic.alter-rename-column} "paymentDenialCode" ${logic.rename-to} carr_clm_pmt_dnl_cd;
alter table public.dme_claims ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} nch_carr_clm_alowd_amt;
alter table public.dme_claims ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} nch_carr_clm_sbmtd_chrg_amt;
alter table public.dme_claims ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} carr_clm_cash_ddctbl_apld_amt;
alter table public.dme_claims ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} nch_clm_bene_pmt_amt;
alter table public.dme_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.dme_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.dme_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.dme_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} carr_clm_prmry_pyr_pd_amt;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.dme_claims ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} nch_clm_prvdr_pmt_amt;
alter table public.dme_claims ${logic.alter-rename-column} "referringPhysicianNpi" ${logic.rename-to} rfr_physn_npi;
alter table public.dme_claims ${logic.alter-rename-column} "referringPhysicianUpin" ${logic.rename-to} rfr_physn_upin;
alter table public.dme_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.dme_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
--
-- DMEClaimLines to dme_claim_lines
--
alter table public."DMEClaimLines" rename to dme_claim_lines;
alter table public.dme_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.dme_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} line_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} line_nch_pmt_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "submittedChargeAmount" ${logic.rename-to} line_sbmtd_chrg_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "allowedChargeAmount" ${logic.rename-to} line_alowd_chrg_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "beneficiaryPartBDeductAmount" ${logic.rename-to} line_bene_ptb_ddctbl_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "beneficiaryPaymentAmount" ${logic.rename-to} line_bene_pmt_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "nationalDrugCode" ${logic.rename-to} line_ndc_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "cmsServiceTypeCode" ${logic.rename-to} line_cms_type_srvc_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "coinsuranceAmount" ${logic.rename-to} line_coinsrnc_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "diagnosisCode" ${logic.rename-to} line_icd_dgns_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "diagnosisCodeVersion" ${logic.rename-to} line_icd_dgns_vrsn_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "firstExpenseDate" ${logic.rename-to} line_1st_expns_dt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hctHgbTestResult" ${logic.rename-to} line_hct_hgb_rslt_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hctHgbTestTypeCode" ${logic.rename-to} line_hct_hgb_type_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "lastExpenseDate" ${logic.rename-to} line_last_expns_dt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "paymentCode" ${logic.rename-to} line_pmt_80_100_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "placeOfServiceCode" ${logic.rename-to} line_place_of_srvc_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "primaryPayerAllowedChargeAmount" ${logic.rename-to} line_prmry_alowd_chrg_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "primaryPayerCode" ${logic.rename-to} line_bene_prmry_pyr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "processingIndicatorCode" ${logic.rename-to} line_prcsg_ind_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "purchasePriceAmount" ${logic.rename-to} line_dme_prchs_price_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "serviceCount" ${logic.rename-to} line_srvc_cnt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "serviceDeductibleCode" ${logic.rename-to} line_service_deductible;
alter table public.dme_claim_lines ${logic.alter-rename-column} "betosCode" ${logic.rename-to} betos_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsFourthModifierCode" ${logic.rename-to} hcpcs_4th_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsInitialModifierCode" ${logic.rename-to} hcpcs_1st_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsSecondModifierCode" ${logic.rename-to} hcpcs_2nd_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "hcpcsThirdModifierCode" ${logic.rename-to} hcpcs_3rd_mdfr_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "mtusCode" ${logic.rename-to} dmerc_line_mtus_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "mtusCount" ${logic.rename-to} dmerc_line_mtus_cnt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "pricingStateCode" ${logic.rename-to} dmerc_line_prcng_state_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "screenSavingsAmount" ${logic.rename-to} dmerc_line_scrn_svgs_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "supplierTypeCode" ${logic.rename-to} dmerc_line_supplr_type_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} line_bene_prmry_pyr_pd_amt;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerBillingNumber" ${logic.rename-to} prvdr_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerNPI" ${logic.rename-to} prvdr_npi;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerSpecialityCode" ${logic.rename-to} prvdr_spclty;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerTaxNumber" ${logic.rename-to} tax_num;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerParticipatingIndCode" ${logic.rename-to} prtcptng_ind_cd;
alter table public.dme_claim_lines ${logic.alter-rename-column} "providerPaymentAmount" ${logic.rename-to} line_prvdr_pmt_amt;

${logic.alter-rename-index} public."DMEClaimLines_pkey" rename to dme_claim_lines_pkey;
${logic.alter-rename-index} public."DMEClaims_pkey" rename to dme_claims_pkey;

${logic.hsql-only-alter} table public.dme_claim_lines add constraint dme_claim_lines_pkey primary key (clm_id, line_num);
${logic.hsql-only-alter} table public.dme_claims add constraint dme_claims_pkey primary key (clm_id);

ALTER TABLE public.dme_claim_lines
    ADD CONSTRAINT dme_claim_lines_clmid_to_dme_claims FOREIGN KEY (clm_id) REFERENCES public.dme_claims (clm_id);

ALTER TABLE public.dme_claims
    ADD CONSTRAINT dme_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries(bene_id);
