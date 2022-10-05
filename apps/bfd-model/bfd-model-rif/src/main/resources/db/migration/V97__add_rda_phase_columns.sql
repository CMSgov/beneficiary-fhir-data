/*
 * Adds new columns to the FISS and MCS claims tables to store the phase number
 * and phase sequence number for use in filtering claims.
 */

ALTER TABLE rda.fiss_claims ADD phase smallint;
ALTER TABLE rda.fiss_claims ADD phase_seq_num smallint;
ALTER TABLE rda.mcs_claims ADD phase smallint;
ALTER TABLE rda.mcs_claims ADD phase_seq_num smallint;
