-- BFD-3163
-- Create index on MBI_NUM to support lookup using us-mbi lookup. The MBI lookup
-- needs to check both the current beneficiaries table as well as the
-- beneficiaries_history table.
--
${logic.psql-only}  CREATE INDEX CONCURRENTLY IF NOT EXISTS beneficiaries_history_mbi_idx
${logic.psql-only}      ON beneficiaries_history (mbi_num);

${logic.psql-only}  CREATE INDEX CONCURRENTLY IF NOT EXISTS beneficiaries_mbi_idx
${logic.psql-only}      ON beneficiaries (mbi_num);