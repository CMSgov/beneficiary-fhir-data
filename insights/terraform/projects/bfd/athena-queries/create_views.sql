--
-- This view makes working with the 'bfd_insights_bfd_prod_api_requests' table more convenient
-- by exposing new columns that have more intuitive names or values. There is a one to one
-- correspondence of rows from the underlying table to this view (there is no filtering
-- of data). This view can be enhanced as needed by adding new convenience columns.
--
create or replace view api_requests as
select
    mdc_bene_id as benes,
    mdc_http_access_response_status as response_status,
    case mdc_http_access_request_clientssl_dn
        when 'C=US, CN=dpc.prod.client' then 'dpc'
        when 'C=US,CN=dpc.prod.client' then 'dpc'
        when 'CN=bluebutton-backend-prod-data-server-client-test' then 'bfd_test'
        when 'CN=bcda-prod-client' then 'bcda'
        when 'CN=ab2d-prod-client' then 'ab2d'
        when 'CN=ab2d-prod-validation-client' then 'ab2d_test'
        when 'EMAILADDRESS=ryan@adhocteam.us, CN=BlueButton Root CA, OU=BlueButton on FHIR API Root CA, O=Centers for Medicare and Medicaid Services, L=Baltimore, ST=Maryland, C=US' then 'bb2'
        when '1.2.840.113549.1.9.1=#16117279616e406164686f637465616d2e7573,CN=BlueButton Root CA,OU=BlueButton on FHIR API Root CA,O=Centers for Medicare and Medicaid Services,L=Baltimore,ST=Maryland,C=US' then 'bb2'
        -- bfd_migration_test below was used during early 2022 for validation of database migrations
        when 'CN=bfd.cms.gov' then 'bfd_migration_test'
        else 'unknown'
    end AS partner,
    *
from bfd_insights_bfd_prod_api_requests;

--
-- This view pivots the 'benes' column out of the 'api_requests' view which results in
-- each row of the 'api_requests' table being transformed into N rows where N is the
-- number of benes in the 'benes' column with a new column added that contains the
-- value of the item from the comma separated 'benes' column.
--
-- For example, if the api_requests contains these rows:
--    benes|partner|...
--    123|ab2d|...
--    123,456|bb2|...
-- Then this view will return these rows:
--    bene|benes|partner|...
--    123|123|ab2d|...
--    123|123,456|bb2|...
--    456|123,456|bb2|...
--
create or replace view api_requests_by_bene AS
    select
        try(cast(trim(bene) as bigint)) as bene,
        api_requests.*
    from api_requests
    cross join unnest(split(benes, ',')) as x(bene);

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
