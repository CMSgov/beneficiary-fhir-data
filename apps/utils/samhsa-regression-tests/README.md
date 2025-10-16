# `samhsa-regression-tests`

This Python utility is composed of a single script, `main.py`, that when ran with the correct inputs will verify that SAMHSA filtering on the V3 Server is working as expected. This is a full E2E-test, so _real_ data is seeded from the appropriate database before sending requests to the V3 Server.

The script does the following:

1. Concurrently loads SAMHSA claims from the database by matching on known SAMHSA codes that exist in various columns and tables
2. Matches the aforementioned SAMHSA claims to a single beneficiary via their `bene_sk`
3. Sends concurrent requests using a certificate authorized to view SAMHSA claims and one that is not for the beneficiary with SAMHSA claims
4. Verifies that the "SAMHSA-allowed" request returned SAMHSA claims and the "SAMHSA-disallowed" request did not
5. If _all_ beneficiaries pass verification, the script returns a success result; otherwise, the script returns a failure result and exits with a non-zero error code

## Usage

Using `uv run main.py`, pass the required arguments, e.g.:

```bash
uv run main.py --hostname test.fhirv3.bfd.cmscloud.local \
  --security-labels ~/Repositories/beneficiary-fhir-data/apps/bfd-server-ng/src/main/resources/security_labels.yml \
  --host-cert ~/.certs/keystore.testv3.pem \
  --samhsa-cert ~/.certs/test-samhsa.pem \
  --samhsa-cert-key ~/.certs/test-samhsa.pem \
  --no-samhsa-cert ~/.certs/test.pem \
  --no-samhsa-cert-key ~/.certs/test.pem \
  --db-conn-str "database=..." # OR, pass via typical "PG..." environment variables!
```

Run `uv run main.py --help` for more information on avaiable arguments.
