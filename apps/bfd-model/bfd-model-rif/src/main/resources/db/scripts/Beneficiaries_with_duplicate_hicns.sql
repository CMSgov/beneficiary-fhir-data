/*
 * Queries for finding HICN and HICN hash collisions between beneficiary records.
 *
 * These queries are intended to be run manually, by copy-pasting them into an
 * interactive `psql` session, or somesuch.
 */

-- How many "hicn"s are associated with more than one "beneficiaryId", whether or not those
-- "beneficiaryId"s resolve to an actual "Beneficiaries" record?
--
-- On 2019-06-25, this was run in PROD:
-- * returned 74,142 rows
-- * with a max count of 4
-- * in 3683200.979 ms (1:02h)

-- setup for parallel processing
SET max_parallel_workers = 6;
SET max_parallel_workers_per_gather = 6;
SET parallel_leader_participation = off;
SET parallel_tuple_cost = 0;
SET parallel_setup_cost = 0;
SET min_parallel_table_scan_size = 0;

WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT bene_id, bene_crnt_hic_num as hicn 
      FROM beneficiaries
    UNION
    SELECT
        DISTINCT bene_id, bene_crnt_hic_num as hicn
      FROM beneficiaries_history
  )
SELECT
    hicn, count(bene_id)
  FROM hicn_id_tuples
  GROUP BY hicn
  HAVING count(bene_id) > 1
  ORDER BY count(bene_id) DESC, hicn ASC;

-- How many distinct "hicn"s exist, where those "hicn"s resolve to an actual "Beneficiaries"
-- record?
--
-- On 2019-06-25, this was run in PROD:
-- * returned a count of 95,676,673
-- * in 2890261.631 ms (0:48h)

WITH
  hicns AS (
    SELECT
        DISTINCT bene_crnt_hic_num as hicn
      FROM beneficiaries
    UNION
    SELECT
        DISTINCT bh.bene_crnt_hic_num as hicn
      FROM beneficiaries_history bh
      INNER JOIN beneficiaries b
        ON bh.bene_id = b.bene_id
  )
SELECT
    count(hicn)
  FROM hicns;

-- How many hicn(s) are associated with more than one bene_id", where those bene_id(s)
-- resolve to an actual beneficiaries record?
--
-- On 2019-06-25, this was run in PROD:
-- * returned 51,181 rows
-- * with a max count of 4
-- * in 4058618.761 ms (1:08h)

WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT bene_id, bene_crnt_hic_num as hicn
      FROM beneficiaries
    UNION
    SELECT
        DISTINCT bh.bene_id, bh.bene_crnt_hic_num as hicn
      FROM beneficiaries_history bh
      INNER JOIN beneficiaries b
        ON bh.bene_id = b.bene_id
  )
SELECT
    hicn, count(bene_id)
  FROM hicn_id_tuples
  GROUP BY hicn
  HAVING count(bene_id) > 1
  ORDER BY count(bene_id) DESC, hicn ASC;

-- How many "hicnUnhashed"s are associated with more than one "beneficiaryId", where those
-- "beneficiaryId"s resolve to an actual "Beneficiaries" record?
--
-- On 2019-06-07, this was run in PROD:
-- * returned 41,906 rows (why is this different than the hashed "hicn"s field?)
--     * Any "Beneficiary" with a NULL "hicnUnhashed" (e.g. synthetic benes) will still end up with the same hash... but I'd only expect that to add one row.
--     * Doesn't this delta mean that we have hash collisions?
-- * including 88,263 blank HICNs
-- * including a small number of HICNs that are clearly invalid, e.g. "{1234567890" (digits changed, but WTF is with the curly bracket?)
-- * including a rather large amount of too-long HICNs, e.g. "123456789X1" (digits changed, but that's unexpected to have an extra numeric suffix)
-- * with a max count of 4 for non-blank HICNs
-- * in 3385631.032 ms (0:56h)

WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT bene_id, hicn_unhashed
      FROM beneficiaries
    UNION
    SELECT
        DISTINCT bh.bene_id, bh.hicn_unhashed
      FROM beneficiaries_history bh
      INNER JOIN beneficiaries b
        ON bh.bene_id = b.bene_id
  )
SELECT
    hicn_unhashed, count(bene_id)
  FROM hicn_id_tuples
  GROUP BY hicn_unhashed
  HAVING count(bene_id) > 1
  ORDER BY count(bene_id) DESC, hicn_unhashed ASC;

-- How many HICNs are missing?
--
-- On 2022-02-01, this was run in PROD:
-- * returned:
--     bene_hicn_hashed_null | bene_hicn_hashed_empty | bene_hicn_unhashed_null | bene_hicn_unhashed_empty | bene_history_hicn_hashed_null | bene_history_hicn_hashed_empty | bene_history_hicn_unhashed_null | bene_history_hicn_unhashed_empty
--    -----------------------+------------------------+-------------------------+--------------------------+-------------------------------+--------------------------------+---------------------------------+----------------------------------
--                         0 |                      0 |                    4160 |                        0 |                             0 |                              0 |                           84103 |                                0
-- * in 810767.424 ms (40:26s)

SELECT
  (SELECT count(*) FROM beneficiaries WHERE bene_crnt_hic_num IS NULL) AS bene_hicn_hashed_null,
  (SELECT count(*) FROM beneficiaries WHERE bene_crnt_hic_num = '') AS bene_hicn_hashed_empty,
  (SELECT count(*) FROM beneficiaries WHERE hicn_unhashed IS NULL) AS bene_hicn_unhashed_null,
  (SELECT count(*) FROM beneficiaries WHERE hicn_unhashed = '') AS bene_hicn_unhashed_empty,
  (SELECT count(*) FROM beneficiaries_history WHERE bene_crnt_hic_num IS NULL) AS bene_history_hicn_hashed_null,
  (SELECT count(*) FROM beneficiaries_history WHERE bene_crnt_hic_num = '') AS bene_history_hicn_hashed_empty,
  (SELECT count(*) FROM beneficiaries_history WHERE hicn_unhashed IS NULL) AS bene_history_hicn_unhashed_null,
  (SELECT count(*) FROM beneficiaries_history WHERE hicn_unhashed = '') AS bene_history_hicn_unhashed_empty;
