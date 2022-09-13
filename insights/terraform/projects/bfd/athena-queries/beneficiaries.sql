CREATE OR REPLACE VIEW "bfd_insights_bfd_prod_beneficiaries" AS
    SELECT
        TRY(CAST(TRIM(bene) as bigint)) as bene,
        "bfd_insights_bfd_prod_partners".*
    FROM "bfd_insights_bfd_prod_partners"
    CROSS JOIN UNNEST(split(benes, ',')) as x(bene)
    WHERE
        (partner = 'ab2d'
        OR partner = 'bb2'
        OR partner = 'bcda'
        OR partner = 'dpc');
