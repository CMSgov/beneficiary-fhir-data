drop table if exists migration_errors;

create table migration_errors
(
    table_name                      varchar(80) not null,
    bene_id							bigint		not null,
    clm_id							bigint,
    line_num						smallint,
    err_cnt							smallint	not null,
    constraint migration_errors_pkey primary key (table_name,bene_id)
);
