Get all MBIs and BENE_SKs for a bene

```sql
SELECT 
    hstry.bene_mbi_id, 
    hstry.bene_sk, 
    hstry.bene_mbi_id = bene.bene_mbi_id AS is_current_mbi
FROM CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
JOIN CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY hstry ON bene.bene_xref_efctv_sk_computed = hstry.bene_xref_efctv_sk_computed
WHERE 
    bene.bene_mbi_id = '<mbi>' 
    AND (hstry.bene_mbi_id IS NOT NULL OR hstry.bene_sk != bene.bene_sk)
    AND NOT EXISTS (SELECT 1 FROM idr.overshare_mbis iom WHERE iom.bene_mbi_id = bene.bene_mbi_id or iom.bene_mbi_id = hstry.bene_mbi_id)
GROUP BY hstry.bene_mbi_id, hstry.bene_sk, bene.bene_mbi_id
```

Get all Part D contracts with start/end dates for a bene

```sql
with contracts as (
select 
    bene.bene_sk,
    bene.bene_mbi_id,
    usg.bene_enrlmt_efctv_dt, 
    usg.bene_elctn_enrlmt_disenrlmt_cd,
    usg.idr_trans_obslt_ts, 
    usg.bene_cntrct_num, 
    usg.bene_pbp_num, 
    usg.bene_elctn_aplctn_dt,
from CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_ELCTN_PRD_USG usg
join CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene on usg.bene_sk = bene.bene_sk 
join CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_CNTRCT_PBP_NUM cntrct on cntrct.cntrct_pbp_sk = usg.cntrct_pbp_sk
where 
    usg.idr_trans_obslt_ts >= '9999-12-31'
    -- only part D contracts
    and cntrct.cntrct_drug_plan_ind_cd = 'Y'
    and cntrct.cntrct_pbp_sk_obslt_dt >= '9999-12-31'
),
contract_dts as (
select 
    enrollments.bene_sk,
    enrollments.bene_mbi_id,
    enrollments.bene_enrlmt_efctv_dt, 
    enrollments.bene_elctn_enrlmt_disenrlmt_cd,
    enrollments.idr_trans_obslt_ts, 
    enrollments.bene_cntrct_num, 
    enrollments.bene_pbp_num, 
    disenrollments.bene_elctn_aplctn_dt,
    row_number() over (order by enrollments.bene_enrlmt_efctv_dt) as dt_order,
from contracts enrollments
left join contracts disenrollments 
    on enrollments.bene_enrlmt_efctv_dt = disenrollments.bene_enrlmt_efctv_dt 
    and enrollments.bene_sk = disenrollments.bene_sk and disenrollments.bene_elctn_enrlmt_disenrlmt_cd = 'D'
where 
    enrollments.bene_mbi_id = '<mbi>' 
    -- select only enrollment records
    and enrollments.bene_elctn_enrlmt_disenrlmt_cd = 'E'
)
select 
    c1.bene_sk, 
    c1.bene_mbi_id, 
    c1.bene_cntrct_num, 
    c1.bene_enrlmt_efctv_dt, 
    -- if there's no disenrollment, the end date is the next contract start date - 1 day
    coalesce(c1.bene_elctn_aplctn_dt, c2.bene_enrlmt_efctv_dt - interval '1 day') as end_dt
from contract_dts c1 
-- match the next enrollment record if there is no disenrollment
left join contract_dts c2 on c2.dt_order = c1.dt_order + 1
order by c1.bene_enrlmt_efctv_dt
```
