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
select min(thing) from (
	select CAST(clm_id AS numeric) thing 
	from carrier_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) thing 
	from dme_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) thing 
	from hha_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) thing 
	from hospice_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) thing 
	from inpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) thing 
	from outpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_id AS numeric) thing 
	from snf_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	) as stuff;
	
--
--
--
select min(thing) from (
	select CAST(pde_id AS numeric) thing 
	from partd_events
	where bene_id LIKE '-%' AND pde_id LIKE '-%'
	) as stuff;

--
--
--
WITH ids AS (
	select CAST(clm_grp_id AS numeric) thing 
	from carrier_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) thing 
	from dme_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) thing 
	from hha_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) thing 
	from hospice_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) thing 
	from inpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) thing 
	from outpatient_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
	union
	select CAST(clm_grp_id AS numeric) thing 
	from snf_claims
	where bene_id LIKE '-%' AND clm_id LIKE '-%'
  ORDER BY thing
),
groupings AS (
  SELECT
    ROW_NUMBER() OVER (ORDER BY thing) - thing AS grouping,
    thing
  FROM ids
)
SELECT MIN(thing) AS grouping_start,
       MAX(thing) AS grouping_end
FROM groupings
GROUP BY grouping
ORDER BY MIN(thing);
