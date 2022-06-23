-- Replacement table for rda_api_claim_message_meta_data that uses a natural key
-- (claim_type/sequence_number) to save space.  Easier to create a new table then
-- later drop the old one than to deal with changing key in existing table and there
-- is no production data to migrate over.

${logic.hsql-only} CREATE TYPE json AS longvarchar;

CREATE TABLE rda.claim_message_meta_data (
    claim_type       char(1)                  NOT NULL,
    sequence_number  bigint                   NOT NULL,
    claim_id         varchar(25)              NOT NULL,
    mbi_id           bigint,
    claim_state      varchar(1),
    transaction_date DATE,
    claim_location   json,
    received_date    timestamp with time zone NOT NULL,
    CONSTRAINT claim_message_meta_data_pkey PRIMARY KEY (claim_type, sequence_number),
    CONSTRAINT claim_message_meta_data_mbi FOREIGN KEY (mbi_id) REFERENCES rda.mbi_cache(mbi_id)
);

CREATE INDEX claim_message_meta_data_received_date_idx ON rda.claim_message_meta_data(received_date);
