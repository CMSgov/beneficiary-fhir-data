# Beneficiary FHIR Data Server (BFD)
====================================

## About

Beneficiary FHIR Data (BFD) Server: The BFD Server is an internal backend system used at CMS to represent Medicare beneficiaries' demographic, enrollment, and claims data in [FHIR](https://www.hl7.org/fhir/overview.html) format.

### DASG Mission
Drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

### BFD Mission
Enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

### BFD Vision
Provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data.

### License

This project is in the worldwide [public domain](LICENSE.md). As stated in [LICENSE](LICENSE.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.

## BFD User Documentation

The following provide information on how to use BFD:

* [Request Audit Headers](./docs/request-audit-headers.md):
  This document details the HTTP headers that should be included when calling BFD,
    to ensure that proper audit information is available to the BFD team.
* [Request Options](./docs/request-options.md):
  This document details the request options that can be used when calling BFD.

### Security

We work with sensitive information: do not put any PHI or PII in the public repo for BFD.

If you believe you’ve found or been made aware of a security vulnerability, please refer to the CMS Vulnerability Disclosure Policy (here is a [link](https://www.cms.gov/Research-Statistics-Data-and-Systems/CMS-Information-Technology/CIO-Directives-and-Policies/Downloads/CMS-Vulnerability-Disclosure-Policy.pdf) to the most recent version as of the time of this commit.
