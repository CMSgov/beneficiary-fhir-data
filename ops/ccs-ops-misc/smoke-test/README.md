# Smoke Test

This script can be run to quickly test the response times of a set of endpoints in BFD. It reads in a configuration file of the calls to make and then runs them. The endpoints are all run concurrently in separate goroutines to speed up the test. Within each spawned goroutine, calls to a single endpoint will be run multiple times synchronously, and the response times are averaged together.

### Running

The script requires three options to be set as environment variables:
- `CLIENT_CERT`: the path to the combined client cert/key in PEM format.
- `BASE_URL`: the protocol/host/port combination of the BFD instance or load balancer that you're calling.
- `ENDPOINTS`: a JSON file describing the endpoints to call. More on that format below.

Here's an example of how to run it:
```bash
CLIENT_CERT=$BFD_PATH/apps/bfd-server/dev/ssl-stores/client-unsecured.pem \
BASE_URL='https://localhost:6500' \
ENDPOINTS=endpoints.json \
go run smoke.go
```

If you want to build it to run somewhere else, you can use `go build` and then set the same environment variables as above but run the binary.

### Endpoints file format
The headers field is optional, but useful if you need to add an additional custom header to a call.

```json
{
  "endpoints": [
    {
      "label": "Coverage - By Beneficiary - 10p",
      "path": "/v2/fhir/Coverage?_count=10&beneficiary=123456",
      "headers": [
        "Name: value"
      ]
    }
  ]
}
```

### Output format
The smoketests print to stdout in the following format. Each call in the endpoints file is made multiple times, and the average response time is presented in milliseconds.
```bash
Avg(ms) Endpoint
109     /v1/fhir/Coverage?_count=10&beneficiary=123456
109     /v1/fhir/Patient?_id=123456&_format=application%2Ffhir%2Bjson&_IncludeIdentifiers=true
618     /v1/fhir/ExplanationOfBenefit?patient=123456&_format=application%2Ffhir%2Bjson&_page=50
```

### Interpreting the output
There are two ways to interpret the results of the smoke test. First, you can check the results against previous baselines. The calls are also made with a custom `BULK-CLIENTID` of `db-smoketest`. This header is tracked in New Relic Transactions. From there it's easy to see more specific stats like how long was spent in the database.

### Potential future extensions
- The script is currently geared to measure response times, but it could be extended to handle network, client, and server errors gracefully instead of logging and exiting. 
- These are fast enough a generate a small enough load that they could easily be extended to run on a recurring basis as regression tests on every PR.
