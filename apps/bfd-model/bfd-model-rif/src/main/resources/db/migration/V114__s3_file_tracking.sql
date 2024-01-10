
/*
 * Table to track CCW S3 manifest files in database.
 */
CREATE TABLE s3_manifest_files (
    manifest_id bigint NOT NULL,
    s3_path varchar(1024) NOT NULL,
    status varchar(24) NOT NULL,
    discovery_timestamp timestamp with time zone NOT NULL,
    completion_timestamp timestamp with time zone,
    CONSTRAINT s3_manifest_files_key PRIMARY KEY (manifest_id)
);

-- used to generate primary key id
create sequence s3_manifest_files_manifest_id_seq ${logic.sequence-start} 1 ${logic.sequence-increment} 1;

-- used when checking status of a file from S3 bucket
create unique index s3_manifest_files_s3_path on s3_manifest_files(s3_path);

/*
 * Table to track CCW S3 data files in database.
 */
CREATE TABLE s3_data_files (
    manifest_id bigint NOT NULL,
    file_name varchar(128) NOT NULL,
    file_type varchar(50) NOT NULL,
    s3_path varchar(1024) NOT NULL,
    status varchar(24) NOT NULL,
    discovery_timestamp timestamp with time zone NOT NULL,
    completion_timestamp timestamp with time zone,
    CONSTRAINT s3_data_files_key PRIMARY KEY (manifest_id, file_name),
    CONSTRAINT "fk_s3_data_files_manifest_id" FOREIGN KEY (manifest_id) REFERENCES s3_manifest_files(manifest_id)
);
