* RFC Proposal ID: RFC0006 
* Status: First Draft
* Start Date: 2020-04-01
* RFC PR: 
* JIRA Ticket(s):
    
This RFC changes how the BFD handles requests for explanation of benefits (EOB). 
Internally, it uses a more efficient database query to fetch EOBs. 
For partners, it provides a way for BFD clients to request resources with lower timeouts and higher throughput.  

## Motivation
The latency of the BFD's EOB search is directly related to the number of claims a beneficiary has. 
Since the number of claims per beneficiary varies widely, a BFD client must set 10 to 30-second timeout on their EOB fetches to accommodate a large number of claims in the result set. 

Paging is the term given to the technique to split a single large result set into many smaller result sets. 
Since smaller result sets take less processing time, paging should imply lower request latency. 
The FHIR specification and the BFD API support paging. 
Unfortunately, the BFD's current paging algorithm is an inefficient implementation of offset paging, where the BFD data server queries for the entire result set and then discards the results outside of the requested page. 

The latency variability (also known as jitter) of a BFD EOB request leads to several problems: 
- As mentioned, it requires the BFD client to set long timeout values
- SLO and SLA latency metrics are difficult to specify because normal operations may take a long time to complete
- Request retries, usually a good practice in distributed systems, can lead to a very wasteful failure-mode where computationally expensive partial results are repeatedly discarded. 
- Jitter adversely affects the throughput of a system. 

## Proposed Solution
The proposal is to replace the current offset paging algorithm with a cursor-based algorithm. 
Internally, the BFD can implement cursor-based paging with an efficient seek query. 
From a database perspective, the work done is nearly constant for each request. 

### Client Perspective 
The proposal slightly changes the bundle resource returned by the BFD when a _count parameter is specified. 
The `total` field and the `last` and `prev` links are no longer present. 
The contents of the bundle's next link have a `cursor` parameter instead of a `startIndex` parameter. 
The proposal does not alter the EOB resources themselves. 
We expect clients to need little or no code change for this proposal if they are already using paging in their requests. 

Here an example request that requests results with paging.
```
$curl 'https://<host>/v1/fhir/ExplanationOfBenefit?
_format=application%2Fjson%2Bfhir&_count=10&patient=-19990000000001'
```

Here is a partial result with the current offset paging scheme. 

```
{
  "resourceType": "Bundle",
  "id": "6c027b21-67e6-480f-a515-78cb41d362aa",
  "meta": {
    "lastUpdated": "2020-03-31T19:37:08.147-04:00"
  },
  "type": "searchset",
  "total": 52,
  "link": [
    {
      "relation": "first",
      "url": "https://localhost:7443/v1/fhir/ExplanationOfBenefit?_format=application%2Fjson%2Bfhir&startIndex=0&_count=10&patient=-19990000000001"
    },
    {
      "relation": "next",
      "url": "https://localhost:7443/v1/fhir/ExplanationOfBenefit?_format=application%2Fjson%2Bfhir&startIndex=10&_count=10&patient=-19990000000001"
    },
    {
      "relation": "last",
      "url": "https://localhost:7443/v1/fhir/ExplanationOfBenefit?_format=application%2Fjson%2Bfhir&startIndex=50&_count=10&patient=-19990000000001"
    },
    {
      "relation": "self",
      "url": "https://localhost:7443/v1/fhir/ExplanationOfBenefit?_count=10&_format=application%2Fjson%2Bfhir&patient=-19990000000001"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "ExplanationOfBenefit",
        "id": "carrier-10344810963",
        "meta": {
          "lastUpdated": "2020-03-31T19:29:46.332-04:00"
        },
        "extension": [
          {
            "url": "https://bluebutton.cms.gov/resources/variables/prpayamt",
            "valueMoney": {
              "value": 0.00,
              "system": "urn:iso:std:iso:4217",
              "code": "USD"
            }
```

Here is the partial result for the same request when cursor paging is enabled. 

```
{
  "resourceType": "Bundle",
  "id": "a18ca539-b4bb-4385-956f-1f977acd4351",
  "meta": {
    "lastUpdated": "2020-03-31T19:37:08.147-04:00"
  },
  "type": "searchset",
  "link": [
    {
      "relation": "first",
      "url": "https://localhost:7443/v1/fhir/ExplanationOfBenefit?_format=application%2Fjson%2Bfhir&_count=10&patient=-19990000000001"
    },
    {
      "relation": "next",
      "url": "https://localhost:7443/v1/fhir/ExplanationOfBenefit?_format=application%2Fjson%2Bfhir&_count_=10&patient=-19990000000001&cursor=carrier_9324614917"
    },
    {
      "relation": "self",
      "url": "https://localhost:7443/v1/fhir/ExplanationOfBenefit?_format=application%2Fjson%2Bfhir&_count_=10&patient=-19990000000001"
    }
  ],
  "entry": [
    {
      "resource": {
        "resourceType": "ExplanationOfBenefit",
        "id": "carrier-10344810963",
        "meta": {
          "lastUpdated": "2020-03-31T19:29:46.332-04:00"
        },
```


### Experimental Results

A proof-of-concept implementation of the cursor paging was implemented and deployed to the BFD's TEST environment. 
The service was lightly loaded, but the test harness made requests in parallel with 4 workers.  The test fetched EOBs for a random 1000 synthetic beneficiaries for 5 minutes. 
Each request iterated over all the EOBs of a beneficiary. 
The results showed that cursor-based paging had significantly less latency variability. 

|               |	Mean  | P50   | P90   | P95   | P99   |
|---------------|-------|-------|-------|-------|-------|
| No Paging     | 145ms |	126ms	| 220ms | 263ms | 394ms |
| Offset Paging	| 91ms	| 86ms	| 117ms	| 132ms	| 155ms |
| Cursor Paging	| 24ms	| 17ms	| 58ms	| 63ms	| 73ms  |

### Rollout
If this RFC is adopted, there will be phased rollout of cursor-based paging.

1. Cursor based paging is experiemental. Both offset paging and cursor paging are supported. Default is offset paging. Cursor based paging is used when an optional `cursor` parameter is present. 
2. Cursor based paging is the default. Offset is still supported but its use is depricated.  
3. Cursor based paging is the only type of paging supported. Offset based paging is removed.  


## Prior Art
The proposal is base on a talk by Markus Winand’s “Pagination Done the PostgreSQL Way”. 
The slide deck is easy to follow and recommended for engineers who want to understand more about the seek technique. 

## Future Work
The BFD's team goals to improve latency, capacity, and throughput continue. 
Significant areas for improvement remain. 
This proposal improves the BFD's database queries but does not address other bottlenecks like JSON serialization. 
With cursor paging, testing reveals much fewer latency spikes. Nevertheless, some spikes remain and require further investigations. 

## References

1. ["Pagination Done the PostgreSQL Way"](https://wiki.postgresql.org/wiki/File:Pagination_Done_the_PostgreSQL_Way.pdf) by Markus Winands. 
2. ["Experimental Paging"](https://github.com/CMSgov/beneficiary-fhir-data/tree/rick/paging-experiment) Cursor paging POC implementation
3. ["BFD Server Performance, Part 1: Initial Investigations"](https://confluence.cms.gov/display/BB/2020/02/07/BFD+Server+Performance%2C+Part+1%3A+Initial+Investigations) by Karl Davis
4. ["BFD Server Performance, Part 2: Bottlenecks"](https://confluence.cms.gov/display/BB/2020/02/07/BFD+Server+Performance%2C+Part+2%3A+Bottlenecks) by Karl Davis