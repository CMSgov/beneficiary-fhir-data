CREATE OR REPLACE VIEW "bfd_insights_bfd_prod_daily_benes" AS
    SELECT
        COUNT(*) AS "num_benes",
        DATE_FORMAT(from_iso8601_timestamp("timestamp"), '%Y-%m-%d') AS "day"
    FROM
        "bfd_insights_bfd_prod_beneficiaries"
    WHERE
        "bene" > 0
        AND "mdc_http_access_response_status" = '200'
    GROUP BY DATE_FORMAT(from_iso8601_timestamp("timestamp"), '%Y-%m-%d')
    ORDER BY DATE_FORMAT(from_iso8601_timestamp("timestamp"), '%Y-%m-%d');
