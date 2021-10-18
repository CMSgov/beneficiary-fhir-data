# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-rda-claims-jsonb`
* Start Date: 2021-10-18
* RFC PR: [CMSgov/beneficiary-fhir-data#???](https://github.com/CMSgov/beneficiary-fhir-data/pull/???)
* JIRA Ticket(s):
    * [DCGEO-219](https://jira.cms.gov/browse/DCGEO-219)

The current schema uses a normalized relational schema structure that requires seven tables (4 for FISS and 3 for MCS) plus accompanying foreign keys and indexes.
By switching to a single table per claim type with a JSONB column to store all of a claim's data in a single column we can simplify the schema and imporve performance.
Benchmarking on a local postresql database saw ingestion rates 3-5 times faster with this structure.

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

Normalized relational databases provide great flexibility for querying relational data using joins and/or sub-queries.
However the BFD database exists solely to serve the BFD API server and the queries that it needs for its operations.
These queries generally consist of a simple query on one or two columns from the claim table followed by reading all of the associated detail data into a DTO object for each claim to allow delivery to clients.
Consolidating the records from the normalized relational structure into an object graph requires JPA to perform extra database queries and extra work in memory to assemble the records from detail tables.

Postgresql (and Amazon Aurora) supports storing object graphs as JSON directly in a single column of a record.
Using this feature will allow the use of only one table per claim type.
The records in this table would contain a column for each of the queries supported by the BFD API plus one additional column to hold the entire claim as JSON.
With this structure JPA would be able to find and retrieve an entire claim using only one query.
Also the work performed in memory to convert the claim JSON into an object graph would be simpler since the heirarchical structure of the graph directly matches that of the JSON.

Benchmarking a prototype of this concept against a local postgresql database revealed that claims could be ingested 5.7x faster for FISS claims and 3.5x faster for MCS claims.
The larger throughput improvement for FISS claims corresponds to the greater complexity of the FISS schema (4 tables) vs MCS (3 tables) in the normalized relational schema.

In addition to acheiving higher throughput during claim ingestion, the JSONB based schema resulted in a simpler database schema.
That simpler schema (1 table each for FISS and MCS claims) would also require far less maintenance over time.
Table changes would only be required when a new type of query is added to BFD API.
Addition of new fields and sub-objects to the claim data returned by the RDA API would not require any schema migration since that data would simply change the JSON written to the JSONB column.
Contrast this to adding a new field containing multiple sub-objects (e.g. payers) to MCS claims.
With a normalized relational schema this would require adding a new table to hold the individual payer records.
Along with that extra table the database would have to maintain additional indexes and foreign/primary key constraints.

## Proposed Solution
[Proposed Solution]: #proposed-solution
**This section discusses changes specifically for FISS claims.  The changes for MCS claims are directly analagous to the ones for FISS.**

The new schema simplifies the normalized one for by reducing the table count from four:

````
+------------------------------------------------------+
|                                                      |
|                      FISS CLAIM                      |
|                                                      |
+-------+------------------+-------------------+-------+
        |                  |                   |
        |                  |                   |
        |                  |                   |
+-------v-------+  +-------v-------+   +-------v-------+
|               |  |               |   |               |
|   DIAG CODE   |  |     PAYER     |   |   PROC CODE   |
|               |  |               |   |               |
+---------------+  +---------------+   +---------------+
````

To one:

````
+------------------------------------------------------+
|                                                      |
|                      FISS CLAIM                      |
|                                                      |
+------------------------------------------------------+
````

The single table for FISS claims has a small number of columns:
- `dcn`: primary key
- `mbi`: to support MBI query
- `mbiHash`: to support hashed MBI query
- `stmtCovToDate`: to support date query
- `sequenceNumber`: to track version of the claim
- `lastUpdated`: to track last time the record was updated
- `claim`: entire claim object as JSON

Each of the queryable non-JSON columns has an associated index.
The `claim` column is not indexed.

When the API receives a request for a claim with a particular MBI a simple query is performed on that column.
The `claim` value of the matching record is returned as part of the query.
In memory this JSON is converted into an object graph using an open source library.
This conversion happens seamlessly using a JPA feature, the `Convert` field annotation.

Code changes are minimal across the BFD code base since the claim data objects are simply converted from JPA entities to Plain Old Java Objects by removing the JPA annotations.
Two new entity classes are added for the two remaining database tables.
These have a field for each of the queryable columns plus a field to hold the claim POJO.
Clients perform queries using the new entity class but then use the returned POJO exactly as they previously used the old JPA entities.


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design
**This section discusses changes specifically for FISS claims.  The changes for MCS claims are directly analagous to the ones for FISS.**


The `bfd-model-rda` objects are modified as follows:

- All of the JPA annotations are removed from the existing JPA entity classes (PreAdjFissClaim, PreAdjFissPayer, etc).
- All of the static inner classes for composite keys are removed from the existing entity classes as well.
- Redundant fields are removed from the detail objects (e.g. `dcn` is no longer needed in `PreAdjFissPayer`).
- One new JPA entity class is added for each claim type (`PreAdjFissClaimJson` and `PreAdjMcsClaimJson`).  Details below.
- An abstract base class implementing the JPA `AttributeConverter` interface is added.  Details below.
- A concrete implementation of this base class is added for each claim type (`PreAdjFissClaimConverter` and `PreAdjMcsClaimConverter`).

The database schema is simplified to a single table for each claim type:

````sql
create table "pre_adj"."FissClaimsJson" (
    "dcn"            varchar(23)   not null,
    "mbi"            varchar(13),
    "mbiHash"        varchar(64),
    "stmtCovToDate"  date,
    "sequenceNumber" bigint        not null,
    "lastUpdated"    timestamp with time zone,
    "claim"          ${type.jsonb} not null,
    constraint "FissClaimsJson_pkey" primary key ("dcn")
);
````
The schema also contains one index for each of the queryable columns.

Note the template macro for the `claim` column type.
This macro is used since the type of the column will be different in postgresql vs HSQLDB:
- In postgresql the JSONB type is used to allow use of JSON path querying for ad-hoc queries.
- In HSQLDB `varchar(max)` is used since `JSONB` is not supported for that database.
With this macro in place integration and unit tests can work with either postgresql or HSQLDB with no code changes.

The JPA entity for the `FissClaimsJson` object is correspondingly simple:

````java
@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldNameConstants
@Table(name = "`FissClaimsJson`", schema = "`pre_adj`")
public class PreAdjFissClaimJson {
  public PreAdjFissClaimJson(PreAdjFissClaim claim) {
    this(
        claim.getDcn(),
        claim.getMbi(),
        claim.getMbiHash(),
        claim.getStmtCovToDate(),
        claim.getLastUpdated(),
        claim.getSequenceNumber(),
        claim);
  }

  @Id
  @Column(name = "`dcn`", length = 23, nullable = false)
  @EqualsAndHashCode.Include
  private String dcn;

  @Column(name = "`mbi`", length = 13)
  private String mbi;

  @Column(name = "`mbiHash`", length = 64)
  private String mbiHash;

  @Column(name = "`stmtCovToDate`")
  private LocalDate stmtCovToDate;

  @Column(name = "`lastUpdated`", nullable = false)
  private Instant lastUpdated;

  @Column(name = "`sequenceNumber`", nullable = false)
  private Long sequenceNumber;

  @Column(name = "`claim`", nullable = false, columnDefinition = "jsonb")
  @Convert(converter = PreAdjFissClaimConverter.class)
  private PreAdjFissClaim claim;
}
````

Although the underlying column type is JSONB, the `claim` field in the entity is the root POJO for a claim.
JPA supports automatic conversion between any database type and a Java type through the `@Convert` annotation.
This annotation tells JPA what class to use to perform the conversion.

The implementation of this converter uses Jackson to convert between an object and JSON.
The code to call Jackson is generic so a single base class supports any type of POJO.
A concrete subclass is defined for each POJO type and simply calls the base class constructor to set the correct `Class<T>` for the POJO.

````java
public class AbstractJsonConverter<T> implements AttributeConverter<T, String> {
  /**
   * {@code ObjectMapper} instances are thread safe so this singleton instance ensures consistent
   * formatting behavior for all instances.
   */
  private static final ObjectMapper objectMapper =
      new ObjectMapper()
          .enable(SerializationFeature.INDENT_OUTPUT)
          .registerModule(new Jdk8Module())
          .registerModule(new JavaTimeModule())
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  private final Class<T> klass;

  protected AbstractJsonConverter(Class<T> klass) {
    this.klass = klass;
  }

  @Override
  public String convertToDatabaseColumn(T attribute) {
    return objectToJson(attribute);
  }

  @Override
  public T convertToEntityAttribute(String dbData) {
    return jsonToObject(dbData);
  }

  @Nullable
  private String objectToJson(@Nullable Object value) {
    try {
      if (value == null) {
        return null;
      }
      return objectMapper.writeValueAsString(value);
    } catch (final Exception ex) {
      throw new RuntimeException(
          String.format("Failed to convert %s to JSON: %s", klass.getSimpleName(), ex.getMessage()),
          ex);
    }
  }

  @Nullable
  private T jsonToObject(@Nullable String value) {
    try {
      if (value == null) {
        return null;
      }
      return objectMapper.readValue(value, klass);
    } catch (final Exception ex) {
      throw new RuntimeException(
          String.format("Failed to convert JSON to %s: %s", klass.getSimpleName(), ex.getMessage()),
          ex);
    }
  }
}
````

Changes outside of the `bfd-model-rda` module are straight forward.
For example, in the `bfd-server-war` module's `pre_adj` package the following are changed:

- `ClaimTypeV2` and `ClaimResponseTypeV2` are modified to use the new entity class and retrieve the root POJO from its `claim` field.
- `FissClaimTransformerV2` and `FissClaimResponseTransformerV2` are modified to reference the new entity class and retrieve the root POJO from its `claim` field.

Other than those changes and a few test changes the rest of the server module is unchanged.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Collect a list of action items to be resolved or officially deferred before this RFC is submitted for final comment, including:

None.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

This is a departure from existing practice in the BFD database schema.
As such it creates code and design differences that could trip up new developers.
However this is offset by:
- The database schema is greatly simplified.
- The likelihood of schema migrations being needed for future RDA API changes are greatly reduced.
- The implementation is simple to understand and would work as-is with other entities in the future if desired.

Postgresql JSONB columns store the full JSON in binary form.
This increases storage reqirements for the schema since the field names in the JSON are repeated in every record.
The same benchmark that measured faster ingestion rates also measured increased storage requirements:
- Storage per FISS claim increased from 1,460 bytes per claim to 1,958.
- Storage per MCS claim increased from 1,036 bytes per claim to 1,822.
The size measurement was not extremely precise as the size increased between updates.
This variation was likely due to internal postgresql details.

Although the overal storage in the database was increased somewhat with JSONB the overall performance would not be affected.
Since the JSONB columns is not indexed, the size of the indexes being queried would be the same with JSONB as with the current schema.
Overall I/O with the database should be lower with this schema since only one query is needed for each claim instead of one per table.


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

Since this proposal could be boiled down to using a NoSQL approach for FISS and MCS claims why not go all the way?
A case could be made to move this data into Amazon's DynamoDB since it is built for this sort of design.
While there could be advantages to that approach the proposed design has some practical advantages:
- Using postgresql allows us to reap he benefits of a NoSQL approach without the need to add a whole new database technology to BFD.
- RDS is already approved for storing PII so the changes can be implemented at any time without having to wait for review and approval of DynamoDB.


## Prior Art
[Prior Art]: #prior-art

None

## Future Possibilities
[Future Possibilities]: #future-possibilities

Assuming this proposal works as expected the same technique could ultimately be applied to the rest of the BFD database.

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.
