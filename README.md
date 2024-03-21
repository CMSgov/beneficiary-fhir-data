Beneficiary FHIR Data System (BFD)
====================================
test commit

## About

This project contains modules and documentation in support of the Beneficiary FHIR Data (BFD) Server. 
The BFD Server is an API designed to serve Medicare beneficiaries' demographic, enrollment, and claims data using the [HL7® FHIR® Standard](https://www.hl7.org/fhir/overview.html) format.

It's the vision of this project to provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data.
This aligns with the overarching mission to enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they
need to make informed decisions about their healthcare.

The high-level purpose and location of each piece of the project is listed below.

* [apps](apps) - contains the source code for each of the deployed BFD applications
  * [bfd-data-fda](apps/bfd-data-fda) - downloads FDA Drug code names into a resource used during BFD drug code name lookups
  * [bfd-data-npi](apps/bfd-data-npi) - downloads CMS NPI (National Provider Identification) names into a resource used during BFD NPI lookups
  * [bfd-db-migrator](apps/bfd-db-migrator) - application for safely applying schema updates and data migrations to the BFD database
  * [bfd-model](apps/bfd-model) - contains data models used throughout the BFD project
  * [bfd-pipeline](apps/bfd-pipeline) - application for loading claim data from the provider into the BFD database
  * [bfd-server](apps/bfd-server) - application for serving the BFD database data in FHIR format to users via the BFD API
  * [bfd-shared-test-utils](apps/bfd-shared-test-utils) - utilities shared across BFD projects used in testing
  * [bfd-shared-utils](apps/bfd-shared-utils) - utilities shared across BFD projects used in the application code
  * [utils](apps/utils) - non-application scripts used for testing, development, and database management
* [insights](insights) - contains documentation and resources for maintaining BFD Insights, a platform using AWS Cloudwatch to provide analytics and metrics for BFD applications
* [ops](ops) - contains the scripts and resources required for packaging and deploying BFD applications
* [rfcs](docs/rfcs) - holds the archived and active RFC (Request for Comment) documents for BFD

## Documentation

### Users

Many useful guides, documentation items, and runbooks can be found on the [BFD Wiki](https://github.com/CMSgov/beneficiary-fhir-data/wiki), hosted in this repo.

This includes information about [making requests to bfd](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Making-Requests-to-BFD), 
[synthetic data](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide), and more.

### Contributors and Maintainers

The [BFD Wiki](https://github.com/CMSgov/beneficiary-fhir-data/wiki) contains useful
resources like the [development environment setup guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Local-Environment-Setup-for-BFD-Development),
[style guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/BFD-Code-Style-Guide), [runbooks](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Runbooks) for various scenarios, and more.

## Security

We work with sensitive information: do not put any PHI or PII in the public repo for BFD.

If you believe you’ve found or been made aware of a security vulnerability, please refer to 
the [CMS Vulnerability Disclosure Policy](https://www.cms.gov/Research-Statistics-Data-and-Systems/CMS-Information-Technology/CIO-Directives-and-Policies/Downloads/CMS-Vulnerability-Disclosure-Policy.pdf) 
to the most recent version as of the time of this commit.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [LICENSE](LICENSE.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
