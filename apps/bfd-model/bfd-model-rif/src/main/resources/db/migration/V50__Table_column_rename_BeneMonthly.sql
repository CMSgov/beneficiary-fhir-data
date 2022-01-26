-- Rename tables and table columns; syntax:
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

-- psql only
${logic.psql-only-alter} index if exists public."BeneficiaryMonthly_pkey" rename to beneficiary_monthly_pkey;

${logic.psql-only-alter} table public.beneficiary_monthly rename constraint "BeneficiaryMonthly_parentBeneficiary_to_Beneficiary" to beneficiary_monthly_bene_id_to_beneficiary;

-- hsql only
${logic.hsql-only-alter} table public.beneficiary_monthly add constraint beneficiary_monthly_pkey primary key (bene_id, year_month);
    
${logic.hsql-only-alter} table public.beneficiary_monthly ADD CONSTRAINT beneficiary_monthly_bene_id_to_beneficiary FOREIGN KEY (bene_id) REFERENCES public.beneficiaries (bene_id);

-- both psql and hsql support non-primary key index renaming
ALTER INDEX "BeneficiaryMonthly_partDContractNumId_yearMonth_parentBene_idx" RENAME TO beneficiary_monthly_year_month_partd_contract_bene_id_idx;
ALTER INDEX "BeneficiaryMonthly_partDContractNumberId_yearmonth_idx" RENAME TO beneficiary_monthly_partd_contract_number_year_month_idx;
