/*
 * This migration adds a message_errors table for tracking messages that resulted in
 * an error during the message ingestion process via the gRPC pipeline job.  This table will
 * provide a means to store the instigating data so it can be reviewed and troubleshooted.
 *
 * Keeping messages that generated errors in a separate persistent table will prevent it
 * from being lost due to subsequent claim updates that may clear the error, thus
 * making it more challenging to resolve the original issue.
 */

CREATE TABLE rda.message_error (
    id              bigint                      not null primary key,
    sequence_number bigint                      not null,
    claim_id        varchar(25)                 not null,
    claim_type      char                        not null,
    received_date   timestamp with time zone    not null,
    errors          ${type.text}                not null,
    message         ${type.text}                not null,
);

create sequence rda.rda_api_claim_message_error_id_seq as bigint ${logic.sequence-start} 1 ${logic.sequence-increment} 25 no cycle;
