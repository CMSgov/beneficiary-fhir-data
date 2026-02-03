
CREATE TABLE idr_new.beneficiary
(
    bene_sk                     bigint NOT NULL,
    bene_xref_efctv_sk          bigint NOT NULL,
    bene_xref_efctv_sk_computed bigint GENERATED ALWAYS AS (
        CASE
            WHEN ((bene_xref_efctv_sk <> bene_sk) AND ((bene_kill_cred_cd)::text <> '2'::text)
) THEN bene_sk
    ELSE bene_xref_efctv_sk
END) STORED NOT NULL,
    bene_mbi_id character varying(11) NOT NULL,
    bene_1st_name character varying(30) NOT NULL,
    bene_midl_name character varying(15) NOT NULL,
    bene_last_name character varying(40) NOT NULL,
    bene_brth_dt date NOT NULL,
    bene_death_dt date NOT NULL,
    bene_vrfy_death_day_sw character varying(1) NOT NULL,
    bene_sex_cd character varying(1) NOT NULL,
    bene_race_cd character varying(2) NOT NULL,
    geo_usps_state_cd character varying(2) NOT NULL,
    geo_zip5_cd character varying(5) NOT NULL,
    geo_zip_plc_name character varying(100) NOT NULL,
    bene_line_1_adr character varying(45) NOT NULL,
    bene_line_2_adr character varying(45) NOT NULL,
    bene_line_3_adr character varying(40) NOT NULL,
    bene_line_4_adr character varying(40) NOT NULL,
    bene_line_5_adr character varying(40) NOT NULL,
    bene_line_6_adr character varying(40) NOT NULL,
    cntct_lang_cd character varying(3) NOT NULL,
    idr_ltst_trans_flg character varying(1) NOT NULL,
    idr_trans_efctv_ts timestamp with time zone NOT NULL,
    idr_trans_obslt_ts timestamp with time zone NOT NULL,
    idr_insrt_ts_bene timestamp with time zone NOT NULL,
    idr_updt_ts_bene timestamp with time zone NOT NULL,
    bene_kill_cred_cd character varying(1) NOT NULL,
    src_rec_updt_ts timestamp with time zone NOT NULL,
    idr_insrt_ts_xref timestamp with time zone NOT NULL,
    idr_updt_ts_xref timestamp with time zone NOT NULL,
    bfd_created_ts timestamp with time zone NOT NULL,
    bfd_updated_ts timestamp with time zone NOT NULL,
    bfd_part_a_coverage_updated_ts timestamp with time zone DEFAULT now(),
    bfd_part_b_coverage_updated_ts timestamp with time zone DEFAULT now(),
    bfd_part_c_coverage_updated_ts timestamp with time zone DEFAULT now(),
    bfd_part_d_coverage_updated_ts timestamp with time zone DEFAULT now(),
    bfd_part_dual_coverage_updated_ts timestamp with time zone DEFAULT now(),
    bfd_patient_updated_ts timestamp with time zone DEFAULT now()
);

CREATE TABLE idr_new.beneficiary_dual_eligibility
(
    bene_sk                  bigint                   NOT NULL,
    bene_mdcd_elgblty_bgn_dt date                     NOT NULL,
    bene_mdcd_elgblty_end_dt date                     NOT NULL,
    bene_dual_stus_cd        character varying(2)     NOT NULL,
    bene_dual_type_cd        character varying(1)     NOT NULL,
    geo_usps_state_cd        character varying(2)     NOT NULL,
    idr_ltst_trans_flg       character varying(1)     NOT NULL,
    idr_trans_efctv_ts       timestamp with time zone NOT NULL,
    idr_trans_obslt_ts       timestamp with time zone NOT NULL,
    idr_insrt_ts             timestamp with time zone NOT NULL,
    idr_updt_ts              timestamp with time zone NOT NULL,
    bfd_created_ts           timestamp with time zone NOT NULL,
    bfd_updated_ts           timestamp with time zone NOT NULL
);


CREATE VIEW idr_new.beneficiary_dual_eligibility_latest AS
SELECT DISTINCT
        ON (bene_sk) bene_sk,
        bene_mdcd_elgblty_bgn_dt,
        bene_mdcd_elgblty_end_dt,
        bene_dual_stus_cd,
        bene_dual_type_cd,
        geo_usps_state_cd,
        idr_ltst_trans_flg,
        idr_trans_efctv_ts,
        idr_trans_obslt_ts,
        idr_insrt_ts,
        idr_updt_ts,
        bfd_created_ts,
        bfd_updated_ts
        FROM idr_new.beneficiary_dual_eligibility
        WHERE (((idr_ltst_trans_flg)::text = 'Y'::text) AND (bene_mdcd_elgblty_bgn_dt <= (now() - '12:00:00':: interval)))
        ORDER BY bene_sk, bene_mdcd_elgblty_bgn_dt DESC;



CREATE TABLE idr_new.beneficiary_entitlement
(
    bene_sk                  bigint                   NOT NULL,
    bene_rng_bgn_dt          date                     NOT NULL,
    bene_rng_end_dt          date                     NOT NULL,
    bene_mdcr_entlmt_type_cd character varying(1)     NOT NULL,
    bene_mdcr_entlmt_stus_cd character varying(1),
    bene_mdcr_enrlmt_rsn_cd  character varying(1),
    idr_ltst_trans_flg       character varying(1)     NOT NULL,
    idr_trans_efctv_ts       timestamp with time zone NOT NULL,
    idr_trans_obslt_ts       timestamp with time zone NOT NULL,
    idr_insrt_ts             timestamp with time zone NOT NULL,
    idr_updt_ts              timestamp with time zone NOT NULL,
    bfd_created_ts           timestamp with time zone NOT NULL,
    bfd_updated_ts           timestamp with time zone NOT NULL
);


CREATE VIEW idr_new.beneficiary_entitlement_latest AS
SELECT DISTINCT
        ON (bene_sk, bene_mdcr_entlmt_type_cd) bene_sk,
        bene_rng_bgn_dt,
        bene_rng_end_dt,
        bene_mdcr_entlmt_type_cd,
        bene_mdcr_entlmt_stus_cd,
        bene_mdcr_enrlmt_rsn_cd,
        idr_ltst_trans_flg,
        idr_trans_efctv_ts,
        idr_trans_obslt_ts,
        idr_insrt_ts,
        idr_updt_ts,
        bfd_created_ts,
        bfd_updated_ts,
        min (bene_rng_bgn_dt) OVER (PARTITION BY bene_sk, bene_mdcr_entlmt_type_cd) AS original_bgn_dt
        FROM idr_new.beneficiary_entitlement
        WHERE (((idr_ltst_trans_flg)::text = 'Y'::text) AND (bene_rng_bgn_dt <= (now() - '12:00:00':: interval)))
        ORDER BY bene_sk, bene_mdcr_entlmt_type_cd, bene_rng_bgn_dt DESC;

CREATE TABLE idr_new.beneficiary_entitlement_reason
(
    bene_sk                 bigint                   NOT NULL,
    bene_rng_bgn_dt         date                     NOT NULL,
    bene_rng_end_dt         date                     NOT NULL,
    bene_mdcr_entlmt_rsn_cd character varying(1),
    idr_ltst_trans_flg      character varying(1)     NOT NULL,
    idr_trans_efctv_ts      timestamp with time zone NOT NULL,
    idr_trans_obslt_ts      timestamp with time zone NOT NULL,
    idr_insrt_ts            timestamp with time zone NOT NULL,
    idr_updt_ts             timestamp with time zone NOT NULL,
    bfd_created_ts          timestamp with time zone NOT NULL,
    bfd_updated_ts          timestamp with time zone NOT NULL
);



CREATE VIEW idr_new.beneficiary_entitlement_reason_latest AS
SELECT DISTINCT
        ON (bene_sk) bene_sk,
        bene_rng_bgn_dt,
        bene_rng_end_dt,
        bene_mdcr_entlmt_rsn_cd,
        idr_ltst_trans_flg,
        idr_trans_efctv_ts,
        idr_trans_obslt_ts,
        idr_insrt_ts,
        idr_updt_ts,
        bfd_created_ts,
        bfd_updated_ts
        FROM idr_new.beneficiary_entitlement_reason
        WHERE (((idr_ltst_trans_flg)::text = 'Y'::text) AND (bene_rng_bgn_dt <= (now() - '12:00:00':: interval)))
        ORDER BY bene_sk, bene_rng_bgn_dt DESC;

CREATE TABLE idr_new.beneficiary_mbi_id
(
    bene_mbi_id        character varying(11)    NOT NULL,
    bene_mbi_efctv_dt  date                     NOT NULL,
    bene_mbi_obslt_dt  date                     NOT NULL,
    idr_ltst_trans_flg character varying(1)     NOT NULL,
    idr_trans_efctv_ts timestamp with time zone NOT NULL,
    idr_trans_obslt_ts timestamp with time zone NOT NULL,
    idr_insrt_ts       timestamp with time zone NOT NULL,
    idr_updt_ts        timestamp with time zone NOT NULL,
    bfd_created_ts     timestamp with time zone NOT NULL,
    bfd_updated_ts     timestamp with time zone NOT NULL
);

CREATE TABLE idr_new.beneficiary_overshare_mbi
(
    bene_mbi_id    character varying(11)    NOT NULL,
    bfd_created_ts timestamp with time zone NOT NULL
);


CREATE VIEW idr_new.valid_beneficiary AS
SELECT bene_sk,
       bene_xref_efctv_sk,
       bene_xref_efctv_sk_computed,
       bene_mbi_id,
       bene_1st_name,
       bene_midl_name,
       bene_last_name,
       bene_brth_dt,
       bene_death_dt,
       bene_vrfy_death_day_sw,
       bene_sex_cd,
       bene_race_cd,
       geo_usps_state_cd,
       geo_zip5_cd,
       geo_zip_plc_name,
       bene_line_1_adr,
       bene_line_2_adr,
       bene_line_3_adr,
       bene_line_4_adr,
       bene_line_5_adr,
       bene_line_6_adr,
       cntct_lang_cd,
       idr_ltst_trans_flg,
       idr_trans_efctv_ts,
       idr_trans_obslt_ts,
       idr_insrt_ts_bene,
       idr_updt_ts_bene,
       bene_kill_cred_cd,
       src_rec_updt_ts,
       idr_insrt_ts_xref,
       idr_updt_ts_xref,
       bfd_created_ts,
       bfd_updated_ts,
       bfd_part_a_coverage_updated_ts,
       bfd_part_b_coverage_updated_ts,
       bfd_part_c_coverage_updated_ts,
       bfd_part_d_coverage_updated_ts,
       bfd_part_dual_coverage_updated_ts,
       bfd_patient_updated_ts
FROM idr_new.beneficiary b
WHERE (NOT (EXISTS (SELECT 1
                    FROM idr_new.beneficiary_overshare_mbi om
                    WHERE ((om.bene_mbi_id)::text = (b.bene_mbi_id)::text))));

CREATE VIEW idr_new.beneficiary_identity AS
SELECT DISTINCT bene.bene_sk,
                bene.bene_xref_efctv_sk_computed,
                bene.bene_mbi_id,
                bene_mbi.bene_mbi_efctv_dt,
                bene_mbi.bene_mbi_obslt_dt
FROM (idr_new.valid_beneficiary bene
    LEFT JOIN idr_new.beneficiary_mbi_id bene_mbi
      ON ((((bene.bene_mbi_id)::text = (bene_mbi . bene_mbi_id)::text) AND ((bene_mbi.idr_ltst_trans_flg)::text = 'Y'::text))));


--
-- Name: beneficiary_low_income_subsidy; Type: TABLE; Schema: idr; Owner: -
--

CREATE TABLE idr_new.beneficiary_low_income_subsidy
(
    bene_sk               bigint                   NOT NULL,
    bene_rng_bgn_dt       date                     NOT NULL,
    bene_rng_end_dt       date                     NOT NULL,
    bene_lis_copmt_lvl_cd character varying(1)     NOT NULL,
    bene_lis_ptd_prm_pct  character varying(3),
    idr_ltst_trans_flg    character varying(1)     NOT NULL,
    idr_trans_efctv_ts    timestamp with time zone NOT NULL,
    idr_trans_obslt_ts    timestamp with time zone NOT NULL,
    idr_insrt_ts          timestamp with time zone NOT NULL,
    idr_updt_ts           timestamp with time zone NOT NULL,
    bfd_created_ts        timestamp with time zone NOT NULL,
    bfd_updated_ts        timestamp with time zone NOT NULL
);


--
-- Name: beneficiary_ma_part_d_enrollment; Type: TABLE; Schema: idr; Owner: -
--

CREATE TABLE idr_new.beneficiary_ma_part_d_enrollment
(
    bene_sk                    bigint                   NOT NULL,
    cntrct_pbp_sk              bigint                   NOT NULL,
    bene_pbp_num               character varying(3)     NOT NULL,
    bene_enrlmt_bgn_dt         date                     NOT NULL,
    bene_enrlmt_end_dt         date,
    bene_cntrct_num            character varying(5)     NOT NULL,
    bene_cvrg_type_cd          character varying(2)     NOT NULL,
    bene_enrlmt_pgm_type_cd    character varying(4)     NOT NULL,
    bene_enrlmt_emplr_sbsdy_sw character varying(1),
    idr_ltst_trans_flg         character varying(1)     NOT NULL,
    idr_trans_efctv_ts         timestamp with time zone NOT NULL,
    idr_trans_obslt_ts         timestamp with time zone NOT NULL,
    idr_insrt_ts               timestamp with time zone NOT NULL,
    idr_updt_ts                timestamp with time zone NOT NULL,
    bfd_created_ts             timestamp with time zone NOT NULL,
    bfd_updated_ts             timestamp with time zone NOT NULL
);


--
-- Name: beneficiary_ma_part_d_enrollment_rx; Type: TABLE; Schema: idr; Owner: -
--

CREATE TABLE idr_new.beneficiary_ma_part_d_enrollment_rx
(
    bene_sk                        bigint                   NOT NULL,
    cntrct_pbp_sk                  bigint                   NOT NULL,
    bene_cntrct_num                character varying(5)     NOT NULL,
    bene_pbp_num                   character varying(3)     NOT NULL,
    bene_enrlmt_bgn_dt             date                     NOT NULL,
    bene_pdp_enrlmt_mmbr_id_num    character varying(20)    NOT NULL,
    bene_pdp_enrlmt_grp_num        character varying(15)    NOT NULL,
    bene_pdp_enrlmt_prcsr_num      character varying(10)    NOT NULL,
    bene_pdp_enrlmt_bank_id_num    character varying(6),
    bene_enrlmt_pdp_rx_info_bgn_dt date                     NOT NULL,
    idr_ltst_trans_flg             character varying(1)     NOT NULL,
    idr_trans_efctv_ts             timestamp with time zone NOT NULL,
    idr_trans_obslt_ts             timestamp with time zone NOT NULL,
    idr_insrt_ts                   timestamp with time zone NOT NULL,
    idr_updt_ts                    timestamp with time zone NOT NULL,
    bfd_created_ts                 timestamp with time zone NOT NULL,
    bfd_updated_ts                 timestamp with time zone NOT NULL
);


--
-- Name: beneficiary_part_c_and_d_enrollment; Type: VIEW; Schema: idr; Owner: -
--

CREATE VIEW idr_new.beneficiary_part_c_and_d_enrollment AS
SELECT e.bene_sk,
       e.bene_enrlmt_pgm_type_cd,
       e.bene_enrlmt_bgn_dt,
       e.bene_enrlmt_end_dt,
       e.bene_cntrct_num,
       e.bene_pbp_num,
       e.bene_cvrg_type_cd,
       e.bene_enrlmt_emplr_sbsdy_sw,
       COALESCE(rx.bene_enrlmt_pdp_rx_info_bgn_dt, '9999-12-31'::date) AS bene_enrlmt_pdp_rx_info_bgn_dt,
       rx.bene_pdp_enrlmt_mmbr_id_num,
       rx.bene_pdp_enrlmt_grp_num,
       rx.bene_pdp_enrlmt_prcsr_num,
       rx.bene_pdp_enrlmt_bank_id_num
FROM (idr_new.beneficiary_ma_part_d_enrollment e
    LEFT JOIN idr_new.beneficiary_ma_part_d_enrollment_rx rx
      ON (((e.bene_sk = rx.bene_sk) AND (e.bene_enrlmt_bgn_dt = rx.bene_enrlmt_bgn_dt) AND
           ((e.bene_cntrct_num)::text = (rx . bene_cntrct_num)::text) AND ((e.bene_pbp_num)::text = (rx.bene_pbp_num)::text) AND ((e.bene_enrlmt_pgm_type_cd)::text = ANY ((ARRAY['2'::character varying, '3'::character varying])::text[])))));


--
-- Name: beneficiary_status; Type: TABLE; Schema: idr; Owner: -
--

CREATE TABLE idr_new.beneficiary_status
(
    bene_sk            bigint                   NOT NULL,
    bene_mdcr_stus_cd  character varying(2)     NOT NULL,
    mdcr_stus_bgn_dt   date                     NOT NULL,
    mdcr_stus_end_dt   date                     NOT NULL,
    idr_ltst_trans_flg character varying(1)     NOT NULL,
    idr_trans_efctv_ts timestamp with time zone NOT NULL,
    idr_trans_obslt_ts timestamp with time zone NOT NULL,
    idr_insrt_ts       timestamp with time zone NOT NULL,
    idr_updt_ts        timestamp with time zone NOT NULL,
    bfd_created_ts     timestamp with time zone NOT NULL,
    bfd_updated_ts     timestamp with time zone NOT NULL
);


--
-- Name: beneficiary_status_latest; Type: VIEW; Schema: idr; Owner: -
--

CREATE VIEW idr_new.beneficiary_status_latest AS
SELECT DISTINCT
        ON (bene_sk) bene_sk,
        bene_mdcr_stus_cd,
        mdcr_stus_bgn_dt,
        mdcr_stus_end_dt,
        idr_ltst_trans_flg,
        idr_trans_efctv_ts,
        idr_trans_obslt_ts,
        idr_insrt_ts,
        idr_updt_ts,
        bfd_created_ts,
        bfd_updated_ts
        FROM idr_new.beneficiary_status
        WHERE (((idr_ltst_trans_flg)::text = 'Y'::text) AND (mdcr_stus_bgn_dt <= (now() - '12:00:00':: interval)))
        ORDER BY bene_sk, mdcr_stus_bgn_dt DESC;


--
-- Name: beneficiary_third_party; Type: TABLE; Schema: idr; Owner: -
--

CREATE TABLE idr_new.beneficiary_third_party
(
    bene_sk            bigint                   NOT NULL,
    bene_buyin_cd      character varying(2)     NOT NULL,
    bene_tp_type_cd    character varying(1)     NOT NULL,
    bene_rng_bgn_dt    date                     NOT NULL,
    bene_rng_end_dt    date                     NOT NULL,
    idr_ltst_trans_flg character varying(1)     NOT NULL,
    idr_trans_efctv_ts timestamp with time zone NOT NULL,
    idr_trans_obslt_ts timestamp with time zone NOT NULL,
    idr_insrt_ts       timestamp with time zone NOT NULL,
    idr_updt_ts        timestamp with time zone NOT NULL,
    bfd_created_ts     timestamp with time zone NOT NULL,
    bfd_updated_ts     timestamp with time zone NOT NULL
);


--
-- Name: beneficiary_third_party_latest; Type: VIEW; Schema: idr; Owner: -
--

CREATE VIEW idr_new.beneficiary_third_party_latest AS
SELECT DISTINCT
        ON (bene_sk, bene_tp_type_cd) bene_sk,
        bene_buyin_cd,
        bene_tp_type_cd,
        bene_rng_bgn_dt,
        bene_rng_end_dt,
        idr_ltst_trans_flg,
        idr_trans_efctv_ts,
        idr_trans_obslt_ts,
        idr_insrt_ts,
        idr_updt_ts,
        bfd_created_ts,
        bfd_updated_ts
        FROM idr_new.beneficiary_third_party
        WHERE (((idr_ltst_trans_flg)::text = 'Y'::text) AND (bene_rng_bgn_dt <= (now() - '12:00:00':: interval)))
        ORDER BY bene_sk, bene_tp_type_cd, bene_rng_bgn_dt DESC;


CREATE TABLE idr_new.contract_pbp_contact
(
    cntrct_pbp_sk                bigint                   NOT NULL,
    cntrct_plan_cntct_obslt_dt   date                     NOT NULL,
    cntrct_plan_cntct_type_cd    character varying(3)     NOT NULL,
    cntrct_plan_free_extnsn_num  character varying(7)     NOT NULL,
    cntrct_plan_cntct_free_num   character varying(10)    NOT NULL,
    cntrct_plan_cntct_extnsn_num character varying(7)     NOT NULL,
    cntrct_plan_cntct_tel_num    character varying(10)    NOT NULL,
    cntrct_pbp_end_dt            date                     NOT NULL,
    cntrct_pbp_bgn_dt            date                     NOT NULL,
    cntrct_plan_cntct_st_1_adr   character varying(55)    NOT NULL,
    cntrct_plan_cntct_st_2_adr   character varying(55)    NOT NULL,
    cntrct_plan_cntct_city_name  character varying(30)    NOT NULL,
    cntrct_plan_cntct_state_cd   character varying(2)     NOT NULL,
    cntrct_plan_cntct_zip_cd     character varying(9)     NOT NULL,
    bfd_created_ts               timestamp with time zone NOT NULL
);

CREATE TABLE idr_new.contract_pbp_number
(
    cntrct_pbp_sk           bigint                   NOT NULL,
    cntrct_drug_plan_ind_cd character varying(1)     NOT NULL,
    cntrct_pbp_type_cd      character varying(2)     NOT NULL,
    bfd_created_ts          timestamp with time zone NOT NULL,
    cntrct_pbp_name         character varying(75),
    cntrct_num              character varying(5),
    cntrct_pbp_num          character varying(3),
    cntrct_pbp_sgmt_num     character varying(3)
);

CREATE TABLE idr_new.load_progress
(
    id                integer                  NOT NULL,
    table_name        text                     NOT NULL,
    last_ts           timestamp with time zone NOT NULL,
    last_id           bigint                   NOT NULL,
    batch_start_ts    timestamp with time zone NOT NULL,
    batch_complete_ts timestamp with time zone NOT NULL,
    batch_partition   text DEFAULT ''::text NOT NULL,
    job_start_ts      timestamp with time zone NOT NULL
);

ALTER TABLE idr_new.load_progress ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME idr_new.load_progress_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE ONLY idr_new.beneficiary_dual_eligibility
    ADD CONSTRAINT beneficiary_dual_eligibility_pkey PRIMARY KEY (bene_sk, bene_mdcd_elgblty_bgn_dt, idr_trans_efctv_ts);

ALTER TABLE ONLY idr_new.beneficiary_entitlement
    ADD CONSTRAINT beneficiary_entitlement_pkey PRIMARY KEY (bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_mdcr_entlmt_type_cd, idr_trans_efctv_ts);

ALTER TABLE ONLY idr_new.beneficiary_entitlement_reason
    ADD CONSTRAINT beneficiary_entitlement_reason_pkey PRIMARY KEY (bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, idr_trans_efctv_ts);

ALTER TABLE ONLY idr_new.beneficiary_low_income_subsidy
    ADD CONSTRAINT beneficiary_low_income_subsidy_pkey PRIMARY KEY (bene_sk, bene_rng_bgn_dt);

ALTER TABLE ONLY idr_new.beneficiary_ma_part_d_enrollment
    ADD CONSTRAINT beneficiary_ma_part_d_enrollment_pkey PRIMARY KEY (bene_sk, bene_enrlmt_bgn_dt, bene_enrlmt_pgm_type_cd);

ALTER TABLE ONLY idr_new.beneficiary_ma_part_d_enrollment_rx
    ADD CONSTRAINT beneficiary_ma_part_d_enrollment_rx_pkey PRIMARY KEY (bene_sk, bene_cntrct_num, bene_pbp_num, bene_enrlmt_bgn_dt, bene_enrlmt_pdp_rx_info_bgn_dt);


ALTER TABLE ONLY idr_new.beneficiary_mbi_id
    ADD CONSTRAINT beneficiary_mbi_id_pkey PRIMARY KEY (bene_mbi_id, idr_trans_efctv_ts);


ALTER TABLE ONLY idr_new.beneficiary_overshare_mbi
    ADD CONSTRAINT beneficiary_overshare_mbi_pkey PRIMARY KEY (bene_mbi_id);

ALTER TABLE ONLY idr_new.beneficiary
    ADD CONSTRAINT beneficiary_pkey PRIMARY KEY (bene_sk, idr_trans_efctv_ts);

ALTER TABLE ONLY idr_new.beneficiary_status
    ADD CONSTRAINT beneficiary_status_pkey PRIMARY KEY (bene_sk, mdcr_stus_bgn_dt, mdcr_stus_end_dt, idr_trans_efctv_ts);

ALTER TABLE ONLY idr_new.beneficiary_third_party
    ADD CONSTRAINT beneficiary_third_party_pkey PRIMARY KEY (bene_sk, bene_rng_bgn_dt, bene_rng_end_dt, bene_tp_type_cd, idr_trans_efctv_ts);

ALTER TABLE ONLY idr_new.contract_pbp_contact
    ADD CONSTRAINT contract_pbp_contact_pkey PRIMARY KEY (cntrct_pbp_sk);

ALTER TABLE ONLY idr_new.contract_pbp_number
    ADD CONSTRAINT contract_pbp_number_pkey PRIMARY KEY (cntrct_pbp_sk);

ALTER TABLE ONLY idr_new.load_progress
    ADD CONSTRAINT load_progress_table_name_batch_partition_key UNIQUE (table_name, batch_partition);

CREATE INDEX beneficiary_bene_mbi_id_idx ON idr_new.beneficiary USING btree (bene_mbi_id);

CREATE INDEX beneficiary_bene_xref_efctv_sk_computed_idx ON idr_new.beneficiary USING btree (bene_xref_efctv_sk_computed);

CREATE UNIQUE INDEX contract_pbp_number_cntrct_pbp_num_cntrct_num_idx ON idr_new.contract_pbp_number USING btree (cntrct_pbp_num, cntrct_num);