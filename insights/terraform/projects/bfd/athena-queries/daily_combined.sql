CREATE OR REPLACE VIEW "bfd_insights_bfd_prod_daily_combined" AS
SELECT
    COALESCE(a."num_benes", 0) AS benes_queried,
    COALESCE(b."num_benes", 0) AS benes_first_seen,
    date_parse(COALESCE(a."day", b."day"), '%Y-%m-%d') AS day
FROM
    "bfd_insights_bfd_prod_daily_benes" AS a
    FULL JOIN "bfd_insights_bfd_prod_daily_unique_benes" AS b
        ON (a."day" = b."day" OR a."day" IS NULL OR b."day" IS NULL);
