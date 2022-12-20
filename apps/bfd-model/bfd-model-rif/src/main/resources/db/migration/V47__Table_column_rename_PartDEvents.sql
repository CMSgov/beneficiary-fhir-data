-- The V40 through V52 migrations rename our tables and columns for CCW-sourced data, such that:
-- 1. We follow PostgreSQL's general snake_case naming conventions, to improve the developer experience: DB
--    object names won't have to be quoted all over the place, anymore.
-- 2. Column names match those in our upstream source system, the CCW, to improve traceability as data flows
--    through our systems.
-- 3. Rename the "parentXXX" foreign key columns to instead have names that match their target column.

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
-- PartDEvents to partd_events
--
alter table public."PartDEvents" rename to partd_events;
alter table public.partd_events ${logic.alter-rename-column} "eventId" ${logic.rename-to} pde_id;
alter table public.partd_events ${logic.alter-rename-column} "beneficiaryId" ${logic.rename-to} bene_id;
alter table public.partd_events ${logic.alter-rename-column} "claimGroupId" ${logic.rename-to} clm_grp_id;
alter table public.partd_events ${logic.alter-rename-column} lastupdated ${logic.rename-to} last_updated;
alter table public.partd_events ${logic.alter-rename-column} "adjustmentDeletionCode" ${logic.rename-to} adjstmt_dltn_cd;
alter table public.partd_events ${logic.alter-rename-column} "brandGenericCode" ${logic.rename-to} brnd_gnrc_cd;
alter table public.partd_events ${logic.alter-rename-column} "compoundCode" ${logic.rename-to} cmpnd_cd;
alter table public.partd_events ${logic.alter-rename-column} "catastrophicCoverageCode" ${logic.rename-to} ctstrphc_cvrg_cd;
alter table public.partd_events ${logic.alter-rename-column} "partDPlanCoveredPaidAmount" ${logic.rename-to} cvrd_d_plan_pd_amt;
alter table public.partd_events ${logic.alter-rename-column} "dispenseAsWrittenProductSelectionCode" ${logic.rename-to} daw_prod_slctn_cd;
alter table public.partd_events ${logic.alter-rename-column} "daysSupply" ${logic.rename-to} days_suply_num;
alter table public.partd_events ${logic.alter-rename-column} "drugCoverageStatusCode" ${logic.rename-to} drug_cvrg_stus_cd;
alter table public.partd_events ${logic.alter-rename-column} "dispensingStatusCode" ${logic.rename-to} dspnsng_stus_cd;
alter table public.partd_events ${logic.alter-rename-column} "fillNumber" ${logic.rename-to} fill_num;
alter table public.partd_events ${logic.alter-rename-column} "finalAction" ${logic.rename-to} final_action;
alter table public.partd_events ${logic.alter-rename-column} "grossCostAboveOutOfPocketThreshold" ${logic.rename-to} gdc_abv_oopt_amt;
alter table public.partd_events ${logic.alter-rename-column} "grossCostBelowOutOfPocketThreshold" ${logic.rename-to} gdc_blw_oopt_amt;
alter table public.partd_events ${logic.alter-rename-column} "lowIncomeSubsidyPaidAmount" ${logic.rename-to} lics_amt;
alter table public.partd_events ${logic.alter-rename-column} "nationalDrugCode" ${logic.rename-to} prod_srvc_id;
alter table public.partd_events ${logic.alter-rename-column} "partDPlanNonCoveredPaidAmount" ${logic.rename-to} ncvrd_plan_pd_amt;
alter table public.partd_events ${logic.alter-rename-column} "nonstandardFormatCode" ${logic.rename-to} nstd_frmt_cd;
alter table public.partd_events ${logic.alter-rename-column} "otherTrueOutOfPocketPaidAmount" ${logic.rename-to} othr_troop_amt;
alter table public.partd_events ${logic.alter-rename-column} "paymentDate" ${logic.rename-to} pd_dt;
alter table public.partd_events ${logic.alter-rename-column} "pharmacyTypeCode" ${logic.rename-to} phrmcy_srvc_type_cd;
alter table public.partd_events ${logic.alter-rename-column} "planContractId" ${logic.rename-to} plan_cntrct_rec_id;
alter table public.partd_events ${logic.alter-rename-column} "planBenefitPackageId" ${logic.rename-to} plan_pbp_rec_num;
alter table public.partd_events ${logic.alter-rename-column} "patientLiabilityReductionOtherPaidAmount" ${logic.rename-to} plro_amt;
alter table public.partd_events ${logic.alter-rename-column} "pricingExceptionCode" ${logic.rename-to} prcng_excptn_cd;
alter table public.partd_events ${logic.alter-rename-column} "prescriberId" ${logic.rename-to} prscrbr_id;
alter table public.partd_events ${logic.alter-rename-column} "prescriberIdQualifierCode" ${logic.rename-to} prscrbr_id_qlfyr_cd;
alter table public.partd_events ${logic.alter-rename-column} "patientPaidAmount" ${logic.rename-to} ptnt_pay_amt;
alter table public.partd_events ${logic.alter-rename-column} "patientResidenceCode" ${logic.rename-to} ptnt_rsdnc_cd;
alter table public.partd_events ${logic.alter-rename-column} "quantityDispensed" ${logic.rename-to} qty_dspnsd_num;
alter table public.partd_events ${logic.alter-rename-column} "gapDiscountAmount" ${logic.rename-to} rptd_gap_dscnt_num;
alter table public.partd_events ${logic.alter-rename-column} "prescriptionOriginationCode" ${logic.rename-to} rx_orgn_cd;
alter table public.partd_events ${logic.alter-rename-column} "prescriptionReferenceNumber" ${logic.rename-to} rx_srvc_rfrnc_num;
alter table public.partd_events ${logic.alter-rename-column} "prescriptionFillDate" ${logic.rename-to} srvc_dt;
alter table public.partd_events ${logic.alter-rename-column} "serviceProviderId" ${logic.rename-to} srvc_prvdr_id;
alter table public.partd_events ${logic.alter-rename-column} "serviceProviderIdQualiferCode" ${logic.rename-to} srvc_prvdr_id_qlfyr_cd;
alter table public.partd_events ${logic.alter-rename-column} "submissionClarificationCode" ${logic.rename-to} submsn_clr_cd;
alter table public.partd_events ${logic.alter-rename-column} "totalPrescriptionCost" ${logic.rename-to} tot_rx_cst_amt;

-- psql only
${logic.psql-only-alter} index if exists public."PartDEvents_pkey" rename to partd_events_pkey;
${logic.psql-only-alter} table public.partd_events rename constraint "PartDEvents_beneficiaryId_to_Beneficiaries" to partd_events_bene_id_to_beneficiaries;

-- hsql only
${logic.hsql-only-alter} table public.partd_events add constraint partd_events_pkey primary key (pde_id); 
${logic.hsql-only-alter} table public.partd_events add constraint partd_events_bene_id_to_beneficiaries FOREIGN KEY (bene_id) REFERENCES public.beneficiaries(bene_id);

-- both psql and hsql support non-primary key index renaming
ALTER INDEX "PartDEvents_beneficiaryId_idx" RENAME TO partd_events_bene_id_idx;
