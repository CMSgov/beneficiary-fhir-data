DROP TABLE IF EXISTS idr.beneficiary;
DROP SCHEMA IF EXISTS idr;

CREATE SCHEMA idr
    CREATE TABLE beneficiary(
        bene_sk BIGINT NOT NULL PRIMARY KEY, 
        bene_id UUID NOT NULL DEFAULT gen_random_uuid(),
        bene_mbi_id VARCHAR(11),
        bene_1st_name VARCHAR(30),
        bene_last_name VARCHAR(40),
        created_ts TIMESTAMPTZ NOT NULL,
        updated_ts TIMESTAMPTZ);
