# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0009-pipeline-orchestration-baby-steps`
* Start Date: 2021-02-22
* RFC PR: [CMSgov/beneficiary-fhir-data#462](https://github.com/CMSgov/beneficiary-fhir-data/pull/462)
* JIRA Ticket(s):
    * [BFD-652: Story: Improve BFD Pipeline Job Orchestration](https://jira.cms.gov/browse/BFD-652)


<!--
Write a brief summary here: a one paragraph explanation of the feature.
Try to structure it like an "elevator pitch":
  it should provide readers with a high-level understanding of the goals and proposed solution.

Please note: many of the other sections below will not be needed for some proposals;
  don't waste time writing responses that don't deliver real value.
For any such not-needed section, simply write in "N/A".
-->

The BFD Pipeline already has several ETL-ish tasks/jobs that it runs.
Over the next year or two, it is likely to acquire several more,
  including, for example the DC Geo data load.
This proposal details architectural changes that
  can and should be made in order to accomodate these requirements
  (since the BFD Pipeline architecture was originally designed with only one task/job in mind).

## Status
[Status]: #status

* Implementation Status: Done
* JIRA Ticket(s):
    * [BFD-704](https://jira.cms.gov/browse/BFD-704)
    
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

<!--
Why are we doing this?
What use cases does it support?
What is the expected outcome?
Why is now the appropriate time to address this?
-->

We're at a first inflection point in the complexity of BFD's ETL jobs/tasks,
  with the number of such jobs going past just a few.
Accordingly, it's time to re-evaluate some of the architectural decisions in the BFD Pipeline,
  to better accomodate the increasing number of jobs
  and ensure that the application remains maintainable.

By making modest architectural changes now,
  and planning for further medium-term changes,
  we can help ensure the continued success of our ETL efforts.


## Proposed Solution
[Proposed Solution]: #proposed-solution

<!--
Explain the proposal as if it was already implemented and shipped,
  and you were just explaining it to another developer or user.
That generally means:

* Introducing new named concepts.
* Identifying and address each of the various audiences who might (or should) care about this proposal.
  Explaining the solution using concepts and terms relevant to eaach of them.
  Explaining how they should _think_ about the solution; detailing the impact as concretely as possible.
  Possible audiences might include:
    * Internal team: engineers, operators, product management, business owners.
    * External users: engineers, operators, product management, business owners, end users.
* Explaining the feature largely in terms of examples.
    * Screencasts are often a good idea.
        * On Mac OS X, you can use the built-in Quicktime Player or the built-in Mac OS X Mojave (and up) feature.
    * Diagrams are often a good idea.
        * Keep it simple! Use something like <http://asciiflow.com/>.
* As part of implementing this proposal, will any documentation updates be needed, e.g. changelogs, Confluence pages, etc.?
  If so, draft them now! Include the draft as a subsection or addendum.
-->

As always, the trick here is to "right-size" our architectural choices;
  we don't want to under-engineer things and end up with a bunch of things getting hacked in, in ugly ways;
  we don't want to over-engineer things and thus end up with unnecessary long-term development and maintenance expenses.
Accordingly, this proposal lays out some short-term steps that will unblock new ETL jobs immediately,
  and also suggests further phases/options that can be pursued in the future, if/as needed.

Folks tend to overthink ETL a bit.
At its core, ETL is really just one or more tasks/jobs that run,
  which happen to move data around.
Those jobs often (though not always) have dependencies,
  such that Job A has to complete before Job B should start.
Those jobs need to be scheduled, such that they run when a trigger condition occurs,
  though those conditions are often just along the lines of "try to run every five minutes".
If you stop and think about it, this is all very similar to any other workflow orchestration problem:
  figuring out what to run, when.

Of course, much like the broader workflow orchestration field,
  there are a lot of folks looking to push/sell very complicated solutions.
Those are overkill for ETL just as often as they are for anything else.
BFD's problems do not currently need a complex business rules engine,
  nor do they currently need a complex ETL orchestration engine...
  but I repeat myself.

To be sure, that could happen in the future!
But that would be a future where BFD has **dozens** of ETL jobs,
  **and** one where those jobs have complex interdependencies.
Not so this year, or next.

What does BFD need then?
First, let's talk about what ETL BFD currently does and how it might be quickly improved.
The BFD Pipeline already has several different ETL jobs that it runs:

1. The original `DatabaseSchemaManager` job, that updates the database schema if/as needed.
2. The original `RifLoader.process()` job, that loads the CCW data from RIF files in S3.
3. A newer `RifLoaderIdleTasks.fixupBeneficiaryExecutor()` job, that updates MBI hashes in the `Beneficiaries` table.
4. A newer `RifLoaderIdleTasks.fixupHistoryExecutor()` job, that updates MBI hashes in the `BeneficiariesHistory` table.

But aside from #2 in the list above, the other jobs are all a bit "shoehorned" in.
Which is fine! ... for a few things,
  but we will be adding more jobs this year and it will quickly become _less_ fine.


### Immediate Changes

In the short term, this RFC proposes "un-shoehorning" the other three jobs, above (#1, #3, and #4).
From some analysis, it appears that the BFD Pipeline already has
  a solid foundation for a more robust job orchestration system;
  with just some moderate refactoring it can easily support a number of additional jobs.

This refactoring should result in a restructuring of the BFD Pipeline's modules,
  ending with a set of modules like the following:

* `bfd-pipeline-shared-utils`: The set of utility and framework code shared across the application, including:
    * The interface definition(s) for ETL jobs.
    * The DB schema upgrade ETL job.
* `bfd-pipeline-ccw`: The ETL jobs related to the CCW, including #2, #3, and #4, above.
* `bfd-pipeline-app`: The application launcher and job management/orchestration code.

In the future, we should also expect modules like the following to exist:

* `bfd-pipeline-synthetic-data`: The ETL job(s) related to synthethic data, including any data generated by Synthea.
* `bfd-pipeline-dc-geo`: The ETL job(s) related to the DC Geo effort.

With these modest short-term changes, the BFD Pipeline will be able to:

* Run multiple jobs, not in parallel.
* Simply implement new jobs, with only minor dependencies on the larger application.
* Isolate job code and execution, within the BFD Pipeline application and JVM.
* Scale compute vertically, if performance needs to be dialed up.


### Additional Options/Phases

As/if needed, the following additional options/phases can be implemented,
  to meet additional business/product goals,
  as they become needed.


#### Option: Do We Need To Run Jobs In Parallel?

If one job's runtime is long enough to violate SLOs for other jobs,
  then it may become necessary to run jobs in parallel,
  such that Job B which does not depend on Job A will run at the same time as Job A.

The simplest solution to this business need is to update the job management system such that:

* Job dependencies can be expressed and are honored (e.g. "Job C should always run after Job B").
* Jobs are run in an executor with more than one thread.

That's it: nice & simple.

Please note that running jobs in parallel does not necessarily mean running jobs on multiple hosts.
Multiple jobs can easily run in parallel on a single host.
  particularly since most job threads will spend their time waiting around for I/O to complete.
See the "Option: Do We Need To Run Jobs On More Than One Host?" section for details on _that_ business need.


#### Option: Do We Need Better Obervability?

Perhaps engineers/operators often find themselves wondering things like:

* When did Job A last execute and how long did it take to complete?
* Did Job B fail last night?
* and so on...

If so, there are a couple of potential solutions, including:

* Comprehensive logging,
    with good documentation that tells operators
    how to answer common questions from the logs.
* Storing job status and results in a database,
    with good documentation that tells operators
    how to answer common questions from the database.

If something fancier is needed, choose the database option
  and consider adding a simple web interface on top of it.
But try to avoid that, as BFD would now have yet another piece of infrastructure to maintain and operate.


#### Option: Do We Need To Run Jobs On More Than One Host?

Perhaps, for performance reasons, vertical scaling of a single job-running-system is no longer sufficient.
In that case, the next obvious step to improving performance would be to run jobs across multiple hosts,
  such that Job A could run on Host "Foo" at the same time that Job B runs on Host "Bar".

The simplest solution to this business need is to update the BFD Pipeline such that:

* It is auto-scaled across EC2 hosts.
* Jobs guard/lock against duplicate execution, using a shared system,
    such as the BFD database.
* Jobs use the same shared system to track execution and check dependencies.

Unlike the previous options, this solution starts to add a moderate amount of complexity.
It's not complex!
But it's also no longer a simple, easy change.
This isn't to say that it's a bad idea;
  just that this is where real investment starts to be required.
In this case, it's 1-3 senior engineer-sprints worth of work.


#### Option: Do We Want a More Off-the-Shelf Job Orchestrator?

Honestly, this is not so much a business _need_, as a technical _want_.
Accordingly, I'm inclinced to argue against it:
  unless you can articulate (and prove) what such an off-the-shelf orchestrator,
  e.g. [Airflow](https://airflow.apache.org/),
  will **specifically** get you,
  **and** prove/demonstrate that said orchestrator is mature and reliable and flexible enough to meet our needs,
  don't do this;
  it's a bad idea.

Nevertheless, if you're dead set on this, I'd suggest the following:

* Lean heavily towards mature distributed systems deployed widely at very large companies.
    * You will hate how complex/expensive they are to deploy and operate,
        but you will appreciate the relative feature set, lack of bugs, etc.
    * This means: more like [Netflix's Conductor](https://netflix.github.io/conductor/) 
        and [AWS' Step Functions](https://aws.amazon.com/step-functions/);
        less like [Airflow](https://airflow.apache.org/).
* Consider investing in [Nomad](https://www.nomadproject.io/)
    or [Kubernetes](https://kubernetes.io/) at the same time,
    so that you can avoid vendor lock-in
    and also so developers can continue to benefit from being able to run code locally.
    * Note: this is another way of saying, "AWS Step Functions" are a great example of vendor lock-in.
* Plan on this taking **at least** a full PI to fully prototype, implement, and deploy.


#### Option: Do We Need To "Fan Out" the Execution of a Single Job Across Multiple Workers?

Suppose that **very** tight SLOs are mandated,
  such that a single ETL job needs to "fan out" its processing across multiple hosts
  (not just threads).
This pre-supposes that you've exhausted the limits of vertical scaling
  (which, it's worth noting, seems unlikely).

The proposed solution here is basically:
  see the above "Option: Do We Want a More Off-the-Shelf Job Orchestrator?" section
  and figure out how to fan-out on top of an existing, mature platform.
While it sounds like fun (to weirdos like me),
  implementing a bespoke system that can do this correctly is not for the faint of heart.


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

<!--
This is the technical portion of the RFC. Explain the design in sufficient detail that:

* Its interaction with other features is clear.
* It is reasonably clear how the feature would be implemented.
* Corner cases are dissected by example.

The section should return to the examples given in the previous section, and explain more fully how the detailed proposal makes those examples work.
-->

This detailed design is limited to just a few of the options, above.
The other options/sections are more complex pieces of work and thus ought to be considered separately,
  outside of this RFC.

#### Immediate Changes

Digging into the current code a bit, the BFD Pipeline already has a lot of the boring machinery required to run multiple jobs:
  a task executor instrumented for observability,
  task wrappers to handle failures appropriately,
  startup and shutdown routines,
  etc.
Except... that's all currently configured to only ever run one job,
  and all of the other current jobs get kicked off by that main job before or after it runs its main logic.
That machinery is captured in the `DataSetMonitor` class.

Per this proposal, let's refactor `DataSetMonitor` in the short-term such that:

* It runs an ordered list of jobs (rather than a single one), one after the other.
* It has a more generic name that reflects its larger scope, e.g. `PipelineManager`.
* It gets restructured out of the CCW-specific `bfd-pipeline-rif-extract` module
    into one that reflects its larger scope.
* All of the current four ETL tasks are run as generic, independent jobs,
    rather than being shoehorned into the CCW data load job.
* Add a sample, disabled job that demonstrates how new jobs can be added and managed.


#### Option: Do We Need To Run Jobs In Parallel?

Relationships between jobs would be expressed as a directed-acyclic graph (DAG).
For the current jobs, that DAG would look like this:

```
            +----------------------------+
            |                            |
            | Database Schema Management |
            |                            |
            +--------------+-------------+
                           ^
                           |
                           |
                   +-------+------+
                   |              |
                   | CCW RIF Load |
                   |              |
                   +-+----------+-+
                     ^          ^
                     |          |
                     |          |
+--------------------+--+    +--+----------------------+
|                       |    |                         |
| Beneficiary MBI Fixup |    | Beneficiary History MBI |
|                       |    |                         |
+-----------------------+    +-------------------------+
```

That DAG will need to be expressed declaratively in the BFD code, e.g. `jobA.dependsOn(jobB, "version 42")`.
Then the `PipelineManager` and related classes will need to be updated to honor it.
Once that is complete, the `PipelineManager` likely won't have to do anything more exciting than
  increasing the size of its main job threadpool from `1` to `N>1`, and jobs will start running in parallel.
Easy peasy, lemon squeezy.

Fun fact: this option may also allow us to speed up the execution of the existing CCW RIF Load task,
  if the loads for each RIF record type are run in parallel, where possible.


#### Option: Do We Need Better Obervability?

Observability is a big old rabbit hole; it all depends on what questions engineers and operators want answered.
That said, we should start with a few relatively simple steps,
  and see if that provides enough information to answer the most commong questions:

1. Record the start and end of each job run in a database, but only for runs where the job actually has work to do.
2. Publish those same start and end events to New Relic, as well.
3. Publish those same start and end events to our logs (and thus Splunk), as well.

Those runs/events should also include basic job status details,
  e.g. whether or not the job succeeded.
For failures, it'd be nice to also include a brief one-liner error message, as well.

That should go a long way towards helping folks understand the state and behavior of our ETL systems.


### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

<!--
Collect a list of action items to be resolved or officially deferred before this RFC is submitted for final comment, including:

* What parts of the design do you expect to resolve through the RFC process before this gets merged?
* What parts of the design do you expect to resolve through the implementation of this feature before stabilization?
* What related issues do you consider out of scope for this RFC that could be addressed in the future independently of the solution that comes out of this RFC?
-->

No unresolved questions have been identified at this time.


### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

<!--
Why should we *not* do this?
-->

The most common concern I hear with this single-codebase approach is something along the lines of,
  "I don't want to have to deal with someone else's framework/application;
  I just want to create and deploy this new thing as a standalone task."

This is a **very** understandable concern!
There's definitely a tradeoff in keeping things in one codebase:
  it lowers long-term maintenance costs by ensuring the BFD team only has one codebase to support,
  but can increase the costs of initial development by forcing the new job developer(s) to become familiar with that codebase.

However, those risks are very manageable, if addressed appopriately.
Which is what we're trying to do here with this design:
  we want to keep the "surface area" of contact between each job and the framework,
  and between the different ETL jobs,
  as minimal as possible.
Inasmuch as we can manage it, each job will basically be just a Java `Runnable`
  and can do whatever it needs to, however it wants to do it.

Other concerns I hear with this approach basically boil down to,
  "but I want to use [language/platform X], instead."
This is **also** a very understandable concern!
There's definitely a tradeoff in restricting the programming language and platform used for ETL tasks:
  it lowers long-term maintenance costs by ensuring the BFD team only has one language and platform to support,
  but can increase the costs of initial development by forcing the new job developer(s) to become familiar with that language and platform.

For the long-term health of the BFD platform, this is just the appropriate tradeoff to make.
The BFD team is not and likely won't ever be particularly large,
  and thus needs to aggressively limit the scope of the services that they support.

Occassionally, this concern gets reframed as "BFD should _move_ all its ETL jobs to [language/platform X]
  and this new job will just use it for now to prove why that's such a great idea."
I've been guilty of this move sometimes, myself!
But historical reality rather convincingly demonstrates that, no,
  the BFD team will never have the resources and freedom to move everything to the new platform.
Instead, the BFD team will now be saddled with the maintenance burdens of _two_ platforms, rather than one,
  with no corresponding increase in team size or budget.
Unfortunately, unless the team proposing this is also proposing to migrate everything themselves,
  this is not a realistic approach.


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

<!--
* Why is this design the best in the space of possible designs?
* What other designs have been considered and what is the rationale for not choosing them?
* What is the impact of not doing this?
-->

These questions are addressed as part of other sections, instead.

## Prior Art
[Prior Art]: #prior-art

<!--

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

-->

The following discussion is very relevant to this RFC:
  [Hacker News: How to Become a Data Engineer in 2021](https://news.ycombinator.com/item?id=25728198).

Here are my notes from the original article:

* Older tools such as Informatica, Pentaho, and Talend are characterized as legacy approaches.
* I don't really buy the assertin that data engineers need to be practiced with
    a DB's underlying DB structures and algorithms, e.g. B-trees.
  That said, I would agree that they should be at least passingly familiar with them.
    * I think the most important insight from this is really related to index caching:
        modern DBs will try to keep "hot" index pages cached in memory
        and may exhibit pathological behavior when they can't.
      Which portions of the trees are likely to be "hot"?
      The upper levels of the tree, as bounded by the system's page size.
    * This insight has been particularly important when interacting with PostgreSQL's query planner.
      If the DB determines that it can't keep "enough" index pages in memory, it will refuse to use the index.
      In addition to being frustrating due to the poor visibility developers have into this behavior,
        it also cautions against viewing table partiioning as a silver bullet:
        there's no reason to assume that simply having more, smaller index trees will perform any better.
      And on the flip side, it points towards DB sharding as potentially being necessary in the future.
      If our indices ever outgrow what we can fit into memory on large RDS instances,
        sharding seems like a likely (albeit expensive) solution.
      To be clear, I'm still of the opinion that we're a long way away from needing to shard,
        but it's worth keeping in mind for the future,
        in addition to evaluating alternative DB platforms.
* Both the article and discussion repeatedly make the point that SQL is an essential technology.
  This rings true: it is still clearly the best tool for many data problems.
* The article calls out Python's poor performance as a concern.
  I share this concern, but think it's nevertheless worth exploring Airflow and other Python-based options.
* When reading the article's "Big Data Tools" section it's worth keeping in mind
    what problems _we_ are trying to solve for BFD.
* Problems we don't have:
    * We can _invent_ an event streaming problem for ourselves but we don't intrinsically _need_ to apply that technique.
    * We don't have much in the way of data processing to do.
    * We don't need an analytics platform.
* Problems we do have:
    * We're doing a massive amount of very simple ETL under modest time constraints.
    * Actually, it's mostly "EL" not "ETL": we don't want to apply many data transformations at load time.
      If we had to reload/reprocess all records every time we changed our mapping we would be in a very bad place.
* It's also worth keeping in mind the scale of our systems:
  We have terabytes of data but **not** petabytes.
  Billions of records but **not** trillions.
  We're not really a big data system, as such.
  Instead, BFD is just a data-lake-sized online database, heavily optimized to support a limited number of query types.

Here are my notes from the article's discussion on HN:

* Lots of mentions of Snowflake, though that doesn't seem germane to the problems we're looking at here.
  (Worth considering later, though.)
* [dbt](https://www.getdbt.com/) sounds interesting, but again: we don't want to do much transformation prior to load.
    * If we ever wanted to dual-purpose the DB as an analytics platform, I think we should look at dbt.
* [Fivetran](https://fivetran.com/) sounds interesting, but appears to not offer a hosted option,
    and is thus a non-starter, unless/until they get FedRAMP'd.
* It references this,
    [Emerging Architectures for Modern Data Infrastructure](https://a16z.com/2020/10/15/the-emerging-architectures-for-modern-data-infrastructure/),
    which is interesting in general, but also has the a useful new (to me) acronym:
    "ELT" for "extract, then load, then transform" and calls it out as being less brittle than traditional ETL.
  Nice term for capturing what we do in BFD.
* A comment mentioned "Data Vault", which turned out to be an interesting read:
    [Data vault modeling](https://en.wikipedia.org/wiki/Data_vault_modeling).
  I'm not sold on the suggested storage structure, but the underlying philosophy makes sense.
* These two comments ring true: <https://news.ycombinator.com/item?id=25733701>
    and <https://news.ycombinator.com/item?id=25732147>.
  Developers uncomfortable with SQL should be encouraged and supported to "push through" that.
* [AWS Step Functions](https://aws.amazon.com/step-functions/) appear to be the preferred approach
    when going serverless.
* As a complete sidenote, I wandered across this very useful article while reading this discussion and related items:
    [We’re All Using Airflow Wrong and How to Fix It](https://medium.com/bluecore-engineering/were-all-using-airflow-wrong-and-how-to-fix-it-a56f14cb0753).
  It makes the case that Airflow's built-in operators are buggy and hard to debug
    and argues for instead using just the Kubernetes operator to run custom code for every task.
  It's a compelling argument, especially since we could just as easily substitute in Docker, instead.


## Future Possibilities
[Future Possibilities]: #future-possibilities

TODO

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

TODO

The following addendums are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.