-- hash is the primary key but we usually search for the plain id.
CREATE TABLE id_hashes (
    id varchar(80) NOT NULL,
    hash varchar(64) NOT NULL,
    CONSTRAINT id_hashes_key PRIMARY KEY (hash)
);

-- non unique index although generally there will only be one record per id.
CREATE INDEX id_hashes_id on id_hashes (id);
