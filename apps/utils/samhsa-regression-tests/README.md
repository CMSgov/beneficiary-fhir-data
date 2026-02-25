# `samhsa-regression-tests`

This Python utility is composed of a single script, `main.py`, that when ran with the correct inputs will verify that SAMHSA filtering on the V3 Server is working as expected. This is a full E2E-test, so _real_ data is seeded from the appropriate database before sending requests to the V3 Server.

The script does the following:

1. Concurrently loads SAMHSA claims from the database by matching on known SAMHSA codes that exist in various columns and tables
2. Matches the aforementioned SAMHSA claims to a single beneficiary via their `bene_sk`
3. Sends concurrent requests using a certificate authorized to view SAMHSA claims and one that is not for the beneficiary with SAMHSA claims
4. Verifies that the "SAMHSA-allowed" request returned SAMHSA claims and the "SAMHSA-disallowed" request did not
5. If _all_ beneficiaries pass verification, the script returns a success result; otherwise, the script returns a failure result and exits with a non-zero error code

## Help text

```text
Usage: main.py [OPTIONS]

Options:
  -h, --hostname TEXT            The hostname with optional port of the V3
                                 server to send requests to  [required]
  -s, --security-labels PATH     Path to the security labels YML file
                                 [required]
  -c, --samhsa-cert PATH         Path to the PEM-encoded certificate
                                 authorized to retrieve SAMHSA data. Optional
                                 for local testing, REQUIRED for remote
  -k, --samhsa-cert-key PATH     Path to the private key of the certificate
                                 authorized to retrieve SAMHSA data. Optional
                                 for local testing, REQUIRED for remote
  -C, --no-samhsa-cert PATH      Path to the PEM-encoded certificate NOT
                                 authorized to retrieve SAMHSA data. Optional
                                 for local testing, REQUIRED for remote
  -K, --no-samhsa-cert-key PATH  Path to the private key of the certificate
                                 NOT authorized to retrieve SAMHSA data.
                                 Optional for local testing, REQUIRED for
                                 remote
  -H, --host-cert PATH           Path to the PEM-encoded certificate to verify
                                 the V3 Server with; if empty, no verification
                                 will be done
  -d, --db-conn-str TEXT         Database connection string in key-value
                                 string form; if empty, details will be taken
                                 from typical 'PG...' environment variables
  -t, --tablesample FLOAT        Tamplesample percentage from 0-100 of which
                                 claim_item and claim_institutional rows will
                                 be sampled; defaults to 10%
  -l, --limit INTEGER            Limit of unique claim IDs (not necessarily
                                 beneficiaries) to return from queries
                                 onclaim_item and claim_institutional;
                                 defaults to 300 rows
  --help                         Show this message and exit.
```

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

If running a local test (discerned via `--hostname`), any `--...cert` parameter may be omitted, e.g.:

```bash
uv run main.py --hostname localhost:8080 \
  --security-labels ~/Repositories/beneficiary-fhir-data/apps/bfd-server-ng/src/main/resources/security_labels.yml \
  --db-conn-str "database=..." # OR, pass via typical "PG..." environment variables!
```

Run `uv run main.py --help` for more information on avaiable arguments.
