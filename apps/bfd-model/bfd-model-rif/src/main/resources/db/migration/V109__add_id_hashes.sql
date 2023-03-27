-- Lookup table for DatabaseIdHasher persistent cache.
CREATE TABLE id_hashes (
    id varchar(80) NOT NULL,
    hash varchar(64) NOT NULL,
    CONSTRAINT id_hashes_key PRIMARY KEY (id)
);
