# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0010-custom-system-uris`
* Start Date: 2021-11-30
* RFC PR: [beneficiary-fhir-data/rfcs#0010](https://github.com/CMSgov/beneficiary-fhir-data/pull/888)
* JIRA Ticket(s):
    * [BFD-1459](https://jira.cms.gov/browse/BFD-1459)

<!--
Write a brief summary here: a one paragraph explanation of the feature. Try to structure it like an "elevator pitch": it should provide readers with a high-level understanding of the goals and proposed solution.

Please note: many of the other sections below will not be needed for some proposals;
  don't waste time writing responses that don't deliver real value.
For any such not-needed section, simply write in "N/A".
-->

The question being considered by this RFC is this:
  what base URI/URL should the BFD API use in its data payloads for custom/non-standard FHIR `system`s?
This RFC makes the recommendation,
  as explained and justified in later sections,
  that BFD should stick with a base of
  `https://bluebutton.cms.gov/resources/` for all custom URIs.

Before we go further, a quick note on terminology:
  this RFC talks a lot about
  [URI](https://datatracker.ietf.org/doc/html/rfc3986)s and
  [URL](https://datatracker.ietf.org/doc/html/rfc1738)s,
  which are _similar_ but nevertheless distinct concepts.
Briefly, a URI is a "magic string" in a specific format that uniquely identifies something.
Whereas a URL is also a "magic string" but one that web browsers and other software applications can use to retrieve something.
Often, URLs start with "http://", indicating that a web browser can likely open it.
In the vein of "a square is a rectangle but not all rectangles are squares",
  a URL is a URI but not all URIs are URLs.

BFD's data payloads include a number of custom URIs for FHIR
  [Coding.system](https://www.hl7.org/fhir/datatypes-definitions.html#Coding.system)s,
  [Identifier.system](https://www.hl7.org/fhir/datatypes-definitions.html#Identifier.system)s,
  and other such data types.
These URIs are serve two functions:
Firstly, they are "magic strings" that end user applications will use to select the pieces of our data payload that their applications care about.
Second, they can also be treated as URLs that provide developer documentation, e.g. data field definitions.
While many of the elements in our data payload can use industry-standard URIs for industry-standardized data,
  a large percentage of our data elements/codings are custom to CMS and thus must use custom URIs.

As an example, consider the following sample `Patient` resource payload from BFD:
  <https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/v2/patientRead.json>.
If an application wanted to select the `Patient.identifier` entry that had a beneficiary's MBI,
  they would search for the entry with a `system` value of `http://hl7.org/fhir/sid/us-mbi`.
HL7, a standards body, has said that `system` should be used for all MBIs,
  in all FHIR-compliant APIs, and so BFD does not need a custom URI there.
If, however, an application wants to select the `Patient.identifier` entry that contains a beneficiary's CCW beneficiary ID,
  they would search for the entry with a `system` value of `https://bluebutton.cms.gov/resources/variables/bene_id`.
The "CCW beneficiary ID" field is only used within/by CMS,
  so no standards body has assigned it a standard URI,
  and BFD must provide a custom URI for it.


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
Why are we doing this?
What use cases does it support?
What is the expected outcome?
Why is now the appropriate time to address this?
-->

At the end of the day here,
  our goal is to arrive at an API design decision that makes things easiest for our users.
We may have other, internal, concerns (mostly organizational/political),
  but providing the best possible API to our users is our primary concern.

Along those lines, it's very important to note that...
  this is not an important issue to our users,
  unless we do something that makes their life more difficult.

From the perspective of their applications,
  our URIs are just magic strings that they will copy-paste into their code.
From the perspective of their engineers when building out their application,
  it's helpful for our URIs to be descriptive,
  such that they can guess at what field they correspond to.
From the perspective of their business decision makers,
  the only thing that really matters is that these URIs are stable:
  that we do not change our API in ways that are backwards-incompatible with their existing applications.
That's about the sum total of our users' concerns:

* Ensure that our URIs are documented,
    such that they can be copy-pasted into their application code.
* Ensure that our URIs are descriptive,
    such that engineers building applications against our APIs can make reasonable guesses as to their meaning.
* Ensure that our APIs continue to work with their applications.

As a bonus, it's helpful if our URIs are also URLs for documentation relating to themselves,
  such that engineers can copy-paste them into their browsers and get detailed information on the field they represent/identify.

Those are our first-order concerns.
Internally, we of course have other goals.

Primary among those is a natural desire for our URLs to match our product and/or organizational structure.
For example, if the Awesome FHIR API is being offered by the Office of Fun,
  in the Entertaining Parties (EP) agency,
  which is a part of the Department of Population Happiness (DPH),
  which is part of the UK government,
  a base URI like the following might make sense:
  `https://awesomefhir.fun.ep.dph.gov.uk/`.
Or variants of it.
For BFD, that could look something like this:
  `https://bfd.dasg.oeda.cms.gov/`.
Or variants of it.


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

This RFC makes the recommendation that BFD should stick with a base of
  `https://bluebutton.cms.gov/resources/` for all custom URIs,
  _at least_ until a v3 of the API.

The primary justification here is simplicity for end users:
  having separate base URIs for new fields would make the API slightly more complex for end users to interact with.
If we're going to use a different custom URI, we should make that change all at once, across the whole API.

_Changing_ the URIs in v1 or v2 would be a major backwards-incompatibility,
  it would break almost all existing customer applications,
  which is something we strive very hard to avoid doing.

Additionally, it's hard to come up with a different answer that is both future-proof **and** would deliver any real user value,
  even in a future version of the API, e.g. v3.
* A base URI such as `https://bfd.dasg.oeda.cms.gov/` would capture details of CMS' _current_ internal organizational chart,
    in ways that would be awkward in the face of potential future re-organizations.
  If, for example, the Data and Analytics Strategy Group (DASG) gets renamed,
    would we want to change the URI again?
* A base URI such as `https://claims-data-api.bfd.cms.gov/` is perhaps a safer and more ontologically-correct choice,
    but it's hard to imagine any existing user caring such that they'd feel it justifies changing
    the URIs already baked into the v1/v2 version of their application.
  It's also perhaps a bit of an organizational overreach,
    as BFD is hardly the only "claims data API" at CMS.
* A base URI such as `https://bfd.cms.gov/` is perhaps a safer and more ontologically-correct choice,
    but it's hard to imagine any existing user caring such that they'd feel it justifies changing
    the URIs already baked into the v1/v2 version of their application.


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

<!--
This is the technical portion of the RFC. Explain the design in sufficient detail that:

* Its interaction with other features is clear.
* It is reasonably clear how the feature would be implemented.
* Corner cases are dissected by example.

The section should return to the examples given in the previous section, and explain more fully how the detailed proposal makes those examples work.
-->

Taking a look at BFD's
  [TransformerConstants](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-server/bfd-server-war/src/main/java/gov/cms/bfd/server/war/commons/TransformerConstants.java)
  file reveals the following base URI structure:

```
https://bluebutton.cms.gov/resources/
  codesystem/
    adjudication/
      <various EOB.adjudication.category discriminators>
    information/
      <various EOB.information.category discriminators>
    variables/
      <all sorts of stuff, but mostly EOB.extension discriminators>
  identifier/
    <several identifier.system discriminators>
```

_Most_ of the leaf URI "path" components refer to variables found in the
  [CCW Data Dictionaries](https://www2.ccwdata.org/web/guest/data-dictionaries).
As such, the URI structure should be extended for incorporation of future, non-CCW fields.
For PACA, something like the following would be recommended,
  which would ensure proper namespacing of similar-yet-actually-distinct fields:

```
https://bluebutton.cms.gov/resources/
  fiss/
  mcs/
  rda/
```

Similarly, new CCW fields should use a base URI of `https://bluebutton.cms.gov/resources/ccw/`.

Any backwards-compatible changes to terminologies represented by these URIs should keep the same URIs.


### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

<!--
Collect a list of action items to be resolved or officially deferred before this RFC is submitted for final comment, including:

* What parts of the design do you expect to resolve through the RFC process before this gets merged?
* What parts of the design do you expect to resolve through the implementation of this feature before stabilization?
* What related issues do you consider out of scope for this RFC that could be addressed in the future independently of the solution that comes out of this RFC?
-->

The primary unresolved question is whether or not this decision should be revisited in a future v3 of the API.
This RFC recommends reconsidering it, but framing the decision in terms of user value,
  e.g. "Is this worth doing if it adds burden to application developers that we will already have a hard time convincing to adopt the new version of our API?"

An additional unresolved question is how to handle backwards-incompatible changes to terminologies / code sets.
That question is not urgent, though, and should not block the adoption of this RFC's other recommendations.
See this Slack thread for details: <https://cmsgov.slack.com/archives/CMT1YS2KY/p1642702722039300>.


### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

<!--
Why should we *not* do this?
-->

Most of the potential drawbacks/concerns with this solution have already been discussed,
  in previous sections, e.g. concerns around modeling organizational/product structure.
Beyond those concerns,
  the real drawback here is potential confusion from users on seeing "bluebutton" in API responses,
  for non-Blue Button 2.0 APIs.
If we do receive that feedback from users,
  it is likely easy enough to resolve with a brief explanation in the APIs' documentation, e.g.:

> Note: The custom URIs used in this API are prefixed with
>   `https://bluebutton.cms.gov/resources/` for compatibility reasons.


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

<!--
* Why is this design the best in the space of possible designs?
* What other designs have been considered and what is the rationale for not choosing them?
* What is the impact of not doing this?
-->

_Discussed elsewhere._


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

_Not particularly relevant to this issue._


## Future Possibilities
[Future Possibilities]: #future-possibilities

<!--
Think about what the natural extension and evolution of your proposal would be and how it would affect the language and project as a whole in a holistic way.
Try to use this section as a tool to more fully consider all possible interactions with the project and language in your proposal.
Also consider how the this all fits into the roadmap for the project and of the relevant sub-team.

This is also a good place to "dump ideas", if they are out of scope for the RFC you are writing but otherwise related.

If you have tried and cannot think of any future possibilities, you may simply state that you cannot think of anything.

Note that having something written down in the future-possibilities section is not a reason to accept the current or a future RFC;
  such notes should be in the section on motivation or rationale in this or subsequent RFCs.
The section merely provides additional information.
-->

_Discussed elsewhere._


## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)
