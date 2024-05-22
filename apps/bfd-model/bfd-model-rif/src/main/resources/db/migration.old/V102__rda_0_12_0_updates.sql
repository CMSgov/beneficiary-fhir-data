-- As part of the 0.12 RDA API update, new claim level fields added and revenue lines support added

alter table rda.fiss_claims add received_date_text varchar(10);
alter table rda.fiss_claims add curr_tran_date_text varchar(10);
alter table rda.fiss_claims add drg_cd varchar(4);
alter table rda.fiss_claims add group_code varchar(2);
alter table rda.fiss_claims add clm_typ_ind varchar(1);

/*
 * fiss_revenue_lines
 */
CREATE TABLE rda.fiss_revenue_lines (
    dcn varchar(23) NOT NULL,
    rda_position smallint NOT NULL,
    non_bill_rev_code varchar(1) NOT NULL,
    rev_cd varchar(4),
    rev_units_billed integer,
    rev_serv_unit_cnt integer,
    serv_dt_cymd date,
    serv_dt_cymd_text varchar(10),
    hcpc_cd varchar(5),
    hcpc_ind varchar(1),
    hcpc_modifier varchar(2),
    hcpc_modifier2 varchar(2),
    hcpc_modifier3 varchar(2),
    hcpc_modifier4 varchar(2),
    hcpc_modifier5 varchar(2),
    apc_hcpcs_apc varchar(5),
    aco_red_rarc varchar(5),
    aco_red_carc varchar(3),
    aco_red_cagc varchar(2),
    CONSTRAINT fiss_revenue_lines_key PRIMARY KEY (dcn, rda_position),
    CONSTRAINT fiss_revenue_lines_parent FOREIGN KEY (dcn) REFERENCES rda.fiss_claims(dcn)
);
