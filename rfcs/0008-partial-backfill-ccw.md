# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0008-partial-backfill-ccw`
* Start Date: 2020-01-04
* RFC PR: [CMSgov/beneficiary-fhir-data#427](https://github.com/CMSgov/beneficiary-fhir-data/pull/427)
* JIRA Ticket(s):
    * [BFD-589: Epic: Partial Backfill of CCW Records](https://jira.cms.gov/browse/BFD-589)


<!--
Write a brief summary here: a one paragraph explanation of the feature. Try to structure it like an "elevator pitch": it should provide readers with a high-level understanding of the goals and proposed solution.

Please note: many of the other sections below will not be needed for some proposals;
  don't waste time writing responses that don't deliver real value.
For any such not-needed section, simply write in "N/A".
-->

BFD recently started receiving new claim fields from the CCW for new and updated claims,
  but has not yet "backfilled" those fields into claims that were inserted prior to the change.
The CCW team will, hopefully early in 2021,
  be ready to send BFD the data required for that backfill.
However, that backfill data will all be in new RIF layouts,
  which will only contain the record primary keys and the fields to be backfilled.
BFD does not support such layouts and will need to.
Adding that fucntionality will be a moderate-to-major architectural change for BFD.


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


## Motivation
[Motivation]: #motivation

<!--
Why are we doing this? What use cases does it support? What is the expected outcome? Why is now the appropriate time to address this?
-->

BFD peers and users need and expect this data to be there, even for older claims.
To a large extent, we haven't really completed the work of adding the new fields until this backfill is performed.

It's worth explaining why this will require moderate-mojor architectural changes to BFD,
  as that is not obvious unless you're already familiar with BFD's architecture and code.
The short of it is this: BFD relies heavily on the fixed set of nine (or so) RIF layouts that the CCW sends.

Those layouts are fed into a source code generator that automatically produces:

* RIF/CSV parsing code to read those layouts.
* The database schema that those records will be inserted/update into.
* Java objects to model those DB tables (i.e. JPA/Hibernate entity classes).
* And, by extension, **much** of the code that performs the inserts and updates into the DB.

This was a great design up until now!
All of that automatic code generation was actually added as an enhancement
  to remove a very large class of bugs that BFD kept running into.
And it succeeded: the BFD team has not had to spend its time chasing down subtle copy-paste and data mapping bugs
  that had been leading to pernicious (and expensive to resolve!) data corruption.

The main shortcoming of the approach, though, is its current reliance on the fact that
  the RIF layout matches the Java classes/objects that the CSV records are read into,
  which in turn match the JPA entities that the CSV records become,
  which in turn matches the DB schema that the records are inserted/updated into.
Adding an extra layer in there, to allow for RIF layouts that don't match the JPA entities,
  is not a small thing.


## Proposed Solution
[Proposed Solution]: #proposed-solution

<!--
Explain the proposal as if it was already implemented and shipped, and you were just explaining it to another developer or user.
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

When the CCW sends "merge" data to BFD,
  that data is loaded by the ETL systems like any other expected data,
  without requiring any special operator intervention.

Such data can be identified by inspecting the RIF manifest files in S3.
For example, here's a sample manifest for such data
  (note the "`_MERGE`" suffix on the `type` attribute):

```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<dataSetManifest xmlns="http://cms.hhs.gov/bluebutton/api/schema/ccw-rif/v9" 
  timestamp="1994-11-05T13:15:30Z" sequenceId="42">

  <entry name="sample-a-bcarrier.txt" type="CARRIER_MERGE" />

</dataSetManifest>
```


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

<!--
This is the technical portion of the RFC. Explain the design in sufficient detail that:

* Its interaction with other features is clear.
* It is reasonably clear how the feature would be implemented.
* Corner cases are dissected by example.

The section should return to the examples given in the previous section, and explain more fully how the detailed proposal makes those examples work.
-->

The detailed design is TBD;
  I want to think this one through "in code", first, via a prototype or three.

One thing I'm explicitly _not_ doing is shying away from using this challenge
  as a place to explore how we might also meet other current and upcoming architectural needs.
This is an intentional decision: we **need** to start making progress on some of these needs/challenges
  and this effort is as good an excuse as any.
This additional exploration work will need to be balanced against the **more urgent**
  need to actually _ship_ the partial backfill solution, though.


#### Design Option A: Hacky Approach

I'm just mentioning this one for completeness' sake, as it is not suitable to a recurring need like this will be
  (we may need to run a partial backfill quarterly, going forwards).
That said, one approach is to just build a custom version of the BFD Pipeline where:

* most fields are `Optional`
* most of the tests are removed (as they will fail to compile given the `Optional` change)
* the BFD Server is not built
* `RifLoader` was configured to **only** run `_MERGE` data sets

The BFD team would also have to carefully orchestrate things to ensure
  that this custom build was produced and deployed at the correct time,
  and then reverted/undeployed back to the normal BFD Pipeline after the backfill was complete.

Why is this approach a bad idea?
Well, as mentioned, it's very manual and would have to be repeated from scratch every quarter.
Being so manual, it's extremely error-prone, which is bad by itself,
  but given how painful a DB restore would be, is just far too risky.


#### Design Option B: Overhaul the Existing Code

The second-most minimal approach would be to update the existing code to add a couple new layers to the ETL:

1. Create separate structs/classes to represent parsed RIF records.
2. Add a RIF-to-JPA entity mapping layer in that supports both partial and full loads.

That's a brief description for some incredibly complex work.
It'd require extensiive changes and extensions to our automatic source code generation,
  which is by far the trickiest bit of code we have.

There are a number of risks with this approach:

* It's a lot of tedious and tricky work.
    * This is true of all our options, but still worth mentioning.
* It risks some performance degradation,
    as moving data through those extra layers will require a lot of additional memory copies, GC pressure, etc.
* Keeping the changes current/merged with the main branch in Git will be very difficult.

That said, it's not a _bad_ approach if the backfill is the only thing we want to try and address.


#### Design Option C: Create a New Pipeline

This is the most far-reaching option: create a new BFD Pipeline application that
  not only supports the partial backfill need
  but also is architecturally in line with other needs we have coming for BFD's ETL.
So far, those needs are:

A. Support `_MERGE` data sets from the CCW to partially backfill new fields into older claims.
B. Better orchestrate DB schema upgrades, to reduce the large risk and personnel stress that they currently incur.
C. Provide more metadata and automatic generation of code and resources,
     which can be consumed downstream to improve our internal and external developer documentation.
D. Support data sources beyond just the CCW.
E. Support our potential future performance and size scaling needs,
     as the amounts and types of data being managed by BFD continues to increase.
F. Automatically orchestrate with the current BFD Pipeline application
     to ensure that only one ETL process is running at a time,
     in order to avoid creating data races.

There are a number of risks with this approach:

* It's a lot of complex architectural and implementation work.
    * I mean, it sounds like a lot of fun to _me_, but that's kinda' my thing.
    * The risks of errors remina high, but those risks can be fully mitigated with an adequate focus on testing.
* It took a while to tune the current ETL system to achieve its current level of performance.
  Unless the lessons from that effort are carried forward into this work,
    this approach risks performance degradation.
* This will require a solid & consistent investment of time and effort from one or more senior engineers.
    * This risk can be mitigated by keeping the initial prototypes small and flexible,
        to prove out and test the approach before too much investment is made into a full implementation.

Something that will need to explored and decided on with this option is whether
  to go with a bespoke ETL application that feeds directly into the database
  or to instead build on top of open source orchestration and messaging platforms,
  such as [Apache Airflow](https://airflow.apache.org/) for orchestration
  and [Apache Kafka](https://kafka.apache.org/) for pub/sub messaging.
Here's how I'm framing this question right now,
  when I reach out and ask other folks for their input:

> This year, we need to add some major new functionality to our data/ETL pipeline,
>   to meet new business needs.
> It’s enough of a change from before that it’s one of those rare (for me)
>   “well, maybe we should just rewrite it,” moments.
>
> I’m considering moving towards something more dependent on frameworks, such as Airflow and/or Kafka.
> What we have right now is reasonably simple bespoke code (Java).
> But we’re going to need to orchestrate several different types of ETL and ensure (best we can, anyways)
>   that the end result is consistent & safe and I suspect that Airflow and/or Kafka may help.
> We’re an AWS-only shop, if that matters.
>
> Does anyone here have experience with those two frameworks/tools for use in ETL and have opinions?
>
> FWIW, I’m normally pretty anti-framework for ETL;
>   most frameworks I’ve tried in the past required so much custom code, anyways,
>   that it was hard to see the point —
>   especially once you account for all the time you’ll spend debugging the tools.

It's worth noting that AWS does appear to offer managed variants for both Airflow and Kafka.


### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

TODO

Collect a list of action items to be resolved or officially deferred before this RFC is submitted for final comment, including:

* What parts of the design do you expect to resolve through the RFC process before this gets merged?
* What parts of the design do you expect to resolve through the implementation of this feature before stabilization?
* What related issues do you consider out of scope for this RFC that could be addressed in the future independently of the solution that comes out of this RFC?


### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

TODO

Why should we *not* do this?


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

TODO

* Why is this design the best in the space of possible designs?
* What other designs have been considered and what is the rationale for not choosing them?
* What is the impact of not doing this?


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

TODO: Discuss BFD Insights' approach (AWS Glue, I think?).

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
