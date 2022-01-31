/*
 * Queries for finding the ID ranges of our synthetic data, developed around 2021-05-20.
 * 
 * It's worth noting that this general class of problem is commonly referred to as a "gaps and
 * islands problem" and you can search for solutions to it using that term. The queries here were
 * derived from this particular Stack Overflow answer, though:
 * <https://stackoverflow.com/a/17046749>.
 * 
 * This was definitely a bit of a journey: careful readers will note that the queries used evolved
 * over time, as more was discovered about the data and as techniques were refined. Main takeaways:
 * 
 * 1. Our ID ranges have lots of gaps, and thus "take up" more ID space than needed.
 *     * We only have 1,539,848 total synthetic claims, but use ID ranges of -1 through
 *       -100,000,000,000 and 1,145,251,452 through 5,114,848,764.
 * 2. The prod-sbx environment also has some or all of the "random" sample data loaded.
 *     * There are definitely "random" benes there. Not sure about claims, though. Certainly not
 *       for all of the claim types (as evidenced by the claim types I didn't have to do filtering
 *       for, below).
 * 3. A quick peek at clm_grp_id shows that those values are also all over the place.
 * 4. Given that clm_id is a `varchar(15)` column and we're only up to numbers that use thirteen
 *    of those characters, we have plenty of remaining room there.
 * 5. Given that clm_grp_id is a `numeric(12,0)` and the max value just for carrier_claims is
 *    99,999,991,267, we're basically out of room there.
 *
 * These queries are intended to be run manually, by copy-pasting them into an interactive `psql`
 * session, or somesuch.
 */


-- Find the beneficiaries.bene_id groupings.
WITH ids AS (
  SELECT
    CAST(bene_id AS numeric) AS id
  FROM beneficiaries
  ORDER BY CAST(bene_id AS numeric)
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-11 in prod_sbx:
--  grouping_start  |  grouping_end
-- -----------------+-----------------
--  -88888888888888 | -88888888888888
--  -20140000010000 | -20140000000001
--  -20000000010000 | -20000000000001
--  -19990000010000 | -19990000000001
--             -400 |            -400
--             -223 |            -207
--             -204 |            -201
--                1 |        60000000
-- (8 rows)
-- 
-- Time: 106524.199 ms


-- Find the carrier_claims.clm_id groupings.
WITH ids_filtered AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM carrier_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_rounded AS (
  SELECT DISTINCT
    -- From a quick check, we know that the non-dupe synthetic IDs range from -9090999 through -24670827521
    -- with mostly discontinuous numbers. To avoid getting back about a million rows (literally!), we round
    -- to significant digits.
    ((id / 1000000000)::bigint * 1000000000)::bigint AS id
  FROM ids_filtered
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids_rounded
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-19 in prod_sbx (using ids_filtered):
--  grouping_start | grouping_end
-- ----------------+--------------
--    -24670827521 | -24670827521
--    ... (lot of sparse ranges here, skipped for brevity)
--    -20300133924 | -20300133924
--    -11638675969 | -11638675969
--    ... (lot of sparse ranges here, skipped for brevity)
--     -8471294025 |  -8471294025
--        -9090999 |     -9090999
-- (926397 rows)
-- 
-- Time: 50784.720 ms

-- Got this result on 2021-05-19 in prod_sbx (using ids_rounded):
--  grouping_start | grouping_end
-- ----------------+--------------
--    -25000000000 | -25000000000
--    -24000000000 | -24000000000
--    -23000000000 | -23000000000
--    -22000000000 | -22000000000
--    -21000000000 | -21000000000
--    -20000000000 | -20000000000
--    -12000000000 | -12000000000
--    -11000000000 | -11000000000
--    -10000000000 | -10000000000
--     -9000000000 |  -9000000000
--     -8000000000 |  -8000000000
--               0 |            0 -- This one's a lie due to the rounding -- see the other results.
-- (12 rows)
-- 
-- Time: 44374.912 ms


-- Find the dme_claims.clm_id groupings.
WITH ids AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM dme_claims
  ORDER BY CAST(clm_id AS numeric)
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-19 in prod_sbx:
--  grouping_start | grouping_end
-- ----------------+--------------
--        -9099909 |     -9099909
-- (1 row)
-- 
-- Time: 0.944 ms


-- Find the hha_claims.clm_id groupings.
WITH ids AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM hha_claims
  ORDER BY CAST(clm_id AS numeric)
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-20 in prod_sbx:
--  grouping_start | grouping_end
-- ----------------+--------------
--        -9998909 |     -9998909
-- (1 row)
-- 
-- Time: 1.876 ms


-- Find the hospice_claims.clm_id groupings.
WITH ids AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM hospice_claims
  ORDER BY CAST(clm_id AS numeric)
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-20 in prod_sbx:
--  grouping_start | grouping_end
-- ----------------+--------------
--        -9398989 |     -9398989
-- (1 row)
-- 
-- Time: 1.259 ms


-- Find the inpatient_claims.clm_id groupings.
WITH ids_filtered AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM inpatient_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_rounded AS (
  SELECT DISTINCT
    -- From a quick check, we know that the non-dupe synthetic IDs range from -9090999 through -24670827521
    -- with mostly discontinuous numbers. To avoid getting back about a million rows (literally!), we round
    -- to significant digits.
    ((id / 1000000000)::bigint * 1000000000)::bigint AS id
  FROM ids_filtered
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids_rounded
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-20 in prod_sbx (using ids_filtered):
--  grouping_start | grouping_end
-- ----------------+--------------
--     -5024534892 |  -5024534892
--    ... (lot of sparse ranges here, skipped for brevity)
--     -4076224917 |  -4076224917
--     -2063001944 |  -2063001944
--    ... (lot of sparse ranges here, skipped for brevity)
--     -1132194757 |  -1132194757
--       -92949189 |    -92949189
-- (5072 rows)
-- 
-- Time: 17.801 ms

-- Got this result on 2021-05-20 in prod_sbx (using ids_rounded):
--  grouping_start | grouping_end
-- ----------------+--------------
--     -5000000000 |  -5000000000
--     -4000000000 |  -4000000000
--     -2000000000 |  -2000000000
--     -1000000000 |  -1000000000
--               0 |            0 -- This one's a lie due to the rounding -- see the other results.
-- (5 rows)
-- 
-- Time: 8.198 ms


-- Find the outpatient_claims.clm_id groupings.
WITH ids_filtered AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM outpatient_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Note: it appears that for the synthetic outpatient claims ALL we have are ones with positive IDs.
    -- AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_rounded AS (
  SELECT DISTINCT
    -- From a quick check, we know that the non-dupe synthetic IDs range from -9090999 through -24670827521
    -- with mostly discontinuous numbers. To avoid getting back about a million rows (literally!), we round
    -- to significant digits.
    ((id / 1000000000)::bigint * 1000000000)::bigint AS id
  FROM ids_filtered
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids_filtered
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-20 in prod_sbx (using ids_filtered):
--  grouping_start | grouping_end
-- ----------------+--------------
--        -9292929 |     -9292929
--      1145251452 |   1145251452
--      ... (lot of sparse ranges here, skipped for brevity)
--      5114848764 |   5114848764
-- (170705 rows)
-- 
-- Time: 593.635 ms

-- Got this result on 2021-05-20 in prod_sbx (using ids_rounded):
--  grouping_start | grouping_end
-- ----------------+--------------
--               0 |            0 -- This one's a lie due to the rounding -- see the other results.
--      1000000000 |   1000000000
--      2000000000 |   2000000000
--      4000000000 |   4000000000
--      5000000000 |   5000000000
-- (5 rows)
--
-- Time: 210.042 ms


-- Find the snf_claims.clm_id groupings.
WITH ids AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM snf_claims
  ORDER BY CAST(clm_id AS numeric)
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-20 in prod_sbx:
--  grouping_start | grouping_end
-- ----------------+--------------
--        -9393939 |     -9393939
-- (1 row)
-- 
-- Time: 1.375 ms


-- Find the partd_events.clm_id groupings.
WITH ids_filtered AS (
  SELECT
    CAST(pde_id AS numeric) AS id
  FROM partd_events
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND pde_id LIKE '-%'
  ORDER BY CAST(pde_id AS numeric)
),
ids_rounded AS (
  SELECT DISTINCT
    -- From a quick check, we know that the non-dupe synthetic IDs range from -9090999 through -24670827521
    -- with mostly discontinuous numbers. To avoid getting back about a million rows (literally!), we round
    -- to significant digits.
    ((id / 1000000000)::bigint * 1000000000)::bigint AS id
  FROM ids_filtered
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids_filtered
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-20 in prod_sbx (using ids_filtered):
--  grouping_start | grouping_end
-- ----------------+--------------
--     -7859229193 |  -7859229193
--     ... (lot of sparse ranges here, skipped for brevity)
--          -33560 |       -33560
--            -999 |         -999
--            -323 |         -200
-- (413347 rows)
-- 
-- Time: 17.801 ms

-- Got this result on 2021-05-20 in prod_sbx (using ids_rounded):
--  grouping_start | grouping_end
-- ----------------+--------------
--     -8000000000 |  -8000000000
--     -7000000000 |  -7000000000
--     -6000000000 |  -6000000000
--     -5000000000 |  -5000000000
--     -4000000000 |  -4000000000
--     -3000000000 |  -3000000000
--     -2000000000 |  -2000000000
--     -1000000000 |  -1000000000
--               0 |            0 -- This one ISN'T really a lie: the non-rounded results show that it approaches 0.
-- (9 rows)
-- 
-- Time: 1672.299 ms


-- Find the combined claim groupings.
WITH ids_filtered_carrier AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM carrier_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_dme AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM dme_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_hha AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM hha_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_hospice AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM hospice_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_filtered_inpatient AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM inpatient_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_filtered_outpatient AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM outpatient_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Note: it appears that for the synthetic outpatient claims ALL we have are ones with positive IDs.
    -- AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_snf AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM snf_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_filtered_pde AS (
  SELECT
    CAST(pde_id AS numeric) AS id
  FROM partd_events
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND pde_id LIKE '-%'
  ORDER BY CAST(pde_id AS numeric)
),
ids_filtered_all AS (
  SELECT * FROM ids_filtered_carrier
  UNION ALL
  SELECT * FROM ids_dme
  UNION ALL
  SELECT * FROM ids_hha
  UNION ALL
  SELECT * FROM ids_hospice
  UNION ALL
  SELECT * FROM ids_filtered_inpatient
  UNION ALL
  SELECT * FROM ids_filtered_outpatient
  UNION ALL
  SELECT * FROM ids_snf
  UNION ALL
  SELECT * FROM ids_filtered_pde
),
ids_rounded_all AS (
  -- We want to round, as follows:
  -- -11 to -100 = -100
  --  -2 to  -10 =  -10
  --          -1 =   -1
  --           0 =    0
  --           1 =    1
  --   2 to   10 =   10
  --  11 to  100 =  100
  -- ... and so on.
  SELECT DISTINCT
    CASE
      WHEN id = 0 THEN 0
      WHEN id > 0 THEN power(10, ceil(log(id)))::bigint
      WHEN id < 0 THEN -1 * power(10, ceil(log(abs(id))))::bigint
      ELSE NULL
    END AS id
  FROM ids_filtered_all
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY id) - id AS grouping,
    id
  FROM ids_rounded_all
)
SELECT MIN(id) AS grouping_start,
       MAX(id) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(id);

-- Got this result on 2021-05-20 in prod_sbx:
--  grouping_start | grouping_end
-- ----------------+---------------
--   -100000000000 | -100000000000
--    -10000000000 |  -10000000000
--     -1000000000 |   -1000000000
--      -100000000 |    -100000000
--       -10000000 |     -10000000
--        -1000000 |      -1000000
--         -100000 |       -100000
--           -1000 |         -1000
--     10000000000 |   10000000000
-- (9 rows)
-- 
-- Time: 158150.881 ms


-- Calculate how many synthetic claims we have, total.
WITH ids_filtered_carrier AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM carrier_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_dme AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM dme_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_hha AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM hha_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_hospice AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM hospice_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_filtered_inpatient AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM inpatient_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_filtered_outpatient AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM outpatient_claims
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Note: it appears that for the synthetic outpatient claims ALL we have are ones with positive IDs.
    -- AND clm_id LIKE '-%'
  ORDER BY CAST(clm_id AS numeric)
),
ids_snf AS (
  SELECT
    CAST(clm_id AS numeric) AS id
  FROM snf_claims
  ORDER BY CAST(clm_id AS numeric)
),
ids_filtered_pde AS (
  SELECT
    CAST(pde_id AS numeric) AS id
  FROM partd_events
  WHERE
    -- This query runs out of memory if we don't restrict it to only include the smaller set of synthetic
    -- claims (excluding the randomly generated test claims).
    bene_id LIKE '-%'
    -- Also filter out the duplicate synthetic IDs (>0), just to make things simpler.
    AND pde_id LIKE '-%'
  ORDER BY CAST(pde_id AS numeric)
),
ids_filtered_all AS (
  SELECT * FROM ids_filtered_carrier
  UNION ALL
  SELECT * FROM ids_dme
  UNION ALL
  SELECT * FROM ids_hha
  UNION ALL
  SELECT * FROM ids_hospice
  UNION ALL
  SELECT * FROM ids_filtered_inpatient
  UNION ALL
  SELECT * FROM ids_filtered_outpatient
  UNION ALL
  SELECT * FROM ids_snf
  UNION ALL
  SELECT * FROM ids_filtered_pde
)
SELECT count(*) FROM ids_filtered_all;

-- Got this result on 2021-05-20 in prod_sbx:
--   count
-- ---------
--  1539848
-- (1 row)
-- 
-- Time: 47612.129 ms
