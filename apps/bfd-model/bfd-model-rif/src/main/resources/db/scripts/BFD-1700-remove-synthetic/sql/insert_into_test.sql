-- Truncate tables and re-insert from local CSV files. Note that you have to run the truncate all
-- at once to avoid problems with foreign key constraints.

BEGIN TRANSACTION;

TRUNCATE beneficiaries, beneficiaries_history, beneficiaries_history_invalid_beneficiaries, beneficiary_monthly, carrier_claim_lines, carrier_claims, dme_claim_lines, dme_claims, hha_claim_lines, hha_claims, hospice_claim_lines, hospice_claims, inpatient_claim_lines, inpatient_claims, outpatient_claim_lines, outpatient_claims, partd_events, snf_claim_lines, snf_claims;

\copy beneficiaries from 'beneficiaries.csv' delimiter ',' csv header;
\copy beneficiaries_history from 'beneficiaries_history.csv' delimiter ',' csv header;
\copy beneficiaries_history_invalid_beneficiaries from 'beneficiaries_history_invalid_beneficiaries.csv' delimiter ',' csv header;
\copy beneficiary_monthly from 'beneficiary_monthly.csv' delimiter ',' csv header;
\copy carrier_claims from 'carrier_claims.csv' delimiter ',' csv header;
\copy carrier_claim_lines from 'carrier_claim_lines.csv' delimiter ',' csv header;
\copy dme_claims from 'dme_claims.csv' delimiter ',' csv header;
\copy dme_claim_lines from 'dme_claim_lines.csv' delimiter ',' csv header;
\copy hha_claims from 'hha_claims.csv' delimiter ',' csv header;
\copy hha_claim_lines from 'hha_claim_lines.csv' delimiter ',' csv header;
\copy hospice_claims from 'hospice_claims.csv' delimiter ',' csv header;
\copy hospice_claim_lines from 'hospice_claim_lines.csv' delimiter ',' csv header;
\copy inpatient_claims from 'inpatient_claims.csv' delimiter ',' csv header;
\copy inpatient_claim_lines from 'inpatient_claim_lines.csv' delimiter ',' csv header;
\copy outpatient_claims from 'outpatient_claims.csv' delimiter ',' csv header;
\copy outpatient_claim_lines from 'outpatient_claim_lines.csv' delimiter ',' csv header;
\copy partd_events from 'partd_events.csv' delimiter ',' csv header;
\copy snf_claims from 'snf_claims.csv' delimiter ',' csv header;
\copy snf_claim_lines from 'snf_claim_lines.csv' delimiter ',' csv header;

COMMIT TRANSACTION;
