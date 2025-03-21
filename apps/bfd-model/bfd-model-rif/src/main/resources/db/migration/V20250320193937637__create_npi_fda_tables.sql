CREATE TABLE IF NOT EXISTS ccw.npi_data (
    npi CHARACTER VARYING,
    entity_type CHARACTER VARYING,
    org_name CHARACTER VARYING,
    taxonomy_code CHARACTER VARYING,
    taxonomy_display CHARACTER VARYING,
    provider_name_prefix CHARACTER VARYING,
    provider_first_name CHARACTER VARYING,
    provider_middle_name CHARACTER VARYING,
    provider_last_name CHARACTER VARYING,
    provider_name_suffix CHARACTER VARYING,
    provider_credential CHARACTER VARYING,
    PRIMARY KEY(npi)
    );
CREATE TABLE IF NOT EXISTS ccw.npi_fda_meta (
    table_name CHARACTER VARYING,
    last_updated DATE,
    PRIMARY KEY(table_name)
);
