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
WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT "beneficiaryId" AS bene_id, "hicn" AS hicn
      FROM "Beneficiaries"
    UNION
    SELECT
        DISTINCT "beneficiaryId" AS bene_id, "hicn" AS hicn
      FROM "BeneficiariesHistory"
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
        DISTINCT "hicn" AS hicn
      FROM "Beneficiaries"
    UNION
    SELECT
        DISTINCT "BeneficiariesHistory"."hicn" AS hicn
      FROM "BeneficiariesHistory"
      INNER JOIN "Beneficiaries"
        ON "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
  )
SELECT
    count(hicn)
  FROM hicns;

-- How many "hicn"s are associated with more than one "beneficiaryId", where those "beneficiaryId"s
-- resolve to an actual "Beneficiaries" record?
--
-- On 2019-06-25, this was run in PROD:
-- * returned 51,181 rows
-- * with a max count of 4
-- * in 4058618.761 ms (1:08h)
WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT "beneficiaryId" AS bene_id, "hicn" AS hicn
      FROM "Beneficiaries"
    UNION
    SELECT
        DISTINCT "BeneficiariesHistory"."beneficiaryId" AS bene_id, "BeneficiariesHistory"."hicn" AS hicn
      FROM "BeneficiariesHistory"
      INNER JOIN "Beneficiaries"
        ON "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
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
        DISTINCT "beneficiaryId" AS bene_id, "hicnUnhashed" AS hicn_unhashed
      FROM "Beneficiaries"
    UNION
    SELECT
        DISTINCT "BeneficiariesHistory"."beneficiaryId" AS bene_id, "BeneficiariesHistory"."hicnUnhashed" AS hicn_unhashed
      FROM "BeneficiariesHistory"
      INNER JOIN "Beneficiaries"
        ON "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
  )
SELECT
    hicn_unhashed, count(bene_id)
  FROM hicn_id_tuples
  GROUP BY hicn_unhashed
  HAVING count(bene_id) > 1
  ORDER BY count(bene_id) DESC, hicn_unhashed ASC;

-- How many HICNs are missing?
--
-- On 2019-06-25, this was run in PROD:
-- * returned:
--     bene_hicn_hashed_null | bene_hicn_hashed_empty | bene_hicn_unhashed_null | bene_hicn_unhashed_empty | bene_history_hicn_hashed_null | bene_history_hicn_hashed_empty | bene_history_hicn_unhashed_null | bene_history_hicn_unhashed_empty
--    -----------------------+------------------------+-------------------------+--------------------------+-------------------------------+--------------------------------+---------------------------------+----------------------------------
--                         0 |                      0 |                   88246 |                        0 |                             0 |                              0 |                              17 |                                0
-- * in 810767.424 ms (0:14h)
SELECT
  (SELECT count(*) FROM "Beneficiaries" WHERE "hicn" IS NULL) AS bene_hicn_hashed_null,
  (SELECT count(*) FROM "Beneficiaries" WHERE "hicn" = '') AS bene_hicn_hashed_empty,
  (SELECT count(*) FROM "Beneficiaries" WHERE "hicnUnhashed" IS NULL) AS bene_hicn_unhashed_null,
  (SELECT count(*) FROM "Beneficiaries" WHERE "hicnUnhashed" = '') AS bene_hicn_unhashed_empty,
  (SELECT count(*) FROM "BeneficiariesHistory" WHERE "hicn" IS NULL) AS bene_history_hicn_hashed_null,
  (SELECT count(*) FROM "BeneficiariesHistory" WHERE "hicn" = '') AS bene_history_hicn_hashed_empty,
  (SELECT count(*) FROM "BeneficiariesHistory" WHERE "hicnUnhashed" IS NULL) AS bene_history_hicn_unhashed_null,
  (SELECT count(*) FROM "BeneficiariesHistory" WHERE "hicnUnhashed" = '') AS bene_history_hicn_unhashed_empty;
