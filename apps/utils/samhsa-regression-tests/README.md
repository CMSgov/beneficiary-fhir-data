# `samhsa-regression-tests`

This Python utility is composed of a single script, `main.py`, that when ran with the correct inputs will verify that SAMHSA filtering on the V3 Server is working as expected. This is a full E2E-test, so _real_ data is seeded from the appropriate database before sending requests to the V3 Server.

The script does the following:

1. Concurrently loads SAMHSA claims from the database by matching on known SAMHSA codes that exist in various columns and tables
2. Matches the aforementioned SAMHSA claims to a single beneficiary via their `bene_sk`
3. Sends concurrent requests using a certificate authorized to view SAMHSA claims and one that is not for the beneficiary with SAMHSA claims
4. Verifies that the "SAMHSA-allowed" request returned SAMHSA claims and the "SAMHSA-disallowed" request did not
5. If _all_ beneficiaries pass verification, the script returns a success result; otherwise, the script returns a failure result and exits with a non-zero error code

## Usage

```bash
just samhsa-regression-test --env test
```
