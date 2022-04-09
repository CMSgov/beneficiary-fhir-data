# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-dsl-driven-rda-code-generation`
* Start Date: 2021-10-11
* Revised Date: 2022-04-07
* RFC PR: [CMSgov/beneficiary-fhir-data#1047](https://github.com/CMSgov/beneficiary-fhir-data/pull/1047)
* Obsolete RFC PR: [CMSgov/beneficiary-fhir-data#788](https://github.com/CMSgov/beneficiary-fhir-data/pull/788)
* JIRA Ticket(s):
  * [DCGEO-196](https://jira.cms.gov/browse/DCGEO-196)
  * [PACA-280](https://jira.cms.gov/browse/PACA-280)
* Proof of Concept PR: [CMSgov/beneficiary-fhir-data#1049](https://github.com/CMSgov/beneficiary-fhir-data/pull/1049)

RDA API fields are changing rapidly as the RDA API moves towards its production release.
Each API change triggers BFD code changes in several places.
An automated process to generate BFD code based on a simple metadata file would eliminate tedious and error-prone coding and keep all important details about the RDA data in a single place.
Doing so would replace a large amount of handwritten code with a single maven plugin to generate code that enforces RDA coding conventions and ensures generated code correctly matches the data.
Areas affected by this process could include hibernate entities, data transformations to copy protobuf objects into entities, database migration SQL, randomized synthetic data production, and data transformations to copy synthea data into protobuf objects.

In addition, the existing RIF annotation processor that generates code based on a combination of data in a spreadsheet and hardcoded values in the java source can be replaced by the same DSL based code generator.


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

The RDA API is evolving rapidly and adding new fields as it moves towards production.
Even after a production release is completed more fields will be added rapidly as that API development evolves from a first release focused on reliability and features to followup releases filling in more and more of the data available in the backend systems.

Initially code to handle RDA API data was handwritten as there were relatively few fields at that time and the RDA API team had not yet established conventions for mapping the data into protobuf.
Now the number of fields is growing and those conventions are well established.
When estimating the ultimate size of the RDA API message objects the team indicated that there might ultimately be 2-5 times as many fields as now.

With handwritten code the addition of new fields by the RDA API team can trigger many code changes across the project.
These can include:
- SQL migration scripts.
- Hibernate database entity classes.
- Data transformation/validation code to copy data from protobuf messages into entity objects.
- Random synthetic data generation classes.
- Data transformation code to copy data from Synthea data files into protobuf messages.

Changes made to each of these areas require careful attention to ensure the logic, data types, and validation rules are correct.
These changes have to be made consistently in different places in the code.
And yet most of this code is repetitive since the fields follow established conventions.
For example, every maximum field length in the database must be properly reflected in the database entities, enforced in the data validation code, and honored in the synthetic data generators.
This can certainly be done with handwritten code but is error-prone and requires developer time to write/modify the code and review the associated PR.

This RFC proposes replacing all of the handwritten code with a code generator that creates code using metadata in a YAML file.
The code generator would remove the need to write and maintain large amounts of code.
In the proof of concept the code generator replaced seven handwritten entity classes (over 1,000 LOC) and two transformer classes (over 1,400 lines) with a single 741 line YAML file.
In addition, over 1,500 LOC of unit tests for the handwritten transformers could be eliminated and replaced with much smaller tests that verify the reusable transformation components used by the code generator work properly.

**The YAML file is much simpler to understand than the handwritten code and less error-prone.**
A full implementation would add further savings by replacing still more handwritten code.

For an illustration of the code savings consider the difference in complexity between the [YAML DSL files from the POC](https://github.com/CMSgov/beneficiary-fhir-data/tree/brianburton/paca-394-rif-dsl-prototype-rebase/apps/bfd-model/bfd-model-rda/mappings) and these handwritten classes:

- [PreAdjFissClaims hand written entity](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-model/bfd-model-rda/src/main/java/gov/cms/bfd/model/rda/PreAdjFissClaim.java)
- [FissClaimTransformer hand written transformer class](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/main/java/gov/cms/bfd/pipeline/rda/grpc/source/FissClaimTransformer.java)
- [RandomFissClaimGenerator hand written synthetic data class](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-pipeline/bfd-pipeline-rda-grpc/src/main/java/gov/cms/bfd/pipeline/rda/grpc/server/RandomFissClaimGenerator.java)

The POC also replicates the output of the current RIF annotation processor using the DSL as input.
The [YAML DSL files for RIF data](https://github.com/CMSgov/beneficiary-fhir-data/tree/brianburton/paca-394-rif-dsl-prototype-rebase/apps/bfd-model/bfd-model-rif/mappings) are also available for review in GitHub.

*Note: Code examples in this document are taken from proof of concept work performed in the `paca-394-rif-dsl-prototype-rebase` branch of the BFD repo.  The code in that branch is functional though incomplete and provides insight into the issues involved in following this recommendation.*


## Proposed Solution
[Proposed Solution]: #proposed-solution

### DSL for RDA API Data

A code generator processes YAML based metadata files to create all of the code required to work with the RDA API data messages, objects, and fields.
The YAML describes in a declarative way what every RDA API message is, what table that message is stored in, what columns are in the table, and how to transform the data from the RDA API messages into values in those columns.
For example:

````YAML
  - id: McsClaim
    messageClassName: gov.cms.mpsm.rda.v1.mcs.McsClaim
    entityClassName: gov.cms.bfd.model.rda.PreAdjMcsClaim
    transformerClassName: gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer2
    table:
      name: McsClaims
      schema: pre_adj
      primaryKeyColumns:
        - idrClmHdIcn
      columns:
        - name: idrClmHdIcn
          sqlType: varchar(15)
          nullable: false
        - name: sequenceNumber
          sqlType: bigint
        - name: idrClaimType
          sqlType: varchar(1)
        - name: idrDtlCnt
          sqlType: int
        - name: idrBeneLast_1_6
          sqlType: varchar(6)
        - name: idrBeneFirstInit
          sqlType: varchar(1)
        - name: idrBeneMidInit
          sqlType: varchar(1)
        - name: idrCoinsurance
          sqlType: decimal(7,2)
        - name: idrClaimReceiptDate
          sqlType: date
    transformations:
      - from: idrClmHdIcn
        optional: false
      - from: sequenceNumber
      - from: idrClaimType
        transformer: MessageEnum
        transformerOptions:
          enumClass: gov.cms.mpsm.rda.v1.mcs.McsClaimType
      - from: idrDtlCnt
      - from: idrBeneLast16
        to: idrBeneLast_1_6
      - from: idrBeneFirstInit
      - from: idrBeneMidInit
      - from: idrCoinsurance
      - from: idrClaimReceiptDate
````

This example illustrates some of the advantages of using a declarative file:

- Standard conventions can be supported as defaults.  For example:
  - The `to` only needs to be defined if it differs from the `from`.  Generally columns have the same name as their RDA API field but not always.
  - A `javaType` only needs to be defined if it differs from the default for the column's data type.
  - A `transformer` only needs to be defined if it differs from the default for the column's data type.
- Transformers can be easily created and added to the plugin.  Each has a name used to reference it in the YAML.
- The code generator follows a simple set of rules to choose a default transformation if no `transformer`  is provided in the YAML.
- A single field can have multiple transformations.  For example the MBI field can be copied directly to a column and also used to store a hashed value in another column.
- Transformers can have their own specific options if their behavior is modifiable from default settings.
- A few structural transforms can be specified using a virtual `from` that triggers the transform.  These are useful for columns whose values are known at runtime but not taken directly from the messages (like array indexes, the current timestamp, parent primary key column value, etc).

A code generator can ensure the relationships between tables in JPA are handled correctly.
In JPA detail tables require properly defined composite key class and the parent requires a special collection field with appropriate annotation.
These relationships can be trivially defined as `array`s in the YAML and the code generator takes care of getting the implementation details correct:

````yaml
  - id: FissClaim
    messageClassName: gov.cms.mpsm.rda.v1.fiss.FissClaim
    entityClassName: gov.cms.bfd.model.rda.PreAdjFissClaim
    transformerClassName: gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer2
    table:
      name: FissClaims
      schema: pre_adj
      primaryKeyColumns:
        - dcn
      columns:
        - name: dcn
          sqlType: varchar(23)
          nullable: false
        - name: sequenceNumber
          sqlType: bigint
          nullable: false
        - name: hicNo
          sqlType: varchar(12)
          nullable: false
        # ... skipping lots of columns for the sake of this example ...
    transformations:
      - from: dcn
        optional: false
      - from: seq
        to: sequenceNumber
        optional: false
      - from: hicNo
        optional: false
      # ... skipping lots of transforms for the sake of this example ...
    arrays:
      # Every FissClaim can have multiple procedure codes, diagnosis codes, and payers
      - from: fissProcCodes
        to: procCodes
        mapping: FissProcCode
      - from: fissDiagCodes
        to: diagCodes
        mapping: FissDiagnosisCode
      - from: fissPayers
        to: payers
        mapping: FissPayer
        namePrefix: "payer"
````

The example illustrates the three detail tables associated with each `FissClaim`.
The RDA API sends these as `repeated` fields in the protobuf definition and the plugin maps them to detail entities in the JPA classes.
Each one references the field in the protobuf message and the entity class and the mapping used to define the detail table.

The code generator is implemented as a maven plugin.
This has several advantages:
- The plugin fits seamlessly into the BFD build process.  Nothing in the BFD build or release process has to change.
- Code is automatically generated and made available to the compiler.
- IDEs can display and debug the generated code without any special settings.  This includes allowing breakpoints within the generated code if needed.

### DSL For RIF Data

The same DSL can be used to generate code for processing RIF data.
Using the DSL driven code generator for RIF data has several advantages over the existing RIF annotation processor.
- The same DSL serves to define all data ingested by BFD.  This reduces the learning curve for new developers.
- The YAML files contain all of the meta-data for each entity and eliminate the knowledge split between an Excel spreadsheet and java code in the existing process.
- The YAML files are plain text.  They can be easily diffed by GitHub during PR reviews.
- Maven plugins are easier to debug in an IDE (using `mvnDebug`) than Java annotation processors.
- Maven plugins appear to integrate better with IDEA than the annotation processor.

The current POC expands on the original to add full support for generating the following code for RIF data:
- entity classes (parent and, when needed, line) for each RIF file type (Beneficiary, DME, Carrier, etc)
- column enum class for each RIF file
- parser to convert RIF records into entities
- CSV writer to convert entities into arrays of header/value pairs

Except for the parser the code generated by the POC is nearly identical to that generated by the current annotation processor.
Differences include:
- entity classes from the POC use lombok where possible
- DSL includes comments taken from the Excel spreadsheet and add them as javadoc comments in the entities
- parser classes use the same framework as RDA transformer classes rather than generating code identical to the annotation processor

Adding RIF support required extending the DSL slightly to allow specific features to be enabled or disabled.
Support was also added for generating code that takes values from RIF data in Lists rather than protobuf generated message classes.

The concept of arrays from the RDA API mapped well to lines in RIF data.
RIF objects just have one array each rather than many but otherwise work similarly.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

#### DSL Syntax

The DSL file is a YAML file containing an array of mappings.
Each mapping defines one RDA API message object.
Top level properties in a mapping are:
- id: Unique identifier (name) of the mapping.
- sourceType: Specifies how to access values from message objects.  Possible values are `Grpc` or `RifCsv`.
- messageClassName: Name of the protobuf message class as defined in the RDA API proto files or the CSV adaptor class for RIF data.
- entityClassName: Name of the generated JPA entity class to store objects of this type.
- entityInterfaces: Array of interface names that entity class should implement.
- transformerClassName: Name of the generated class to implement the transformation of protobuf message objects into JPA entities.
- table: Object defining details about the database table that the JPA entity maps to.
- enumTypes: Array of objects defining any `enum` classes that the generator should create for use in the transformation.
- transformations: Array of objects defining what transformations to apply to an RDA API message to copy its data into a JPA entity.
- arrays: Array of objects defining detail objects (one-to-many relationships) contained within the message.
- joins: Array of objects defining specific join relationships between this entity and some other entity.

The `table` objects contain the following properties:
- name: Name of the table as it appears in the database.
- schema: Name of the schema containing the table (optional).
- primaryKeyColumns: Array of column names for the primary key.
- quoteNames: When true indicates schema, database, and column names should be quoted in JPA entities.
- equalsNeeded: When true indicates entity classes need to include equals/hashCode methods.
- compositeKeyClassName: Name of static inner class used for composity keys (usually LineId or PK). 
- columns: Array of objects defining the columns in the table.  Columns have the following properties:
  - name: Name of the column as it appears in the database and entity class.
  - dbName: Name of the column in the database (if different than the field name in the entity).
  - sqlType: Type of the column as it would appear in SQL DDL defining the column.
  - javaType: Type of the field in the JPA entity class that holds this column's value.
  - javaAccessorType: Type of the field's accessor methods if it differs frm field type (for long/String in RIF).
  - nullable: Boolean indicating if the column is nullable.
  - comment: Arbitrary text to add to fields as javadoc in entity classes.

The `enumType` objects are somtimes used as flags in mapping messages to entities.
For example the RDA API sends data in one of two possible sub-objects for payer details in FissPayer messages and the transformer copies data from whichever one has data and sets an enum column in the entity to indicate which of the two it copied the data from.
The properties of these objects in the DSL are:
- name: Name of the enum class in java.
- values: Array of enum value names added to the enum in the java code.
They are also used in RIF mappings to trigger creation of the column enums.

The `transformation` objects define how to copy data from a field in an RDA API message to a field in the JPA entity.
Generally each field has one transformation applied to it, but the DSL allows multiple transformations to be applied.
A simple example of needing multiple transformations is a the `mbi` field.
It requires two transformations: one to copy the field as is and another to store its hashed value.
The properties of `transformation` objects in the DSL are:
- from: Name of the field to transform as it appears in an RDA API message.
- to: Name of the field in the JPA entity to store the the transformed value into.
- optional: Boolean indicating whether a missing field value in the RDA API message is allowed.
- transformer: Specifies the name of the transformation to apply.  These are predefined in the code generator.
- transformerOptions: Optional object containing key value pairs of configuration options for the transformer.  These settings are specific to the particular transformer.

The `array` objects contain the following properties:
- from: Name of the field containing the array in the RDA API message.
- to: Name of the field containing the collection of entities in the JPA entity.
- mapping: Id of a mapping that will be used to transform each of the messages in the array.
- namePrefix: Additional prefix added to field names for array elements when logging transformation errors.  These are optional but they make the error messages much more readable.

The `join` objects contain the following properties:
- fieldName: Name of the java field in the entity class.
- entityClass: Name of the entity being joined.
- collectionType: Type of collection to use for field (List or Set).
- readOnly: When true no setter is generated for the field.
- joinType: Type of JPA join annotation to use (OneToMany, ManyToOne, OneToOne).
- mappedBy: value for mappedBy parameter in JPA annotation.
- orphanRemoval: value for orphanRemoval parameter in JPA annotation.
- fetchType: value for fetchType parameter in JPA annotation.
- cascadeTypes: array of values for cascade parameter in JPA annotation.

Most of these properties have reasonable defaults to handle the most common use cases.


#### Maven Plugin Design

Every maven plugin implements one or more goals.
Each goal defines some operation specific to the plugin.
The code generation plugin has a goal for each type of code to be generated.

- The `entities` goal generates Hibernate database entity classes with all appropriate annotations, getters, setters, equals/hashCode, and entity relationship mappings.
- The `transformers` goal generates data transformation/validation classes to copy data from RDA API protbuf messages into database entity classes.
- The `csv-writers` goal generates RIF CsvWriter classes for each parent entity.
- The `sql` goal generates sample SQL DDL for each table that can be used as the basis for creating Flyway migration files.
- The `random-data` goal generates random data generation classes to create randomized data of appropriate size and type for each object/field in the RDA API messages. (Not in POC)
- The `synthea-bridge` goal generates data transformation classes to copy data from Synthea RIF data files into protobuf messages. (Not in POC)

All of the goals provided by the plugin follow the same basic steps:

- Read the mapping files using a library such as Jackson to map the file's contents into java beans.
- Process the mappings in a goal specific way to generate java code using a library such as javapoet to generate the java files.
- Write the generated class files to a directory specified by the `pom.xml` file.

Different goals use different subsets of the metadata in the DSL file to generate different types of source code.
- The `entities` goal only processes the `table` object since it generates the relevant Hibernate entities and all of the data required to do so is defined in the `table`.
- A `sql` goal would also process the `table` object to generate SQL `CREATE TABLE` and `ALTER TABLE` DDL code that a developer could use as the basis of a Flyway migration file.
- The `transformers` goal additionally needs to process the `transformations` and `arrays` sections since these define the relationships between the RDA API message data and the fields in the Hibernate entities.

The plugin itself is easily executed in any module by adding a `<plugin>` element to the `<build>` element of the module's `pom.xml` file.  
The `pom.xml` file specifies where to store the generated source code and the plugin automatically adds the directory to the maven compiler's source path.

For example:

````XML
    <build>
        <plugins>
            <plugin>
                <groupId>gov.cms.bfd</groupId>
                <artifactId>bfd-model-dsl-codegen-plugin</artifactId>
                <version>${project.version}</version>
                <configuration>
                    <mappingFile>${mapping.directory}/mapping.yaml</mappingFile>
                    <outputDirectory>${project.build.directory}/generated-sources/transformers</outputDirectory>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>transformers</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
````

The classes generated by the plugin rely on a few utility classes that are defined in a separate library module.
This library is added as a dependency in the modules that require them.


#### Generating JPA Entity classes

The `entities` goal generates JPA/Hibernate entities that are virtually identical (minus javapoet's code indentation, etc) to what we are currently maintaining by hand (or generating via annotation processor for RIF).
This includes the use of the same lombok, JPA, and hibernate annotations.
The existing integration tests continue to pass with the generated entities.

This simplified example illustrates how a table and its columns can be defined in YAML:

````yaml
    entityClassName: gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode
    table:
      name: McsDiagnosisCodes
      schema: pre_adj
      primaryKeyColumns:
        - idrClmHdIcn
        - priority
      columns:
        - name: idrClmHdIcn
          sqlType: varchar(15)
          nullable: false
        - name: priority
          sqlType: smallint
          javaType: short
          nullable: false
        - name: idrDiagIcdType
          sqlType: varchar(1)
        - name: idrDiagCode
          sqlType: varchar(7)
        - name: lastUpdated
          sqlType: timestamp with time zone
````

Some details illustrated by this example:

- Specifying a fully defined `entityClassName` ensures that the plugin makes no assumptions about what packages the code it generates will live in.
- The `schema` would be optional and associated annotations only added to the entity if it has been defined.
- The `name` and `sqlType` would be required.
- All other values have reasonable defaults.  For example any `varchar(n)` or `char(n)` defaults to a `String` as the `javaType`.  Similarly `timestamp` maps to `Instant` and `date` maps to `LocalDate` by default.
- All columns default to being `nullable` unless otherwise set using `nullable: false` since almost all RDA API fields are optional.
- The `primaryKeyColumns` are used to add `Id` annotations to those fields in the entity classes as well as to define the `hashCode` and `equals` methods following Hibernate rules.
- Tables with multiple `primaryKeyColumns` automatically generate a static class for the composite key object associated with the table.

Each of the most commonly used `sqlType`s have an associated default `javaType` and appropriate logic for defining the generated `Column` annotation in the entity class.
For example the code generator knows how to parse a max length out of the `varchar(n)` and `char(n)` types.
Also it knows that it needs to add a `columnDefinition` value for `decimal(m,n)` types but not for most other types.

Each `array` definined in the YAML file results in a `Set<TEntity>` field being created in the parent entity.  For example:

````yaml
  - id: FissClaim
    messageClassName: gov.cms.mpsm.rda.v1.fiss.FissClaim
    entityClassName: gov.cms.bfd.model.rda.PreAdjFissClaim
    transformerClassName: gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer2
    table:
      name: FissClaims
      schema: pre_adj
      primaryKeyColumns:
        - dcn
      columns:
        - name: dcn
          sqlType: varchar(23)
          nullable: false
    transformations:
      - from: dcn
        optional: false
        # ... skipping lots of transformations for the sake of this example ...
    arrays:
      - from: fissProcCodes
        to: procCodes
        mapping: FissProcCode
````

generates code like this in the `PreAdjFissClaim` class:

````java
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(
    onlyExplicitlyIncluded = true
)
@FieldNameConstants
@Table(
    name = "`FissClaims`",
    schema = "`pre_adj`"
)
public class PreAdjFissClaim {
  @Column(
      name = "`dcn`",
      nullable = false,
      length = 23
  )
  @Id
  @EqualsAndHashCode.Include
  private String dcn;
  @OneToMany(
      mappedBy = "dcn",
      fetch = FetchType.EAGER,
      orphanRemoval = true,
      cascade = CascadeType.ALL
  )
  @Builder.Default
  private Set<PreAdjFissProcCode> procCodes = new HashSet<>();
````

If a table has multiple primary key columns the plugin knows to also generate a java bean for the composite key.
For example:

````yaml
  - id: McsDiagnosisCode
    messageClassName: gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode
    entityClassName: gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode
    table:
      name: McsDiagnosisCodes
      schema: pre_adj
      primaryKeyColumns:
        - idrClmHdIcn
        - priority
      columns:
        - name: idrClmHdIcn
          sqlType: varchar(15)
          nullable: false
        - name: priority
          sqlType: smallint
          javaType: short
          nullable: false
        - name: idrDiagIcdType
          sqlType: varchar(1)
    transformations:
      - from: PARENT
        to: idrClmHdIcn
      - from: INDEX
        to: priority
      - from: idrDiagIcdType
        transformer: MessageEnum
        transformerOptions:
          enumClass: gov.cms.mpsm.rda.v1.mcs.McsDiagnosisIcdType
          unrecognizedNameSuffix: EnumUnrecognized
      - from: idrDiagCode
      - from: NOW
        to: lastUpdated
````

Generate code like this:

````java
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(
    onlyExplicitlyIncluded = true
)
@FieldNameConstants
@Table(
    name = "`McsDiagnosisCodes`",
    schema = "`pre_adj`"
)
@IdClass(PreAdjMcsDiagnosisCode.PK.class)
public class PreAdjMcsDiagnosisCode {
  @Column(
      name = "`idrClmHdIcn`",
      nullable = false,
      length = 15
  )
  @Id
  @EqualsAndHashCode.Include
  private String idrClmHdIcn;

  @Column(
      name = "`priority`",
      nullable = false
  )
  @Id
  @EqualsAndHashCode.Include
  private short priority;

  @Column(
      name = "`idrDiagIcdType`",
      length = 1
  )
  private String idrDiagIcdType;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class PK implements Serializable {
    private String idrClmHdIcn;

    private short priority;
  }
}
````

#### Generating Transformation class

The current code base has a handwritten transformer class for each type of claim returned by the RDA API (i.e. `FissClaimTransformer` and `McsClaimTransformer`).
These classes contain code to copy RDA API data from protobuf messages into corresponding JPA entities.
Field values are copied using a `DataTransformation` object that provides methods for validating and parsing the individual field values.
There is a `DataTransformer` method for each type of field.
These methods support the conventions RDA API uses when mapping its data into protobuf.

The plugin generated code follows this same pattern.
It generates a transformation class for every mapping that has a `transformerClassName` value.
The transformers contain essentially the same sequence of method calls that a developer would currently write by hand.

Each transformation class has one public method that accepts an RDA API message object and a `DataTransformer` and returns a corresponding entity object.
The caller provides the `DataTransformer` since it collects any validation errors and the caller can query it afterwards to determine if there were any errors.

````java
public class FissClaimTransformer2 {
  private final Function<String, String> idHasher;

  // fields generated by transformer objects inserted here

  public FissClaimTransformer2(Function<String, String> idHasher,
      EnumStringExtractor.Factory enumExtractorFactory) {
    this.idHasher = idHasher;
    // initializers for fields generated by transformer objects inserted here
    }

  // convenience method that handles creating a DataTransformer and throwing an exception if transformation fails.
  public PreAdjFissClaim transformMessage(FissClaim from) {
    final DataTransformer transformer = new DataTransformer();;
    final PreAdjFissClaim to = transformMessage(from, transformer, Instant.now());
    if (transformer.getErrors().size() > 0) {
      throw new DataTransformer.TransformationException("data transformation failed", transformer.getErrors());
    }
    return to;
  }
  
  public PreAdjFissClaim transformMessage(
      FissClaim from, DataTransformer transformer, Instant now) {
    final PreAdjFissClaim to = transformMessageToFissClaim(from,transformer,now,"");
    transformMessageArrays(from,to,transformer,now,"");
    return to;
  }

  // all the generated private methods to handle individual mappings
````

The `transformMessageToFissClaim()` and `transformMessageArrays()` methods are private methods created for each mapping.
Generally there is one `transformMessageTo...` method for the parent mapping plus one for each array mapping.
There will currently be only one `transformMessageArrays` method since the RDA API only uses arrays at the top level.
But the plugin would be capable of handling arrays in the child objects as well in case that changes in the future.

#### Field Transformations

A transformation takes data from one field in the input message, validates it, and copies it to a destination field in the entity.

Each transformation is implemented as a Java class that implements an interface.
The interface contains three methods:
- One to get a list of field definitions for any class level fields needed by the transformer.
- One to get initialization code for each such field for use in the generated transformer's constructor.
- One to generate any java statements needed to perform the transformation.

The transformations needed to fully implement the current RDA API and RIF data models include:

- Amount: parse and copy a dollar amount string
- Char: copy a single character into a char field
- Date: parse and copy a date string
- EnumValueIfPresent: set an enum column if a specific field is present in the RDA message
- IdHash: hash and copy a string (MBI hash)
- Int: copy an integer value
- IntString: parse and copy an integer string
- LongString: parse and copy a long int string
- MessageEnum: extract string value from an enum and copy it
- RifTimestamp: parse and copy a time stamp in RIF formats
- String: copy a string
- Timestamp: copy the current timestamp

The simplest case for a transformer just inserts a single method call:

````java
public class TimestampFieldTransformer extends AbstractFieldTransformer {
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    return destSetter(column, NOW_VALUE);
  }
}
````

In this example `destSetter` is a helper method in the abstract base class to create a code block that sets the value of the destination (entity) field.

The most complex transformation would add a field with an `EnumStringExtractor` object, create code to initialize it appropriately, and code to invoke it to copy the enum's value into an entity field:

````java
public class MessageEnumFieldTransformer extends AbstractFieldTransformer {
  @Override
  public CodeBlock generateCodeBlock(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    final ClassName enumClass =
        PoetUtil.toClassName(transformation.transformerOption(ENUM_CLASS_OPT).get());
    CodeBlock.Builder builder = CodeBlock.builder();
    if (column.isChar()) {
      builder.addStatement(
          "$L.copyEnumAsCharacter($L, $L.getEnumString($L), $L)",
          TRANSFORMER_VAR,
          fieldNameReference(mapping, column),
          extractorName(mapping, transformation),
          SOURCE_VAR,
          destSetRef(column));
    } else {
      builder.addStatement(
          "$L.copyEnumAsString($L,$L,1,$L,$L.getEnumString($L),$L)",
          TRANSFORMER_VAR,
          fieldNameReference(mapping, column),
          column.isNullable(),
          column.computeLength(),
          extractorName(mapping, transformation),
          SOURCE_VAR,
          destSetRef(column));
    }
    return builder.build();
  }

  @Override
  public List<FieldSpec> generateFieldSpecs(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    final ClassName messageClass = PoetUtil.toClassName(mapping.getMessageClassName());
    final ClassName enumClass =
        PoetUtil.toClassName(transformation.transformerOption(ENUM_CLASS_OPT).get());
    FieldSpec.Builder builder =
        FieldSpec.builder(
            ParameterizedTypeName.get(
                ClassName.get(EnumStringExtractor.class), messageClass, enumClass),
            extractorName(mapping, transformation),
            Modifier.PRIVATE,
            Modifier.FINAL);
    return ImmutableList.of(builder.build());
  }

  @Override
  public List<CodeBlock> generateFieldInitializers(
      MappingBean mapping, ColumnBean column, TransformationBean transformation) {
    final ClassName messageClass = PoetUtil.toClassName(mapping.getMessageClassName());
    final ClassName enumClass =
        PoetUtil.toClassName(transformation.transformerOption(ENUM_CLASS_OPT).get());
    final boolean hasUnrecognized =
        transformation
            .transformerOption(HAS_UNRECOGNIZED_OPT)
            .map(Boolean::parseBoolean)
            .orElse(true);
    CodeBlock initializer =
        CodeBlock.builder()
            .addStatement(
                "$L = $L.createEnumStringExtractor($L,$L,$L,$L,$T.UNRECOGNIZED,$L,$L)",
                extractorName(mapping, transformation),
                ENUM_FACTORY_VAR,
                sourceEnumHasValueMethod(messageClass, transformation),
                sourceEnumGetValueMethod(messageClass, transformation),
                sourceHasUnrecognizedMethod(hasUnrecognized, messageClass, transformation),
                sourceGetUnrecognizedMethod(hasUnrecognized, messageClass, transformation),
                enumClass,
                unsupportedEnumValues(enumClass, transformation),
                extractorOptions(transformation))
            .build();
    return ImmutableList.of(initializer);
  }
````

#### Array Transformation (Detail records)

Arrays are recognized and code is generated to transform the array elements appropriately to produce the detail objects for the JPA entities.
Detail tables require additional columns containing their parent record's primary key.
Additionally, each detail object in the array is assigned a "priority" number equal to its array index.
The `priority` is used when sorting the detail records so that their original order in the RDA API message is preserved.

The DSL has two special `from` values (`PARENT` and `INDEX`) for this purpose.

- `PARENT` tells the transformer to copy the value of the `to` field from the parent into the detail object.
- `INDEX` tells the transformer to set the value of the `to` field to the object's array index.

The generated code for transforming arrays first creates the detail objects and then applies any `PARENT` or `INDEX` transformations.

Here is a subset of the YAML for a detail object containing a number of fields including `dcn` (copied from parent) and `priority` (set to array index):

````yaml
  - id: FissProcCode
    transformations:
      - from: PARENT
        to: dcn
      - from: INDEX
        to: priority
      - from: procCd
        to: procCode
        optional: false
      - from: procFlag
      - from: procDt
        to: procDate
      - from: NOW
        to: lastUpdated
````

Here is the code generated to create the array object, initialize the fields from the parent, and add the object to its parent.

````java
  private void transformMessageArrays(FissClaim from, PreAdjFissClaim to,
      DataTransformer transformer, Instant now, String namePrefix) {
    for (short index = 0; index < from.getFissProcCodesCount(); ++index) {
      final String itemNamePrefix = namePrefix + "procCode" + "-" + index + "-";
      final FissProcedureCode itemFrom = from.getFissProcCodes(index);
      final PreAdjFissProcCode itemTo = transformMessageImpl(itemFrom,transformer,now,itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getProcCodes().add(itemTo);
    }
  }
````


### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Collect a list of action items to be resolved or officially deferred before this RFC is submitted for final comment, including:

None yet.


### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

Why should we *not* do this?

**Reason 1: Code generators can be complex**

A case can be made that lots of handwritten code can be more directly comprehensible than a code generator.
This is particularly true if the design of the code generator hard codes too many things and embeds too much knowledge of the data it generates code for (e.g. if it adds or looks for fields with specific names that aren't defined in the meta data).

Both of these concerns can be addressed by careful design and coding of the plugin.
Embedding knowledge of *conventions* is perfectly OK.
That is why the plugin exists: to centralize that knowledge in a reusable component.
However embedding knowledge of fields themselves is harmful since it splits knowledge of the fields between the metadata and the plugin source code.

Complexity of the plugin can be addressed through design.
Use of a strategy pattern for transformations can provide a clear interface and convention for how those work.
Adding comments with example output to each section that generates code can make the intent of that code clearer.

**Reason 2: RDA API Conventions may change**

Since RDA API is not yet in production won't their conventions change substantially in the near future?
That would be a danger either with handwritten code or with a plugin.
The plugin centralizes the implementation of those conventions so we can leverage that to simplify adapting to the change.
Simply change the plugin and the new conventions apply to all classes and fields automatically.

A similar approach has been taken with the handwritten code too.
However, though embedding the conventions in library classes and methods is helpful it can still lead to widespread code changes if you need to add a parameter to a library method.
Suddenly dozens of lines of code need to be changed by hand to add that new parameter.
A code generator can do that sort of thing automatically.

**Reason 3: A single plugin used in multiple modules implies too much knowledge**

The code using the plugin is in separate modules for a reason.
Won't using a plugin require adding many dependencies to the plugin module that don't make sense there?

This can be addressed by defining interfaces that the plugin generated code calls to perform some of its work.
Then the module that uses the generated code can implement the interface with whatever extra knowledge it needs.
This was done in the proof of concept where the code that actually extracts string values from RDA API enums was called through an interface.
The interface was defined in the plugin library and a factory to create a concrete implementation was passed to the constructor of the generated code.
This allowed the plugin to generate all of its code without any access to the RDA API stubs themselves.


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

A spreadsheet could be used for the DSL.
However, we decided that a YAML file format has several advantages over a spreadsheet for this process:
* Existing open source libraries such as jackson can directly convert YAML into java beans.
* RDA API and RIF data are inherently hierarchical and YAML naturally supports hierarchical data.
* YAML is pure text, so it can be edited from within an IDE and diffs of the file can be reviewed as part of the GitHub PR review process.

We considered using java annotation processing but decided that a maven plugin has some advantages:
* The maven plugin works directly within the maven build process rather than adding the complexity of java annotation processing.
* The same plugin can be invoked from multiple modules to generate different portions of the code exactly where it is needed.
* Maven plugins interact more seamlessly with IDEs.

We considered defining a full-blown imperative DSL using groovy or something else but:
* Writing transformations in java fits more naturally into the BFD code base and team expertise.
* Declarative structure allows the plugin to guarantee adherence to the RDA API conventions and proper code review. (i.e. no cheats or workarounds can be inserted as code in the DSL file)
* Transformations implemented in java within the plugin have a standard structure that makes them easier to develop and debug.

We considered using a dynamic transformation system rather than a code generator.
The same metadata could be used to configure a class at runtime to perform all of the same transformations on the fly.
This idea would have the advantage of eliminating the need for a maven plugin since a single class in a java library could dynamically perform all of the same work as the generated code.
There are downsides to this approach though:
* It would not work for creating entity classes or SQL migration files.
* A fully dynamic object would have a performance penalty compared to compiled code.  For example with generated code the java JIT could determine that specific code branches are unused in the transformation classes for one of the claim types but always used for a different one. Since we will be processing millions of messages per day this could become a bottleneck or increase CPU resource requirements.
* Breakpoints can be set in specific places in the generated code to see what's happening if a bug is encountered.   Dynamic code is more general and isolating a problem to a specific field can be more difficult.  (e.g. skipping past the first 30 fields in your debugger to see what happens in the field you actually care about can be painful.)

Continuing with the existing hard-written code would have a number of disadvantages:
* It would make it more difficult to react to changes in the RDA API going forward.
* It would complicate PR reviews for RDA API changes since multiple files would have to be reviewed for each change.
* Changes in conventions by the RDA API team would require changing logic in multiple places rather than just in the plugin.  An example of this would be if they changed how they map enums to strings in their responses.


## Prior Art
[Prior Art]: #prior-art

The RIF entities are currently generated using java annotation processing and reading metadata from a spreadsheet.
The proposed plugin would replace this.

## Future Possibilities
[Future Possibilities]: #future-possibilities

The plugin can be adapted as new ways of using the RDA API data appear over time.
A similar approach could be used in the future to consume different types of APIs or data.
For example data from a REST API or a different file format could be handled along similar lines.

## Addendums
[Addendums]: #addendums

The following addenda are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addenda may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.
