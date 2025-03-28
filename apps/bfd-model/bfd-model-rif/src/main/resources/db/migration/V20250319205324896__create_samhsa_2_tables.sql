DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tag_code') THEN
        CREATE TYPE tag_code AS ENUM('R', '42CFRPart2');
    END IF;
END$$;

CREATE TABLE IF NOT EXISTS ccw.hha_tags (
    clm_id BIGINT NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES ccw.hha_claims(clm_id)
    );
CREATE INDEX IF NOT EXISTS hha_tags_clm_id_idx
    ON ccw.hha_tags(clm_id);

CREATE TABLE IF NOT EXISTS ccw.carrier_tags (
    clm_id BIGINT NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES ccw.carrier_claims(clm_id)
    );
CREATE INDEX IF NOT EXISTS carrier_tags_clm_id_idx
    ON ccw.carrier_tags(clm_id);

CREATE TABLE IF NOT EXISTS ccw.dme_tags (
    clm_id BIGINT NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES ccw.dme_claims(clm_id)
    );
CREATE INDEX IF NOT EXISTS dme_tags_clm_id_idx
    ON ccw.dme_tags(clm_id);

CREATE TABLE IF NOT EXISTS ccw.hospice_tags (
    clm_id BIGINT NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES ccw.hospice_claims(clm_id)
    );
CREATE INDEX IF NOT EXISTS hospice_tags_clm_id_idx
    ON ccw.hospice_tags(clm_id);

CREATE TABLE IF NOT EXISTS ccw.outpatient_tags (
    clm_id BIGINT NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES ccw.outpatient_claims(clm_id)
    );
CREATE INDEX IF NOT EXISTS outpatient_tags_clm_id_idx
    ON ccw.outpatient_tags(clm_id);

CREATE TABLE IF NOT EXISTS ccw.snf_tags (
    clm_id BIGINT NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES ccw.snf_claims(clm_id)
    );
CREATE INDEX IF NOT EXISTS snf_tags_clm_id_idx
    ON ccw.snf_tags(clm_id);

CREATE TABLE IF NOT EXISTS ccw.inpatient_tags (
    clm_id BIGINT NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES ccw.inpatient_claims(clm_id)
    );
CREATE INDEX IF NOT EXISTS inpatient_tags_clm_id_idx
    ON ccw.inpatient_tags(clm_id);

CREATE TABLE IF NOT EXISTS rda.mcs_tags (
    clm_id CHARACTER VARYING(15) NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES rda.mcs_claims(idr_clm_hd_icn)
    );
CREATE INDEX IF NOT EXISTS mcs_tags_clm_id_idx
    ON rda.mcs_tags(clm_id);

CREATE TABLE IF NOT EXISTS rda.fiss_tags (
    clm_id CHARACTER VARYING(43) NOT NULL,
    code TAG_CODE NOT NULL,
    details JSONB,
    PRIMARY KEY(clm_id, code),
    FOREIGN KEY (clm_id)
        REFERENCES rda.fiss_claims(claim_id)
    );
CREATE INDEX IF NOT EXISTS fiss_tags_clm_id_idx
    ON rda.fiss_tags(clm_id);
