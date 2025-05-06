Beneficiary FHIR Data System (BFD)
====================================

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
 
 ## About the Project 
<!-- This should be a longer-form description of the project. It can include history, background, details, problem statements, links to design documents or other supporting materials, or any other information/context that a user or contributor might be interested in. --> 
 
 ## Project Vision 
<!-- Provide the long-term goals and aspirations for this project. --> 
 
 ## Project Mission 
<!-- Provide the core mission and objectives driving this project. --> 
 
 ## Agency Mission 
<!-- Provide the mission of the agency and how this project aligns. --> 
 
 ## Team Mission 
<!-- Provide the team's mission and how they work together. --> 
 
 ## Core Team 
An up-to-date list of core team members can be found in [COMMUNITY.md](COMMUNITY.md). At this time, the project is still building the core team and defining roles and responsibilities. We are eagerly seeking individuals who would like to join the community and help us define and fill these roles. 
 
 ## Documentation Index 
<!-- TODO: This is a like a 'table of contents' for your documentation. Tier 0/1 projects with simple README.md files without many sections may or may not need this, but it is still extremely helpful to provide 'bookmark' or 'anchor' links to specific sections of your file to be referenced in tickets, docs, or other communication channels. --> 
 **{list of .md at top directory and descriptions}** 
 
 ## Repository Structure 
<!-- TODO: Using the 'tree -d' command can be a helpful way to generate this information, but, be sure to update it as the project evolves and changes over time. --> 
 <!--TREE START--><!--TREE END--> 
 **{list directories and descriptions}** 
 
 ## Development and Software Delivery Lifecycle 
The following guide is for members of the project team who have access to the repository as well as code contributors. The main difference between internal and external contributions is that external contributors will need to fork the project and will not be able to merge their own pull requests. For more information on contributing, see: [CONTRIBUTING.md](./CONTRIBUTING.md). 
 
 ## Local Development 
<!--- TODO - with example below: 
This project is monorepo with several apps. Please see the [api](./api/README.md) and [frontend](./frontend/README.md) READMEs for information on spinning up those projects locally. Also see the project [documentation](./documentation) for more info. --> 
 
 ## Coding Style and Linters 
<!-- TODO - Add the repo's linting and code style guidelines --> 
 Each application has its own linting and testing guidelines. Lint and code tests are run on each commit, so linters and tests should be run locally before commiting. 
 
 ## Branching Model 
 <!-- TODO - with example below:
 This project follows [trunk-based development](https://trunkbaseddevelopment.com/), which means:

* Make small changes in [short-lived feature branches](https://trunkbaseddevelopment.com/short-lived-feature-branches/) and merge to `main` frequently.
* Be open to submitting multiple small pull requests for a single ticket (i.e. reference the same ticket across multiple pull requests).
* Treat each change you merge to `main` as immediately deployable to production. Do not merge changes that depend on subsequent changes you plan to make, even if you plan to make those changes shortly.
* Ticket any unfinished or partially finished work.
* Tests should be written for changes introduced, and adhere to the text percentage threshold determined by the project.

This project uses **continuous deployment** using [Github Actions](https://github.com/features/actions) which is configured in the [./github/workflows](.github/workflows) directory.

Pull-requests are merged to `main` and the changes are immediately deployed to the development environment. Releases are created to push changes to production.--> 
 
 ## Contributing 
Thank you for considering contributing to an Open Source project of the US Government! For more information about our contribution guidelines, see [CONTRIBUTING.md](CONTRIBUTING.md). 
 
 ## Codeowners 
 The contents of this repository are managed by {responsible organization(s)}. Those responsible for the code and documentation in this repository can be found in [COMMUNITY.md](COMMUNITY.md). 
 
 ## Community 
The {name_of_project_here} team is taking a community-first and open source approach to the product development of this tool. We believe government software should be made in the open and be built and licensed such that anyone can download the code, run it themselves without paying money to third parties or using proprietary software, and use it as they will.

We know that we can learn from a wide variety of communities, including those who will use or will be impacted by the tool, who are experts in technology, or who have experience with similar technologies deployed in other spaces. We are dedicated to creating forums for continuous conversation and feedback to help shape the design and development of the tool.

We also recognize capacity building as a key part of involving a diverse open source community. We are doing our best to use accessible language, provide technical and process documents, and offer support to community members with a wide variety of backgrounds and skillsets. 
 
 ## Community Guidelines 
Principles and guidelines for participating in our open source community are can be found in [COMMUNITY_GUIDELINES.md](COMMUNITY_GUIDELINES.md). Please read them before joining or starting a conversation in this repo or one of the channels listed below. All community members and participants are expected to adhere to the community guidelines and code of conduct when participating in community spaces including: code repositories, communication channels and venues, and events. 
 
 ## Feedback 
If you have ideas for how we can improve or add to our capacity building efforts and methods for welcoming people into our community, please let us know at **{contact_email}**. If you would like to comment on the tool itself, please let us know by filing an **issue on our GitHub repository.** 
 
 ### Open Source Policy 
We adhere to the [CMS Open Source Policy](https://github.com/CMSGov/cms-open-source-policy). If you have any questions, just [shoot us an email](mailto:opensource@cms.hhs.gov). 
 
 ### Security and Responsible Disclosure Policy 
*Submit a vulnerability:* Vulnerability reports can be submitted through [Bugcrowd](https://bugcrowd.com/cms-vdp). Reports may be submitted anonymously. If you share contact information, we will acknowledge receipt of your report within 3 business days.
For more information about our Security, Vulnerability, and Responsible Disclosure Policies, see [SECURITY.md](SECURITY.md). 
 
 ### Software Bill of Materials (SBOM) 
A Software Bill of Materials (SBOM) is a formal record containing the details and supply chain relationships of various components used in building software.
In the spirit of [Executive Order 14028 - Improving the Nation's Cyber Security](https://www.gsa.gov/technology/it-contract-vehicles-and-purchasing-programs/information-technology-category/it-security/executive-order-14028), a SBOM for this repository is provided here: https://github.com/{repo_org}/{repo_name}/network/dependencies.
For more information and resources about SBOMs, visit: https://www.cisa.gov/sbom.