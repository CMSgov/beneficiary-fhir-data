# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-monorepo` (fill me in with a unique ident)
* Start Date: 2019-07-20
* RFC PR: [rust-lang/rfcs#0000](https://github.com/rust-lang/rfcs/pull/0000)
* JIRA Ticket(s):
    * [BLUEBUTTON-1086: Switch BFS to monorepo](https://jira.cms.gov/browse/BLUEBUTTON-1086)

The Beneficiary FHIR Server systems should move from being split across 11 Git repositories to a single, combined repository: a "monorepo".
This has a number of benefits: simpler onboarding, better testing of changes, and more efficient day-to-day operations.

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
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
* [Implementation](#implementation)

## Motivation
[Motivation]: #motivation

The most immediate motivation for this change is that we're all tired of dealing with the current setup.
Having 11 repositories makes everything harder: we keep having to split up what are conceptually single changes into multiple separate pull requests.
Aside from the hassle of the extra moving pieces, it makes it effectively impossible to test such multi-PR changesets in our AWS TEST environment.
Given our team's current focus on major architectural changes, the sooner we make the switch to a monorepo, the better.

Initially, everything was split out to support goals that are no longer relevant:

* It was expected that we'd be using a more traditional named-version release strategy,
    where being able to tag things individually is important.
  However, we have long since moved to a Continuous Deployment strategy,
    where the separate repositories are more of a liability than a benefit.
* The approach was also intended to allow for more code sharing between projects (i.e. between BFS and BB2),
    which is awkward with monolithic repositories.

## Proposed Solution
[Proposed Solution]: #proposed-solution

It's pretty simple: move our source code (and its history) into a single, combined repository: `https://github.com/CMSgov/beneficiary-fhir-server/`.
That repository will be laid out as follows:

```
beneficiary-fhir-server.git/
  dev/
  original-project-1/
  original-project-2/
  ...
  original-project-11/
  README.md
  (and other files...)
```

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

The following repositories will be migrated to the new `beneficiary-fhir-server` repository:

* <https://github.com/CMSgov/bluebutton-parent-pom>
* <https://github.com/CMSgov/bluebutton-data-model>
* <https://github.com/CMSgov/bluebutton-data-pipeline>
* <https://github.com/CMSgov/bluebutton-data-server>
* <https://github.com/CMSgov/bluebutton-data-server-perf-tests>
* <https://github.com/CMSgov/bluebutton-ansible-playbooks-data>
* <https://github.com/CMSgov/ansible-role-bluebutton-data-pipeline>
* <https://github.com/CMSgov/ansible-role-bluebutton-data-server>
* <https://github.com/CMSgov/bluebutton-ansible-playbooks-data-sandbox>
    * Hopefully not needed much longer, so may be removed eventually.
* <https://github.com/CMSgov/bluebutton-functional-tests>
    * Not yet used, but we should fix that (eventually).
* <https://github.com/CMSgov/bluebutton-data-ccw-db-extract>
    * Out of date, but we should fix that (eventually).
* <https://github.com/CMSgov/bluebutton-text-to-fhir>
    * Unmaintained, and will be removed after migration.
* <https://github.com/CMSgov/bluebutton-csv-codesets>
    * Unmaintained, and will be removed after migration.

See this proposed migration script:
[../dev/monorepo-build.sh](../dev/monorepo-build.sh).

Small side note: by default, `git log <somefile>` does not show history across filenames,
  but `git blame` does. Adding the `--follow` flag to `git log`,
  e.g. `git log --follow <somefile>` solves this.

In addition to the migration and refactoring contained in the script,
  a number of additional reorganization/refactoring items will be attempted manually
  as part of the overall migration process.
A working list of ideas for these items is contained in comments at the bottom of the script.
In a number of cases, our code is split up across more modules than necessary,
  which just makes things harder to do than necessary.
These tasks need to be performed manually as there aren't great command line options for many of the steps,
  e.g. changing a Maven project's `groupId`, `artifactId`, etc.
To some extent, these changes will be limited to those that can be completed within a reasonable time
  as the project will be in a pseudo code freeze for the duration of the process.

One drawback to this approach is that it can only migrate the `master` branch of each original repository;
  non-`master` branches, such as those for unmerged pull requests, will not be moved automatically.
Any pull requests that can be merged prior to the move, should be.
Any other branches that are no longer needed should be deleted prior to the move.d

A one-time manual recreation of all remaining branches will have to be performed, via the following steps:

1. Visit each PR in a web browser.
2. Adjust the browser URL by appending "`.diff`" to it.
   This will produce a Git patch file, containing all of the PR's changes (collapsed to a single changeset with no comments).
3. Download/save the patch file locally.
4. Apply the patch by running: `git apply <patch_file> --directory=<target_subdir_of_monorepo> && git commit`.

Once the migration has been completed, the original repositories should be archived,
  via the Settings page for each repository in GitHub.

During the migration, developers should be encouraged to spend time updating Confluence, JIRA, etc.
A full "code freeze" would be excessive, but at the same time,
  patches made against the old projects will likely not apply cleanly against the new ones.

Afterwards, a brief post about the transition should also be published to the Blue Button site and Google Group.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

The following questions need to be resolved before this RFC is submitted for final comment:

* (none at this time)

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

Given our continuous deployment approach, this doesn't really apply,
  but it's worth noting that monorepos don't lend themselves to tagging releases of individual subprojects.

In addition, Jenkins' multibranch pipeline job type is the default choice these days (for good reason: it's excellent)
  but doesn't really support the concept of multiple joba from/for a single repo.
To some extent, that's fine: we _want_ to build everything anytime anything changes,
  as this will allow us to test and deploy all pieces of our code when we branch.
It will be a bit awkward for tasks that make sense to run in Jenkins
  but aren't really part of our default build workflow,
  e.g. full performance tests or Jenkins-as-cron kind of items.
At least one option for managing that is having a Jenkins job parameter
  such as "`jobType`" that the Jenkinsfile uses to select which _actual_ job code to go run.
Other options exist as well, so this drawback feels manageable.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

N/A

## Prior Art
[Prior Art]: #prior-art

It's worth noting that lots of organizations far larger than us (Facebook, Google, etc.) are all using monorepos.

## Future Possibilities
[Future Possibilities]: #future-possibilities

N/A

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* [../dev/monorepo-build.sh](../dev/monorepo-build.sh)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.

## Implementation
[Implementation]: #implementation

* Implementation Status: Done
* JIRA Ticket(s):
    * [BLUEBUTTON-1086](https://jira.cms.gov/browse/BLUEBUTTON-1086)