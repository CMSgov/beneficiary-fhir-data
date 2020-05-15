# Introduction
In the initial Insights design, there are three tiers of tables:
- Raw - Raw data imported from specific projects. Semi-structured
- Core - Structured DASG-tables that consolidate all 
- Marts - Per project summaries of the core and raw tables 

In a grandiose sense, these tiers are examples of the data lake, the data warehouse, and data mart architectures. This note discusses early thoughts on the design of the core tables. 

## Purpose
DASG is a family of projects which provide CMS data to a variety of data consumers at a high level. The core tables provide a DASG-wide view of the activities of all DASG projects. The core tables have to combine and generalize the records from each project. 

Some common use-cases drive the design of the core tables: 
- The ability to tie a release of data to all the software that impacted the release of data.
- The ability to present DASG with standard metrics for each project
- The ability to give to a beneficiary a report about accesses to their CMS data by DASG

## Common Concepts
For this discussion, here are few terms:
- Entity - The information that is referenced by an event. A typical entity is an FHIR resource for a specific beneficiary 
- Agent - The external software that receives data from DASG
- Source - The DASG software that generates an event
- Organization - The external entity that runs the agents that receive CMS data. An Agent is a piece of software, while Organizations are people and legal entities. 

## Prior Work
Two standards inspire the design of the core tables:
- FHRI Audit Events - Provide a very general way to express audit events. FHIR Audit Events build on previous work done on auditing. 
- OpenTracing - The OpenTracing standard builds on the work of Zipkin and other distributed tracing standards. 

In essence, the core table design is a combination of these two standards molded to be specific to the DASG use-case. 

## Events Table
The events table has been designed to combine both FHIR Audit Event and the OpenTracing standards. 

| category    | name               | Type               | Comments                                              | Required                 | FHIR               | OpenTracing           |
|-------------|--------------------|--------------------|-------------------------------------------------------|--------------------------|--------------------|-----------------------|
| span        | id                 | UUID               | event id                                              | yes                      |                    |                       |
|             | other_ids          | List(UUID)         | aka ids                                               | no                       |                    |                       |
|             | child_of           | UUID               |                                                       | no                       |                    | childOf reference     |
|             | follows_from       | List(UUID)         |                                                       | no                       |                    | followsFrom reference |
|             | period             | Timestamp          | UTC                                                   | no                       | period             | span                  |
|             | recorded           | Timestamp          | UTC                                                   | yes                      |                    |                       |
| operation   | type               | String             | Details below                                         | yes                      | type, subtype      | operation_type        |
|             | mutation           | String             | CRUD + E                                              | yes                      | action             |                       |
| result      | error              | Boolean            |                                                       | yes                      |                    | error                 |
|             | status_code        | Number             |                                                       | yes                      | outcome            | standard tag          |
|             | status_description | String             |                                                       | yes                      | outcomeDescription | standard tag          |
| agent       | agent              | String             | type                                                  | yes, if agent is present |                    |                       |
|             | organization       | String             |                                                       |                          |                    |                       |
|             | client_id          | UUID               | ID not value                                          | yes, if agent is present |                    |                       |
|             | security_token_id  | String             |                                                       | yes, if agent is present |                    |                       |
|             | ip_addr            | String             |                                                       |                          |                    |                       |
| source      | component          | String             | Component that records the event.                     | yes                      |                    | component             |
|             | vpc                | String             | env or other                                          |                          |                    |                       |
|             | instance_id        | String             | AWS instance id                                       | yes                      |                    |                       |
| diagnostics | mdc                | map(String,String) | standard open tracing tags                            | no                       |                    |                       |
| entity      | type               | String             | List of EOB,Coverage,Patient,Roster                   | yes                      |                    |                       |
|             | entity_id          | UUID               |                                                       | no                       |                    |                       |
|             | bene_id            | bene_id            |                                                       | no                       |                    |                       |
|             | mbi_hash           | String             |                                                       | no                       |                    |                       |
|             | hicn_hash          | String             |                                                       | no                       |                    |                       |
|             | bene_list          | String             | Reference to a list when                              | no                       |                    |                       |
|             | security_label     | String             | PHI,PII                                               | yes                      | security_label     |                       |
|             | name               | String             | Use only when a identifier is not available           | no                       | enity.name         |                       |
|             | description        | String             | Use only when a identifier is not available           | no                       | entity.description |                       |

## Other Tables
### Beneficiary Table
There will most likely be several bene tables: a cross-walk table that correlates different beneficiary identifiers; a lookup table with demographics enrich reports; a table to calculate DPC's BAC. 
### Agents Table
A list of all software that receives DASG data. 
### Sources Table
A list of all the software that runs in DASG systems. 
### Organizations Table
A common table with entries for all organizations.