--
-- NOTES:
--   1. when you rename a table, indexes/constraints will trickle down to contraint & index directives,
--      BUT not contraint or index names themselves
--   2. don't try to rename a column that already has the name (i.e., "hicn" ${logic.rename-to} hicn)
--   3. optionally rename contraint and/or index names (i.e., remove camelCase)
--
-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file prior to performing table or column rename.
-- SCRIPT './bfd_schema_pre.txt';
--
-- SNFClaims to snf_claims
--
alter table public."SNFClaims" rename to snf_claims;
alter table public.snf_claims ${logic.alter-rename-column} "claimId" ${logic.rename-to} clm_id;
alter table public.snf_claims ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.snf_claims ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.snf_claims ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.snf_claims ${logic.alter-rename-column} "dateFrom" ${logic.rename-to} clm_from_dt;
alter table public.snf_claims ${logic.alter-rename-column} "dateThrough" ${logic.rename-to} clm_thru_dt;
alter table public.snf_claims ${logic.alter-rename-column} "claimAdmissionDate" ${logic.rename-to} clm_admsn_dt;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisRelatedGroupCd" ${logic.rename-to} clm_drg_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimFacilityTypeCode" ${logic.rename-to} clm_fac_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimFrequencyCode" ${logic.rename-to} clm_freq_cd;
alter table public.snf_claims ${logic.alter-rename-column} "admissionTypeCd" ${logic.rename-to} clm_ip_admsn_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "mcoPaidSw" ${logic.rename-to} clm_mco_pd_sw;
alter table public.snf_claims ${logic.alter-rename-column} "claimNonPaymentReasonCode" ${logic.rename-to} clm_mdcr_non_pmt_rsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "nonUtilizationDayCount" ${logic.rename-to} clm_non_utlztn_days_cnt;
alter table public.snf_claims ${logic.alter-rename-column} "paymentAmount" ${logic.rename-to} clm_pmt_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalDisproportionateShareAmt" ${logic.rename-to} clm_pps_cptl_dsprprtnt_shr_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalExceptionAmount" ${logic.rename-to} clm_pps_cptl_excptn_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalFSPAmount" ${logic.rename-to} clm_pps_cptl_fsp_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalIMEAmount" ${logic.rename-to} clm_pps_cptl_ime_amt;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSCapitalOutlierAmount" ${logic.rename-to} clm_pps_cptl_outlier_amt;
alter table public.snf_claims ${logic.alter-rename-column} "prospectivePaymentCode" ${logic.rename-to} clm_pps_ind_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimPPSOldCapitalHoldHarmlessAmount" ${logic.rename-to} clm_pps_old_cptl_hld_hrmls_amt;
alter table public.snf_claims ${logic.alter-rename-column} "sourceAdmissionCd" ${logic.rename-to} clm_src_ip_admsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimServiceClassificationTypeCode" ${logic.rename-to} clm_srvc_clsfctn_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "utilizationDayCount" ${logic.rename-to} clm_utlztn_day_cnt;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisAdmittingCode" ${logic.rename-to} admtg_dgns_cd;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisAdmittingCodeVersion" ${logic.rename-to} admtg_dgns_vrsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "attendingPhysicianNpi" ${logic.rename-to} at_physn_npi;
alter table public.snf_claims ${logic.alter-rename-column} "attendingPhysicianUpin" ${logic.rename-to} at_physn_upin;
alter table public.snf_claims ${logic.alter-rename-column} "coinsuranceDayCount" ${logic.rename-to} bene_tot_coinsrnc_days_cnt;
alter table public.snf_claims ${logic.alter-rename-column} "claimQueryCode" ${logic.rename-to} claim_query_code;
alter table public.snf_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimActionCode" ${logic.rename-to} fi_clm_actn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "fiscalIntermediaryClaimProcessDate" ${logic.rename-to} fi_clm_proc_dt;
alter table public.snf_claims ${logic.alter-rename-column} "fiDocumentClaimControlNumber" ${logic.rename-to} fi_doc_clm_cntl_num;
alter table public.snf_claims ${logic.alter-rename-column} "fiscalIntermediaryNumber" ${logic.rename-to} fi_num;
alter table public.snf_claims ${logic.alter-rename-column} "fiOriginalClaimControlNumber" ${logic.rename-to} fi_orig_clm_cntl_num;
alter table public.snf_claims ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternalFirstCode" ${logic.rename-to} fst_dgns_e_cd;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternalFirstCodeVersion" ${logic.rename-to} fst_dgns_e_vrsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "coveredCareThroughDate" ${logic.rename-to} nch_actv_or_cvrd_lvl_care_thru;
alter table public.snf_claims ${logic.alter-rename-column} "bloodDeductibleLiabilityAmount" ${logic.rename-to} nch_bene_blood_ddctbl_lblty_am;
alter table public.snf_claims ${logic.alter-rename-column} "beneficiaryDischargeDate" ${logic.rename-to} nch_bene_dschrg_dt;
alter table public.snf_claims ${logic.alter-rename-column} "deductibleAmount" ${logic.rename-to} nch_bene_ip_ddctbl_amt;
alter table public.snf_claims ${logic.alter-rename-column} "medicareBenefitsExhaustedDate" ${logic.rename-to} nch_bene_mdcr_bnfts_exhtd_dt_i;
alter table public.snf_claims ${logic.alter-rename-column} "partACoinsuranceLiabilityAmount" ${logic.rename-to} nch_bene_pta_coinsrnc_lblty_am;
alter table public.snf_claims ${logic.alter-rename-column} "bloodPintsFurnishedQty" ${logic.rename-to} nch_blood_pnts_frnshd_qty;
alter table public.snf_claims ${logic.alter-rename-column} "claimTypeCode" ${logic.rename-to} nch_clm_type_cd;
alter table public.snf_claims ${logic.alter-rename-column} "noncoveredCharge" ${logic.rename-to} nch_ip_ncvrd_chrg_amt;
alter table public.snf_claims ${logic.alter-rename-column} "totalDeductionAmount" ${logic.rename-to} nch_ip_tot_ddctn_amt;
alter table public.snf_claims ${logic.alter-rename-column} "nearLineRecordIdCode" ${logic.rename-to} nch_near_line_rec_ident_cd;
alter table public.snf_claims ${logic.alter-rename-column} "claimPrimaryPayerCode" ${logic.rename-to} nch_prmry_pyr_cd;
alter table public.snf_claims ${logic.alter-rename-column} "primaryPayerPaidAmount" ${logic.rename-to} nch_prmry_pyr_clm_pd_amt;
alter table public.snf_claims ${logic.alter-rename-column} "patientStatusCd" ${logic.rename-to} nch_ptnt_status_ind_cd;
alter table public.snf_claims ${logic.alter-rename-column} "qualifiedStayFromDate" ${logic.rename-to} nch_qlfyd_stay_from_dt;
alter table public.snf_claims ${logic.alter-rename-column} "qualifiedStayThroughDate" ${logic.rename-to} nch_qlfyd_stay_thru_dt;
alter table public.snf_claims ${logic.alter-rename-column} "noncoveredStayFromDate" ${logic.rename-to} nch_vrfd_ncvrd_stay_from_dt;
alter table public.snf_claims ${logic.alter-rename-column} "noncoveredStayThroughDate" ${logic.rename-to} nch_vrfd_ncvrd_stay_thru_dt;
alter table public.snf_claims ${logic.alter-rename-column} "weeklyProcessDate" ${logic.rename-to} nch_wkly_proc_dt;
alter table public.snf_claims ${logic.alter-rename-column} "operatingPhysicianNpi" ${logic.rename-to} op_physn_npi;
alter table public.snf_claims ${logic.alter-rename-column} "operatingPhysicianUpin" ${logic.rename-to} op_physn_upin;
alter table public.snf_claims ${logic.alter-rename-column} "organizationNpi" ${logic.rename-to} org_npi_num;
alter table public.snf_claims ${logic.alter-rename-column} "otherPhysicianNpi" ${logic.rename-to} ot_physn_npi;
alter table public.snf_claims ${logic.alter-rename-column} "otherPhysicianUpin" ${logic.rename-to} ot_physn_upin;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisPrincipalCode" ${logic.rename-to} prncpal_dgns_cd;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisPrincipalCodeVersion" ${logic.rename-to} prncpal_dgns_vrsn_cd;
alter table public.snf_claims ${logic.alter-rename-column} "providerNumber" ${logic.rename-to} prvdr_num;
alter table public.snf_claims ${logic.alter-rename-column} "providerStateCode" ${logic.rename-to} prvdr_state_cd;
alter table public.snf_claims ${logic.alter-rename-column} "patientDischargeStatusCode" ${logic.rename-to} ptnt_dschrg_stus_cd;
alter table public.snf_claims ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} clm_tot_chrg_amt;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis1Code" ${logic.rename-to} icd_dgns_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis2Code" ${logic.rename-to} icd_dgns_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis3Code" ${logic.rename-to} icd_dgns_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis4Code" ${logic.rename-to} icd_dgns_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis5Code" ${logic.rename-to} icd_dgns_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis6Code" ${logic.rename-to} icd_dgns_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis7Code" ${logic.rename-to} icd_dgns_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis8Code" ${logic.rename-to} icd_dgns_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis9Code" ${logic.rename-to} icd_dgns_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis10Code" ${logic.rename-to} icd_dgns_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis11Code" ${logic.rename-to} icd_dgns_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis12Code" ${logic.rename-to} icd_dgns_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis13Code" ${logic.rename-to} icd_dgns_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis14Code" ${logic.rename-to} icd_dgns_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis15Code" ${logic.rename-to} icd_dgns_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis16Code" ${logic.rename-to} icd_dgns_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis17Code" ${logic.rename-to} icd_dgns_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis18Code" ${logic.rename-to} icd_dgns_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis19Code" ${logic.rename-to} icd_dgns_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis20Code" ${logic.rename-to} icd_dgns_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis21Code" ${logic.rename-to} icd_dgns_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis22Code" ${logic.rename-to} icd_dgns_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis23Code" ${logic.rename-to} icd_dgns_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis24Code" ${logic.rename-to} icd_dgns_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis25Code" ${logic.rename-to} icd_dgns_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal1Code" ${logic.rename-to} icd_dgns_e_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal2Code" ${logic.rename-to} icd_dgns_e_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal3Code" ${logic.rename-to} icd_dgns_e_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal4Code" ${logic.rename-to} icd_dgns_e_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal5Code" ${logic.rename-to} icd_dgns_e_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal6Code" ${logic.rename-to} icd_dgns_e_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal7Code" ${logic.rename-to} icd_dgns_e_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal8Code" ${logic.rename-to} icd_dgns_e_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal9Code" ${logic.rename-to} icd_dgns_e_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal10Code" ${logic.rename-to} icd_dgns_e_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal11Code" ${logic.rename-to} icd_dgns_e_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal12Code" ${logic.rename-to} icd_dgns_e_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal1CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal2CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal3CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal4CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal5CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal6CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal7CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal8CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal9CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal10CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal11CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosisExternal12CodeVersion" ${logic.rename-to} icd_dgns_e_vrsn_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis1CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis2CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis3CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis4CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis5CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis6CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis7CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis8CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis9CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis10CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis11CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis12CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis13CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis14CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis15CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis16CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis17CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis18CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis19CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis20CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis21CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis22CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis23CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis24CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "diagnosis25CodeVersion" ${logic.rename-to} icd_dgns_vrsn_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "procedure1Code" ${logic.rename-to} icd_prcdr_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "procedure2Code" ${logic.rename-to} icd_prcdr_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "procedure3Code" ${logic.rename-to} icd_prcdr_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "procedure4Code" ${logic.rename-to} icd_prcdr_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "procedure5Code" ${logic.rename-to} icd_prcdr_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "procedure6Code" ${logic.rename-to} icd_prcdr_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "procedure7Code" ${logic.rename-to} icd_prcdr_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "procedure8Code" ${logic.rename-to} icd_prcdr_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "procedure9Code" ${logic.rename-to} icd_prcdr_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "procedure10Code" ${logic.rename-to} icd_prcdr_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "procedure11Code" ${logic.rename-to} icd_prcdr_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "procedure12Code" ${logic.rename-to} icd_prcdr_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "procedure13Code" ${logic.rename-to} icd_prcdr_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "procedure14Code" ${logic.rename-to} icd_prcdr_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "procedure15Code" ${logic.rename-to} icd_prcdr_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "procedure16Code" ${logic.rename-to} icd_prcdr_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "procedure17Code" ${logic.rename-to} icd_prcdr_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "procedure18Code" ${logic.rename-to} icd_prcdr_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "procedure19Code" ${logic.rename-to} icd_prcdr_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "procedure20Code" ${logic.rename-to} icd_prcdr_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "procedure21Code" ${logic.rename-to} icd_prcdr_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "procedure22Code" ${logic.rename-to} icd_prcdr_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "procedure23Code" ${logic.rename-to} icd_prcdr_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "procedure24Code" ${logic.rename-to} icd_prcdr_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "procedure25Code" ${logic.rename-to} icd_prcdr_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "procedure1CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd1;
alter table public.snf_claims ${logic.alter-rename-column} "procedure2CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd2;
alter table public.snf_claims ${logic.alter-rename-column} "procedure3CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd3;
alter table public.snf_claims ${logic.alter-rename-column} "procedure4CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd4;
alter table public.snf_claims ${logic.alter-rename-column} "procedure5CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd5;
alter table public.snf_claims ${logic.alter-rename-column} "procedure6CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd6;
alter table public.snf_claims ${logic.alter-rename-column} "procedure7CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd7;
alter table public.snf_claims ${logic.alter-rename-column} "procedure8CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd8;
alter table public.snf_claims ${logic.alter-rename-column} "procedure9CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd9;
alter table public.snf_claims ${logic.alter-rename-column} "procedure10CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd10;
alter table public.snf_claims ${logic.alter-rename-column} "procedure11CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd11;
alter table public.snf_claims ${logic.alter-rename-column} "procedure12CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd12;
alter table public.snf_claims ${logic.alter-rename-column} "procedure13CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd13;
alter table public.snf_claims ${logic.alter-rename-column} "procedure14CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd14;
alter table public.snf_claims ${logic.alter-rename-column} "procedure15CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd15;
alter table public.snf_claims ${logic.alter-rename-column} "procedure16CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd16;
alter table public.snf_claims ${logic.alter-rename-column} "procedure17CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd17;
alter table public.snf_claims ${logic.alter-rename-column} "procedure18CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd18;
alter table public.snf_claims ${logic.alter-rename-column} "procedure19CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd19;
alter table public.snf_claims ${logic.alter-rename-column} "procedure20CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd20;
alter table public.snf_claims ${logic.alter-rename-column} "procedure21CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd21;
alter table public.snf_claims ${logic.alter-rename-column} "procedure22CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd22;
alter table public.snf_claims ${logic.alter-rename-column} "procedure23CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd23;
alter table public.snf_claims ${logic.alter-rename-column} "procedure24CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd24;
alter table public.snf_claims ${logic.alter-rename-column} "procedure25CodeVersion" ${logic.rename-to} icd_prcdr_vrsn_cd25;
alter table public.snf_claims ${logic.alter-rename-column} "procedure1Date" ${logic.rename-to} prcdr_dt1;
alter table public.snf_claims ${logic.alter-rename-column} "procedure2Date" ${logic.rename-to} prcdr_dt2;
alter table public.snf_claims ${logic.alter-rename-column} "procedure3Date" ${logic.rename-to} prcdr_dt3;
alter table public.snf_claims ${logic.alter-rename-column} "procedure4Date" ${logic.rename-to} prcdr_dt4;
alter table public.snf_claims ${logic.alter-rename-column} "procedure5Date" ${logic.rename-to} prcdr_dt5;
alter table public.snf_claims ${logic.alter-rename-column} "procedure6Date" ${logic.rename-to} prcdr_dt6;
alter table public.snf_claims ${logic.alter-rename-column} "procedure7Date" ${logic.rename-to} prcdr_dt7;
alter table public.snf_claims ${logic.alter-rename-column} "procedure8Date" ${logic.rename-to} prcdr_dt8;
alter table public.snf_claims ${logic.alter-rename-column} "procedure9Date" ${logic.rename-to} prcdr_dt9;
alter table public.snf_claims ${logic.alter-rename-column} "procedure10Date" ${logic.rename-to} prcdr_dt10;
alter table public.snf_claims ${logic.alter-rename-column} "procedure11Date" ${logic.rename-to} prcdr_dt11;
alter table public.snf_claims ${logic.alter-rename-column} "procedure12Date" ${logic.rename-to} prcdr_dt12;
alter table public.snf_claims ${logic.alter-rename-column} "procedure13Date" ${logic.rename-to} prcdr_dt13;
alter table public.snf_claims ${logic.alter-rename-column} "procedure14Date" ${logic.rename-to} prcdr_dt14;
alter table public.snf_claims ${logic.alter-rename-column} "procedure15Date" ${logic.rename-to} prcdr_dt15;
alter table public.snf_claims ${logic.alter-rename-column} "procedure16Date" ${logic.rename-to} prcdr_dt16;
alter table public.snf_claims ${logic.alter-rename-column} "procedure17Date" ${logic.rename-to} prcdr_dt17;
alter table public.snf_claims ${logic.alter-rename-column} "procedure18Date" ${logic.rename-to} prcdr_dt18;
alter table public.snf_claims ${logic.alter-rename-column} "procedure19Date" ${logic.rename-to} prcdr_dt19;
alter table public.snf_claims ${logic.alter-rename-column} "procedure20Date" ${logic.rename-to} prcdr_dt20;
alter table public.snf_claims ${logic.alter-rename-column} "procedure21Date" ${logic.rename-to} prcdr_dt21;
alter table public.snf_claims ${logic.alter-rename-column} "procedure22Date" ${logic.rename-to} prcdr_dt22;
alter table public.snf_claims ${logic.alter-rename-column} "procedure23Date" ${logic.rename-to} prcdr_dt23;
alter table public.snf_claims ${logic.alter-rename-column} "procedure24Date" ${logic.rename-to} prcdr_dt24;
alter table public.snf_claims ${logic.alter-rename-column} "procedure25Date" ${logic.rename-to} prcdr_dt25;
--
-- SNFClaimLines to snf_claim_lines
--
alter table public."SNFClaimLines" rename to snf_claim_lines;
alter table public.snf_claim_lines ${logic.alter-rename-column} "parentClaim" ${logic.rename-to} clm_id;
alter table public.snf_claim_lines ${logic.alter-rename-column} "lineNumber" ${logic.rename-to} clm_line_num;
alter table public.snf_claim_lines ${logic.alter-rename-column} "hcpcsCode" ${logic.rename-to} hcpcs_cd;
alter table public.snf_claim_lines ${logic.alter-rename-column} "revenueCenter" ${logic.rename-to} rev_cntr;
alter table public.snf_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQualifierCode" ${logic.rename-to} rev_cntr_ndc_qty_qlfr_cd;
alter table public.snf_claim_lines ${logic.alter-rename-column} "nationalDrugCodeQuantity" ${logic.rename-to} rev_cntr_ndc_qty;
alter table public.snf_claim_lines ${logic.alter-rename-column} "nonCoveredChargeAmount" ${logic.rename-to} rev_cntr_ncvrd_chrg_amt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "rateAmount" ${logic.rename-to} rev_cntr_rate_amt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "totalChargeAmount" ${logic.rename-to} rev_cntr_tot_chrg_amt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "deductibleCoinsuranceCd" ${logic.rename-to} rev_cntr_ddctbl_coinsrnc_cd;
alter table public.snf_claim_lines ${logic.alter-rename-column} "unitCount" ${logic.rename-to} rev_cntr_unit_cnt;
alter table public.snf_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianNPI" ${logic.rename-to} rndrng_physn_npi;
alter table public.snf_claim_lines ${logic.alter-rename-column} "revenueCenterRenderingPhysicianUPIN" ${logic.rename-to} rndrng_physn_upin;
--
-- BeneficiariesHistory to beneficiaries_history
--
alter table public."BeneficiariesHistory" rename to beneficiaries_history;
alter table public.beneficiaries_history ${logic.alter-rename-column} "beneficiaryHistoryId" ${logic.rename-to} bene_history_id;
alter table public.beneficiaries_history ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.beneficiaries_history ${logic.alter-rename-column} "birthDate" ${logic.rename-to} bene_birth_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} "sex" ${logic.rename-to} bene_sex_ident_cd;
alter table public.beneficiaries_history ${logic.alter-rename-column} "hicn" ${logic.rename-to} bene_crnt_hic_num;
alter table public.beneficiaries_history ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.beneficiaries_history ${logic.alter-rename-column} "hicnUnhashed" ${logic.rename-to} hicn_unhashed;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiHash" ${logic.rename-to} mbi_hash;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} efctv_bgn_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} "mbiObsoleteDate" ${logic.rename-to} efctv_end_dt;
alter table public.beneficiaries_history ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
--
-- BeneficiariesHistoryInvalidBeneficiaries to beneficiaries_history_invalid_beneficiaries
--
alter table public."BeneficiariesHistoryInvalidBeneficiaries" rename to beneficiaries_history_invalid_beneficiaries;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryHistoryId" ${logic.rename-to} bene_history_id;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "birthDate" ${logic.rename-to} bene_birth_dt;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "sex" ${logic.rename-to} bene_sex_ident_cd;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "hicn" ${logic.rename-to} bene_crnt_hic_num;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.beneficiaries_history_invalid_beneficiaries ${logic.alter-rename-column} "hicnUnhashed" ${logic.rename-to} hicn_unhashed;
--
-- BeneficiaryMonthly to beneficiary_monthly
--
alter table public."BeneficiaryMonthly" rename to beneficiary_monthly;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "yearMonth" ${logic.rename-to} year_month;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "parentBeneficiary" ${logic.rename-to} bene_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDContractNumberId" ${logic.rename-to} partd_contract_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCContractNumberId" ${logic.rename-to} partc_contract_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "fipsStateCntyCode" ${logic.rename-to} fips_state_cnty_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "medicareStatusCode" ${logic.rename-to} medicare_status_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "entitlementBuyInInd" ${logic.rename-to} entitlement_buy_in_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "hmoIndicatorInd" ${logic.rename-to} hmo_indicator_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCPbpNumberId" ${logic.rename-to} partc_pbp_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partCPlanTypeCode" ${logic.rename-to} partc_plan_type_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDPbpNumberId" ${logic.rename-to} partd_pbp_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDSegmentNumberId" ${logic.rename-to} partd_segment_number_id;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDRetireeDrugSubsidyInd" ${logic.rename-to} partd_retiree_drug_subsidy_ind;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "partDLowIncomeCostShareGroupCode" ${logic.rename-to} partd_low_income_cost_share_group_code;
alter table public.beneficiary_monthly ${logic.alter-rename-column} "medicaidDualEligibilityCode" ${logic.rename-to} medicaid_dual_eligibility_code;
--
-- LoadedFiles to loaded_files
--
-- We have a bit of a funky condition between psql and hsql; for both loaded_files and loaded_batches
-- there is a column called "created". For psql there is no need to do a rename; in fact if we tried
-- to do something like:
--
--      psql: alter table public.loaded_files rename column "created" to created
--
-- we'd get an error. So in theory, maybe we don't even need to do a rename for that type of condition.
-- However, in hsql, if we don't do a rename, we end up with a column called "created" (ltierally,
-- meaning the double-quotes are an integral part of the column name). So for hsql we do need to
-- perform the rename so we can rid the column name of the double-quotes.
--
--      ${logic.hsql-only-alter}
--          psql: "--"
--          hsql: "alter"
--
alter table public."LoadedFiles" rename to loaded_files;
alter table public.loaded_files ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_fileid;
alter table public.loaded_files ${logic.alter-rename-column} "rifType" ${logic.rename-to} rif_type;
${logic.hsql-only-alter} table public.loaded_files ${logic.alter-rename-column} "created" ${logic.rename-to} created;
--
-- LoadedBatches to loaded_batches
--
alter table public."LoadedBatches" rename to loaded_batches;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedBatchId" ${logic.rename-to} loaded_batchid;
alter table public.loaded_batches ${logic.alter-rename-column} "loadedFileId" ${logic.rename-to} loaded_fileid;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "beneficiaries" ${logic.rename-to} beneficiaries;
${logic.hsql-only-alter} table public.loaded_batches ${logic.alter-rename-column} "created" ${logic.rename-to} created;
--
-- MedicareBeneficiaryIdHistory to medicare_beneficiaryid_history
--
alter table public."MedicareBeneficiaryIdHistory" rename to medicare_beneficiaryid_history;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "medicareBeneficiaryIdKey" ${logic.rename-to} bene_mbi_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "claimAccountNumber" ${logic.rename-to} bene_clm_acnt_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "beneficiaryIdCode" ${logic.rename-to} bene_ident_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiCrntRecIndId" ${logic.rename-to} bene_crnt_rec_ind_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiSequenceNumber" ${logic.rename-to} mbi_sqnc_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} mbi_efctv_bgn_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEndDate" ${logic.rename-to} mbi_efctv_end_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEffectiveReasonCode" ${logic.rename-to} mbi_bgn_rsn_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiEndReasonCode" ${logic.rename-to} mbi_end_rsn_cd;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiCardRequestDate" ${logic.rename-to} mbi_card_rqst_dt;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiAddUser" ${logic.rename-to} creat_user_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiAddDate" ${logic.rename-to} creat_ts;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiUpdateUser" ${logic.rename-to} updt_user_id;
alter table public.medicare_beneficiaryid_history ${logic.alter-rename-column} "mbiUpdateDate" ${logic.rename-to} updt_ts;
--
-- MedicareBeneficiaryIdHistoryInvalidBeneficiaries to medicare_beneficiaryid_history_invalid_beneficiaries
--
alter table public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries" rename to medicare_beneficiaryid_history_invalid_beneficiaries;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryIdKey" ${logic.rename-to} bene_mbi_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "claimAccountNumber" ${logic.rename-to} bene_clm_acnt_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "beneficiaryIdCode" ${logic.rename-to} bene_ident_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiCrntRecIndId" ${logic.rename-to} bene_crnt_rec_ind_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiSequenceNumber" ${logic.rename-to} mbi_sqnc_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "medicareBeneficiaryId" ${logic.rename-to} mbi_num;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEffectiveDate" ${logic.rename-to} mbi_efctv_bgn_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEndDate" ${logic.rename-to} mbi_efctv_end_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEffectiveReasonCode" ${logic.rename-to} mbi_bgn_rsn_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiEndReasonCode" ${logic.rename-to} mbi_end_rsn_cd;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiCardRequestDate" ${logic.rename-to} mbi_card_rqst_dt;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiAddUser" ${logic.rename-to} creat_user_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiAddDate" ${logic.rename-to} creat_ts;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiUpdateUser" ${logic.rename-to} updt_user_id;
alter table public.medicare_beneficiaryid_history_invalid_beneficiaries ${logic.alter-rename-column} "mbiUpdateDate" ${logic.rename-to} updt_ts;
--
-- take care of primary keys
--
-- For hsql we need to (re-) create the primary key constraint since we deleted them at
-- beginning of script. For psql, we can just do a rename.
--
--      {alter-rename-index}
--          psql: "alter index if exists"
--          hsql: "--"
--
${logic.alter-rename-index} public."BeneficiariesHistoryInvalidBeneficiaries_pkey" rename to beneficiaries_history_invalid_beneficiaries_pkey;
${logic.alter-rename-index} public."BeneficiariesHistory_pkey" rename to beneficiaries_history_pkey;
${logic.alter-rename-index} public."Beneficiaries_pkey" rename to beneficiaries_pkey;
${logic.alter-rename-index} public."BeneficiaryMonthly_pkey" rename to beneficiary_monthly_pkey;
${logic.alter-rename-index} public."CarrierClaimLines_pkey"rename to carrier_claim_lines_pkey;
${logic.alter-rename-index} public."CarrierClaims_pkey" rename to carrier_claims_pkey;
${logic.alter-rename-index} public."DMEClaimLines_pkey" rename to dme_claim_lines_pkey;
${logic.alter-rename-index} public."DMEClaims_pkey" rename to dme_claims_pkey;
${logic.alter-rename-index} public."HHAClaimLines_pkey" rename to hha_claim_lines_pkey;
${logic.alter-rename-index} public."HHAClaims_pkey" rename to hha_claims_pkey;
${logic.alter-rename-index} public."HospiceClaimLines_pkey" rename to hospice_claim_lines_pkey;
${logic.alter-rename-index} public."HospiceClaims_pkey" rename to hospice_claims_pkey;
${logic.alter-rename-index} public."InpatientClaimLines_pkey" rename to inpatient_claim_lines_pkey;
${logic.alter-rename-index} public."InpatientClaims_pkey" rename to inpatient_claims_pkey;
${logic.alter-rename-index} public."MedicareBeneficiaryIdHistoryInvalidBeneficiaries_pkey" rename to medicare_beneficiaryid_history_invalid_beneficiaries_pkey;
${logic.alter-rename-index} public."MedicareBeneficiaryIdHistory_pkey" rename to medicare_beneficiaryid_history_pkey;
${logic.alter-rename-index} public."OutpatientClaimLines_pkey" rename to outpatient_claim_lines_pkey;
${logic.alter-rename-index} public."OutpatientClaims_pkey" rename to outpatient_claims_pkey;
${logic.alter-rename-index} public."PartDEvents_pkey" rename to partd_events_pkey;
${logic.alter-rename-index} public."SNFClaimLines_pkey" rename to snf_claim_lines_pkey;
${logic.alter-rename-index} public."SNFClaims_pkey" rename to snf_claims_pkey;
${logic.alter-rename-index} public."LoadedBatches_pkey" rename to loaded_batches_pkey;
${logic.alter-rename-index} public."LoadedFiles_pkey" rename to loaded_files_pkey;
--
--      ${logic.hsql-only-alter}
--          psql: "--"
--          hsql: "alter"
--
${logic.hsql-only-alter} table public.beneficiaries add constraint beneficiaries_pkey primary key (bene_id);
${logic.hsql-only-alter} table public.beneficiaries_history add constraint beneficiaries_history_pkey primary key (bene_history_id);  
${logic.hsql-only-alter} table public.beneficiaries_history_invalid_beneficiaries add constraint beneficiaries_history_invalid_beneficiaries_pkey primary key (bene_history_id); 
${logic.hsql-only-alter} table public.beneficiary_monthly add constraint beneficiary_monthly_pkey primary key (bene_id, year_month);
${logic.hsql-only-alter} table public.carrier_claim_lines add constraint carrier_claim_lines_pkey primary key (clm_id, line_num);
${logic.hsql-only-alter} table public.carrier_claims add constraint carrier_claims_pkey primary key (clm_id);
${logic.hsql-only-alter} table public.dme_claim_lines add constraint dme_claim_lines_pkey primary key (clm_id, line_num);
${logic.hsql-only-alter} table public.dme_claims add constraint dme_claims_pkey primary key (clm_id);
${logic.hsql-only-alter} table public.hha_claim_lines add constraint hha_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.hha_claims add constraint hha_claims_pkey primary key (clm_id);  
${logic.hsql-only-alter} table public.hospice_claim_lines add constraint hospice_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.hospice_claims add constraint hospice_claims_pkey primary key (clm_id); 
${logic.hsql-only-alter} table public.inpatient_claim_lines add constraint inpatient_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.inpatient_claims add constraint npatient_claims_pkey primary key (clm_id); 
${logic.hsql-only-alter} table public.medicare_beneficiaryid_history_invalid_beneficiaries add constraint medicare_beneficiaryid_history_invalid_beneficiaries_pkey primary key (bene_mbi_id); 
${logic.hsql-only-alter} table public.medicare_beneficiaryid_history add constraint medicare_beneficiaryid_history_pkey primary key (bene_mbi_id);   
${logic.hsql-only-alter} table public.outpatient_claim_lines add constraint outpatient_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.outpatient_claims add constraint outpatient_claims_pkey primary key (clm_id); 
${logic.hsql-only-alter} table public.partd_events add constraint partd_events_pkey primary key (pde_id); 
${logic.hsql-only-alter} table public.snf_claim_lines add constraint snf_claim_lines_pkey primary key (clm_id, clm_line_num); 
${logic.hsql-only-alter} table public.snf_claims add constraint snf_claims_pkey primary key (clm_id);
--${logic.hsql-only-alter} table public.loaded_batches add constraint loaded_batches_pkey primary key (loaded_batchid);
--${logic.hsql-only-alter} table public.loaded_files add constraint loaded_files_pkey primary key (loaded_fileid);
--
-- rename indexes (index names are limited to 64 chars)
--
ALTER INDEX "BeneficiariesHistory_beneficiaryId_idx" RENAME TO beneficiaries_history_beneid_idx;
ALTER INDEX "BeneficiariesHistory_hicn_idx" RENAME TO beneficiaries_history_hicn_idx;
ALTER INDEX "Beneficiaries_history_mbi_hash_idx" RENAME TO beneficiaries_history_mbi_hash_idx;
ALTER INDEX "Beneficiaries_hicn_idx" RENAME TO beneficiaries_hicn_idx;
ALTER INDEX "Beneficiaries_mbi_hash_idx" RENAME TO beneficiaries_mbi_hash_idx; 
ALTER INDEX "BeneficiaryMonthly_partDContractNumId_yearMonth_parentBene_idx" RENAME TO beneficiary_monthly_year_month_partd_contract_beneid_idx;
ALTER INDEX "BeneficiaryMonthly_partDContractNumberId_yearmonth_idx" RENAME TO beneficiary_monthly_partd_contract_number_year_month_idx;
ALTER INDEX "CarrierClaims_beneficiaryId_idx" RENAME TO carrier_claims_beneid_idx;
ALTER INDEX "DMEClaims_beneficiaryId_idx" RENAME TO dme_claims_beneid_idx;
ALTER INDEX "HHAClaims_beneficiaryId_idx" RENAME TO hha_claims_beneid_idx;
ALTER INDEX "HospiceClaims_beneficiaryId_idx" RENAME TO hospice_claims_beneid_idx;
ALTER INDEX "InpatientClaims_beneficiaryId_idx" RENAME TO inpatient_claims_beneid_idx;
ALTER INDEX "LoadedBatches_created_index" RENAME TO loaded_batches_created_idx;
ALTER INDEX "MedicareBeneficiaryIdHistory_beneficiaryId_idx" RENAME TO medicare_beneficiaryid_history_beneid_idx;
ALTER INDEX "OutpatientClaims_beneficiaryId_idx" RENAME TO outpatient_claims_beneid_idx;
ALTER INDEX "PartDEvents_beneficiaryId_idx" RENAME TO partd_events_beneid_idx;
ALTER INDEX "SNFClaims_beneficiaryId_idx" RENAME TO snf_claims_beneid_idx;

-- FIX THIS - why do we even have these???
ALTER INDEX "Beneficiaries_partd_contract_number_apr_id_idx" RENAME  TO beneficiaries_partd_contract_number_apr_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_aug_id_idx" RENAME  TO beneficiaries_partd_contract_number_aug_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_dec_id_idx" RENAME  TO beneficiaries_partd_contract_number_dec_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_feb_id_idx" RENAME  TO beneficiaries_partd_contract_number_feb_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jan_id_idx" RENAME  TO beneficiaries_partd_contract_number_jan_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jul_id_idx" RENAME  TO beneficiaries_partd_contract_number_jul_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_jun_id_idx" RENAME  TO beneficiaries_partd_contract_number_jun_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_mar_id_idx" RENAME  TO beneficiaries_partd_contract_number_mar_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_may_id_idx" RENAME  TO beneficiaries_partd_contract_number_may_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_nov_id_idx" RENAME  TO beneficiaries_partd_contract_number_nov_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_oct_id_idx" RENAME  TO beneficiaries_partd_contract_number_oct_id_idx;
ALTER INDEX "Beneficiaries_partd_contract_number_sept_id_idx" RENAME TO beneficiaries_partd_contract_number_sept_id_idx;
--
-- Add foreign key constraints
--
ALTER TABLE public.beneficiaries_history
    ADD CONSTRAINT beneficiaries_history_bene_id_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.beneficiary_monthly
    ADD CONSTRAINT beneficiary_monthly_beneid_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.carrier_claim_lines
    ADD CONSTRAINT carrier_claim_lines_clmid_to_carrier_claims FOREIGN KEY (clm_id) REFERENCES public.carrier_claims (clm_id);

ALTER TABLE public.carrier_claims
    ADD CONSTRAINT carrier_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.dme_claim_lines
    ADD CONSTRAINT dme_claim_lines_clmid_to_dme_claims FOREIGN KEY (clm_id) REFERENCES public.dme_claims (clm_id);

ALTER TABLE public.dme_claims
    ADD CONSTRAINT dme_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries(bene_id);

ALTER TABLE public.hha_claim_lines
    ADD CONSTRAINT hha_claim_lines_parent_claim_to_hha_claims FOREIGN KEY (clm_id) REFERENCES public.hha_claims (clm_id);

ALTER TABLE public.hha_claims
    ADD CONSTRAINT hha_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.hospice_claim_lines
    ADD CONSTRAINT hospice_claim_lines_parent_claim_to_hospice_claims FOREIGN KEY (clm_id) REFERENCES public.hospice_claims (clm_id);

ALTER TABLE public.hospice_claims
    ADD CONSTRAINT hospice_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.inpatient_claim_lines
    ADD CONSTRAINT inpatient_claim_lines_parent_claim_to_inpatient_claims FOREIGN KEY (clm_id) REFERENCES public.inpatient_claims (clm_id);

ALTER TABLE public.inpatient_claims
    ADD CONSTRAINT inpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.medicare_beneficiaryid_history
    ADD CONSTRAINT medicare_beneficiaryid_history_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.outpatient_claim_lines
    ADD CONSTRAINT outpatient_claim_lines_parent_claim_to_outpatient_claims FOREIGN KEY (clm_id) REFERENCES public.outpatient_claims (clm_id);

ALTER TABLE public.outpatient_claims
    ADD CONSTRAINT outpatient_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.partd_events
    ADD CONSTRAINT partd_events_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.snf_claim_lines
    ADD CONSTRAINT snf_claim_lines_parent_claim_to_snf_claims FOREIGN KEY (clm_id) REFERENCES public.snf_claims (clm_id);

ALTER TABLE public.snf_claims
    ADD CONSTRAINT snf_claims_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

ALTER TABLE public.loaded_batches
    ADD CONSTRAINT loaded_batches_loaded_fileid FOREIGN KEY (loaded_fileid) REFERENCES public.loaded_files (loaded_fileid);

-- uncomment the following SCRIPT directive to dump the hsql database schema
-- to a file; helpful in tracking down misnamed table or columns in an hsql db.
-- SCRIPT './bfd_schema_post.txt';