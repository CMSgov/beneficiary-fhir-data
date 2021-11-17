-- ---------------------
-- beneficiaries
-- ---------------------
alter table beneficiaries
    drop constraint if exists beneficiaries_pkey;

alter table beneficiaries
    add constraint beneficiaries_pkey primary key (bene_id);

-- ---------------------
-- beneficiaries_history
-- ---------------------
alter table beneficiaries_history
    add constraint beneficiaries_history_bene_id_to_beneficiaries
        foreign key (bene_id)
            references beneficiaries (bene_id) match simple
            on update no action
            on delete no action;

-- ---------------------
-- beneficiary_monthly
-- ---------------------
alter table beneficiary_monthly
    add constraint beneficiary_monthly_parent_beneficiary_to_beneficiary
        foreign key (parent_beneficiary) references beneficiaries(bene_id);

-- ------------------------------
-- medicare_beneficiaryid_history
-- ------------------------------
alter table medicare_beneficiaryid_history
    add constraint medicare_beneficiaryid_history_bene_id_to_beneficiaries
        foreign key (bene_id)
            references beneficiaries (bene_id) match simple
            on update no action
            on delete no action;

-- ----------------------------
-- carrier_claims
-- ----------------------------
alter table carrier_claims
    drop constraint if exists carrier_claims_pkey;

alter table carrier_claims
    add constraint carrier_claims_pkey primary key (clm_id);

alter table carrier_claims
    add constraint carrier_claims_bene_id_to_beneficiary
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- carrier_claim_lines
-- ----------------------------
alter table carrier_claim_lines
    add constraint carrier_claim_lines_parent_claim_to_dme_claims
        foreign key (parent_claim) references carrier_claims(clm_id);

-- ----------------------------
-- dme_claims
-- ----------------------------
alter table dme_claims
    drop constraint if exists dme_claims_pkey;

alter table dme_claims
    add constraint dme_claims_pkey primary key (clm_id);

alter table dme_claims
    add constraint dme_claims_bene_id_to_beneficiary
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- dme_claim_lines
-- ----------------------------
alter table dme_claim_lines
    add constraint dme_claim_lines_parent_claim_to_dme_claims
        foreign key (parent_claim) references dme_claims(clm_id);

-- ----------------------------
-- hha_claims
-- ----------------------------
alter table hha_claims
    drop constraint if exists hha_claims_pkey;

alter table hha_claims
    add constraint hha_claims_pkey primary key (clm_id);

alter table hha_claims
    add constraint hha_claims_bene_id_to_beneficiary
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- hha_claim_lines
-- ----------------------------
alter table hha_claim_lines
    add constraint hha_claim_lines_parent_claim_to_hha_claims
        foreign key (parent_claim) references hha_claims(clm_id);

-- ----------------------------
-- hospice_claims
-- ----------------------------
alter table hospice_claims
    drop constraint if exists hospice_claims_pkey;

alter table hospice_claims
    add constraint hospice_claims_pkey primary key (clm_id);

alter table hospice_claims
    add constraint hospice_claims_bene_id_to_beneficiary
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- hospice_claim_lines
-- ----------------------------
alter table hospice_claim_lines
    add constraint hospice_claim_lines_parent_claim_to_hospice_claims
        foreign key (parent_claim) references hospice_claims(clm_id);

-- ----------------------------
-- inpatient_claims
-- ----------------------------
alter table inpatient_claims
    drop constraint if exists inpatient_claims_pkey;

alter table inpatient_claims
    add constraint inpatient_claims_pkey primary key (clm_id);

alter table inpatient_claims
    add constraint inpatient_claims_bene_id_to_beneficiary
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- inpatient_claim_lines
-- ----------------------------
alter table inpatient_claim_lines
    add constraint inpatient_claim_lines_parent_claim_to_inpatient_claims
        foreign key (parent_claim) references inpatient_claims(clm_id);

-- ----------------------------
-- outpatient_claims
-- ----------------------------
alter table outpatient_claims
    drop constraint if exists outpatient_claims_pkey;

alter table outpatient_claims
    add constraint outpatient_claims_pkey primary key (clm_id);

alter table outpatient_claims
    add constraint outpatient_claims_bene_id_to_beneficiary
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- outpatient_claim_lines
-- ----------------------------
alter table outpatient_claim_lines
    add constraint outpatient_claim_lines_parent_claim_to_outpatient_claims
        foreign key (parent_claim) references outpatient_claims(clm_id);

-- ----------------------------
-- partd_events
-- ----------------------------
alter table partd_events
    add constraint partd_events_bene_id_to_beneficiaries
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- snf_claims
-- ----------------------------
alter table snf_claims
    drop constraint if exists snf_claims_pkey;

alter table snf_claims
    add constraint snf_claims_pkey primary key (clm_id);

alter table snf_claims
    add constraint snf_claims_bene_id_to_beneficiary
        foreign key (bene_id) references beneficiaries(bene_id);

-- ----------------------------
-- snf_claim_lines
-- ----------------------------
alter table snf_claim_lines
    add constraint snf_claim_lines_parent_claim_to_snf_claims
        foreign key (parent_claim) references snf_claims(clm_id);
