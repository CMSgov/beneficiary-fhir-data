--
--
--
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

--
--
--
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
	
--
--
--
select min(id) from (
	select CAST(pde_id AS numeric) id 
	from partd_events
	where bene_id LIKE '-%' AND pde_id LIKE '-%'
	) as ids;

--
--
--
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
