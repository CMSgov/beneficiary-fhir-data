# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0019-semantic-versioning`
* Start Date: 2023-04-21
* RFC PR: [beneficiary-fhir-data/rfcs#0019](https://github.com/CMSgov/beneficiary-fhir-data/pull/1642)
* JIRA Ticket(s):
    * [BFD-1743](https://jira.cms.gov/browse/BFD-1743)

BFD needs a top-level, automation-friendly, human-readable, and overall meaningful version string to apply to releases and their related artifacts.
This will help:
- _release engineers_ maintain a change log to communicate enhancements and relative risk for deployment operations
- _product engineers_ reason about the sum of changes to the product over time
- _operations engineers_ target specific artifacts for deployments to various environments
- _automation engineers_ make automated processes _automatic_
- _performance engineers_ compare changes and relative performance
- _technical writers_ manage product documentation
- _product managers_ understand when features are reaching customers
- _customers_ anticipate new features as they become available
- _api users_ learn about and take advantage of both new and existing product features

## Status
[Status]: #status

* Status: Approved <!-- (Proposed/Approved/Rejected/Implemented) -->
* Implementation JIRA Ticket(s):
    * [BFD-1820](https://jira.cms.gov/browse/BFD-1820)

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Status](#status)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [Proposed Solution: Detailed Design](#proposed-solution-detailed-design)
    * [Proposed Solution: Unresolved Questions](#proposed-solution-unresolved-questions)
    * [Proposed Solution: Drawbacks](#proposed-solution-drawbacks)
    * [Proposed Solution: Notable Alternatives](#proposed-solution-notable-alternatives)
* [Prior Art](#prior-art)
* [Future Possibilities](#future-possibilities)
* [Addenda](#addenda)

## Motivation
[Motivation]: #motivation

BFD already includes simple versions for certain aspects of the system but their utility is somewhat limited:
- The FHIR API Server endpoint version is incremented to communicate backward incompatibility (`v1` vs `v2`)
- Pipeline ETL Manifest schema version also communicates backward incompatibility, but when/how this is incremented is somewhat arbitrary (`v9` vs `v10`)
- Database schema and migration script versions increments for each new script, featuring a [requirement for backward compatibility](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/docs/rfcs/0011-separate-flyway-from-pipeline.md#proposed-solution) between two consecutive versions (`V100` and `V101`) **but** compatibility is not guaranteed between three (`V100` and `V102`) 
- Jenkins build versions serve as a reference point to specific job executions, but are otherwise meaningless (`#260` vs `#261`)

Though helpful, existing resource versions are insufficient for deriving relevant information for the change outside the specific contexts in which the versions are applied; in other words, these version strings are not _semantic_.
However, it's possible to generate a more meaningful, top-level version string that can serve as a heuristic to facilitate managing, documenting, and communicating changes among BFD contributors and customers alike.

## Proposed Solution
[Proposed Solution]: #proposed-solution

BFD uses a version string that conforms to [semver.org@2.0.0](https://semver.org/spec/v2.0.0.html#summary).
This version string is applied to various release artifacts that can be used by contributors and customers to facilitate communications surrounding changes across the BFD system.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

1. A BFD contributor as _release engineer_ will identify a specific point in the _repository's history as being important_ by creating a [Git Tag](https://git-scm.com/book/en/v2/Git-Basics-Tagging).
2. The git tag will conform to the [semver@2.0.0 specification](https://semver.org/spec/v2.0.0.html#semantic-versioning-specification-semver) including precedence rules for constructing the _next_ version from the _current_/_latest_ tagged release version.
3. Specific to the BFD system, a release intended for general availability (GA) will match the latest version of the BFD FHIR API endpoint that is enabled by default;
that is, the latest versioned BFD FHIR API endpoint whose availability is **not** subject to a feature flag will be used as the `X`-value in the typical `X.Y.Z` component of a semver@2.0.0 version string.
4. The release engineer will generate relevant, point-in-time artifacts from this tag, such as the change log and data dictionary, among others.
5. This tag will serve as the basis of a [GitHub Release](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases) to which the point-in-time artifacts are attached.
6. The tag will also be used to identify the final _deployable_ artifacts, such as machine images, as part of the existing and future build, delivery, and deployment processes.
7. For generally available releases, the versioned GitHub release will be shared with our partners for their reference.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

N/A.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

There are few drawbacks to providing a pragmatic solution to versioning for this product, mainly limited to an additional step to the existing process, even if temporarily.
- Adoption does not require extensive engineering work, though it would be wise to only adopt this when the resolution and storage of versioned releases is available
- If an alternative versioning scheme is deemed superior, it should be trivial to transition away from this versioning scheme, provided that the alternative satisfies similar signaling requirements
- In the unlikely event that release versions are deemed harmful, backing away from these changes will not be trivial after some amount of automation is constructed around it, but those changes still shouldn't be extensive.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

This RFC **is** among the _notable alternatives_ from a previously rejected RFC. Please see RFC [0018-bfd-release-versioning](./0018-bfd-release-versioning.md) and its [Notable Alternatives](./0018-bfd-release-versioning.md#a-more-semverorg200-adherent-solutions) for more information.

## Prior Art
[Prior Art]: #prior-art

### From the Wider Ecosystem
After briefly reviewing the ecosystem for release strategies, it appears that the systems that rely on BFD generally define their own releases using a combination of one or more of the following:
- simple incremental versions (e.g. build numbers)
- deployment dates
- agile sprint/program increment boundaries

## Future Possibilities
[Future Possibilities]: #future-possibilities

### Automation 
- initially, the release will be manual where the specific steps are executed by the release engineer
- in time, creating releases will require fewer manual inputs and adjustments; release engineering responsibilities will be limited to supervision
- ultimately, this process should lend itself to a reality where BFD enjoys continuous integration, continuous delivery, and continuous deployment by default

### Intermediate Artifact Versioning and Artifact Repositories
With a well-defined strategy for versioned releases, applying the version string to intermediate artifacts, such as the Java Archive (JAR) should become more obvious.
This will also include additional usage of artifact repositories, such as AWS Code Artifact or one of the CMS-managed Artifactory offerings.

### Previously Discussed Possibilities
- When there exists an acceptable versioning strategy, additional thought is needed to better define a release and change management process
- Once the versioning strategy is adopted, automation opportunities abound:
  - Adopt [actions/labeler](https://github.com/actions/labeler) to automatically label PRs based on the content of their changes
  - Adopt [GitHub release.yml](https://docs.github.com/en/repositories/releasing-projects-on-github/automatically-generated-release-notes) to assist with release note generation based on PR labeling inclusion/exclusion rules
  - Adopt [google-github-actions/release-please-action](https://github.com/google-github-actions/release-please-action) to automate and batch changes in the release management process
- Using JIRA releases could go further in helping plan releases in the future **and** illustrate relationships between the work and the releases, e.g.
  - which versions are impacted by a known bug?
  - which release should customers anticipate for a given feature?
  - when has a release been _cut_ and fully deployed?

## Addenda
[Addendums]: #addendums

The following addenda are required reading before voting on this proposal:

* [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html)
* [Sentimental Versioning Version One dot Oh, okay then.](http://sentimentalversioning.org/)
* [Rejected BFD RFC 0018-bfd-release-versioning.md](0018-bfd-release-versioning.md)
