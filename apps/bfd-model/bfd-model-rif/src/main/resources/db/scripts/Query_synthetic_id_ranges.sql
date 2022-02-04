/*
 * Queries that should be run in prod-sandbox prior to generating new synthea data in order to ensure that any identifiers 
 * that are generated do not conflict with existing synthetic data. In the case of claimId (for all non-partd event tables) 
 * and pde_id (for the partd event table) new synthetic data is appended below the minimum value currently present. 
 
 * In the case of beneficiary IDs (bene_id) and claim group IDs (clm_grp_id) it is necessary to 
 * use a more complex query to extract all currently occupied ranges of IDs and find a gap that is big enough for the new generated data.
 */
 

-- Find the beneficiaries.bene_id range to avoid collisions with synthetic data. 
-- The range should have a relatively large gap, preferably contiguous with the last range used.
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

-- Find  min claim Ids for all claims except Part D Events.
-- The new synthetic data claim Id start should be 1 less than the value returned to avoid collisions with future synthetic data batches.
select min(id) from (
	select CAST(clm_id AS numeric) id 
	from carrier_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) id 
	from dme_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) id 
	from hha_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) id 
	from hospice_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) id 
	from inpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) id 
	from outpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) id 
	from snf_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	) as ids;
	
-- Find  min Part D Event (PDE) Ids.
-- The new synthetic data PDE event Id start should be 1 less than the value returned to avoid collisions with future synthetic data batches.
select min(id) from (
	select CAST(pde_id AS numeric) id 
	from partd_events
	where bene_id LIKE '-%' AND pde_id LIKE '-%'
	) as ids;

-- Find the claim group id range to avoid collisions with synthetic data. 
-- The range should have a relatively large gap, preferably contiguous with the last range used.
-- This query should be run before and after generating any synthetic data in prod SBX
WITH ids AS (
	select CAST(clm_grp_id AS numeric) id 
	from carrier_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) id 
	from dme_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) id 
	from hha_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) id 
	from hospice_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) id 
	from inpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) id 
	from outpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) id 
	from snf_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
  ORDER BY id
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
