# README

## Overview
`bene-prefs` is a DASG system that fetches Medicare Beneficiary Preferences data directly from IDR-C and provides this data in a familiar format to BFD Partners: AB2D, BCDA, and DPC.

The [templates directory](./app/templates) stores [jinja templates](https://jinja.palletsprojects.com/en/stable/) where there is a template for each partner's:
- snowflake sql query `<partner>.sql.j2`
- fixed-width preferences file format `<partner>.prefs.j2`
- the resultant name of the preferences file `<partner>.file-name.j2`.

Below are the steps taken to generate the partner's preferences files:
1. `last_execution` is stored from the DynmoDB table that tracks execution timestamps
1. `query` is stored from the rendered query template using `last_execution` from the previous step and the current time
1. `results` are stored from the execution of the previous step's `query`
1. `preferences_data` is stored from the rendered preferences template using the `results` from the previous step
1. `file_name` is rendered to provide a name to the resultant preferences file
1. `preferences_data` is written to partner-specific AWS S3 Bucket under the `file_name` object.
1. `last_execution` is updated in the DynamoDB table that tracks execution timestamps.

### Quirks and Features

- Scheduled (twice-daily) execution to fetch timely beneficiary preference data
  - Monday through Saturday at 1017 UTC and 2217 UTC ([05|06]17 and [21|22]17 for EST|EDT)
  - Sundays are excluded with no evidence of preference inserts occurring on a Sunday to the beginning of the preference data
  ```sql
  select count(*), dayofweek(idr_insrt_ts), dayname(idr_insrt_ts)
  from idrc_prd.cms_vdm_view_mdcr_prd.v2_mdcr_bene_shrng_prefnc
  group by dayofweek(idr_insrt_ts), dayname(idr_insrt_ts)
  order by dayofweek(idr_insrt_ts)
  ```
- Queries, response files, and file names are supported by maintainable templates including
- Supports _historical_ cross-reference relationships: a beneficiary's preference today are automatically applied to their previous identifiers using BFD's tested logic for bene cross-references

### Future Work
As of this writing, this **does not** support prospective/forward-looking cross-references automatically.
That is, if a beneficiary opts out of data sharing **today** and they are issued a new MBI **tomorrow**, there is no automatic linking of that new MBI to their preferences record.
A solution will be available shortly after parity with the existing EFT-based process has been reached in production.
