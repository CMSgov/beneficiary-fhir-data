CREATE TABLE foo (
    "dcn" varchar(23) NOT NULL,
    "priority" smallint NOT NULL,
    "last_updated" timestamp with time zone
);
CREATE INDEX foo_dcn_idx on foo("dcn");

-- sequence
CREATE SEQUENCE foo_pub_seq AS bigint ${logic.sequence-start} 1 ${logic.sequence-increment} 25 NO cycle;
