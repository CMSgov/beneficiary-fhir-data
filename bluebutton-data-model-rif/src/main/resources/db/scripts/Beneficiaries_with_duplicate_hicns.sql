/*
 * Queries for finding HICN and HICN hash collisions between beneficiary records.
 *
 * These queries are intended to be run manually, by copy-pasting them into an
 * interactive `psql` session, or somesuch.
 */

-- How many "hicn"s are associated with more than one "beneficiaryId", whether or not those
-- "beneficiaryId"s resolve to an actual "Beneficiaries" record?
--
-- On 2019-06-07, this was run in PROD:
-- * returned 2,697,057 rows
-- * with a max count of 4
-- * in 1478941.350 ms (0:25h)
WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT "beneficiaryId" AS bene_id, "hicn" AS hicn
      FROM "Beneficiaries"
    UNION ALL
    SELECT
        DISTINCT "beneficiaryId" AS bene_id, "hicn" AS hicn
      FROM "BeneficiariesHistory"
  )
SELECT
    DISTINCT hicn, count(bene_id)
  FROM hicn_id_tuples
  GROUP BY hicn
  HAVING count(bene_id) > 1
  ORDER BY count(bene_id) DESC, hicn ASC;

-- How many distinct "hicn"s exist, where those "hicn"s resolve to an actual "Beneficiaries"
-- record?
--
-- On 2019-06-07, this was run in PROD:
-- * returned a count of 95,312,280
-- * in 2890261.631 ms (0:48h)
WITH
  hicns AS (
    SELECT
        DISTINCT "hicn" AS hicn
      FROM "Beneficiaries"
    UNION ALL
    SELECT
        DISTINCT "BeneficiariesHistory"."hicn" AS hicn
      FROM "BeneficiariesHistory"
      INNER JOIN "Beneficiaries"
        ON "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
  )
SELECT
    count(DISTINCT hicn)
  FROM hicns;

-- How many "hicn"s are associated with more than one "beneficiaryId", where those "beneficiaryId"s
-- resolve to an actual "Beneficiaries" record?
--
-- On 2019-06-07, this was run in PROD:
-- * returned 2,674,583 rows
-- * with a max count of 4
-- * in 2160094.639 ms (0:36h)
WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT "beneficiaryId" AS bene_id, "hicn" AS hicn
      FROM "Beneficiaries"
    UNION ALL
    SELECT
        DISTINCT "BeneficiariesHistory"."beneficiaryId" AS bene_id, "BeneficiariesHistory"."hicn" AS hicn
      FROM "BeneficiariesHistory"
      INNER JOIN "Beneficiaries"
        ON "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
  )
SELECT
    DISTINCT hicn, count(bene_id)
  FROM hicn_id_tuples
  GROUP BY hicn
  HAVING count(bene_id) > 1
  ORDER BY count(bene_id) DESC, hicn ASC;


-- How many "hicnUnhashed"s are associated with more than one "beneficiaryId", where those
-- "beneficiaryId"s resolve to an actual "Beneficiaries" record?
--
-- On 2019-06-07, this was run in PROD:
-- * returned 2,665,137 rows (why is this different than the hashed "hicn"s field?)
-- * including 88,263 blank HICNs (WTF?)
-- * including a small number of HICNs that are clearly invalid, e.g. "{1234567890" (digits changed, but WTF is with the curly bracket?)
-- * including a rather large amount of too-long HICNs, e.g. "123456789X1" (digits changed, but that's unexpected to have an extra numeric suffix)
-- * with a max count of 4
-- * in 1945111.593 ms (0:32h)
WITH
  hicn_id_tuples AS (
    SELECT
        DISTINCT "beneficiaryId" AS bene_id, "hicnUnhashed" AS hicn_unhashed
      FROM "Beneficiaries"
    UNION ALL
    SELECT
        DISTINCT "BeneficiariesHistory"."beneficiaryId" AS bene_id, "BeneficiariesHistory"."hicnUnhashed" AS hicn_unhashed
      FROM "BeneficiariesHistory"
      INNER JOIN "Beneficiaries"
        ON "BeneficiariesHistory"."beneficiaryId" = "Beneficiaries"."beneficiaryId"
  )
SELECT
    DISTINCT hicn_unhashed, count(bene_id)
  FROM hicn_id_tuples
  GROUP BY hicn_unhashed
  HAVING count(bene_id) > 1
  ORDER BY count(bene_id) DESC, hicn_unhashed ASC;