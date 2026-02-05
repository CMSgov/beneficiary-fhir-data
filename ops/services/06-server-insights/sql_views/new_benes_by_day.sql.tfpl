--
-- This view is the basis for the Quicksight dashboard 'BFD Unique Medicare Enrollees' and
-- returns one row for each day that one of the 4 partners (ab2d, bb2, bcda, dpc)
-- accessed a new beneficiary. The content of the returned rows is the number of new beneficiaries
-- that were seen on that day by partner, and from BFD as a whole.
--
-- NOTE: The counts for a given day will not add up in the sense that if
--       ab2d sees 10 new benes and bb2 sees 5 new benes, the total new
--       benes seen by BFD as a whole can be anywhere from 0 to 15 depending
--       on whether there is overlap between the 10 new ab2d benes and the 5
--       new bb2 benes and whether another parter such as bcda already requested
--       some or all of the new benes on a previous day.
--
-- NOTE: We exclude calls to the patient by contract endpoint because while those
--       calls are made by AB2D, the results are not returned to end users. Any end
--       user requests related to that data will be made later via a call to the
--       ExplanationOfBenefit endpoint.
--
create or replace view new_benes_by_day as
select
    cast(day as date) as day,
    coalesce(partner_counts['ab2d'], 0) as ab2d_new_benes,
    coalesce(partner_counts['bb2'], 0) as bb2_new_benes,
    coalesce(partner_counts['bcda'], 0) as bcda_new_benes,
    coalesce(partner_counts['dpc'], 0) as dpc_new_benes,
    coalesce(partner_counts['bulk'], 0) as bulk_new_benes,
    coalesce(partner_counts['bfd'], 0) as bfd_new_benes
from (
    select map_agg(partner, bene_count) as partner_counts, day
    from (
        select partner, day, count(*) as bene_count
        from (
            select
                bene,
                partner,
                date_format(from_iso8601_timestamp(min("timestamp")), '%Y-%m-%d') AS day
            from api_requests_by_bene
            where partner in ('ab2d', 'bb2', 'bcda', 'dpc')
                  and bene > 0
                  and response_status = '200'
                  and "mdc_http_access_request_operation" not like '%by=coverageContractForYearMonth%'
            group by bene, partner
        )
        group by day, partner
        union
        select 'bfd', day, count(*) as bene_count
        from (
            select
                bene,
                date_format(from_iso8601_timestamp(min("timestamp")), '%Y-%m-%d') AS day
            from api_requests_by_bene
            where partner in ('ab2d', 'bb2', 'bcda', 'dpc')
                  and bene > 0
                  and response_status = '200'
                  and "mdc_http_access_request_operation" not like '%by=coverageContractForYearMonth%'
            group by bene
        )
        group by day
        union
        select 'bulk', day, count(*) as bene_count
        from (
            select
                bene,
                date_format(from_iso8601_timestamp(min("timestamp")), '%Y-%m-%d') AS day
            from api_requests_by_bene
            where partner in ('ab2d', 'bcda', 'dpc')
                  and bene > 0
                  and response_status = '200'
                  and "mdc_http_access_request_operation" not like '%by=coverageContractForYearMonth%'
            group by bene
        )
        group by day
    )
    group by day
)
