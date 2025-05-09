# Beneficiary FHIR Data System (BFD)

## About the Project
The Beneficiary FHIR Data (BFD) Server is an API designed to serve Medicare beneficiaries' demographic, enrollment, and claims data using the [HL7® FHIR® Standard](https://www.hl7.org/fhir/overview.html) format.

It's the vision of this project to provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data. This aligns with the overarching mission to enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

## Project Vision
To provide a comprehensive, performant, and trustworthy platform to transform the way that the CMS enterprise shares and uses data.

## Project Mission 
To enable the CMS Enterprise to drive innovation in data sharing so that beneficiaries and their healthcare partners have the data they need to make informed decisions about their healthcare.

## Agency Mission
CMS is dedicated to providing beneficiaries and their healthcare partners with the data they need to make informed healthcare decisions.

## Team Mission
The BFD team works collaboratively to develop and maintain a FHIR-based API for Medicare data access, ensuring performance, security, and compliance with healthcare standards.

## Core Team
An up-to-date list of core team members can be found in [COMMUNITY.md](COMMUNITY.md). The project is still building the core team and defining roles and responsibilities. We are eagerly seeking individuals who would like to join the community and help us define and fill these roles.

## Documentation Index
- [README.md](README.md) - Overview of the BFD project
- [CONTRIBUTING.md](CONTRIBUTING.md) - Guidelines for contributing to BFD
- [LICENSE.md](LICENSE.md) - Project license information
- [SECURITY.md](SECURITY.md) - Security and vulnerability disclosure policies

## Repository Structure
The high-level purpose and location of each piece of the project is listed below:

- **[apps](apps)** - contains the source code for each of the deployed BFD applications
  - [bfd-data-fda](apps/bfd-data-fda) - downloads FDA Drug code names into a resource used during BFD drug code name lookups
  - [bfd-data-npi](apps/bfd-data-npi) - downloads CMS NPI (National Provider Identification) names into a resource used during BFD NPI lookups
  - [bfd-db-migrator](apps/bfd-db-migrator) - application for safely applying schema updates and data migrations to the BFD database
  - [bfd-model](apps/bfd-model) - contains data models used throughout the BFD project
  - [bfd-pipeline](apps/bfd-pipeline) - application for loading claim data from the provider into the BFD database
  - [bfd-server](apps/bfd-server) - application for serving the BFD database data in FHIR format to users via the BFD API
  - [bfd-shared-test-utils](apps/bfd-shared-test-utils) - utilities shared across BFD projects used in testing
  - [bfd-shared-utils](apps/bfd-shared-utils) - utilities shared across BFD projects used in the application code
  - [utils](apps/utils) - non-application scripts used for testing, development, and database management
- **[insights](insights)** - contains documentation and resources for maintaining BFD Insights, a platform using AWS Cloudwatch to provide analytics and metrics for BFD applications
- **[ops](ops)** - contains the scripts and resources required for packaging and deploying BFD applications
- **[rfcs](docs/rfcs)** - holds the archived and active RFC (Request for Comment) documents for BFD

## Development and Software Delivery Lifecycle
The following guide is for members of the project team who have access to the repository as well as code contributors. The main difference between internal and external contributions is that external contributors will need to fork the project and will not be able to merge their own pull requests. For more information on contributing, see: [CONTRIBUTING.md](./CONTRIBUTING.md).

## Local Development
Going to work on this project? Great! There are currently two documented methods for getting a local environment up and running to get you setup for development.

See the [development environment setup guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Local-Environment-Setup-for-BFD-Development) in the BFD Wiki for detailed instructions.

## Coding Style and Linters
Each application has its own linting and testing guidelines. Lint and code tests are run on each commit, so linters and tests should be run locally before committing.

For tips on how to adhere to the code documentation rules and general style tips, check out the [BFD Style Guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/BFD-Code-Style-Guide).

## Branching Model
The BFD project follows the [GitHub Flow Workflow](https://guides.github.com/introduction/flow/). Details on contributing can be found in the [CONTRIBUTING.md](CONTRIBUTING.md) file.

## Documentation

### Users
Many useful guides, documentation items, and runbooks can be found on the [BFD Wiki](https://github.com/CMSgov/beneficiary-fhir-data/wiki), hosted in this repo.

This includes information about [making requests to BFD](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Making-Requests-to-BFD), [synthetic data](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide), and more.

### Contributors and Maintainers
The [BFD Wiki](https://github.com/CMSgov/beneficiary-fhir-data/wiki) contains useful resources like the [development environment setup guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Local-Environment-Setup-for-BFD-Development), [style guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/BFD-Code-Style-Guide), [runbooks](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Runbooks) for various scenarios, and more.

## Contributing
Thank you for considering contributing to an Open Source project of the US Government! For more information about our contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md).

## Codeowners
The contents of this repository are managed by CMS. Those responsible for the code and documentation in this repository can be found in [COMMUNITY.md](COMMUNITY.md).

## Community
The BFD team is taking a community-first and open source approach to the product development of this tool. We believe government software should be made in the open and be built and licensed such that anyone can download the code, run it themselves without paying money to third parties or using proprietary software, and use it as they will.

We know that we can learn from a wide variety of communities, including those who will use or will be impacted by the tool, who are experts in technology, or who have experience with similar technologies deployed in other spaces. We are dedicated to creating forums for continuous conversation and feedback to help shape the design and development of the tool.

We also recognize capacity building as a key part of involving a diverse open source community. We are doing our best to use accessible language, provide technical and process documents, and offer support to community members with a wide variety of backgrounds and skillsets.

## Community Guidelines
Principles and guidelines for participating in our open source community can be found in the [18F Code of Conduct](https://github.com/18F/code-of-conduct/blob/master/code-of-conduct.md). Please read them before joining or starting a conversation in this repo or one of the channels listed below. All community members and participants are expected to adhere to the community guidelines and code of conduct when participating in community spaces including: code repositories, communication channels and venues, and events.

## Open Source Policy
We adhere to the [CMS Open Source Policy](https://github.com/CMSGov/cms-open-source-policy). If you have any questions, just [shoot us an email](mailto:opensource@cms.hhs.gov).

## Security and Responsible Disclosure Policy
*Submit a vulnerability:* Vulnerability reports can be submitted through [Bugcrowd](https://bugcrowd.com/cms-vdp). Reports may be submitted anonymously. If you share contact information, we will acknowledge receipt of your report within 3 business days.

For more information about our Security, Vulnerability, and Responsible Disclosure Policies, see [SECURITY.md](SECURITY.md).

## Security
We work with sensitive information: do not put any PHI or PII in the public repo for BFD.

If you believe you've found or been made aware of a security vulnerability, please refer to the [CMS Vulnerability Disclosure Policy](https://www.cms.gov/Research-Statistics-Data-and-Systems/CMS-Information-Technology/CIO-Directives-and-Policies/Downloads/CMS-Vulnerability-Disclosure-Policy.pdf) for the most recent version as of the time of this commit.

## Software Bill of Materials (SBOM)
A Software Bill of Materials (SBOM) is a formal record containing the details and supply chain relationships of various components used in building software.

In the spirit of [Executive Order 14028 - Improving the Nation's Cyber Security](https://www.gsa.gov/technology/it-contract-vehicles-and-purchasing-programs/information-technology-category/it-security/executive-order-14028), a SBOM for this repository is provided here: https://github.com/CMSgov/beneficiary-fhir-data/network/dependencies.

For more information and resources about SBOMs, visit: https://www.cisa.gov/sbom.

## License
This project is in the worldwide [public domain](LICENSE.md). As stated in [LICENSE](LICENSE.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.