# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0018-bfd-release-versioning`
* Start Date: 2023-03-13
* RFC PR: [beneficiary-fhir-data/rfcs#0018](https://github.com/CMSgov/beneficiary-fhir-data/pull/1642)
* JIRA Ticket(s):
    * [BFD-1743](https://jira.cms.gov/browse/BFD-1743)

BFD needs a top-level, automation-friendly, human-readable, and overall meaningful version string to apply to releases and related artifacts.
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

* Status: Rejected <!-- (Proposed/Approved/Rejected/Implemented) -->
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
However, it's possible to generate a meaningful top-level version string that can serve as a heuristic to facilitate managing, documenting, and communicating changes among BFD contributors and customers alike.

### Non-Goals
Because the proposal already states the goals for introducing a meaningful release version string, it's helpful to state some _non-goals_ or elements that are **NOT** motivating this proposal:
- Internal Software Compatibility and Dependency Resolution: adopting a release version is for the express purpose of communicating change in its various forms, such as:
  - automated signaling for deployment operations
  - customer/user communications of feature availability
  - internal team communication for operations, planning, troubleshooting
  - product planning
- General Software Versioning: this is for the byproducts of the software engineering process in a mono-repository to make sense of the changes introduced as part of releases and release artifacts, i.e. generated, _deployable_ artifacts and reference material
- _Back-portability_ and Multi-Version Support: there are no such scenarios for multiple versions of BFD running simultaneously. This functionality is handled through separately versioned API endpoints.
- Define a Release Management Process: this is just the first step in pursuing some wider release management process for BFD. Some release management scenarios have been used here to describe what's possible once we have a consistent versioning strategy in place, but this RFC does not seek to define any process beyond versions and component version resolution.

## Proposed Solution
[Proposed Solution]: #proposed-solution

BFD uses a version string that shares the _format_ described by [semver.org@2.0.0](https://semver.org/spec/v2.0.0.html#summary) with practical deviations from the specification.
Each component found in the familiar `MAJOR.MINOR.PATCH` format will be further described in the [detailed design below](#proposed-solution-detailed-design), but the modified algorithm for calculating the components can be summarized as follows:

```
Given a version number MAJOR.MINOR.PATCH, increment the:

MAJOR version when you add a database schema migration
MINOR version when you add non-database schema migration changes to the application source code
PATCH version when you add non-application source code changes
Additional labels for pre-release and build metadata are available as extensions to the MAJOR.MINOR.PATCH format.
```
> NOTE: "application source code" above refers to the source code directly responsible for the resultant Java Archive (JAR) artifacts

When applied, the version string clarifies release artifacts. As of March 2023, these artifacts should include:
- [GitHub Release](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)...
  - [Git Source Code Tag Reference](https://git-scm.com/book/en/v2/Git-Internals-Git-References#_tags)
  - Release Notes
  - Change Log Documentation
  - Data Dictionary Artifacts
  - OpenAPI Artifacts
- [Amazon Machine Images (AMI)](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/AMIs.html) as [technical resource tags](https://docs.aws.amazon.com/general/latest/gr/aws_tagging.html#tag-categories)... 
  - `bfd-db-migrator`
  - `bfd-pipeline`
  - `bfd-server`
  - `bfd-server-load`
- [Container Images](https://aws.amazon.com/containers/) as [container image tags](https://docs.docker.com/engine/reference/commandline/image_tag/)...
  - `bfd-server-load-node`
  - `bfd-server-regression`
  - `bfd-synthea-generation`

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Deviations from a standard must be carefully considered before they are accepted by anyone.
What follows are some justifications for the modified semver.org@2.0.0 component version algorithms that are consistent with the BFD system.

#### Incrementing `MAJOR` Component Version

```
Given a version number MAJOR.MINOR.PATCH, increment the:

MAJOR version when you add a database schema migration
```

BFD is a data-driven application.
Changes that involve updates to the data's structure represent the most significant (_major_) and riskiest kinds of change.
These changes have far-reaching impacts to the customer experience.
Ultimately, what is served to the customer is dictated by what and how data is stored in BFD.

As a result, BFD already treats the schema and the scripts that define mutations of the database from one schema to the next as an _intermediate artifact_.
Schema migration scripts are versioned with the assistance of [flyway](https://flywaydb.org/), helping engineers reason about the incremental changes to the database state over time.
Engineers carefully prefix the scripts with simple, strictly _incrementing_ version numbers that define the order in which they must be applied.
At deployment time, the necessary scripts are applied to the relevant data stores in order for `flyway` and the respective database to match schema versions.
This is performed by the `bfd-db-migrator`, which also serves to validate the application's expectations and understanding of the schema (i.e. the application's _model_) against the schema itself.

The existing, latest available version of well-vetted schema migration scripts is an attractive `MAJOR` version component.
However, the traditional semver.org@2.0.0 strategy dictates that the`MAJOR` version component represents backward incompatible or breaking changes; changes to the database's schema, critical as they may be to the application's behavior, **CAN NOT** introduce backward incompatibility as a `MAJOR` change from either the contributor's or customer's perspective.

Backward incompatible changes from the customer's perspective are handled elsewhere.
BFD's primary, customer-facing component is the BFD FHIR API service, `bfd-server`. 
And BFD customers interact with BFD through versioned API endpoints.
As of March 2023, `bfd-server` supported two versioned endpoints: `v1` and `v2`.
The `v2` endpoint was introduced to minimize disruptions to customers where breaking, backward incompatible changes were implemented.
In other words, changes that are both backward-incompatible and customer-facing are only accepted into BFD through the addition of new, versioned API endpoints.

To summarize, under the traditional, semver.org@2.0.0 strategy, no natural, top-level `MAJOR` version can be adopted.
Instead, we look to adjusting the standard.
By adjusting the definition of what a `MAJOR` component version can be (i.e. coupling major versions to database schema versions), it seems obvious that we would want to promote the intermediate schema migration script artifact versions to a global, `MAJOR` version component.
In this way, we can provide a `MAJOR` component version that's meaningful, relatively stable, and easy to automatically derive.

<details><summary>The following example script could be used to derive the `MAJOR` component version, manually or in CI contexts</summary> 

```sh
#!/usr/bin/env bash
shopt -s expand_aliases

if [ "$(uname)" = 'Darwin' ]; then
    # compatibility for local development on macOS
    # requirements enforced through the following naive checks
    command -v gfind >/dev/null && alias "find=gfind" || echo "'gfind' not found; try 'brew install findutils'"
    command -v gsort >/dev/null && alias "sort=gsort" || echo "'gsort' not found; try 'brew install coreutils'"
    command -v gawk  >/dev/null && alias "awk=gawk"   || echo "'gawk' not found; try 'brew install gawk'"
    command -v gtail >/dev/null && alias "tail=gtail" || echo "'gtail' not found; try 'brew install gtail'"
    command -v gsed  >/dev/null && alias "sed=gsed"   || echo "'gsed' not found; try 'brew install gnu-sed'"
fi

REPO_ROOT="$(git rev-parse --show-toplevel)"
RELATIVE_MIGRATIONS_PATH="apps/bfd-model/bfd-model-rif/src/main/resources/db/migration"
MIGRATIONS_PATH="${REPO_ROOT}/${RELATIVE_MIGRATIONS_PATH}"

find "$MIGRATIONS_PATH" -type f -iname 'V*sql' -printf '%f\n' | sort --version-sort | tail -1 | awk -F '__' '{ print $1 }' | sed 's/[^0-9]//g'
```

</details>

#### Incrementing `MINOR` and `PATCH` Component Versions

```
Given a version number MAJOR.MINOR.PATCH, increment the:

MINOR version when you add non-database schema migration changes to the application source code
PATCH version when you add non-application source code changes
```
> NOTE: "application source code" above refers to the source code directly responsible for the resultant Java Archive (JAR) artifacts

semver.org@2.0.0's traditional definition for the component versions is based on compatibility.
Because breaking changes from the customer's perspective are only introduced into BFD through versioned API endpoints, the distinction between `MAJOR` and other version components no longer exist.
This is an opportunity to update the `MINOR` and `PATCH` component version definitions to provide more detail.
Especially with the mono repository strategy, formalizing another existing distinction between changes in the code base is helpful: application vs non-application changes.

The `MAJOR` version based on database schema version is already sufficiently distinct, but for the remaining changes, we split the component versions along the following lines:
- excluding database migrations, changes that result in distinct Java archive (JAR) artifacts, result in an incremented `MINOR` version, e.g. features and fixes to bfd-db-migrator, bfd-pipeline, bfd-server
- any non-application source code changes without direct impacts to the resultant JAR increment the `PATCH` version, e.g. independent infrastructure, configuration, documentation changes.

#### Why semver.org@2.0.0 at all?

Given the deviations from the standard in semver.org@2.0.0, it might be reasonable to ask: _"why refer to it at all?"_
It's a fair question, but aside from the definitions of `MAJOR`, `MINOR`, and `PATCH`, much of [the specification](https://semver.org/spec/v2.0.0.html#semantic-versioning-specification-semver) still applies:
- incremental component version significance is still useful such that:
  - only the most significant version component is incremented in a given release, and less significant components are _zeroed_ 
  - given qualifying changes for `MAJOR`, `MINOR`, and `PATCH` in the same release, only the `MAJOR` version is incremented, e.g. `1.0.0` becomes `2.0.0` **not** `2.1.1`
  - given qualifying changes for `MINOR` and `PATCH` in the same release, only the `MINOR` version is incremented, e.g. `2.0.0` becomes `2.1.0` **not** `2.1.1`
- [specification allows for build metadata and pre-release extensions](https://semver.org/spec/v2.0.0.html#spec-item-10) which keeps the format flexible
- [release precedence is codified](https://semver.org/spec/v2.0.0.html#spec-item-11) and familiar

What's more, the widely understood `MAJOR.MINOR.PATCH` format is well-supported by third party tools and remains the _de facto_ standard.

#### Simple Examples
What can we expect from the following _notional_ scenarios?

##### Upgrade from 1.0.0 to 2.0.0
  - a deployment of this release is relatively _higher_ risk
  - the release _likely_ includes new functionality, draws _more_ attention from customers
  - there's a database migration involved
    - `bfd-pipeline@1.0.0` application instance(s) should stop writing or be _undeployed_ ahead of the migration
    - `bfd-db-migrator@2.0.0` must be deployed
  - `bfd-server@2.0.0` _might_ include new functionality
  - `bfd-pipeline@2.0.0` _might_ include new functionality
  - `bfd-data-dictionary@2.0.0` _might_ include mapping changes

##### Upgrade from 2.0.0 to 2.1.0
  - this deployment has relatively _moderate_ risk
  - this _potentially_ includes new functionality, draws _some_ attention from customers
  - `bfd-db-migrator@2.1.0` need not be deployed
  - `bfd-data-dictionary@2.1.0` is _unlikely_ to include mapping changes

##### Upgrade from 2.1.0 to 2.1.1
  - deployment of this has relatively _low_ risk
  - no new functionality, draws _less_ attention from customers
  - deployment _likely_ limited to configuration that needs to be applied/re-applied
  - may be limited to updates to secondary services for monitoring and performance testing

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

N/A <!-- TODO: Reserve this section for feedback from the wider team -->

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

None.
There are no drawbacks to providing a pragmatic solution to versioning for this product.
- Adoption does not require extensive engineering work, though it would be wise to only adopt this when the resolution and storage of versioned releases is available
- If a truly adherent solution to semver.org@2.0.0 emerges, that strategy could simply pick up where the existing semver.org@2.0.0 compliant scheme left off, and begin the incremental releases based off of the traditional criteria
- If an alternative versioning scheme is deemed superior, it should be trivial to transition away from this versioning scheme, provided that the alternative satisfies similar signaling requirements
- In the unlikely event that release versions are deemed harmful, backing away from these changes will not be trivial after some amount of automation is constructed around it, but those changes still shouldn't be extensive.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

#### Per Artifact Software Versioning

A generalized solution for software versioning was a stated _non-goal_ for this proposal.
Nevertheless, if the BFD system were already taking advantage of meaningful versions for the system's components, communicating change could be all the more obvious by looking to the respective version numbers applied there.

Historically, BFD has avoided versioning artifacts like the Java-based JARs all together.
In part, this was due to the complexities and overhead of maintaining consistent inherited version throughout the project.
Fundamentally, BFD is as a maven project, implemented in Java, and structured with nested parent-child relationships.
As of March 2023, this includes more than 30 `pom.xml`-defined modules.
Here's one such nested parent-child relationship between `bfd-parent`, `bfd-model`, and `bfd-model-rif`: 

```
apps
├── pom.xml (the "bfd-parent")
└── bfd-model
    ├── pom.xml ("bfd-model" is the child of "bfd-parent")
    └── bfd-model-rif
        └── pom.xml ("bfd-model-rif" is the child of "bfd-model")
```

For the majority of BFD's history, the version that `bfd-parent` and its descendants have used is `1.0.0-SNAPSHOT`.
While there were attempts to increment and fully utilize non-snapshot versioning over the course of this project's history, the last adjustment to `bfd-parent`'s version field was made in August 2019.

_Proper_, per artifact versioning would be difficult to achieve consistently for any appreciable amount of time with the current tools and strategies; after its adoption, there would no doubt be an occasional version or dependency mismatch within the product, a problem that is all but unknown in today's usage of a static, `1.0.0-SNAPSHOT`.

What's more, it's unclear what benefits per-artifact versioning might bring to BFD:
- It's widely believed, that **most** changes to BFD impact **most** artifacts, i.e. **most** artifacts exhibit the same rate of change and regularity of deployments with common module updates, etc
- BFD makes only modest use of artifact repositories, generally rebuilding **everything** from source
- BFD follows a kind of immutable infrastructure pattern that calls for rebuilding the _deployable_ artifact (AMIs) for each deployment

If engineers supporting BFD later determine that a per-artifact versioning strategy has demonstrable merit, thoroughly investigating and potentially adopting tools like [Bazel](https://bazel.build/) as a maven replacement would be a good first step toward versioning these intermediate deployment artifacts.


#### A More semver.org@2.0.0 Adherent Solutions

It would have been simpler to point to semver.org@2.0.0 and suggest that BFD adopts that.
Instead, we've only been able to identify one set of alternatives that might come closer to the semver.org@2.0.0 specification.
This alternative revolves around adopting the single backward **incompatible** mechanism supported in BFD today: the FHIR API Server endpoint version.

This seems like an obvious, more adherent, and possibly better solution.
However, it has similar issues to that of the proposed solution _and_ other problems that the proposed solution avoids entirely.

The API version is **very** stable, and infrequently changing.
While this is not problematic, it's just not as meaningful as the proposed solution's change on truly significant updates to the product inherent to the database schema version.
In the end, semantic versions should be semantic (meaningful).
If this strategy was already in-use when `v2` first appeared, BFD's latest release would probably be something like `2.200.0`.

Here are a handful of observations when comparing the proposed solution to _this_ alternative:
1. `v2` has been supported for the past two years. How meaningful is it to remind all parties of the largely static `v2` version's availability?
3. While the feature flag in BFD is enabled for `v2` endpoints by default, `v2` support is **still** technically behind a feature flag as of March 2023. When should BFD truly adopt `v2` as its major version?
2. When changes are released that only include bug fixes to the otherwise static `v1` endpoints, the `PATCH` seems almost inappropriate for a `2.x.x` release.
4. If there were changes introduced for initial support to a new `v3` API endpoint via feature flags, at what point should the `MAJOR` version be incremented?

A blended variation of this and the proposed solution could be possible, e.g.
- `MAJOR` reflects the latest FHIR API server endpoint
- `MINOR` reflects the latest database migration script version
- `PATCH` is reserved for everything else

However, this is still less meaningful, it fails to adhere to the semver.org@2.0.0 specification, and fails to address some of the more problematic observations above.

#### Non-Semantic Versions

There are no hard-and-fast rules to release versioning, especially without an opinionated artifact repository involved.
There is little technical limitation to stop engineers from adopting names of [animated characters](https://www.debian.org/releases/) or [animal with alliterative adjectives](http://www.releases.ubuntu.com/), celestial bodies, tropical islands, capital cities, or other identifiers for product releases.
But, if the overall desire is to facilitate and augment communication surrounding releases, this seems more likely to distract than focus our conversations.

Similarly, releases that are based on dates _seem_ like they could be reasonable, but without some kind of semantic component, those release versions are little better than what we have today, which largely revolves around the vague recollection of date-time groups representing the time of deployments or creation time of resources in the path-to-production.


## Prior Art
[Prior Art]: #prior-art

### From the Wider Ecosystem

After briefly reviewing the ecosystem for release strategies, it appears that the systems that rely on BFD generally define their own releases using a combination of one or more of the following:
- simple incremental versions (e.g. build numbers)
- deployment dates
- agile sprint/program increment boundaries

## Future Possibilities
[Future Possibilities]: #future-possibilities

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
[Addenda]: #addenda

The following addenda are required reading before voting on this proposal:

* [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html)
* [Sentimental Versioning Version One dot Oh, okay then.](http://sentimentalversioning.org/)
