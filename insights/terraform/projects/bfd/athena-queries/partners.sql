CREATE OR REPLACE VIEW "bfd_insights_bfd_prod_partners" AS
SELECT
    mdc_bene_id AS benes,
    CASE "mdc_http_access_request_clientssl_dn"
        WHEN 'C=US, CN=dpc.prod.client' then 'dpc'
        WHEN 'C=US,CN=dpc.prod.client' then 'dpc'
        WHEN 'CN=bluebutton-backend-prod-data-server-client-test' then 'bfd_test'
        WHEN 'CN=bcda-prod-client' then 'bcda'
        WHEN 'CN=ab2d-prod-client' then 'ab2d'
        WHEN 'CN=ab2d-prod-validation-client' then 'ab2d_test'
        WHEN 'EMAILADDRESS=ryan@adhocteam.us, CN=BlueButton Root CA, OU=BlueButton on FHIR API Root CA, O=Centers for Medicare and Medicaid Services, L=Baltimore, ST=Maryland, C=US' then 'bb2'
        WHEN '1.2.840.113549.1.9.1=#16117279616e406164686f637465616d2e7573,CN=BlueButton Root CA,OU=BlueButton on FHIR API Root CA,O=Centers for Medicare and Medicaid Services,L=Baltimore,ST=Maryland,C=US' then 'bb2'
        -- bfd_migration_test below was used during early 2022 for validation of database migrations
        WHEN 'CN=bfd.cms.gov' then 'bfd_migration_test'
        ELSE 'unknown'
    END AS partner,
    *
FROM "bfd_insights_bfd_prod_api_requests" AS api_requests;
