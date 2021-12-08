# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0001-beneficiary-fhir-server-rfc-process-v1`
* Start Date: 2019-05-15
* RFC PR: [bluebutton-data-model/rfcs#0001](https://github.com/CMSgov/bluebutton-data-model/pull/53)
* JIRA Ticket(s):
    * [BLUEBUTTON-948](https://jira.cms.gov/browse/BLUEBUTTON-948)

## Status
[Status]: #status

* Implementation Status: Done
* JIRA Ticket(s):
    * [BLUEBUTTON-949](https://jira.cms.gov/browse/BLUEBUTTON-949)
    
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

The Blue Button Data/backend team should have an informal-as-is-reasonable process for managing and evaluating major proposals.

## Motivation
[Motivation]: #motivation

Why are we doing this? I mean... alll the cool kids are doing it:

* [Blue Button 2.0 Security Impact Assessments](https://confluence.cms.gov/pages/viewpage.action?pageId=143363056)
* [Blue Button 2.0 Auth/frontend Architecture Decision Records](https://confluence.cms.gov/display/BB/Architecture+Decision+Records)
* [BCDA Engineering Tech Specs](https://confluence.cms.gov/display/BCDA/BCDA+Engineering)

More seriously, we want to move from the current process where these proposals are "hidden" to one where the entire team can participate. This is expected to:

1. Lead to more thoughtful input on these decisions, and thus better decisions.
2. Formalize the currently-informal process, leading to clearer outcomes.
3. A historical record of our thought process and decisions, making it easier for new team members to get up to speed.

## Proposed Solution
[Proposed Solution]: #proposed-solution

This very-first RFC is a "process only" RFC, in which the team will detail, discuss, and agree on a formal process for making major decisions,
  e.g. on architecture, process (like this!), and major features.
Specifically, we're reviewing and hopefully approving these documents:

* [./README.md](Blue Button Data Systems RFCs)
* [./0000-template.md](Template/Starter RFC)
* [./0001-beneficiary-fhir-server-rfc-process-v1.md](This RFC: `0001-beneficiary-fhir-server-rfc-process-v1`)

Other future RFCs will likely include:

* "Should we adopt a formal (internal) Code of Conduct / Working Agreement?"
* "Should we switch from self-managed PostgreSQL-on-EC2 to a more managed DB offering?"
* "Should we move our Security Impact Assessment (SIA) process to GitHub?"
* "Should we move our development and/or production systems to Docker?"

Questions to consider when evaluating this particular RFC:

* Is this process inclusive enough?
    * GitHub's pull requests have a great workflow for engineers, but is it user-friendly enough for non-developers?
* Knowing our team, will the process work well for the people and personalities involved, or will it somehow turn toxic?
* How might we change this proposed process to make it more agile?
* Are we all comfortable having these conversations in public, on GitHub?
* Does this RFC follow the proposed process well enough to validate the process and associated templates?

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Are these two documents correct and sufficient?:

* [./README.md](Blue Button Data Systems RFCs)
    * Intended to introduce the RFC process at a high level.
* [./0000-template.md](Template/Starter RFC)
    * Intended to be copy-pasted for each new proposed RFC.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

The following unresolved questions were identified during review of this RFC:

1. How might we protect that shouldn't-be-public information to keep it private, while still (largely) using this open process?
    * I suspect that [git-crypt](https://github.com/AGWA/git-crypt) and/or [Ansible Vault](https://docs.ansible.com/ansible/latest/user_guide/vault.html) are reasonable choices.
    * I think it's safe to defer this issue until it comes up for the first time; better to make a decision on tooling then.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

Why should we *not* do this?

* Is this too much paperwork?
    * Karl's opinion: no, each RFC should only take an hour or two to draft.
* Is our current (informal) process working well enough that this should be *postpone*d?
    * Karl's opinion: Maybe, but I was going to have to start writing these things down anyways, and I'd rather do the writing in Markdown on GitHub than in Confluence.
* Have similar attempts by other communities failed in any spectacular ways that the proposed process doesn't account for?
    * Karl's opinion: Not that I know of. Worst failure case I've seen is folks using the RFC process to float bad ideas and being sad when people reject or ignore them.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

This is very much intended to be, "at least a little bit better than what BCDA and the Blue Button Auth/frontend are doing."
It's not a perfect process, nor does such a perfect process even exist.
Over those processes, though, we have the major advantage that GitHub has excellent support for a review workflow, and Confluence (which they use) doesn't.

Given the Rust lang team's success with it, it clearly scales up _way_ past what we will need it to.
Given our dogfood use of it here, we can be reasonably confident that it works okay for our much smaller needs.

If we don't adopt _some_ formal process, we'll likely continue largely with our current informal process,
  which largely boils down to, "do whatever the loudest person (i.e. Karl) thinks.
That's not ideal.
This is better.

## Prior Art
[Prior Art]: #prior-art

As previously mentioned, there's lots of prior art here:

* [Rust RFCs](https://raw.githubusercontent.com/rust-lang/rfcs/)
* [Blue Button 2.0 Security Impact Assessments](https://confluence.cms.gov/pages/viewpage.action?pageId=143363056)
* [Blue Button 2.0 Auth/frontend Architecture Decision Records](https://confluence.cms.gov/display/BB/Architecture+Decision+Records)
* [BCDA Engineering Tech Specs](https://confluence.cms.gov/display/BCDA/BCDA+Engineering)

We should measure the success of this RFC's proposal against those by comparing the relevant team/community sizes vs. engagement in the RFC process.
What percentage of the Blue Button Data/backend team gets involved in RFCs, on average?

## Future Possibilities
[Future Possibilities]: #future-possibilities

No future changes to this process have (yet) been identified.

It *is* worth noting, though, that this process's main inspiration [Rust RFCs](https://github.com/rust-lang/rfcs/)
  has seen tons of changes to it since the process was first formalized in 2014:
  [Rust RFCs: README.md: History](https://github.com/rust-lang/rfcs/commits/master/README.md).

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.