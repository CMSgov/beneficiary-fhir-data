# Beneficary FHIR Server RFCs
[header-and-summary]: #header-and-summary

Each "Request for Comment (RFC)" is a proposal for a substantial change to our systems and/or processes.
The RFC process is intended to provide some formalization around such proposals,
  allowing the entire team a chance to review them and to have a voice in such decisions before they're made.
It's also intended to democratize the process: anyone can create an RFC.

Pragmatically: such decisions need review and input, and GitHub pull requests are a great system for enabling that.

## Table of Contents
[table-of-contents]: #table-of-contents

* [Beneficiary FHIR Server RFCs](#header-and-summary)
* [Table of Contents](#table-of-contents)
* [When Is An RFC Needed](#when-is-an-rfc-needed)
* [Before Creating an RFC](#before-creating-an-rfc)
* [The RFC Process](#the-rfc-process)
    * [Motion for Final Comment](#motion-for-final-comment)
    * [Postponement](#postponement)
    * [Implementation](#implementation)
* [Inspiration](#inspiration)

## When Is an RFC Needed?
[when-is-an-rfc-needed]: #when-is-an-rfc-needed

What counts as "substantial"? Things like the following:

* A major new feature.
* Deprecation or removal of existing features.
* Most changes that would require a Security Impact Aseessment (SIA).
* Major changes to our processes, particularly any that might break team members' existing workflows.

When (reasonably) in doubt, create an RFC first, as the lack of one might hold up merging your PRs.

## Before Creating an RFC
[before-creating-an-rfc]: #before-creating-an-rfc

Things to do _before_ drafting your RFC:

1. Review the security guidance on [Confluence: Open Source and Public Code in OEDA](https://confluence.cms.gov/display/~AF9D/Open+Source+and+Public+Code+in+OEDA).
    1. Part of the motivation for this public RFC process is that CMS views being open & transparent as an important in and of themselves.
    2. Nonetheless, those goals need to balanced against the risks they present: both from attackers and from a public relations perspective.
    3. When in doubt, please run your draft RFC by your CMS Information System Security Officer (ISSO) **prior** to sharing it publicly.
2. Have a conversation with your company or program Information System Security Officer (ISSO) about your proposal.
    1. Which parts of the proposal might be _For Official Use Only (FOUO)_? Those parts will need to be excluded, or instead put into a separate, encrypted addenda.
    2. Which parts of the proposal might be _System Configuration Management Information (SCMI)_?
       Those parts will need to be excluded, or instead put into a separate, encrypted addenda.
3. Have a conversation with your company and program tech leads.
    1. Would they be likely to support your proposal?
    2. If not, what objections would you need to overcome to get them to vote in favor of it?
4. Have a conversation with other relevant program and/or community members.
    1. Would they be likely to support your proposal?
    2. If not, what objections would you need to overcome to get them to vote in favor of it?

There'a no harm in throwing up an RFC as a "trial balloon" but a much faster way to get feedback on an idea first... is to _talk about it_ with others.
Accordingly, you ought to have these conversations (e.g. in Slack) first, **before** publishing your RFC.

## The RFC Process
[the-rfc-process]: #the-rfc-process

Create and draft your RFC, as follows:

1. Copy `rfcs/0000-template.md` to `rfcs/0000-my-feature.md` (where "my-feature" is descriptive. don't assign an RFC number yet).
2. Fill in the RFC.
   Put care into the details and follow the template as much as is reasonable.
   Try hard to honestly evaluate the benefits and drawbacks; disingenuous RFCs are likely to be poorly received.
3. Submit a GitHub pull request with your new RFC. Be prepared to receive and incorporate feedback!
4. Build consensus and incorporate feedback.
   Not every RFC will be approved/merged!
   If you want your beautiful baby RFC to leave the nest, you'll need to win support for it.
    * Do not squash or merge commits; we want all of the context, conversation, and history to stick around for the benefit of future team members.
5. Expect surprise.
   It's pretty unlikely that the first version of your RFC was right on the mark;
     the final result may be very different (and better!).

### Motion for Final Comment
[motion-for-final-comment]: #motion-for-final-comment

Once your RFC has been sufficiently reviewed and edited via the Pull Request process,
  you will likely want a final decision on it:
Does the team wish to accept this proposal?
To reach that decision, you or anyone can propose a "motion for final comment",
  along with a disposition: *merge*, *close*, *postpone*.

* All relevant engineers receive a vote.
  If this is not obvious, the project's technical lead will decide.
* How is a decision reached?
  *Not* consensus; that's not always possible.
  Instead, a majority + 1 vote is required, with no vetos from relevant technical leads.
* Voting will not last longer than 5 business days. Abstenions count as "nays".

If the RFC receives enough "aye" votes, the PR will be noted as approved and merged.
Be sure to assign it an official RFC number just prior to that merge.

### Postponement
[postponement]: #postponement

Postponement is a way for the team to say, "this is probably a good idea, but not for at least a couple of PIs."
Postponed RFCs do not get merged, just closed.
If/when the time is right, someone can reopen them or re-propose a similar RFC.

### Implementation
[implementation]: #implementation

Your RFC has been approved and merged: yay, we're done! Wait a second...
How's it get implemented?

At this point, the RFC goes into the planning process:
  epics, stories, etc. are created and prioritized.
It is normal and expected (but not at all required!) that folks other than proposers will end up implementing RFCs.

## Inspiration
[inspiration]: #inspiration

This RFC process is largely derived from the following:

* [Rust RFCs](https://github.com/rust-lang/rfcs)
* [Swift Programming Language Evolution](https://github.com/apple/swift-evolution)
    * [Swift Evolution Process](https://github.com/apple/swift-evolution/blob/master/process.md)
    * [Swift Evolution Proposal Template](https://github.com/apple/swift-evolution/blob/master/proposal-templates/0000-swift-template.md)
