# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0008-partial-backfill-ccw` (fill me in with a unique ident)
* Start Date: 2020-01-04
* RFC PR: [rust-lang/rfcs#0000](https://github.com/rust-lang/rfcs/pull/0000)
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

TODO

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
