-- BFD-3163
-- Create index on MBI_NUM to support lookup using us-mbi lookup. The MBI lookup
-- needs to check both the current beneficiaries table as well as the
-- beneficiaries_history table.
--
CREATE INDEX CONCURRENTLY IF NOT EXISTS beneficiaries_history_mbi_idx
    ON beneficiaries_history (mbi_num);

CREATE INDEX CONCURRENTLY IF NOT EXISTS beneficiaries_mbi_idx
    ON beneficiaries (mbi_num);