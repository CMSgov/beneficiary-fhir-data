ALTER TABLE idr.prior_auth
ADD COLUMN bfd_order_refer_careteam_name character varying(135),
ADD COLUMN bfd_order_refer_npi_type      integer,
ADD COLUMN bfd_render_careteam_name      character varying(135),
ADD COLUMN bfd_render_npi_type           integer,
ADD COLUMN bfd_att_phy_careteam_name     character varying(135),
ADD COLUMN bfd_att_phy_npi_type          integer;

ALTER TABLE idr.prior_auth
ADD COLUMN resource_id UUID DEFAULT gen_random_uuid();

CREATE TABLE idr.prior_auth_item (
    mbi_num VARCHAR(11) NOT NULL,
    utn VARCHAR(14) NOT NULL,
    current_segment INT NOT NULL,
    hcpcs_or_cpt_or_hipps VARCHAR(5) NOT NULL,
    price_mod1 VARCHAR(2) NOT NULL,
    price_mod2 VARCHAR(2) NOT NULL,
    place_of_serv VARCHAR(2) NOT NULL,
    rev_code_1 VARCHAR(4) NOT NULL,
    pa_dt_added DATE NOT NULL,
    pa_dt_updated DATE,
    pa_decision VARCHAR(1) NOT NULL,
    pa_req_sub_dt DATE NOT NULL,
    pa_req_rec_dt DATE NOT NULL,
    pa_decision_dt DATE,
    pa_decision_exp_dt DATE,
    service_cnts INT NOT NULL,
    svc_render_st VARCHAR(2) NOT NULL,
    mr_count_ind INT NOT NULL,
    mr_count_st_dt DATE,
    mr_count_end_dt DATE,
    rrb_excl_ind VARCHAR(1) NOT NULL,
    bfd_created_ts TIMESTAMPTZ NOT NULL,
    bfd_updated_ts TIMESTAMPTZ,
    PRIMARY KEY(mbi_num, utn, current_segment)
);

ALTER TABLE idr.prior_auth
DROP CONSTRAINT prior_auth_pkey,
DROP COLUMN current_segment,
ADD CONSTRAINT prior_auth_pkey PRIMARY KEY (mbi_num, utn);

ALTER TABLE idr.prior_auth
DROP COLUMN hcpcs_or_cpt_or_hipps,
DROP COLUMN price_mod1,
DROP COLUMN price_mod2,
DROP COLUMN place_of_serv,
DROP COLUMN rev_code_1,
DROP COLUMN pa_dt_added,
DROP COLUMN pa_dt_updated,
DROP COLUMN pa_decision,
DROP COLUMN pa_req_sub_dt,
DROP COLUMN pa_req_rec_dt,
DROP COLUMN pa_decision_dt,
DROP COLUMN pa_decision_exp_dt,
DROP COLUMN service_cnts,
DROP COLUMN svc_render_st,
DROP COLUMN mr_count_ind,
DROP COLUMN mr_count_st_dt,
DROP COLUMN mr_count_end_dt,
DROP COLUMN rrb_excl_ind;

