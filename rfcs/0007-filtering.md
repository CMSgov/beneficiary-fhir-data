# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0007-filtering-fields`
* Start Date: 2020-08-21
* RFC PR: [rfcs#0007](https://github.com/CMSgov/beneficiary-fhir-data/pull/345)
* JIRA Ticket(s):
    * [https://jira.cms.gov/browse/AB2D-1863](https://jira.cms.gov/browse/AB2D-1863)

This proposal will suggest adding server side filtering to API calls that return EOBs to the client in order to 
reduce on the amount of data that needs to be transferred. 

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

A single BFD EOB retrieval call (UHC) with 2 million beneficiaries took 24 hours to retrieve three months of data.
Larger PDPs can have in excess of 6 million beneficiaries.  To have a request take over 3 days is not acceptable and in
fact the downloaded EOB files will have already been removed from S3.

AB2D receives more data than needed from BFD when we make API calls to gather EOBs (Explanation of Benefits).  AB2D
currently filters out irrelevant fields.  This might not be terrible for dozens of API calls, but for 
millions of requests it can impact our performance significantly.  Since we only need a projection of the data
(10 out of 69 top-level getters; 6 out of 42 fields in the ItemComponent), it makes
sense for the server to send us only the data consumed.  This reduces bandwidth requirements for receiving the data.

For example, GraphQL solves this problem by allowing clients to specify field selection (e.g. a database projection) at
invocation time.

## Proposed Solution
[Proposed Solution]: #proposed-solution

The solution adds code in the BFD server to configure which fields AB2D requires.  Non-required fields are nulled out
before being transformed in the result.  The specific set of required fields should remain stable
and not require modifications.

The filtering will need to happen towards the end of processing. This is to avoid
spaghetti code since transformations from the entity object to the FHIR object happen in several places throughout
the codebase. 


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

The BFD_FIELD_SELECTION header enables this feature.  A value of **ALL** is current behavior, will be the default
(if header is missing) and indicates to return all EOB fields.  A value of **AB2D_VERSION1** indicates return only
the fields specified in this proposal.  By using an enumerated type instead of a boolean, this allows for different
results to be specified in the future should the need arise and for different consumers to have specific field selections
encoded.

The BeanUtils library can be utilized for transforming properties

```
<dependency>
    <groupId>commons-beanutils</groupId>
    <artifactId>commons-beanutils</artifactId>
    <version>1.9.4</version>
</dependency>
```

A new class, `ResourceFieldSelector` located in **gov.cms.bfd.server.war.commons** exposes a public method

```
public enum FieldSelectorProfile {ALL, AB2D_VERSION1}

static public ExplanationOfBenefit selectFields(ExplanationOfBenefit eob, FieldSelectorProfile fsProfile)
```

will be invoked by both
`R4ExplanationOfBenefitsResourceProvider` and `ExplanationOfBenefitsResourceProvider` for additional post processing
of the fields upon the final step of EOB processing.

`FieldSelectorProfile` maintains a `private static final Map<String, SubfieldSelection>` of fields specifying the
selected ones to return to the client for each instance of the enum.

This method will be doing the bulk of the work for this RFC. The filtering will happen
in this method where each field in the EOBs are visited. Reflection will be used to gain the list of all 
fields in the object we are interested in (ExplanationOfBenefits). If the field matches a name in the allowed list it will 
be allowed to stay, otherwise it will be nulled out. Every field in the object will need to be visited so that we don't miss any
fields.

Fields that represent objects are recursed into, applying the same field selection approach.

```
    private enum WhichFields {ALL, EXPLICIT}

    private static class SubfieldSelection {
        final WhichFields pickMe;
        final Map<String, SubfieldSelection> explictFields;

        public SubfieldSelection() {
            this.pickMe = WhichFields.ALL;
            explictFields = Collections.emptyMap();
        }

        public SubfieldSelection(Map<String, SubfieldSelection> explictFields) {
            this.pickMe = WhichFields.EXPLICIT;
            this.explictFields = explictFields;
        }

        public void addField(String fieldName) {
            explictFields.put(fieldName, wildCard);
        }

        public WhichFields getPickMe() {
            return pickMe;
        }

        public Map<String, SubfieldSelection> getExplictFields() {
            return explictFields;
        }
    }

    private static final SubfieldSelection wildCard = new SubfieldSelection();

    private static final Map<String, SubfieldSelection> eobFieldSelection = new HashMap<>();

    static {
        eobFieldSelection.put("patient", wildCard);
        eobFieldSelection.put("provider", wildCard);
        eobFieldSelection.put("organization", wildCard);
        eobFieldSelection.put("facility", wildCard);
        eobFieldSelection.put("type", wildCard);
        eobFieldSelection.put("resourceType", wildCard);
        eobFieldSelection.put("diagnosis", wildCard);
        eobFieldSelection.put("procedure", wildCard);
        eobFieldSelection.put("item", buildItemDef());
        eobFieldSelection.put("careTeam", wildCard);
        eobFieldSelection.put("identifier", wildCard);
    }

    private static SubfieldSelection buildItemDef() {
        Map<String, SubfieldSelection> itemMap = new HashMap<>();
        itemMap.put("service", wildCard);
        itemMap.put("quantity", wildCard);
        itemMap.put("servicedPeriod", wildCard);
        itemMap.put("location", wildCard);
        itemMap.put("careTeamLinkId", wildCard);
        itemMap.put("sequence", wildCard);
        return new SubfieldSelection(itemMap);
    }
    
    // Entry point
    public static void filterEOBs(List<ExplanationOfBenefit> eobs) {
        for (ExplanationOfBenefit eob : eobs) {
            trimFields(eob, eobFieldSelection);
        }
    }

    private static void trimFields(Object objToTrim, Map<String, SubfieldSelection> fieldMapping) {
        Field[] fields = objToTrim.getClass().getDeclaredFields();
        for (Field field : fields) {
            // Do not return this field, null it out.
            if (!fieldMapping.containsKey(field.getName())) {
                PropertyUtils.setProperty(objToTrim, field.getName(), null);
                continue;
            }

            // Return the entire field in the results
            SubfieldSelection selection = fieldMapping.get(field.getName());
            if (selection.getPickMe() == WhichFields.ALL) {
                continue;
            }

            // Return a subset of the fields of the object referenced.
            trimFieldsThrowException(objToTrim, field, selection);
        }
    }

    private static void trimFieldsThrowException(Object objToTrim, Field field, SubfieldSelection selection) {
        try {
            trimFields(field.get(objToTrim), selection.getExplictFields());
        } catch (IllegalAccessException iae) {
            throw new RuntimeException(iae);
        }
    }
``` 

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

How much will this save us?

As part of a larger effort to reduce job times from days to minutes, AB2D is constructing a performance test harness
which will at its basic element, perform a single EOB retrieval.  Anecdotally, BFD EOB retrievals have been calibrated
in the 500 msec range.  This harness will allow a more deliberate measurement of the actual response times, independent
of the current AB2D architecture.  Once the basic harness is ready, before and after retrieval times can be obtained to
provide a firm answer to the question.

Additional goals for the harness are to assess how much can concurrency increase.  Currently, AB2D limits EOB
retrievals to 32 concurrent requests.  The plans are to run cooperative experiments to establish a maximum concurrent
limit.

The harness will make it much more convenient to characterize current performance and to confirm or disprove any
expected benefits of changes.

Proposed profiling is invoking BFD with at least 10,000 EOB Requests by Patient ID (ExplanationOfBenefit?patient=) and to measure
the aggregate time of completion of all calls.  In other words, sum up the time taken by each call over all calls.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

This solution incurs a nominal amount of extra processing to traverse the EOB object and null out fields that do not
need to be returned.

This solution adds AB2D specific code within the BFD source base.  Any new required fields results in changes to BFD
code.  The expectation is that this is rare and involves minimal effort.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

GraphQL is a notable alternative where the invocation explicitly specifies the requested fields.  Since GraphQL
supports introspection, the client can actively determine the available fields.  A hybrid approach using
*registered queries* requires queries to be specified in advance so that they can be locked down by a production server.

The downside is that this would be a significant amount of work for BFD to adopt a different API style while continuing
to support the REST calls.

An alternative for using a library like PropertyUtils would be JXPath, but initial benchmarks showed that PropertyUtils
was consistently faster.

## Future Possibilities
[Future Possibilities]: #future-possibilities

There could be dynamic projections on the database side as well. Right now there are 194 columns
in the beneficiary table, and we certainly do not need all of them, so retrieving only the needed columns
would reduce the load on the database.

Other groups would likely want to utilize filtering as well, but that is out of scope for this RFC.