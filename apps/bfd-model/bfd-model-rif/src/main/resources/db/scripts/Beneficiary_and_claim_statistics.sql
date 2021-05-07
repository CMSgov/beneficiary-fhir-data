/*
 * Queries for high-level statistics on beneficiaries and claims data.
 * 
 * These queries are intended to be run manually, by copy-pasting them into an
 * interactive `psql` session, or somesuch.
 */

-- Count the total number of beneficiaries, in several different ways.
SELECT
  (
    SELECT count(*)
    FROM "Beneficiaries"
  ) AS benes_all,
  (
    SELECT count(*)
    FROM "Beneficiaries"
    WHERE
      "beneficiaryDateOfDeath" IS NULL
      OR "validDateOfDeathSw" <> 'V'
  ) AS benes_not_known_dead,
  (
    SELECT count(*)
    FROM "Beneficiaries"
    WHERE
      (
        "beneficiaryDateOfDeath" IS NULL
        OR "validDateOfDeathSw" <> 'V'
      ) AND
      (
        date_part('year',age("birthDate")) <= 110
      )
  ) AS benes_not_assumed_dead,
  (
    SELECT count(*)
    FROM "Beneficiaries"
    WHERE
      "medicareBeneficiaryId" IS NOT NULL
  ) AS benes_with_mbis;

-- Count the total numbers of beneficiaries and claims.
with
  carrier_claim_counts as (
    select "CarrierClaims"."beneficiaryId" as beneficiary_id, count(distinct "CarrierClaims"."claimId") as carrier_claim_count, count(*) as carrier_claim_line_count
      from "CarrierClaims" inner join "CarrierClaimLines" on "CarrierClaims"."claimId" = "CarrierClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,dme_claim_counts as (
    select "DMEClaims"."beneficiaryId" as beneficiary_id, count(distinct "DMEClaims"."claimId") as dme_claim_count, count(*) as dme_claim_line_count
      from "DMEClaims" inner join "DMEClaimLines" on "DMEClaims"."claimId" = "DMEClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,hha_claim_counts as (
    select "HHAClaims"."beneficiaryId" as beneficiary_id, count(distinct "HHAClaims"."claimId") as hha_claim_count, count(*) as hha_claim_line_count
      from "HHAClaims" inner join "HHAClaimLines" on "HHAClaims"."claimId" = "HHAClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,hospice_claim_counts as (
    select "HospiceClaims"."beneficiaryId" as beneficiary_id, count(distinct "HospiceClaims"."claimId") as hospice_claim_count, count(*) as hospice_claim_line_count
      from "HospiceClaims" inner join "HospiceClaimLines" on "HospiceClaims"."claimId" = "HospiceClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,inpatient_claim_counts as (
    select "InpatientClaims"."beneficiaryId" as beneficiary_id, count(distinct "InpatientClaims"."claimId") as inpatient_claim_count, count(*) as inpatient_claim_line_count
      from "InpatientClaims" inner join "InpatientClaimLines" on "InpatientClaims"."claimId" = "InpatientClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,outpatient_claim_counts as (
    select "OutpatientClaims"."beneficiaryId" as beneficiary_id, count(distinct "OutpatientClaims"."claimId") as outpatient_claim_count, count(*) as outpatient_claim_line_count
      from "OutpatientClaims" inner join "OutpatientClaimLines" on "OutpatientClaims"."claimId" = "OutpatientClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,partd_event_counts as (
    select "PartDEvents"."beneficiaryId" as beneficiary_id, count(*) as partd_event_count
      from "PartDEvents"
      group by beneficiary_id
  )
  ,snf_claim_counts as (
    select "SNFClaims"."beneficiaryId" as beneficiary_id, count(distinct "SNFClaims"."claimId") as snf_claim_count, count(*) as snf_claim_line_count
      from "SNFClaims" inner join "SNFClaimLines" on "SNFClaims"."claimId" = "SNFClaimLines"."parentClaim"
      group by beneficiary_id
  )
select
    count("Beneficiaries"."beneficiaryId") as beneficiaries_count
    ,count("Beneficiaries"."beneficiaryId") filter (
      where (
        coalesce(carrier_claim_counts.carrier_claim_count, 0)
        + coalesce(dme_claim_counts.dme_claim_count, 0)
        + coalesce(hha_claim_counts.hha_claim_count, 0)
        + coalesce(hospice_claim_counts.hospice_claim_count, 0)
        + coalesce(inpatient_claim_counts.inpatient_claim_count, 0)
        + coalesce(outpatient_claim_counts.outpatient_claim_count, 0)
        + coalesce(partd_event_counts.partd_event_count, 0)
        + coalesce(snf_claim_counts.snf_claim_count, 0)
      ) > 0
    ) as beneficiaries_with_claims_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(carrier_claim_counts.carrier_claim_count, 0) > 0) as beneficiaries_with_carrier_claims_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(dme_claim_counts.dme_claim_count, 0) > 0) as beneficiaries_with_dme_claims_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(hha_claim_counts.hha_claim_count, 0) > 0) as beneficiaries_with_hha_claims_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(hospice_claim_counts.hospice_claim_count, 0) > 0) as beneficiaries_with_hospice_claims_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(inpatient_claim_counts.inpatient_claim_count, 0) > 0) as beneficiaries_with_inpatient_claims_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(outpatient_claim_counts.outpatient_claim_count, 0) > 0) as beneficiaries_with_outpatient_claims_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(partd_event_counts.partd_event_count, 0) > 0) as beneficiaries_with_partd_events_count
    ,count("Beneficiaries"."beneficiaryId") filter (where coalesce(snf_claim_counts.snf_claim_count, 0) > 0) as beneficiaries_with_snf_claims_count
    ,sum((
      coalesce(carrier_claim_counts.carrier_claim_count, 0)
      + coalesce(dme_claim_counts.dme_claim_count, 0)
      + coalesce(hha_claim_counts.hha_claim_count, 0)
      + coalesce(hospice_claim_counts.hospice_claim_count, 0)
      + coalesce(inpatient_claim_counts.inpatient_claim_count, 0)
      + coalesce(outpatient_claim_counts.outpatient_claim_count, 0)
      + coalesce(partd_event_counts.partd_event_count, 0)
      + coalesce(snf_claim_counts.snf_claim_count, 0)
    )) as claims_count
    ,sum(coalesce(carrier_claim_counts.carrier_claim_count, 0)) as carrier_claims_count
    ,sum(coalesce(dme_claim_counts.dme_claim_count, 0)) as dme_claims_count
    ,sum(coalesce(hha_claim_counts.hha_claim_count, 0)) as hha_claims_count
    ,sum(coalesce(hospice_claim_counts.hospice_claim_count, 0)) as hospice_claims_count
    ,sum(coalesce(inpatient_claim_counts.inpatient_claim_count, 0)) as inpatient_claims_count
    ,sum(coalesce(outpatient_claim_counts.outpatient_claim_count, 0)) as outpatient_claims_count
    ,sum(coalesce(partd_event_counts.partd_event_count, 0)) as partd_events_count
    ,sum(coalesce(snf_claim_counts.snf_claim_count, 0)) as snf_claims_count
  from 
    "Beneficiaries"
    left join carrier_claim_counts on "Beneficiaries"."beneficiaryId" = carrier_claim_counts.beneficiary_id
    left join dme_claim_counts on "Beneficiaries"."beneficiaryId" = dme_claim_counts.beneficiary_id
    left join hha_claim_counts on "Beneficiaries"."beneficiaryId" = hha_claim_counts.beneficiary_id
    left join hospice_claim_counts on "Beneficiaries"."beneficiaryId" = hospice_claim_counts.beneficiary_id
    left join inpatient_claim_counts on "Beneficiaries"."beneficiaryId" = inpatient_claim_counts.beneficiary_id
    left join outpatient_claim_counts on "Beneficiaries"."beneficiaryId" = outpatient_claim_counts.beneficiary_id
    left join partd_event_counts on "Beneficiaries"."beneficiaryId" = partd_event_counts.beneficiary_id
    left join snf_claim_counts on "Beneficiaries"."beneficiaryId" = snf_claim_counts.beneficiary_id;

-- Results for SAMPLE_C data:
--  beneficiaries_count | beneficiaries_with_claims_count | beneficiaries_with_carrier_claims_count | beneficiaries_with_dme_claims_count | beneficiaries_with_hha_claims_count | beneficiaries_with_hospice_claims_count | beneficiaries_with_inpatient_claims_count | beneficiaries_with_outpatient_claims_count | beneficiaries_with_partd_events_count | beneficiaries_with_snf_claims_count | claims_count | carrier_claims_count | dme_claims_count | hha_claims_count | hospice_claims_count | inpatient_claims_count | outpatient_claims_count | partd_events_count | snf_claims_count 
-- ---------------------+---------------------------------+-----------------------------------------+-------------------------------------+-------------------------------------+-----------------------------------------+-------------------------------------------+--------------------------------------------+---------------------------------------+-------------------------------------+--------------+----------------------+------------------+------------------+----------------------+------------------------+-------------------------+--------------------+------------------
--              1000000 |                          902636 |                                  651658 |                              226704 |                               90276 |                                   22949 |                                    179751 |                                     504134 |                                734509 |                               50436 |    109914678 |             32943217 |          2320363 |           228623 |               106462 |                 384616 |                 6195549 |           67566673 |           169175
-- (1 row)
-- 
-- Time: 499023.863 ms  (8 minutes)
-- 
-- Running this against the production data took 231578878.926 ms  (64 hours)


-- Count each beneficiary's total number of claims and/or claim lines,
-- separately by claim types and combined.
-- Has an (optional) `where` clause that restricts it to benes that have at
-- least one of every claim type.
with
  carrier_claim_counts as (
    select "CarrierClaims"."beneficiaryId" as beneficiary_id, count(distinct "CarrierClaims"."claimId") as carrier_claim_count, count(*) as carrier_claim_line_count
      from "CarrierClaims" inner join "CarrierClaimLines" on "CarrierClaims"."claimId" = "CarrierClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,dme_claim_counts as (
    select "DMEClaims"."beneficiaryId" as beneficiary_id, count(distinct "DMEClaims"."claimId") as dme_claim_count, count(*) as dme_claim_line_count
      from "DMEClaims" inner join "DMEClaimLines" on "DMEClaims"."claimId" = "DMEClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,hha_claim_counts as (
    select "HHAClaims"."beneficiaryId" as beneficiary_id, count(distinct "HHAClaims"."claimId") as hha_claim_count, count(*) as hha_claim_line_count
      from "HHAClaims" inner join "HHAClaimLines" on "HHAClaims"."claimId" = "HHAClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,hospice_claim_counts as (
    select "HospiceClaims"."beneficiaryId" as beneficiary_id, count(distinct "HospiceClaims"."claimId") as hospice_claim_count, count(*) as hospice_claim_line_count
      from "HospiceClaims" inner join "HospiceClaimLines" on "HospiceClaims"."claimId" = "HospiceClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,inpatient_claim_counts as (
    select "InpatientClaims"."beneficiaryId" as beneficiary_id, count(distinct "InpatientClaims"."claimId") as inpatient_claim_count, count(*) as inpatient_claim_line_count
      from "InpatientClaims" inner join "InpatientClaimLines" on "InpatientClaims"."claimId" = "InpatientClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,outpatient_claim_counts as (
    select "OutpatientClaims"."beneficiaryId" as beneficiary_id, count(distinct "OutpatientClaims"."claimId") as outpatient_claim_count, count(*) as outpatient_claim_line_count
      from "OutpatientClaims" inner join "OutpatientClaimLines" on "OutpatientClaims"."claimId" = "OutpatientClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,partd_event_counts as (
    select "PartDEvents"."beneficiaryId" as beneficiary_id, count(*) as partd_event_count
      from "PartDEvents"
      group by beneficiary_id
  )
  ,snf_claim_counts as (
    select "SNFClaims"."beneficiaryId" as beneficiary_id, count(distinct "SNFClaims"."claimId") as snf_claim_count, count(*) as snf_claim_line_count
      from "SNFClaims" inner join "SNFClaimLines" on "SNFClaims"."claimId" = "SNFClaimLines"."parentClaim"
      group by beneficiary_id
  )
select
    "Beneficiaries"."beneficiaryId" as beneficiary_id
    ,(
      coalesce(carrier_claim_counts.carrier_claim_count, 0)
      + coalesce(dme_claim_counts.dme_claim_count, 0)
      + coalesce(hha_claim_counts.hha_claim_count, 0)
      + coalesce(hospice_claim_counts.hospice_claim_count, 0)
      + coalesce(inpatient_claim_counts.inpatient_claim_count, 0)
      + coalesce(outpatient_claim_counts.outpatient_claim_count, 0)
      + coalesce(partd_event_counts.partd_event_count, 0)
      + coalesce(snf_claim_counts.snf_claim_count, 0)
    ) as total_claim_count
    ,(
      coalesce(carrier_claim_counts.carrier_claim_line_count, 0)
      + coalesce(dme_claim_counts.dme_claim_line_count, 0)
      + coalesce(hha_claim_counts.hha_claim_line_count, 0)
      + coalesce(hospice_claim_counts.hospice_claim_line_count, 0)
      + coalesce(inpatient_claim_counts.inpatient_claim_line_count, 0)
      + coalesce(outpatient_claim_counts.outpatient_claim_line_count, 0)
      + coalesce(partd_event_counts.partd_event_count, 0)
      + coalesce(snf_claim_counts.snf_claim_line_count, 0)
    ) as total_claim_line_count
    ,coalesce(carrier_claim_counts.carrier_claim_count, 0) as carrier_claim_count
    ,coalesce(carrier_claim_counts.carrier_claim_line_count, 0) as carrier_claim_line_count
    ,coalesce(dme_claim_counts.dme_claim_count, 0) as dme_claim_count
    ,coalesce(dme_claim_counts.dme_claim_line_count, 0) as dme_claim_line_count
    ,coalesce(hha_claim_counts.hha_claim_count, 0) as hha_claim_count
    ,coalesce(hha_claim_counts.hha_claim_line_count, 0) as hha_claim_line_count
    ,coalesce(hospice_claim_counts.hospice_claim_count, 0) as hospice_claim_count
    ,coalesce(hospice_claim_counts.hospice_claim_line_count, 0) as hospice_claim_line_count
    ,coalesce(inpatient_claim_counts.inpatient_claim_count, 0) as inpatient_claim_count
    ,coalesce(inpatient_claim_counts.inpatient_claim_line_count, 0) as inpatient_claim_line_count
    ,coalesce(outpatient_claim_counts.outpatient_claim_count, 0) as outpatient_claim_count
    ,coalesce(outpatient_claim_counts.outpatient_claim_line_count, 0) as outpatient_claim_line_count
    ,coalesce(partd_event_counts.partd_event_count, 0) as partd_event_count
    ,coalesce(snf_claim_counts.snf_claim_count, 0) as snf_claim_count
    ,coalesce(snf_claim_counts.snf_claim_line_count, 0) as snf_claim_line_count
  from 
    "Beneficiaries"
    left join carrier_claim_counts on "Beneficiaries"."beneficiaryId" = carrier_claim_counts.beneficiary_id
    left join dme_claim_counts on "Beneficiaries"."beneficiaryId" = dme_claim_counts.beneficiary_id
    left join hha_claim_counts on "Beneficiaries"."beneficiaryId" = hha_claim_counts.beneficiary_id
    left join hospice_claim_counts on "Beneficiaries"."beneficiaryId" = hospice_claim_counts.beneficiary_id
    left join inpatient_claim_counts on "Beneficiaries"."beneficiaryId" = inpatient_claim_counts.beneficiary_id
    left join outpatient_claim_counts on "Beneficiaries"."beneficiaryId" = outpatient_claim_counts.beneficiary_id
    left join partd_event_counts on "Beneficiaries"."beneficiaryId" = partd_event_counts.beneficiary_id
    left join snf_claim_counts on "Beneficiaries"."beneficiaryId" = snf_claim_counts.beneficiary_id
  -- The `*_count > 0` where clauses can be disabled. Only use them if you want to find beneficiaries who have at least 1 of ALL claim types.
  where
    carrier_claim_line_count > 0
    and dme_claim_line_count > 0
    and hha_claim_line_count > 0
    and hospice_claim_line_count > 0
    and inpatient_claim_line_count > 0
    and outpatient_claim_line_count > 0
    and partd_event_count > 0
    and snf_claim_line_count > 0
  order by total_claim_line_count desc
  limit 100;

-- Results for SAMPLE_C data:
--  beneficiary_id | total_claim_count | total_claim_line_count |
-- ----------------+-------------------+------------------------+
--  416500         |               896 |                   3786 |        
--  299601         |               995 |                   3659 |        
--  358620         |              1206 |                   3606 |        
--  978915         |              1135 |                   3549 |        
--  876472         |               886 |                   3443 |


-- This version of the query returns line count percentiles, i.e. "X% of the beneficiaries have >= Y claim lines".
with
  carrier_claim_counts as (
    select "CarrierClaims"."beneficiaryId" as beneficiary_id, count(distinct "CarrierClaims"."claimId") as carrier_claim_count, count(*) as carrier_claim_line_count
      from "CarrierClaims" inner join "CarrierClaimLines" on "CarrierClaims"."claimId" = "CarrierClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,dme_claim_counts as (
    select "DMEClaims"."beneficiaryId" as beneficiary_id, count(distinct "DMEClaims"."claimId") as dme_claim_count, count(*) as dme_claim_line_count
      from "DMEClaims" inner join "DMEClaimLines" on "DMEClaims"."claimId" = "DMEClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,hha_claim_counts as (
    select "HHAClaims"."beneficiaryId" as beneficiary_id, count(distinct "HHAClaims"."claimId") as hha_claim_count, count(*) as hha_claim_line_count
      from "HHAClaims" inner join "HHAClaimLines" on "HHAClaims"."claimId" = "HHAClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,hospice_claim_counts as (
    select "HospiceClaims"."beneficiaryId" as beneficiary_id, count(distinct "HospiceClaims"."claimId") as hospice_claim_count, count(*) as hospice_claim_line_count
      from "HospiceClaims" inner join "HospiceClaimLines" on "HospiceClaims"."claimId" = "HospiceClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,inpatient_claim_counts as (
    select "InpatientClaims"."beneficiaryId" as beneficiary_id, count(distinct "InpatientClaims"."claimId") as inpatient_claim_count, count(*) as inpatient_claim_line_count
      from "InpatientClaims" inner join "InpatientClaimLines" on "InpatientClaims"."claimId" = "InpatientClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,outpatient_claim_counts as (
    select "OutpatientClaims"."beneficiaryId" as beneficiary_id, count(distinct "OutpatientClaims"."claimId") as outpatient_claim_count, count(*) as outpatient_claim_line_count
      from "OutpatientClaims" inner join "OutpatientClaimLines" on "OutpatientClaims"."claimId" = "OutpatientClaimLines"."parentClaim"
      group by beneficiary_id
  )
  ,partd_event_counts as (
    select "PartDEvents"."beneficiaryId" as beneficiary_id, count(*) as partd_event_count
      from "PartDEvents"
      group by beneficiary_id
  )
  ,snf_claim_counts as (
    select "SNFClaims"."beneficiaryId" as beneficiary_id, count(distinct "SNFClaims"."claimId") as snf_claim_count, count(*) as snf_claim_line_count
      from "SNFClaims" inner join "SNFClaimLines" on "SNFClaims"."claimId" = "SNFClaimLines"."parentClaim"
      group by beneficiary_id
  )
select
    unnest(percentile_disc(array[0.25, 0.5, 0.75, 0.9, 0.99, 1])
      within group (order by (
        coalesce(carrier_claim_counts.carrier_claim_line_count, 0)
        + coalesce(dme_claim_counts.dme_claim_line_count, 0)
        + coalesce(hha_claim_counts.hha_claim_line_count, 0)
        + coalesce(hospice_claim_counts.hospice_claim_line_count, 0)
        + coalesce(inpatient_claim_counts.inpatient_claim_line_count, 0)
        + coalesce(outpatient_claim_counts.outpatient_claim_line_count, 0)
        + coalesce(partd_event_counts.partd_event_count, 0)
        + coalesce(snf_claim_counts.snf_claim_line_count, 0)
      ))
    )
  from 
    "Beneficiaries"
    left join carrier_claim_counts on "Beneficiaries"."beneficiaryId" = carrier_claim_counts.beneficiary_id
    left join dme_claim_counts on "Beneficiaries"."beneficiaryId" = dme_claim_counts.beneficiary_id
    left join hha_claim_counts on "Beneficiaries"."beneficiaryId" = hha_claim_counts.beneficiary_id
    left join hospice_claim_counts on "Beneficiaries"."beneficiaryId" = hospice_claim_counts.beneficiary_id
    left join inpatient_claim_counts on "Beneficiaries"."beneficiaryId" = inpatient_claim_counts.beneficiary_id
    left join outpatient_claim_counts on "Beneficiaries"."beneficiaryId" = outpatient_claim_counts.beneficiary_id
    left join partd_event_counts on "Beneficiaries"."beneficiaryId" = partd_event_counts.beneficiary_id
    left join snf_claim_counts on "Beneficiaries"."beneficiaryId" = snf_claim_counts.beneficiary_id;

-- When run against the SAMPLE_C data, this gave me the following (in 476630.642 ms):
--  unnest 
-- --------
--      36  -- 25th percentile total claim line count (for sample beneficiaries)
--     111  -- 50th percentile total claim line count (for sample beneficiaries)
--     257  -- 75th percentile total claim line count (for sample beneficiaries)
--     489  -- 90th percentile total claim line count (for sample beneficiaries)
--    1304  -- 99th percentile total claim line count (for sample beneficiaries)
--    9237  -- 100th percentile total claim line count (for sample beneficiaries)
