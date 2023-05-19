-- Copy tables to CSV files on the local machine

\copy beneficiaries to 'beneficiaries.csv' csv header;
\copy beneficiaries_history to 'beneficiaries_history.csv' csv header;
\copy beneficiaries_history_invalid_beneficiaries to 'beneficiaries_history_invalid_beneficiaries.csv' csv header;
\copy beneficiary_monthly to 'beneficiary_monthly.csv' csv header;
\copy carrier_claim_lines to 'carrier_claim_lines.csv' csv header;
\copy carrier_claims to 'carrier_claims.csv' csv header;
\copy dme_claim_lines to 'dme_claim_lines.csv' csv header;
\copy dme_claims to 'dme_claims.csv' csv header;
\copy hha_claim_lines to 'hha_claim_lines.csv' csv header;
\copy hha_claims to 'hha_claims.csv' csv header;
\copy hospice_claim_lines to 'hospice_claim_lines.csv' csv header;
\copy hospice_claims to 'hospice_claims.csv' csv header;
\copy inpatient_claim_lines to 'inpatient_claim_lines.csv' csv header;
\copy inpatient_claims to 'inpatient_claims.csv' csv header;
\copy outpatient_claim_lines to 'outpatient_claim_lines.csv' csv header;
\copy outpatient_claims to 'outpatient_claims.csv' csv header;
\copy partd_events to 'partd_events.csv' csv header;
\copy snf_claim_lines to 'snf_claim_lines.csv' csv header;
\copy snf_claims to 'snf_claims.csv' csv header;
