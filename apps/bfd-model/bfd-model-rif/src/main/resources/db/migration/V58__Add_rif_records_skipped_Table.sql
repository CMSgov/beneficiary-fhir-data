/**
 * Adds a new table for storing CCW RIF records that have been temporarily skipped.
 *
 * Please note that every record in this table is a piece of as-yet-unaddressed technical debt, with an
 * exorbitantly high "interest rate": the older these records get, the harder it will be to figure out what
 * to do with them, and the harder it will be to implement any such process. Ideally, this table is only ever
 * populated for VERY brief periods of time.
 * 
 * Added as part of <https://jira.cms.gov/browse/BFD-1566>.
 */

CREATE TABLE skipped_rif_records (
  -- Sequence-generated PK for these records, as no natural PK is feasible.
  record_id            bigint                     NOT NULL PRIMARY KEY,

  -- A code indicating why this particular RIF record(s) were skipped.
  skip_reason          varchar(50)                NOT NULL,

  -- The timestamp associated with the CCW data set manifest that this record(s) is from.
  rif_file_timestamp timestamp with time zone     NOT NULL,

  -- The `RifFileType` (e.g. `BENEFICIARY`, `CARRIER`, etc.) of the RIF file that this record(s) is from.
  rif_file_type        varchar(48)                NOT NULL,

  -- The `DML_IND` of the RIF record(s).
  dml_ind              varchar(6)                 NOT NULL,

  -- The `bene_id` of the beneficiary that this record(s) is of / associated with.
  bene_id              varchar(15)               NOT NULL,

  -- The RIF/CSV row or rows representing the record(s) (i.e. beneficiary or claim) that was skipped.
  rif_data             ${type.text}               NOT NULL
);

-- The sequence used for the `skipped_rif_records.record_id` column.
CREATE SEQUENCE skipped_rif_records_record_id_seq
  AS bigint ${logic.sequence-start} 1 ${logic.sequence-increment} 1
  NO CYCLE;

-- This index will allow for fast queries to determine whether or not a given `beneficiaries` record has been
-- impacted by the new load filtering. This will allow the BFD Server to report that status in its `Patient`
-- responses.
CREATE INDEX skipped_rif_records_bene_id_idx on skipped_rif_records (bene_id);
