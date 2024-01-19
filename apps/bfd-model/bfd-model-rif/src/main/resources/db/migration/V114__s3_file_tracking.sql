
/*
 * Table to track CCW S3 manifest files in database.
 */
CREATE TABLE s3_manifest_files (
    manifest_id bigint NOT NULL,
    s3_key varchar(1024) NOT NULL,
    status varchar(24) NOT NULL,
    status_timestamp timestamp with time zone,
    manifest_timestamp timestamp with time zone NOT NULL,
    discovery_timestamp timestamp with time zone NOT NULL,
    CONSTRAINT pk_s3_manifest_files_manifest_id PRIMARY KEY (manifest_id)
);

/*
 * Sequence used to generate primary key manifest_id
 */
create sequence s3_manifest_files_manifest_id_seq ${logic.sequence-start} 1 ${logic.sequence-increment} 1;

/*
 * Index used when checking status of a file from S3 bucket
 */
create unique index s3_manifest_files_s3_key on s3_manifest_files(s3_key);

/*
 * Table to track CCW S3 data files in database.
 */
CREATE TABLE s3_data_files (
    manifest_id bigint NOT NULL,
    index smallint NOT NULL,
    file_name varchar(128) NOT NULL,
    file_type varchar(50) NOT NULL,
    s3_key varchar(1024) NOT NULL,
    status varchar(24) NOT NULL,
    status_timestamp timestamp with time zone,
    discovery_timestamp timestamp with time zone NOT NULL,
    CONSTRAINT pk_s3_data_files_key PRIMARY KEY (manifest_id, file_name),
    CONSTRAINT fk_s3_data_files_manifest_id FOREIGN KEY (manifest_id) REFERENCES s3_manifest_files(manifest_id)
);

/*
 * Index used when checking status of a file from S3 bucket
 */
create unique index s3_data_files_s3_key on s3_data_files(s3_key);

/*
 * Columns used to relate a record back the s3_data_files record it came from.
 */
alter table loaded_files add column s3_manifest_id bigint;
alter table loaded_files add column s3_file_index smallint;
