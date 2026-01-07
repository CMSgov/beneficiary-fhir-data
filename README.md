# Beneficiary FHIR Data System (BFD)

## About the Project

This project contains modules and documentation in support of the Beneficiary FHIR Data (BFD) Server.
The BFD Server is an API designed to serve Medicare beneficiaries' demographic, enrollment, and claims data using the [HL7® FHIR® Standard](https://www.hl7.org/fhir/overview.html) format.

## Core Team

Although this is a public repo, contributing to the BFD is for CMS-approved contributors only, not outside contributors.
For more information about our BFD teams, see [COMMUNITY.md](COMMUNITY.md)

## Project Vision

It's the vision of this project to provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data.

## Agency Mission

This aligns with the overarching mission to enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

## Repo Structure

The high-level purpose and location of each piece of the project is listed below.

* [apps](apps) - contains the source code for each of the deployed BFD applications
  * Legacy (V1/V2) App
    * [bfd-data-fda](apps/bfd-data-fda) - downloads FDA Drug code names into a resource used during BFD drug code name lookups
    * [bfd-data-npi](apps/bfd-data-npi) - downloads CMS NPI (National Provider Identification) names into a resource used during BFD NPI lookups
    * [bfd-db-migrator](apps/bfd-db-migrator) - application for safely applying schema updates and data migrations to the BFD database
    * [bfd-model](apps/bfd-model) - contains data models used throughout the BFD project
    * [bfd-pipeline](apps/bfd-pipeline) - application for loading claim data from the provider into the BFD database
    * [bfd-server](apps/bfd-server) - application for serving the BFD database data in FHIR format to users via the BFD API
    * [bfd-shared-test-utils](apps/bfd-shared-test-utils) - utilities shared across BFD projects used in testing
    * [bfd-shared-utils](apps/bfd-shared-utils) - utilities shared across BFD projects used in the application code
    * [utils](apps/utils) - non-application scripts used for testing, development, and database management
  * V3 App
    * [bfd-model-idr](apps/bfd-model-idr) - data models and synthetic data generation used for mapping IDR data
    * [bfd-pipeline-idr](apps/bfd-pipeline-idr) - ETL pipeline for transforming and loading IDR data
    * [bfd-server-ng](apps/bfd-server-ng) - New version of the BFD REST API, serving FHIR data
    * [bfd-db-migrator-ng](apps/bfd-db-migrator-ng) - simple facade for running flyway migrations using maven
* [insights](insights) - contains documentation and resources for maintaining BFD Insights, a platform using AWS Cloudwatch to provide analytics and metrics for BFD applications
* [ops](ops) - contains the scripts and resources required for packaging and deploying BFD applications
* [rfcs](docs/rfcs) - holds the archived and active RFC (Request for Comment) documents for BFD

### V3 App Setup

The V3 app should be set up in the following order since they are dependent on each other
(follow the READMEs listed in each directory):

1. [bfd-model-idr](apps/bfd-model-idr)
2. [bfd-pipeline-idr](apps/bfd-pipeline-idr)
3. [bfd-server-ng](apps/bfd-server-ng)

## Contributing

Many useful guides, documentation items, and runbooks can be found on the [BFD Wiki](https://github.com/CMSgov/beneficiary-fhir-data/wiki), hosted in this repo.

This includes information about [making requests to bfd](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Making-Requests-to-BFD), [synthetic data](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide), and more.

The [BFD Wiki](https://github.com/CMSgov/beneficiary-fhir-data/wiki) contains useful resources like the [development environment setup guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Local-Environment-Setup-for-BFD-Development), [style guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/BFD-Code-Style-Guide), [runbooks](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Runbooks) for various scenarios, and more.

## Community Guidelines

We want to ensure a welcoming environment for all of our projects. Our staff follow the [18F Code of Conduct](https://github.com/18F/code-of-conduct/blob/master/code-of-conduct.md) and all contributors should do the same.

## Feedback

If you have any questions feel free to reach out on the [#bfd](https://cmsgov.slack.com/archives/C010WDXAZFZ) channel in CMS slack!

## Policies

### Open Source Policy

We adhere to the [CMS Open Source
Policy](https://github.com/CMSGov/cms-open-source-policy). If you have any
questions, just [shoot us an email](mailto:opensource@cms.hhs.gov).

### Security and Responsible Disclosure Policy

_Submit a vulnerability:_ Vulnerability reports can be submitted through [Bugcrowd](https://bugcrowd.com/cms-vdp). Reports may be submitted anonymously. If you share contact information, we will acknowledge receipt of your report within 3 business days.

For more information about our Security, Vulnerability, and Responsible Disclosure Policies, see [SECURITY.md](SECURITY.md).

### Software Bill of Materials (SBOM)

A Software Bill of Materials (SBOM) is a formal record containing the details and supply chain relationships of various components used in building software.

In the spirit of [Executive Order 14028 - Improving the Nation’s Cyber Security](https://www.gsa.gov/technology/it-contract-vehicles-and-purchasing-programs/information-technology-category/it-security/executive-order-14028), a SBOM for this repository is provided here: <https://github.com/CMSgov/beneficiary-fhir-data/network/dependencies>.

For more information and resources about SBOMs, visit: <https://www.cisa.gov/sbom>.

## Public domain

This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/) as indicated in [LICENSE](LICENSE).

All contributions to this project will be released under the CC0 dedication. By submitting a pull request or issue, you are agreeing to comply with this waiver of copyright interest.
