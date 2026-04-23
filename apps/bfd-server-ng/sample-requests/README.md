# Sample Requests

Sample requests using synthetic data available in the test/sandbox environments.

The `patient-match-*` samples can be used with the `/v3/fhir/Patient/$idi-match` endpoint.

Example using `xh` (note the relative path used for the request file):

```sh
xh --cert ~/your/cert.pem --cert-key ~/your/key.pem --verify=no POST "https://test.fhirv3.bfd.cmscloud.local/v3/fhir/Patient/\$idi-match" '@sample-requests/patient-match-case-1.json' "X-CLIENT-IP:0.0.0.0" "X-CLIENT-NAME:Frederick" "X-CLIENT-ID:Flinstone"
```
