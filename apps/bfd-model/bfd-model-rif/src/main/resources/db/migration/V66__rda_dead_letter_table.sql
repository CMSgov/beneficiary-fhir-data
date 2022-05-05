/*
 * This migration adds a message_errors table for tracking messages that resulted in
 * an error during the message ingestion process via the gRPC pipeline job.  This table will
 * provide a means to store the instigating data so it can be reviewed and troubleshooted.
 *
 * Keeping messages that generated errors in a separate persistent table will prevent it
 * from being lost due to subsequent claim updates that may clear the error, thus
 * making it more challenging to resolve the original issue.
 */

${logic.hsql-only} CREATE TYPE json AS longvarchar;

CREATE TABLE rda.message_error (
    sequence_number bigint                      not null,
    claim_id        varchar(25)                 not null,
    claim_type      varchar(20)                 not null,
    received_date   timestamp with time zone    not null,
    errors          ${type.jsonb}                not null,
    message         ${type.jsonb}                not null,
    primary key (sequence_number, claim_id, claim_type)
);
