# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-bye-bye-jboss` (fill me in with a unique ident)
* Start Date: 2019-09-17
* RFC PR: [CMSgov/beneficiary-fhir-data#39](https://github.com/CMSgov/beneficiary-fhir-data/pull/39)
* JIRA Ticket(s):
    * [BLUEBUTTON-1112](https://jira.cms.gov/browse/BLUEBUTTON-1112)

This RFC proposes switching the Data Server application from JBoss to Jetty.
Why? Primarily the licensing costs of JBoss,
  though the number of issues we've had with JBoss being unreliable over the years is also a consideration.
Jetty is an open source, well-supported, and widely-used alternative.

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

For business reasons, running JBoss inside our future home, the CMS Cloud Services (CCS) environment,
  will not really be feasible.
Also, as mentioned above, JBoss hasn't been the world's _most_ reliable piece of technology for us.
Accordingly, we need to switch to an alternative application server,
  either prior to or as part of the CCS migration.

## Proposed Solution
[Proposed Solution]: #proposed-solution

Java web applications are often packaged as Web Archive (WAR) files and run inside "application containers"
  such as JBoss, Wildfly, Jetty, Tomcat, Websphere, etc.
These containers provide support for various pieces and versions of the JavaEE specification,
  from the "Web Profile" to the "full platform".

The BFD Server only requires the "Web Profile" portions of JavaEE,
  and was originally designed to run in Jetty.
Early on, we moved from Jetty to a combination of JBoss in production and Wildfly elsewhere,
  as it was felt that the support provided by JBoss was needed.

This RFC proposes undoing that, and moving back to Jetty.
This will be a major change, but if done properly
  our end users will experience no negative impacts and may not notice the change at all.
We'll also need to perform a significant amount of testing,
  as switching application servers can result in very unexpected regressions.

Long-term, though, this change will likely be a major net-positive:

* We'll be able to remove the hilarious amount of fragile Bash scripts
    required to manage our current JBoss and Wildfly usage.
* We'll reduce the differences between our local and production environments,
    as we'll now be using the same application server for both.
* We'll be able to more simply implement planned features,
    such as altering authentication and/or authorization.
* We'll be more easily able to adopt auto-scaling and immutable deploys.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Nothing too magic here: we're gonna' rip out JBoss and Wildfly and replace them with Jetty.
To simplify deployment, we'll add a new `bfd-server-launcher` module to the Data Server project,
  which will build a simple Jetty executable [capsule](http://www.capsule.io/).
This provides a couple of advantages:

1. We already use capsules to deploy the Data Pipeline service,
     so deploying the Data Server in the same way will improve consistency.
2. Having a custom Jetty executable with a mostly-hardcoded configuration will cut down on future configuration errors.

It's worth noting that all of these changes are already implemented in this RFC's PR:
  the development work is done.

For testing, we'll employ a combination of:

* Our existing integration tests, in our local development environments.
* Hand-running similar tests in our TEST environment.
* Running our performance tests against the Jetty-fied Data Server in our TEST environment.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

N/A

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

The major tradeoff here is that JBoss costs a significant amount of money but provides excellent vendor support.
Jetty is open source: we'll need to support it ourselves.
Given the number of issues we've had with JBoss over the years despite the support,
  that feels like an accpetable tradeoff.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

In the Java ecosystem, you can hardly throw a rock without hitting an application server.
So why Jetty?

* Mostly just, "why not?"
* We've used it before and the engineer likely to implement this change
    (Karl) is more familiar and comfortable with it than other application servers, e.g. Tomcat.
* It does reasonably well on the [Tech Empower](https://www.techempower.com/benchmarks) benchmarks.

If problems are encountered during testing, we will **absolutely** consider alternatives.
Unless/until then, though, we're just going with what we know, to reduce risk and time-to-completion.

## Prior Art
[Prior Art]: #prior-art

N/A

## Future Possibilities
[Future Possibilities]: #future-possibilities

N/A

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.

## Implementation
[Implementation]: #implementation

* Implementation Status: Done
* JIRA Ticket(s):
    * [BLUEBUTTON-1112](https://jira.cms.gov/browse/BLUEBUTTON-1112)