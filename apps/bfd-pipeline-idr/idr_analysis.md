```sql
create table CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE (
    bene_sk bigint,
    bene_xref_sk bigint,
    bene_xref_efctv_sk bigint,
    bene_mbi_id varchar(11)
)

create table CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY (
    bene_sk bigint,
    bene_xref_sk bigint,
    bene_xref_efctv_sk bigint,
    bene_mbi_id varchar(11)
)
```

No null MBIs that haven't been merged into another record, this is good

``` sql
-- 1 row, bene_sk 0, dummy data
select count(*) 
from CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE 
where bene_mbi_id is null and bene_sk = bene_xref_efctv_sk
```

Except 9 have no valid MBI in their xref data

```sql
-- 9 rows
select count(*) from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
where not exists(
    select 1 from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY history 
    where history.bene_xref_efctv_sk = bene.bene_xref_efctv_sk and history.bene_mbi_id is not null)
```

No duplicate MBIs that haven't been merged into another record, this is good

```sql
-- 0 rows
select count(*) from (
    select 1 from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
        where bene_sk = bene_xref_efctv_sk
        group by bene.bene_mbi_id
        having count(bene.bene_mbi_id) > 1
)
```

Benes missing an xref_efctv_sk, unclear what this means

```sql
-- 2036 rows
select count(*)
from CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE 
where bene_xref_efctv_sk = 0
```

Some of these also have null MBIs. These records are orphaned. 

```sql
-- 644 rows
select count(*) 
from CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE 
where bene_mbi_id is null and bene_xref_efctv_sk = 0
```

Records are correctly marked as obsolete when they've been merged into another, this is good

```sql
-- 1 row, bene_sk 0, dummy data
select count(*)
from CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene 
where idr_trans_obslt_ts < '9999-12-31' and bene_sk = bene_xref_efctv_sk
```

MBIs assigned to more than one person, this is bad

```sql
-- 1 row
select count(distinct bene_mbi_id) 
from CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
where bene_mbi_id is not null and bene_xref_efctv_sk != 0
group by bene_mbi_id
having count(distinct bene_xref_efctv_sk) > 1
```

This is a weird one - there is exactly one record that is able to be linked by BENE_XREF_SK only and not BENE_XREF_EFCTV_SK.
bene_xref_efctv_sk is set to 0 for 1 of 3 related records - can probably ignore this since there's just one.

```sql
-- 1 row
select bene_sk, bene_mbi_id, bene_xref_sk, bene_xref_efctv_sk from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
 where bene.bene_sk != bene.bene_xref_sk and bene.bene_xref_sk != bene.bene_xref_efctv_sk and bene.bene_xref_sk != 0
and not exists(
    select 1 from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene2 
    where bene2.bene_sk = bene.bene_xref_sk and bene2.bene_xref_efctv_sk = bene.bene_xref_efctv_sk
)
```

All xrefs are tracked in the history table, no need to join against bene_xref_efctv_sk on the bene table to find other relationships, this is good

```sql
-- 0 rows
with missing_hstry as (
    select bene_sk FROM IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY hstry
    where not exists(
        select 1 from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene 
        where bene.bene_sk = hstry.bene_sk and bene.bene_xref_efctv_sk = hstry.bene_xref_efctv_sk
    )
)
select count(*) FROM IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene 
join missing_hstry mh on mh.bene_sk = bene.bene_sk
where bene_xref_efctv_sk != 0
```

Records where the xref_efctv_sk on the history table doesn't match the bene table.
All of these are cases where xref_efctv_sk is zero on the bene table, but not the history table.

```sql
-- 382 rows
select bene.bene_sk, bene.bene_xref_efctv_sk, history.bene_xref_efctv_sk
from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
join IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY history on bene.bene_sk = history.bene_sk and bene.bene_xref_efctv_sk != history.bene_xref_efctv_sk
```

No MBIs present on the history table that aren't present on the bene table, this is good

```sql
-- 0 rows
select count(*) from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
join IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY history on bene.bene_sk = history.bene_sk
where bene.bene_mbi_id is null and bene.bene_xref_efctv_sk = 0 and history.bene_mbi_id is not null
```

Benes without a valid MBI on the history table

```sql
-- 9 rows
select count(*) from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
where not exists(
    select 1 from IDRC_PRD.CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_HSTRY history 
    where history.bene_xref_efctv_sk = bene.bene_xref_efctv_sk and history.bene_mbi_id is not null)
```

Benes with a matching record in V2_MDCR_BENE_XREF that has kill credit = 1 (this means the xref is invalid)

```sql
-- 2801 rows
with invalid_xref as (
  select * from CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE_XREF xref where xref.bene_xref_sk != 0 and xref.bene_kill_cred_cd = '1'
)
select bene.bene_sk, bene.bene_xref_efctv_sk, bene.bene_xref_sk, ix.bene_xref_sk from 
CMS_VDM_VIEW_MDCR_PRD.V2_MDCR_BENE bene
join invalid_xref ix on ix.bene_sk = bene.bene_xref_efctv_sk and ix.bene_xref_sk = bene.bene_sk
```
