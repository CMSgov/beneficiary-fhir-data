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

Within this proposal we will consider a database migrations to be a group of one or more of the following types of
changes that are deployed together:

1. A database schema change consisting of one or more SQL migration scripts executed by Flyway
2. An application change in the ORM layer (typically in relation to a change in the schema)

When reasoning about different types of database migrations, it is necessary to consider that both the schema and the
application may change. We will refer to the schema and application as they existed prior to the deployment as the old
schema and the old application, and the schema and application as they exist after applying the migration as the new
schema and the new applications. It is assumed that the old application is compatible with the old schema and that the
new application is compatible with the new schema (otherwise the migration is invalid altogether). The interactions
between old and new components though is important in understanding this proposal and leads to some classifications
of migrations based on the compatibility of the schema change with old and new versions of the application:

* A backward-compatible database migration is one where the new schema is compatible with the old applications
(otherwise it is considered to be backward-incompatible)
* A forward-compatible database migration is one where the old schema is compatible with the new applications
(otherwise it is considered to be forward-incompatible)
* A fully-compatible database migration is one that is both backward-compatible and forward-compatible
* A fully-incompatible database migration is one that is both backward-incompatible and forward-incompatible

Frequently, database migrations include both a schema change and an accompanying application change although there are
some database migrations that consist only of a schema change. Strictly speaking, a database migration that does not
contain a schema change is just an application change like any other application change. It is useful though for the
purposes of this RFC for application changes deployed independently of a schema change to also be considered database
migrations. Of interest in the next section is the fact that as defined above, a database migration that consists only
of an application change is fully-compatible because it must work with both old and new schema (which are the same) to
be valid.

An example of a database migration that consists of both a schema change and an application change is adding a new
column to a table and modifying the application to start referencing that column. This database migration can be
decomposed into two database migrations: one consisting of the change to add a column to a table, and another to modify
the application to reference the new column. The next section shows that decomposing migrations can affect the
compatibility.

### Examples of common migrations in BFD and their compatibility status

#### Adding a new column to an existing table and changing the application to use that column
* This is backward-compatible because the old application will be unaffected by new columns that it does not reference.
* This is forward-incompatible because the new application requires a column that is not present in the old schema.

#### Renaming a column and updating the application references for that column
* This is a fully-incompatible migration because old applications still reference the column by its old name and new
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
backward-compatible this can lead to errors. This adds one more constraint:

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

### Background on execution time characteristics of migrations

Execution time of a database migration plays a part in deciding how it will be deployed. Three categorizations exist:
* Short-running (often seconds, but arbitrarily anything less than one hour)
* Long-running asynchronous (run time is hours or days but can be run in the background by the DB server)
* Long-running synchronous (run time is hours or days and cannot be run in the background)

Short-running migrations are preferred when there are options available although frequently the task determines whether
a short-running migration can be used. Examples of short-running migrations include creating new empty tables, adding,
dropping, or renaming columns, granting permissions, and performing index/constraint builds on table that are empty or
have a very small number of rows.

For long-running migrations, asynchronous is preferred but not available for many operations. An example is index
building in Postgres by specifying the CONCURRENT keyword. Care must be given to the fact that the application will be
online while the operation is running (so dropping an index that the application needs while building a new one
asynchronously is not acceptable).

Long-running synchronous migrations are the least desirable and require the most care. Examples include creating
a new table as a copy of an existing large table, altering the datatype of a column in a large table, creating an
index in a large table without specifying the CONCURRENT keyword. Due to the Flyway migration
being performed during the startup of the BFD Pipeline application, a long-running synchronous migration that is
deployed with a Jenkins deployment will block the Pipeline application from consuming data for the duration of the
migration. The impact of this limitation will become more significant as the pipeline RDA job goes into production.

This RFC proposal addresses the concern of long-running synchronous migrations blocking the BFD Pipeline as well as
improvements to the Jenkins deployment pipeline to better support these long-running migrations when they are
necessary.

#### Other shortcomings that can be addressed by this proposal

In addition to optimizing the development and deployment of database migrations, the proposed changes can also solve
other issues related to database migrations:

* Database object privileges and high level roles (not individual user roles) are not set consistently or in a way that 
supports database credential rotations. Ideally these privileges and roles would be developed and then deployed across
environments in a Flyway migration so that their creation is automated and embedded in the source code. This cannot
currently be done because the Flyway invocation is part of the BFD Pipeline Application startup which must run as a
specific role that is appropriate for the pipeline but does not have the privileges to create roles or set privileges on
existing objects.

## Proposed Solution
[Proposed Solution]: #proposed-solution

The proposed solution is to remove the invocations of Flyway and Hibernate validation from the BFD applications
and instead run Flyway and Hibernate validation as a step in the deployment that must complete successfully before
deploying the new applications.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

A new Java application 'bfd-db-migrator' will be introduced that performs two functions:
* Invokes Flyway (and thereby runs all pending migrations)
* Runs Hibernate validation against the BFD ORM (after Flyway finishes)
* Exits with a return code that indicates whether all operations were successful or not

This new application will be deployed prior to deploying the other BFD applications in the Jenkins deployment. Once the
application has exited, if all operations were successful, the deployment of the other applications proceeds as it does
currently. In the event that this application indicates that not all operations were successful, the Jenkins deployment
will halt and be marked as a failure.

The new application will run under a new database role. This role will have all privileges needed to execute migrations
and perform Hibernate validations.

The new application will produce log files that contain the Flyway and Hibernate logging that is currently captured in
the BFD Pipeline application logs. These new application logs will be sent to Splunk and Cloudwatch.

Flyway invocations and hibernate validation will be removed from the BFD Pipeline and BFD API Server applications.

Test infrastructure will be altered to mimic the deployment by running the new application prior to starting the local
BFD server for tests that run the BFD server as a standalone process.
Test infrastructure will be altered to invoke the new application logic via method call prior to running tests that
require a BFD database but do not launch a BFD server.

A new Flyway migration will be developed that creates all non-user roles and sets privileges correctly on all existing
database objects.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Where should this new app run? Should we provision an instance for it or run it on the existing
ETL node? Need to consider logging, monitoring and alerting for this application particularly if we support
long-running synchronous migrations.

How should we manage long-running synchronous migrations in Jenkins? If we were to allow the build to run for a long
time (more than an hour) what sort of changes do we need? Renewal of AWS tokens? Should we make it possible to deploy
other changes (not ones with migrations) during a long-running migration? Should we support a way of specifying that a
certain migration should be run as a post-deploy migration (possibly in the background)?

Do we need to continue to invoke Hibernate validation at all? What benefit do we derive from this check that is not
already derived from running the unit and integration tests? What sorts of problems would not be caught by the tests
that would be caught by hibernate validation?

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

This makes the Jenkins deployment more complicated and adds another failure mode.

This makes the application more complicated and introduces a new point of failure.

This makes debugging more complex. There will be new places (log files, possibly directories) where debugging
information will be kept.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

Instead of running Flyway and Hibernate validation in a new application, Flyway could be invoked from the command line
instead. These leaves the question as to where to invoke Hibernate validation. If it continues to be invoked in the
applications, it is possible to encounter validation errors if auto-scaling of an old application occurs just after a
new schema has been deployed. One possible answer is to not invoke Hibernate validation at all (see open questions).

## Prior Art
[Prior Art]: #prior-art

Gitlab migration style guide:
https://docs.gitlab.com/ee/development/migration_style_guide.html

## Future Possibilities
[Future Possibilities]: #future-possibilities

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)
