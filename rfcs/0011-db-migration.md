# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0011-db-migration`
* Start Date: 2022-02-02
* RFC PR: [beneficiary-fhir-data/rfcs#0011](https://github.com/CMSgov/beneficiary-fhir-data/pull/XXXX)
* JIRA Ticket(s):
  * [BFD-XXXX](https://jira.cms.gov/browse/BFD-XXXX)

Database migrations in BFD are critical system events that will continue to be a common occurrence as the database
schema undergoes enhancements for performance, maintainability, and support for new data fields. This RFC proposes
changes to the database migration process to make it more defined, robust, and efficient.

## Status
[Status]: #status

* Status: Proposed
* Implementation JIRA Ticket(s):
  * [BFD-XXXX](https://jira.cms.gov/browse/BFD-XXXX)

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
* [Addendums](#addendums)

## Motivation
[Motivation]: #motivation

Database migrations are a complex topic. Before delving into the details of this proposal it will be helpful to define
some terminology, identify the relevant system components, and differentiate between different types of migrations.

### Background on BFD Applications

It is assumed that readers of this proposal are generally familiar with two open source tools that BFD depends on for
database operations:

* Flyway -- for executing database schema migration scripts
* Hibernate -- for object-relational mapping between the applications and the database including schema validation

TODO: ADD REFERENCE LINKS FOR FLYWAY AND HIBERNATE

The two applications that make up BFD will be considered in terms of their roles in database migration and database
operations:

BFD Pipeline Application:
- Invokes Flyway on startup (which will execute any new migrations)
- Runs hibernate validation on startup
- Reads and writes data to the database

BFD API Server Application:
- Runs hibernate validation on startup
- Reads data from the database (no writes)

### Background on Database Migrations

Within this proposal we will consider database migrations to be a single deployment that consists of one or both of
these types of changes:
1. A database schema change which consists of one or more SQL migration scripts executed by Flyway
2. An application change in the ORM layer (typically in relation to a change in the schema)

When reasoning about different types of database migrations, it is necessary to consider that both the schema and the
application may change. We will refer to the schema and application as they existed prior to the deployment as the old
schema and the old application, and the schema and application as they exist after applying the migration as the new
schema and the new applications. It is assumed that the old application is compatible with the old schema and that the
new application is compatible with the new schema (otherwise the migration is invalid altogether). The interactions
between old and new components though is important in understanding this proposal and leads to some classifications
of migrations:

* A backward-compatible migration is one where the new schema is compatible with the old applications
(otherwise it is considered to be backward-incompatible)
* A forward-compatible migration is one where the new applications are compatible with the old schema
(otherwise it is considered to be forward-incompatible)
* A fully-compatible migration is one that is both backward-compatible and forward-compatible
* A fully-incompatible migration is one that is both backward-incompatible and forward-incompatible

### Examples of common migrations in BFD and their compatibility status

#### Adding a new column to an existing table and changing the application to use that column
* This is backward-compatible because the old application will be unaffected by new columns that it does not reference.
* This is forward-incompatible because the new application requires a column that is not present in the old schema.

#### Renaming a column and updating the application references for that column
* This is an fully-incompatible migration because old applications still reference the column by its old name and new
  applications reference the column by the new name.

#### Dropping a column and changing the application so that it no longer references that column
* This is backward-incompatible because old applications reference the column which will not be present in the new
schema
* This is forward-compatible because new applications work properly with or without the column

#### Adding a new column or table and NOT changing the application to use it
* This is fully-compatible.

#### Updating ONLY the application to start using a column or table that already exists
* This is fully-compatible.

#### Dropping a column from the schema ONLY (that the application does not reference)
* This is fully-compatible.

Of note here is that a migration that is fully-incompatible (like renaming a column in the schema and the application)
can be accomplished in a manner that is fully-compatible by breaking it into multiple migrations (which would have to
be deployed separately).

### Background on types of BFD deployments

There are three types of deployments that come up when considering how to deploy database migrations:

#### Jenkins deployment
* Fully automated deployment via Jenkins
* Typical deployment option for almost all changes (migration or otherwise)
* New applications are deployed with a period of overlap with the old applications
* Requires no downtime
* In-place deployment that requires no additional hardware
* Has constraints on the type of migrations that can be deployed (discussed in detail below)

#### Cloned deployment
* A cloned environment handles traffic while the primary instance undergoes the deployment and then traffic is
redirected back to the primary
* Manual deployment
* Requires no downtime
* Has fewer constraints on the types of migrations that can be deployed than standard Jenkins deploy

#### Downtime deployment
* Service is interrupted for some period of time
* Has additional coordination and communication requirements and acceptance of downtime window and risk mitigation plan
* Manual or Automated deploy are possible during the downtime window
* Can be used when downtime is required or desired due to other factors

In general, a Jenkins deployment is preferred because it is a fully automated process that results in no
downtime, has low risk of human error, no additional hardware costs, and requires no coordination or communication
apart from what is otherwise required for any particular change. The other types of deployments can be used for
special situations where it is determined to be preferable for reasons of cost, risk mitigation, or otherwise. This RFC
will focus on optimizing the Standard Jenkins deployment since it is the preferred deployment and is most commonly used.
Optimizing the Jenkins deployment can also make it a more viable option for certain complex migrations that otherwise
may be candidates for a cloned deployment or a downtime deployment.

### Constraints on migrations that are deployed via Jenkins deployment

During a Jenkins deployment the old API server continues to run and serve traffic during the deployment of the new
applications until the new API server is fully deployed and available. This means that it is possible that the old API
server will attempt to read from the database after the new schema is in place. This leads to a constraint on BFD
migrations:

* Database migrations must be backward-compatible

During a Jenkins deployment the new versions of the two applications are deployed simultaneously. This means that the
order of those deployments is indeterminate and the new API server may come online before the new schema changes are in
place. This leads to another constraint:

* Database migrations must be forward-compatible -OR-
* The schema change must be fully deployed before the application change is deployed

Lastly, due to BFD auto-scaling of the API servers, it is possible that additional API servers running the old software
may come online and perform hibernate validation against the new database schema. Even if the migration is
backward-compatible this can lead to errors (TODO: THINK OF AN EXAMPLE). This adds one more constraint:

* Hibernate validation must be turned off in a deployment prior to running a database migration (and so turned back
on in a deployment following the migration deployments).

Combining these constraints yields the current standard practice for deploying a BFD database migration:

1. Deploy a PR that disables hibernate validation.
2. Deploy a PR that consists of just the schema portion of the migration which must be backward-compatible.
3. Deploy a PR that consists of just the application portion of the migration.
4. Deploy a PR that enables hibernate validation.

With this standard practice there is no requirement for forward-compatibility but backward-compatibility is a
requirement. Except in rare cases, backward-compatible migrations are strongly preferred and can
be used for all changes that commonly occur.

When followed correctly, the four-step process above provides a safe way of performing backward-compatible migrations.
However, the need for four PRs for a single database migration increases the effort to develop and deploy the change
significantly. Having more PRs also complicates the review process and results in a single logical change being
fragmented into multiple commits.

This RFC proposes an architectural change to BFD that will allow any backward-compatible migration to be deployed
as a single PR using a Jenkins deployment.

#### Other shortcomings that can be addressed by this proposal

In addition to optimizing the development and deployment of database migrations, the proposed changes can also solve
other issues related to database migrations:

* Database object privileges and high level roles (not individual user roles) are not set consistently or in a way that 
supports database credential rotations. Ideally these privileges and roles would be developed and then deployed across
environments in a Flyway migration so that their creation is automated and embedded in the source code. This cannot
currently be done because the Flyway invocation is part of the BFD Pipeline Application startup which must run as a
specific role that is appropriate for the pipeline but does not have the privileges to create roles or set privileges on
existing objects.

* TODO : LONG RUNNING MIGRATIONS A/C

## Proposed Solution
[Proposed Solution]: #proposed-solution



### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

This is the technical portion of the RFC. Explain the design in sufficient detail that:

* Its interaction with other features is clear.
* It is reasonably clear how the feature would be implemented.
* Corner cases are dissected by example.

The section should return to the examples given in the previous section, and explain more fully how the detailed proposal makes those examples work.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Collect a list of action items to be resolved or officially deferred before this RFC is submitted for final comment, including:

* What parts of the design do you expect to resolve through the RFC process before this gets merged?
* What parts of the design do you expect to resolve through the implementation of this feature before stabilization?
* What related issues do you consider out of scope for this RFC that could be addressed in the future independently of the solution that comes out of this RFC?

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

Why should we *not* do this?

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

* Why is this design the best in the space of possible designs?
* What other designs have been considered and what is the rationale for not choosing them?
* What is the impact of not doing this?

## Prior Art
[Prior Art]: #prior-art

Discuss prior art, both the good and the bad, in relation to this proposal.
A few examples of what this can include are:

* For feature proposals:
  Does this feature exist in other similar-ish APIs and what experience have their community had?
* For architecture proposals:
  Is this architecture used by other CMS or fedgov systems and what experience have they had?
* For process proposals:
  Is this process used by other CMS or fedgov programs and what experience have they had?
* For other teams:
  What lessons can we learn from what other communities have done here?
* Papers and other references:
  Are there any published papers or great posts that discuss this?
  If you have some relevant papers to refer to, this can serve as a more detailed theoretical background.

This section is intended to encourage you as an author to think about the lessons from other languages, provide readers of your RFC with a fuller picture.
If there is no prior art, that is fine - your ideas are interesting to us whether they are brand new or if it is an adaptation from other languages.

Note that while precedent set by other programs is some motivation, it does not on its own motivate an RFC.
Please also take into consideration that we (and the government in general) sometimes intentionally diverge from common "best practices".

## Future Possibilities
[Future Possibilities]: #future-possibilities

Think about what the natural extension and evolution of your proposal would be and how it would affect the language and project as a whole in a holistic way.
Try to use this section as a tool to more fully consider all possible interactions with the project and language in your proposal.
Also consider how the this all fits into the roadmap for the project and of the relevant sub-team.

This is also a good place to "dump ideas", if they are out of scope for the RFC you are writing but otherwise related.

If you have tried and cannot think of any future possibilities, you may simply state that you cannot think of anything.

Note that having something written down in the future-possibilities section is not a reason to accept the current or a future RFC;
  such notes should be in the section on motivation or rationale in this or subsequent RFCs.
The section merely provides additional information.

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.