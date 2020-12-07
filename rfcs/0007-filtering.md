# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0006-or-condition`
* Start Date: 2020-08-21
* RFC PR: [rust-lang/rfcs#0000](https://github.com/rust-lang/rfcs/pull/0000)
* JIRA Ticket(s):
    * [https://jira.cms.gov/browse/AB2D-1862](https://jira.cms.gov/browse/AB2D-1862)

This proposal will suggest adding an OR clause to queries that gather the contract to patient mapping in order
to improve efficiency on both the client and server. 

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
* [Future Possibilities](#future-possibilities)

## Motivation
[Motivation]: #motivation

AB2D has long running export jobs where the amount of time the job takes is critical for customer happiness. In order to
lessen the amount of time a job takes to run, we will need to be able to improve performance in every section of our code.
One part that takes a long time is the call to the Patient URL that maps contracts to beneficiaries on a monthly basis.
Being able to query multiple months in the same call will reduce the load on the BFD database since it will reduce the 
number of calls needed to it, and be more efficient since we will reduce the number of API calls we need to make. It is more
efficient on the server side to make 1 call to get the data, than it is to make 6 different calls that gather the same amount
of data.

## Proposed Solution
[Proposed Solution]: #proposed-solution

The solution will allow a client to specify any number of months that they want to be passed to the BFD server. The BFD server
will be able to parse what months the client is asking for, and then return all of the months at once, instead of having the client
make individual requests.


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

PatientResourceProvider.queryBeneficiaryIds currently takes in just one field, and one value. This code should
be changed so that it takes in a list of objects. The new object would just be a simple value class that can 
hold a field and value 

PatientResourceProvider.queryBeneficiaryIds can take in an object like so:
// Assume getters and setters
class FieldValue {
    private String field;
    private String value; 
}

https://bluebutton.cms.gov/resources/variables/ptdcntrct01
is the current variable for how data is sent over from AB2D for the month of Jan. to the BFD server

When it is used with an identifier, it is combined with a contract, e.g. 
https://bluebutton.cms.gov/resources/variables/ptdcntrct01|Z0001

In order to send over multiple months using the FHIR client libraries, the systemAndIdentifier method takes
2 strings, the first being the one we're interested in (the system). Months can be separated by a delimiter, so
that multiple months can be included, e.g. https://bluebutton.cms.gov/resources/variables/ptdcntrct01-02-03

This value can be parsed by BFD to determine if there should be an or parameter in the event there's a list of items
sent over. One month would mean that there's no need for a month parameter. The SQL query in queryBeneficiaryIds
can provide an OR clause when necessary.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Before a solution goes into place, how can we validate that this actually does help performance?
Performance testing this with real world examples, e.g. comparing AB2D performance querying 6 months separately
vs. 1 query with 6 months.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

This will require a more complicated implementation on both the client and server side where
the client could ask for fields that may end up changing, so both sides will have to stay in sync.

It would require communication from both sides and lots of testing so that there aren't any issues with
a client asking for a field that does not exist.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives



## Future Possibilities
[Future Possibilities]: #future-possibilities

There could be filtering on the database side as well. Right now there are 194 columns
in the beneficiary table, and we certainly do not need all of them, so filtering on the 
database side would reduce the load.