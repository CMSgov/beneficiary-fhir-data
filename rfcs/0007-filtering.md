# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0007-filtering-fields`
* Start Date: 2020-08-21
* RFC PR: [rust-lang/rfcs#0007](https://github.com/rust-lang/rfcs/pull/0000)
* JIRA Ticket(s):
    * [https://jira.cms.gov/browse/AB2D-1863](https://jira.cms.gov/browse/AB2D-1863)

This proposal will suggest adding server side filtering to API calls that return EOBs to the client in order to 
cut down on the amount of data that needs to be transferred. 

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

AB2D receives lots of data back from BFD when we make API calls to gather EOBs (Explanation of Benefits), 
where we filter out much of the data we receive. This might not be terrible for a few API calls, but for 
thousands it can impact our performance significantly. Since we only need a small portion of the data that we receive, it makes
sense for the server to send us only the data that we end up using. This will also help BFD in reducing
the amount of data that they need to send over the network.  

## Proposed Solution
[Proposed Solution]: #proposed-solution

The solution will allow BFD to keep track of which fields AB2D will use on the server side so that any change in the fields 
that AB2D receives would need to be updated in the BFD code base. Since we don't expect these fields to change too often, 
we can have them stored in the BFD code base. The filtering will need to happen towards the end of processing. This is to avoid
spaghetti code since transformations from the entity object to the FHIR object happen in several places throughout
the codebase. 


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

The BeanUtils library can be utilized for transforming properties

```
<dependency>
    <groupId>commons-beanutils</groupId>
    <artifactId>commons-beanutils</artifactId>
    <version>1.8.2</version>
</dependency>
```

A `private static final Set<String>` of fields will need to be maintained in `TransformerUtils`. This will
be an allow list of fields that AB2D wants to keep in the FHIR object that gets sent back to the client. Just before the
end of the method `findByPatient` in `ExplanationOfBenefitsResourceProvider` the `eobs` can be passed to a new
method in `TransformerUtils`.

This method will be doing the bulk of the work that we are interested in for this RFC. The filtering will happen
in this method where each field in the EOBs are visited. Reflection will be used to gain the list of all 
fields in the object we are interested in (ExplanationOfBenefits). If the field matches a name in the allowed list it will 
be allowed to stay, otherwise it will be nulled out. Every field in the object will need to be visited so that we don't miss any
fields.

```
private static final Set<String> allowAB2DList = new HashSet<>();

static {
    allowedAB2DList.add("patient");
    allowedAB2DList.add("provider");
    allowedAB2DList.add("organization");
    ...
}


static void filterEOBs(List<IBaseResource> eobs) {
    for(ExplanationOfBenefit eob : eobs) {
        Field[] fields = eob.getClass().getDeclaredFields();
        for(Field field : fields) {
            if(!allowedAB2DList.contains(field)) {
                PropertyUtils.setProperty(eob, field.getName(), null);
            }
        }
    }
}
``` 

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

How much will this save us? We will need to benchmark this solution compared to the old one to ensure it will
be worth putting into production.


### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

There will be extra time spent filtering fields out on the server side, but this should save AB2D a lot in terms
of reducing the amount of data we need to receive and then filter. BFD will be sending less data over the network.

This solution will also put the burden of maintaining the fields on BFD's side, which would mean that AB2D would make
a change in a codebase other than our own, but since we don't expect these fields to change very often, that should be a
minimal amount of change.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

One alternative that would give clients more flexibility would be to send the parameters themselves, but with that
added flexibility comes a danger of being able to more easily make a mistake. By setting the parameters on the server
side it's more difficult to change, but also less error prone. 

An alternative for using a library like PropertyUtils would be JXPath, but when I benchmarked the two, PropertyUtils
was consistently faster. Having said that, I would like to run some more realistic tests, which would not be very hard 
to setup, since it would just be a very simple matter of changing one line of code really to compare the two.

## Future Possibilities
[Future Possibilities]: #future-possibilities

There could be filtering on the database side as well. Right now there are 194 columns
in the beneficiary table, and we certainly do not need all of them, so filtering on the 
database side would reduce the load on the database.

Other groups would likely want to utilize filtering as well, but that is out of scope for this RFC.