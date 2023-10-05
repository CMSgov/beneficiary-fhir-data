# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0020-rotate-hash-peppers`
* Start Date: 2023-08-21
* RFC PR: [beneficiary-fhir-data/rfcs#0020](https://github.com/CMSgov/beneficiary-fhir-data/pull/1642)
* JIRA Ticket(s):
    * [BFD-1110](https://jira.cms.gov/browse/BFD-1110)

BFD maintains hashed MBI and hashed HICN values to facilitate FHIR patient search for a beneficiary; every beneficiary's hashed values are persisted in the BFD database and can handle both current as well as historical MBI and HICN values. 

The hashing algorithm, and all criteria necessary to create a matching hashed value, is shared with peering partners; this allows them to construct obfuscated MBI or HICN hash values that can then be safely used as an HTTP GET URL parameter(s).

This RFC proposes a solution that will eliminate the necessity for supporting a shared hash while still complying with the FHIR specification for _patient search_. 
## Status
[Status]: #status

* Status: Approved <!-- (Proposed/Approved/Rejected/Implemented) -->
* Implementation JIRA Ticket(s):
    * [BFD-1110](https://jira.cms.gov/browse/BFD-1110)

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Status](#status)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Background](#background)
  * [Current Patient Search](#current-patient-search)
  * [BFD Hash Algorithm](#bfd-hash-algorithm)
  * [Security Concerns Leaked Algorithm](#security-concerns-leaked-algorithm)
* [Proposed Solution](#proposed-solution)
    * [Proposed Solution: Detailed Design](#proposed-solution-detailed-design)
    * [Proposed Solution: Unresolved Questions](#proposed-solution-unresolved-questions)
    * [Proposed Solution: Drawbacks](#proposed-solution-drawbacks)
    * [Proposed Solution: Notable Alternatives](#proposed-solution-notable-alternatives)
       * [Status Quo](#status-quo)
       * [Hot Stand-by Option 1](#hot-standby-1)
       * [Hot Stand-by Option 2](#hot-standby-2)
       * [Asymmetric Crypto](#asymmetric-crypto)
* [Prior Art](#prior-art)
* [Future Possibilities](#future-possibilities)
* [Addenda](#addenda)
* [Definitions](#definitions)

## Motivation
[Motivation]: #motivation
BFD provides a FHIR (STU3, R4) patient search service in which a Peering Partner (PP) can lookup a patient using either a patient's Medicare Beneficiary ID (MBI) or the less used patient Health Insurance Claim Number (HICN).

The current BFD patient search service is implemented as an HTTP GET which requires an identifier, such as a patient's MBI or HICN, to be obfuscated prior to being used as a URL GET parameter. To achieve this, a level of synchronicity must be maintained between a PP and BFD; in a nutshell, BFD shares a hashing algorithm, including seed value and iteration count, with PPs such that a PP can create/embed a hashed value in a URL, which can then be used by the BFD service to search for a match in the database. This symmetric key/algorithm sharing, if compromised, creates a potential attack vector for gaining access to a patient's PII/PHI data.

This RFC then explores ways to preserve the patient search functionality in a manner that minimizes, or completely eliminates BFD services downtime, especially in a leaked key (algorithm) scenario.

## Background
[Background]: #background
### Current Patient Search
[Current Patient Search]: #current_patient_search
Prior to delving into proposed strategies or solutions, it is best to provide some context on how things currently work, and the challenges and constraints BFD currently faces.
- BFD maintains a hashing algothim that is used to create a hashed value of a patient's MBI and/or HICN.
- BFD shares with its peering partners all components of the hashing algorithm, including the base algorithm, a seed value (salt) and an iteration count.
- PPs request patient information by invoking a BFD _patient search_ RESTful service (HTTP GET) that requires an obfuscated (hashed) patient identifier as a URL parameter.
- BFD service extracts the provided patient identifier and performs one or more database lookups to derive the patients primary key identifier (BENE_ID).
  - it first checks the _BENEFICIARIES_HISTORY_ table for a match (either MBI_HASH or BENE_CRNT_HIC_NUM); found value(s) are added to an internal list.
  - it then checks the _BENEFICIARIES_ table for a match (either MBI_HASH or BENE_CRNT_HIC_NUM); found value(s) are added to a list. The _BENEFICIARIES_ table holds current value for a hashed MBI and a hashed HICN value.
  - the list of hashed values is then traversed resulting in either a hit (code now has a BENE_ID) or no match (HTTP search returns NOT FOUND)
- Maintaining hashed MBI and HICN data represents functionality distinct limited to:
  - a single BFD service, _patient search_ (HTTP GET). 
  - PACA lookups, (only hashed MBI value)
### BFD Hash Algorithm
[BFD Hash Algorithm]: #bfd_hash_algorithm
BFD uses a cryptographically sound, one-way hashing algorithm, that leverages additional best practices to produce an obfuscated base-64 encoded character string. The entire hash-generation process is shared amongst all BFD peering partners, allowing each PP to dynamically generate an obfuscated patient identifier suitable for a patient lookup. For example, a PP could take an MBI value, hash that, and then request information on that patient using the hashed MBI. Because BFD defines, and subsequently shares all aspects of the hashing algorithm, this could be deemed a security issue if the algorithm is leaked.
### Security Concerns Leaked Algorithm
[Security Concerns for a Leaked Algorithm]: #security-concerns-leaked-algorithm
Assuming BFD encounters the situation where the hashing algorithm is leaked, how serious would this be?
* BFD is a closed system, requiring mTLS authentication; essentially BFD has a known set of clients (peering partners) that use an X509 certificate to connect to BFD. BFD server checks that client certificate vs. a known list of certificates that reside in the BFD server _trust store_; so a client must be a known entity just to connect.
* While all BFD clients are known, BFD does not support role-based authentication or authorization; this means that all clients have equal access to any beneficiary info regardless if that client really has no business accessing a given beneficiary's data.
* Hashed patient identifiers are simply one way to _search for_ patient information; but there are other paths to achieve the same. For example, BFD supports the fetching of BENE_ID values based on a Part D contract identifier; those part D contract IDs are not considered PII/PHI data, so a simple request to fetch all patients for a contract ID, yields a list of identifiers (BENE_ID) that are even more useful than a hashed MBI value. With a BENE_ID in hand, pretty much any and all BFD data for a patient could be harvested.
* The BFD hashing algorithm is made up of the base algorithm, a _salt_ value (referred to as _pepper_ in BFD parlance), and an iteration count. A cleartext _key_, in this case a patient identifier (i.e., MBI), is fed into the alg, resulting in a hashed value; so in order to create the hashed identifier, a bad actor would need to know a patient's MBI. So if they already know the MBI value, and all the hashing does is obfuscate that MBI value, then a leaked algorithm, accessing a closed system that is restricted to known peering partners, may not be that big a problem.
* Where the hash leak does become relevant, is if an attacker knows a patient MBI and the hashing algorithm; in that case they can generate a hashed MBI and pass that to search services that require a hashed MBI; that is if they can even connect to BFD (see item 2 above). However, even that scenario would fall into the _merely interesting_ scenario; if the attacker has access to log files, they could simply just scrape all log entries that have an MBI hash in the URL, and just replay those URL(s), again assuming they can get access to BFD services.
* How about the scenario where the _bad actor_ has access to both BFD log entries and BFD's hashing algorithm; using that knowledge, one could try a _brute force_ attack by controlled generation of an MBI (see https://www.cms.gov/medicare/new-medicare-card/understanding-the-mbi-with-format.pdf for the structure of an MBI) until the generated hash MBI matches one from the log file(s). While mathematically challenging to say the least, it is hypothetically possible.
### Mitigation of a Leaked Hash Algorithm
BFD has never encountered the situation where a leaked component of the hashing alogrithm would require mitigation; there have been some muted table-top and/or game-day exercises, but nothing concrete and for real. Speculating on some back-of-the-napkin tasks that would need to be completed in the current environment for such a scenario:
- incident declared denoting hashing algorithm is compromised; depending on the severity of the leak (i.e., how long has it been compromised, etc.) may dictate the severity of the incident.
- BFD services may not need to be taken offline; the hashing algorithm is used by a couple of BFD services; those service calls takes an MBI or HICN hash parameter; for example (cURL syntax):
```
identifier="https://bluebutton.cms.gov/resources/identifier/hicn-hash|6026b9c1f00ba3937a8876f909c1c0d806d36229111e030ab867a8fc91792851"
or
identifier="https://bluebutton.cms.gov/resources/identifier/mbi-hash|1425547895d49fdf40d4128ec03cfd53c6ca2a67af9f7adcc2845a00cf6a5af5"
```
- need to change components of the (leaked) hashing algorithm; the changes would most likely be constrained to the seed and/or iteration count. The new hash algorithm components would be persisted in the BFD SSM parameter store.
- peering partners need to be alerted to the issue and pending hashing mitigation plan; changing the algorithm will require communicating the new hashing algorithm via an agreed upon secure channel. Since this affects all PPs, some amount of collaboration and synchronization of PPs will need to be implemented.
- need to update all records in the _BENEFICIARIES_ table (67M+ records) with new MBI and HICN hash values using the newly minted hashing algorithm being applied to the bene's plaintext MBI and HICN table columns. The hashing of a field is compute intensive and time consuming; since both hashed columns are also indexed in the table, the database update operation will take some time. There are database optimization techniques we can employ for this operation (i.e., drop indexes, perform updates, rebuild indexes) but we have no prior work that defines best practices for this type of update.
- The above database update presents an interesting conundrum; in theory, BFD should have in its toolchest, a simple program (script) that reads/updates the hashed identifiers in every record in the _BENEFICIARIES_ table; in addition a runbook would exist denoting best practices for an operation of this kind; neither exists.
- once the 67+ million _BENEFICIARIES_ records have been updated and indexed, BFD could perform a deployment with the new hashing algorithm at the ready and the database fully compliant with the hashing changes.
- Peering Partners would then need to modify how they construct (or cache) their instance of the BFD hashing algorithm. Once deployed, BFD could return servicing requests and it would be up to each PP to implement their necessary changes to comply with the new hashing algorithm.
- Weekly ETL processing
  - ETL performs hashing of MBI and HICN value(s) at time of INSERT into _BENEFICIARIES_ table.
  - ETL performs re-hashing of MBI and HICN value(s) when detecting a changed MBI or HICN value during an UPDATE to a current _BENEFICIARIES_ table record.
## Proposed Solution
[Proposed Solution]: #proposed-solution

The necessity for maintaining hashed MBI and HICN values is predicated on supporting a couple of BFD services, implemented as HTTP GET(s), which require parameter values to be an integral componment of the URL. This means, that any patient identifier (MBI and/or HICN) raw values must be obfuscated since URLs are logged for every BFD request; those logs then become accessible in multiple downstream outlets such as AWS CloudWatch and Splunk.

The proposed solution is to replace the current HTTP GET _patient search_, which requires a hashed identifier as a URL parameter, with an HTTP POST, in which all parameters can be encapsulated within the POST body. Since the POST body will be protected by TLS security, by definition, only the client who constructed the POST body, and BFD will have access to elements that may contain PII/PHI information.

The FHIR spec (STU3, R4, R5) for a patient search, supports both GET and POST operations (see https://hl7.org/fhir/STU3/search.html).
- GET  [base]/[type]?name=value&...{&_format=[mime-type]}}
   - parameters are a series of name=[value] pairs encoded in the URL (GET)
- POST [base]/[type]/_search{?[parameters]{&_format=[mime-type]}}
   - parameters are a series of name=[value] pairs encoded as an application/x-www-form-urlencoded submission 

Since all BFD services are protected by TLS, changing a service from a GET to a POST operation, by definition, precludes visibility to any data encoded in the POST body. In effect, this removes entirely the necessity to even support/maintain hash values for MBI and HICN. Additional benefits may also be realized with AWS RDS cost savings as well as some small performance gains for the BFD's ETL Pipeline.

From the client (peering partner) perspective, this would streamline service requests that required a hashed identifier as a URL parameter, by no longer requiring calculation of a hashed value prior to invoking a service.
### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Application design:

* Plan a staged migration away from supporting HTTP GET for all services that currently require a hashed patient identifier by supporting both HTTP GET and HTTP POST operations for some finite period (TBD) agreed upong with peering partners.
* The new BFD POST operation must comply with the FHIR Specification.
* This work will need to be done in support of both STU3 (BFD v1) and R4 (BFD v2) services that currently support hashed identifier(s).
* The current _patient search_ is implemented in the following BFD Classes:
  - STU3 : PatientResourceProvider.java
  - R4   : R4patientResourceProvider.java
* Both STU3 and R4 of the current implementaion effectively operate in the same fashion:
  a) validate the input parameters; throw exceptions when appropriate.
  b) check the _LoadedFilterManager_ to see if there is even a chance that the request can be satisifed.
  c) invoke utility method that creates a FHIR Bundle as the response.
* The plan would be to abstract the current _Step B_ and _Step C_ above into a private method(s) that could be shared by both the GET and POST operations.
* Implement a new POST service endpoint that closely mimics current GET parameter validation and then invokes the common functionality for  _Step b_ and _Step c_.
performs _Step a_.
* Modify the current HTTP GET to only support _Step a_ and invoke the shared methods for _Step b_ and _Step c_.
* Current BFD logging and metrics capture may be impacted since logging will no longer provide unique patient identifier for the patient search service. If deemed necessary, a Jetty logging interceptor would need to be developed that logs the request and its parameters in such a way as to obfuscate any PII data.
* In addition to actual service code changes, the LOE will need to accomodate unit, end-to-end, and integration testing. In additition, all documentation artifacts should be checked for and changes made as needed.
* The immediate aftermath of a deployment will probably manifest itself as:
  *  peering partners will continue to use the HTTP GET _patient search_.
  * BFD will publish an end-of-life plan for removing support for the HTTP GET; this gives peering partners a clear timeframe for when they need to implement their changes to use the new HTTP POST.
### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Can we remove support for HICN lookups since no peering partners currently use HICN and have not done so for some time.

Is there a better way to log sensitive (PII,PHI) information?

The RDA (PACA) component within BFD currently depends on hashed MBI_NUM for some of its services; can a _one-size fits all_ strategy of HTTP POST operated for all aspects of BFD.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

Adoption should not require extensive engineering work by either BFD or peering partners; any work required of peering partners represents some risk to adoption. 

While the plan initially supports both GET and POST operations, at some point BFD will want to remove support for GET, which creates a deadline for clients to change their implementation.

BFD will need to resolve how logging of POST requests will be handled.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

This section explores some alternate potential strategies for maintaining and/or mitigating a leaked hash algorithm.
#### Status Quo
[Status Quo]: #status-quo

Currently, if BFD needs to change the hashing algorithm, it may require some sort of maintenance window and/or minor performance degradation. The current data and the services logic is not capable of handling more than a single instance of the hashing algorithm and a single tuple of a hashed MBI and hashed HICN within each patient record (_BENEFICIARIES_ table). However, there are some simple mechanisms that could be used to at least minimize any downtime.

Since we have no existing _Runbook_ and have no significant work specific to replacing the hash algorithm, the following (rough) pseudo-code steps might be executed in an emergency situation:

- define a new hash algorithm; most likely modify salt (seed value) and/or iteration count (or both). The algorithm construct will need to be communicated to all peering partners in a secure message exchange prior to plan execution and subsequent synchronization of a _go-live_ date at which time BFD would only support newly updated hash values.

- create a new instance (a copy) of the _BENEFICIARIES_ table; say _BENEFICIARIES_PREHASH_; for example, in a flyway script (or possibly out-of-band):
```
CREATE BENEFICIARIES_PREHASH AS
  SELECT * FROM BENEFICIARIES;
```
- create and execute a script (python? SQL?) that would:
  - sequentially read/update each record in the new table
  - for each record, calculate new hash value for _MBI_HASH_ and _BENE_CRNT_HIC_NUM_ by hashing respective cleartext columns:
     - hash _MBI_NUM_ to create _MBI_HASH_ value
     - hash _HICN_UNHASHED_ to create _BENE_CRNT_HIC_NUM_ 
  - update (persist) the record
  - create index for _MBI_HASH_; TBD if we even want/need to create index on _BENE_CRNT_HIC_NUM_.
```
CREATE INDEX IF NOT EXISTS beneficiaries_prehash_mbi_idx
    ON BENEFICIARIES_PREHASH
    (mbi_hash);

// If we need support for hashed HICN
CREATE INDEX IF NOT EXISTS beneficiaries_prehash_hicn_idx
    ON BENEFICIARIES_PREHASH
    (bene_crnt_hic_num);
```
- alert peering partners of scheduled (date-time) maintenance window, at which time the new hash values will be enabled in each environment:
  - Perform database table rename operations:
```
alter table BENEFICIARIES rename to BENEFICIARIES_ORIG;
alter table BENEFICIARIES_PREHASH rename to BENEFICIARIES;
```
  - Perform database indices rename operations:
```
ALTER INDEX beneficiaries_mbi_idx
  RENAME TO beneficiaries_orig_mbi_idx;

ALTER INDEX beneficiaries_hicn_idx
  RENAME TO beneficiaries_orig_hicn_idx;

// rename indices we originally created in the _prehash_ table
ALTER INDEX beneficiaries_prehash_mbi_idx
  RENAME TO beneficiaries_mbi_idx;

// if we need continued support hashed HICN lookups
ALTER INDEX beneficiaries_prehash_hicn_idx
  RENAME TO beneficiaries_hicn_idx;
```
- SQL renames are generally extremely fast so the above ALTER statements would complete in seconds.
- Once db changes are complete, the BFD database is effectively operational but a deployment would still be required to ensure ETL is updated with the new hash algorithm.
- Peering partners requiring patient search would remain _broken_ until they have synchronized their codebase/processing with the updated hashing algorithm.
- If the PP is not using the latest hashing algorithm, and BFD has updated its database and algorithm, or vice versa the PP has updated its hashing algorithm but BFD has not yet finished updating the  _BENEFICIARIES_ table, then _patient search_ requests will return an empty result Bundle.
- Some permutations of the above UPDATE logic/processing steps, for which BFD does have prior work, is to wrap the _BENEFICIARIES_ table in an updatable db VIEW, and then toggle between two _BENEFICIARIES_ tables simply by (re-) pointing the VIEW as needed.

**NOTE** - while this document may reference database tables, columns, indexes, etc. it should be noted that any processing changes that affect a database, needs to occur in three distinct environments: _PROD_, _PROD-SBX_ and _TEST_.
##### Benefits
- minor impact on BFD; basically the time to create/update the _new_ _BENEFICIARIES_PREHASH_ table (or some similar approach) and build indexes on hashed columns.
- minor impact to PP; they will need to modify their pre-request URL construction to use the new hash algorithm.
- minor impact to ETL processing (depends on when the operation needs to be implemented).
- since not operating on a current production table (_BENEFICIARIES_), db optimization techniques could be applied without impacting current production.
- bordering on the _do nothing_, but certainly KISS (keep it simple...)
##### Drawbacks
- while not quite _zero downtime_ this tack is the closest what could be achieved based on current capabilities; does not represent a significant level of effort to implement.
- will need write script (i.e., python, SQL) that can re-hash records and update db table.
#### Hot Stand-by Option 1
[Hot Stand-by Option 1]: #hot-standby-1

The premise behind a _hot stand-by_ is to have BFD actively support multiple concurrent hash algorithms and associated _MBI_HASH_ and _BENE_CRNT_HIC_NUM_ values specific to each hash algorithm that was used to generate the hash values.

This set of _shadow_ values for MBI and HICN could be accomplished solely within the _BENEFICIARIES_ data (i.e., maintain two sets of hashed values). Since the bulk of the (performance) cost for a hash value is the hashing operation itself, we could probably not worry about indexing the new fields; they exist as already calculated hash values which will be plugged into the the real hash columns (_MBI_HASH_ and _BENE_CRNT_HIC_NUM_) with a single SQL update script as needed.

 A rough outline of functionality might look like:
```
ALTER TABLE BENEFICIARIES
   ADD COLUMN mbi_hash_standby CHARACTER VARYING(64),
   ADD COLUMN bene_crnt_hic_num_standby CHARACTER VARYING(64);
```
- the _BENEFICIARIES_ table thus contains 2 sets of MBI and HICN data columns
  - MBI_HASH
  - BENE_CRNT_HIC_NUM
  - MBI_HASH_STANDBY
  - BENE_CRNT_HIC_NUM_STANDBY
- some sort of script (python, SQL) could then update the _STANDBY columns using the new hash algorithm.
- when necessary to make the _STANDBY values _active_; a SQL script could be executed to do that; something like:
```
UPDATE BENEFICIARIES SET
  mbi_hash = mbi_hash_standby,
  bene_crnt_hic_num = bene_crnt_hic_num_standby
    WHERE bene_id EXISTS
      AND MBI_HASH_STANDBY IS NOT NULL;
```
##### Benefits
- minimal changes to current processing
  - standby columns need to be added to ORM
  - some script work (python or SQL)
- minor impact to PP; they will need to modify their pre-request URL construction to use the new hash algorithm so it would be presumptuous to consider this a _zero downtime_ solution.
##### Drawbacks
- same table UPDATE, INDEXES introduces some challenges; may need to re-build index concurrently which can be slower than having table locked for indexing which is feasible, as noted in the prior use case using a separate _BENEFICIARIES_PREHASH_ table.
- maintaining data over time (i.e., INSERTs or UPDATEs from weekly ETL) needs to be considered; changes to ETL may be non-trivial.
- may also require some AWS (SSM) work
- will need script (i.e., flyway, python, SQL) that can populate new table columns; possibly custom maven build artifact.
#### Hot Stand-by Option 2
[Hot Stand-by Option 2]: #hot-standby-2

Similar to Option 1, but maintain the _shadow_ values for MBI and HICN in a (new) separate table.

 A rough outline of functionality might look like:
- SSM parameter store upgraded to support multiple hash algorithm instances:
  - SSM hash alg key
  - SSM hash alg salt (seed value)
  - SSM hash alg iteration count
  - SSM hash alg status (active, future, disabled)
- create table for storing concurrent beneficiary hash values; a sample table structure might look like:
```
CREATE TABLE IF NOT EXISTS hashed_beneficiaries
(
    id                bigint NOT NULL,
    ssm_param_id      character varying(32) NOT NULL,
    bene_id           bigint NOT NULL,
    bene_crnt_hic_num character varying(64) NOT NULL,
    mbi_hash          character varying(64),
    efctv_end_dt      date,
    CONSTRAINT hashed_beneficiaries_pkey PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS hashed_beneficiaries_ssm_bene_id_idx
    ON hashed_beneficiaries USING btree (ssm_param_id, bene_id);

CREATE INDEX IF NOT EXISTS hashed_beneficiaries_mbi_hash_idx
    ON hashed_beneficiaries USING btree (mbi_hash);
    
CREATE INDEX IF NOT EXISTS hashed_beneficiaries_crnt_hic_num_idx
    ON hashed_beneficiaries USING btree (bene_crnt_hic_num);
```
- the table could hold multiple records per beneficiary, thereby supporting multiple hash algorithms and associated values for _MBI_NUM_ and _BENE_CRNT_HIC_NUM_
- Depending on usage analysis, a db constraint could be defined such that a relationship exists linking the new table to the _BENEFICIARIES_ table; this relationship would allow for _BENEFICIARIES_ data to be fetched as part of a fetch of a given _HASHED_BENEFICIARIES_ record.
  - records with a non-null _EFCTV_END_DT_ could be used to filter out only those records that are still marked as active.
##### Benefits
- would allow BFD services to minimize downtime by having new hash algorithm and associated hashed fields at the ready. 
- minor impact to PP; they will need to modify their pre-request URL construction to use the new hash algorithm so it would be erroneous to consider this a _zero downtime_ solution.
- this option might be more acceptable if the necessity for changing the hashing algorithm becomes a constant timely operation (i.e., we need to change the hashing alg every 90 days).
##### Drawbacks
- LOE for implementing db schema changes (new table).
- maintaining data over time (i.e., INSERTs or UPDATEs from weekly ETL)
- will need some AWS (SSM) work
- will need script (i.e., python) that can populate new db table; probably custom maven build artifact.
#### Asymmetric Cryptography
[Asymmetric Crypto]: #asymmetric-crypto

As noted previously, distributing a shared hashing algorithm to peering partners is effectively implementing symmetric crypto; so while BFD can take great care in protecting the hashing algorithm, we do not maintain exclusive access over it. So while we have faith in our clients (peering partners) protecting the algorithm, we effectively have minimal control over how it is handled (protected) once it leaves our domain.

If BFD wishes to maintain total control over hashing of URL GET parameters, we could adopt asymmetric crypto for creating the hash value(s). Instead of _sharing_ a secret (the algorithm), we use a public-private keypair to dynamically encrypt/decrypt sensitive URL parameters.

BFD creates a public-private keypair; it then distributes the public key to each of its peering partners. Each PP then uses their copy of the public key to encrypt a plaintext MBI or HICN; the encrypted value is then passed to the BFD _patient search_ as a URL GET parameter. The parameter is fully obfuscated so any public access (logging, TCP snooping, etc.) is limited to just that...a URL that has an encrypted content and the only way to discern the view the sensitive parameter is to decrypt it using BFD's private key.
##### Benefits
- can still support HTTP GET URLs; current logging continues as is, logging the URL.
- extensible choice of algorithms and key sizes make this the safest way to protect data.
- wide range of algorithms and keysizes make this extremely extensible and bullet-proof.
- already using asymmetric in our TLS implementation.
- completely removes necessity to have/maintain hash algorithms and hashed data values; completely removes necessity to store hashed data (_MBI_HASH_ and _BENE_CRNT_HIC_NUM_ values); lookups now occur vs. actual _MBI_NUM_ or HICN_UNHASHED_ value.
##### Drawbacks
- slight overkill for what BFD needs.
- feeling performance will take a hit, but this can be mitigated by using Elliptical Curve Cryptograph (ECC) which provides better performance by using smaller keysizes which are just as resilient as RSA that depends on larger keysizes.
- small learning curve for some devs, but BFD implementation would not be complex.


## Prior Art
[Prior Art]: #prior-art
##### FHIR Specification for HTTP POST
While BFD has no prior work replacing HTTP GET with HTTP POST, the FHIR specification is clear on how this would be implemented.

##### RDA MBI Cache Table
RDA (PACA) has implemented a hashed MBI cache table to their database schema which handles current and previous (old) MBI hash value.

This cache table would need to be updated in a separate process, but does allow for concurrent querying while the update process is running. This is to allow the server to continue serving requests with the old hash value(s) while a background process is updating the new hash values in the table.

For the instance where changing the hashing algorithm would be required, the remediation process would be:

* Stop the pipeline
* UPDATE rda.mbi_cache SET old_hash=hash
* Restart the _BFD server_ with **-DPacOldMbiHashEnabled=true**
* In a background process scan the _MBI_CACHE_ table and set the hash column to the new hash value.
* Once all updates have been completed restart the pipeline.
* At a point when all clients have begun using the new hash algorithm, the _BFD Server_ be can restarted with **-DPacOldMbiHashEnabled=false**.
* Remove support for old hash values; UPDATE _MBI_CACHE_ SET old_hash=null.

## Future Possibilities
[Future Possibilities]: #future-possibilities
Introducing FHIR POST services, provides a clear path for future support of sensitive data parameters.
## Addenda
[Addendums]: #addendums
The following addenda are recommended reading or additional information pertinent to voting on this proposal:
* [FHIR Patient Search](https://hl7.org/fhir/STU3/search.html#2.21.1.2)

* The _New Medicare Card Project_ was established in the _Medicare Access and CHIP Reauthorization Act (MACRA) of 2015_ which mandates the removal of the Social Security Number-based Health Insurance Claim Number (HICN) from Medicare cards by April, 2019. Beginning in January 2020, providers may only use MBIs, with very limited exception.

* [Medicare Beneficiary Identifier](https://www.cms.gov/medicare/new-medicare-card/understanding-the-mbi-with-format.pdf )

* Peering Partner (PP) Usage - Patient Search
 The following list of _peering partners_ provides information as to if and how a PP may be affected by changes to the BFD hash algorithm:
 * AB2D - do not use hashed MBI or HICN; only BENE_ID
 * BCDA
   - dynamically calculate the hashed MBI when building the URL using the hash pepper and iterations (environment variables)
   - may need code changes (if the alg changes) and may require some maintenance window to switch over to the new seed/iteration count.
   - Don't call _patient search_ all that often; only when they want to confirm they have right patient. 
 * DPC
   - Only use hashed MBI; never HICN
   - have a _hashed MBI_ field in their attribution database, but it's not being used.
   - calculate hash in realtime as each call is made.
   - hash algorithm is abstracted away pretty well; If need to change anything it should only be a few lines of code in one place, so not a big impact.
 * BB2
   - dynamically calculate the hash values for MBI to be used to match an enrollee via the Patient search.
   - seed/iteration settings are loaded when the server is deployed; need downtime and a re-deployment to switch those values out.
   - they recall updating their codebase to handle cases where cleartext MBI or HICN values change for enrollees that are already in the _BB2 Crosswalk_ table.
   - Previous to doing this, occasionally run into cases where enrollee MBI would get changed and auths would fail for them.
   - Currently new hashes that match in a patient search will get replaced when the enrollee re-authorizes
   - used HICN lookups sporadically (when MBI failed); mainly limited to synthetic bene(s);

## Definitions
[Definitions]: #definitions
* TLS
 Transport Layer Security, and its predecessor, Secure Sockets Layer (SSL), both frequently referred to as SSL, are cryptographic protocols that provide communications security over a computer network; the connection is private (or secure) because symmetric cryptography is used to encrypt the data transmitted.
* Peering Partner (PP)
 A Client of BFD services is commonly referred to as _peering partner_. BFD currently supports the following PPs:
  * AB2D
  * BCDA
  * DPC
  * BB2
* Symmetric Cryptography
 Symmetric cryptography is a system of encryption where the same key is used to both encrypt and decrypt data. It is generally faster than asymmetric cryptography but is generally **considered less secure since the same key must be shared among all users**.
* Asymmetric Cryptography
 Asymmetric cryptography is a type of encryption that uses two different keys to encrypt and decrypt data. Using a public-private keypair, anyone with the public key can send an encrypted message to the owner of the private key, who can then use the associated private key to decrypt it. The reciprocal is also true; private key holder can create an encrypted message, that holders of the public key can decrypt.
