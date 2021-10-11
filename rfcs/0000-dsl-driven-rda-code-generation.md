# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-dsl-driven-rda-code-generation`
* Start Date: 2021-10-11
* RFC PR: [rust-lang/rfcs#0000](https://github.com/rust-lang/rfcs/pull/0000)
* JIRA Ticket(s):
    * [DCGEO-196](https://jira.cms.gov/browse/DCGEO-196)

RDA API fields are chagning rapidly as the RDA API moves towards its production release and each API change triggers BFD code changes in several places.
An automated process to generate BFD code based on a simple meta data file would eliminate tedious and error prone coding and keep all important details about the RDA data in a single place.
Doing so would replace a large amount of hand written code with a single maven plugin to generate code that enforces RDA coding conventions and ensures generated code correctly matches the data.
Areas affected by this process could include hibernate entities, data transformations to copy protobuf objects into entities, database migration SQL, randomized synthetic data production, and data transformations to copy synthea data into protobuf objects.

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
Even once a production release is completed more fields will be added rapidly as that API development evolves from a first release focused on reliability and features to followup releases filling in more and more of the data available in the backend systems.
Initially code to handle RDA API data was hand-written as there was a small number of fields at that time and the RDA API team had not yet establish conventions for mapping the data into protobuf.
Now the number of fields is growing and those conventions are well established.

With hand-written code the addition of new fields by the RDA API team can trigger many code changes across the project.  These can include:
- SQL migration scripts.
- Hibernate database entity classes.
- Data transformation/validation code to copy data from protobuf messages into entity objects.
- Random synthetic data generation classes.
- Data transformation code to copy data from Synthea data files into protobuf messages.

Each of these areas require careful attention to ensure the logic is correct, data types are correct, validation rules are correct, etc.
These changes have to be made consistently in different places in the code.
And yet most of this code is repetitive since the fields follow established conventions.
For example, every maximum field length in the database must be properly reflected in the database entities, enforced in the data validation code, and honored in the synthetic data generators.
This can certainly be done with hand-written code but is error prone and requires developer time to write/modify the code and review the associated PR.

*Note: Code examples in this document are taken from proof of concept work performed the `brianburton/dcgeo-186-entities-dsl` branch of th BFD repo.  The code in that branch is functional but incomplete but provided insight into the issues involved in following this recommendation.*


## Proposed Solution
[Proposed Solution]: #proposed-solution

A maven plugin processes a YAML based metadata file to create all of the code required to work with the RDA API data messages, objects, and fields.
The YAML explains in a declarative way what every RDA API message is, what table that message is stored in, what the columns of table are, and how to transfom the data from the RDA API messages into data in those columns.  For example:

````YAML
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
        - name: idrDiagCode
          sqlType: varchar(7)
        - name: lastUpdated
          sqlType: timestamp with time zone
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

This example illustrates some of the advantages of using a declarative file:

- Standard conventions can be supported as defaults.  For example:
  - The `to` only needs to be defined if it differs from the `from`.  Generally columns are named directly based on the RDA API field but not always.
  - A `javaType` only needs to be defined if it differs from the default for the column's data type.
  - A `transformer` only needs to be defined if it differs from the default for the column's data type.
- Transformers can be easily created and added to the plugin.  Each has a name used to reference it in the YAML.
- The plugin follows a simple set of rules to choose a default transformation if no `transformer`  is provided in the YAML.
- A single field can have multiple transformations.  For example the MBI field can be copied directly to a column and also used to store a hashed value in another column.
- Transformers can have their own specific options if their behavior is modifiable from default settings.
- A few structural transforms can be specified using a virtual `from` that triggers the transform.  Essentially these are for fields that are known at runtime but not taken directly from the messages (like array indexes, the current timestamp, etc).

Since the RDA API data is used in different modules within the BFD code base the plugin defines goals specific to each type of code that it generates:

- The `entities` goal generates Hibernate database entity classes with all appropriate annotations, getters, setters, equals/hashCode, and entity relationship mappings.
- The `transformers` goal generates data transformation/validation classes to copy data from RDA API protbuf messages into database entity classes.
- The `migration` goal generates SQL DDL for each table that can be used as the basis for creating Flyway migration files.
- The `random-data` goal generates random data generation classes to create randomized data of appropriate size and type for each object/field in the RDA API messages.
- The `synthea-bridge` goal generates data transformation classes to copy data from Synthea RIF data files into protobuf messages.

Getting the relationships between tables in JPA can be somewhat tricky however they can be trivially defined as `array`s in the YANL and the code generator takes care of getting the details correct:

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
Each one references the field in in the protobuf message and the entity class and the mapping used to define the detail table.

The classes generated by the plugin rely on a few utility classes that are defined in a separate library module.
This library is added as a dependency in the modules that require them and is the only part of the plugin that ships with the server and/or pipeline.

The plugin itself is easily triggered from each module by adding a `<plugin>` element to the `<build>` element of the module's `pom.xml` file.  For example:

````XML
    <build>
        <plugins>
            <plugin>
                <groupId>gov.cms.bfd</groupId>
                <artifactId>bfd-model-rda-codegen-plugin</artifactId>
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



### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

The maven plugin would have a simple structure.  Each goal would follow these steps:

- Read the mapping file using a library such as Jackson to map the file's contents into java beans.
- Process the mappings in the file in a goal specific way to generate java code using a library such as javapoet to generate the java files.

The `entities` goal would only need to process the `table` object since it simply generates the relevant Hibernate entities and all of the data required to do so would be defined in the `table`.  Similarly a `sql-definitions` goal could do the same to generate SQL `CREATE TABLE` and `ALTER TABLE` DDL code that a developer could use as the basis of a Flyway migration file.

The `transformers` goal would additionally need to process the `transformations` and `arrays` sections since these define the relationships between the RDA API message data and the fields in the Hibernate entities.

#### Generating JPA Entity classes

The `entities` goal would generate Hibernate entities that are virtually identical (minus javapoet's code indentation, etc) to what we are currently maintaining by hand.
This would include the use of the same lombok, JPA, and hibernate annotations.
The existing integration tests would continue to pass with the generated entities.

This example illustrates how the table and its columns would be defined in YAML:

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

Some technical details for this example:

- Specifying a fully defined `entityClassName` ensures that the plugin makes no assumptions about what packages the code it generates will live in.
- The `schema` would be optional and associated annotations only added to the entity if it has been defined.
- The `name` and `sqlType` would be required.
- All other values would have reasonable defaults.  For example any `varchar(n)` or `char(n)` would default to a `String` as the `javaType` without having to specify one.  Similarly `timestamp` would map to `Instant` and `date` would map to `LocalDate` by default.
- All columns would default to being nullable unless otherwise set using a `nullable: false` since almost all RDA API fields are optional.
- The `primaryKeyColumns` would be used to add `Id` annotations to those fields in the entity classes as well as to define the `hashCode` and `equals` methods following Hibernate rules.
- Tables with multiple `primaryKeyColumns` would automatically generate a static class for the primary key object associated with the table.

Each of the most commonly used `sqlType`s would have an associated default `javaType` and appropriate logic for defining the generated `Column` annotation in the entity class.  For example the plugin would know how to parse a max length out of the `varchar(n)` and `char(n)` types.  Also it would know that it needs to add a `columnDefinition` value for `decimal(m,n)` types but not for most other types.

Each `array` definined in the YAML file would result in a `Set<TEntity>` field being created in the parent entity.  For example:

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

would generate code like this in the `PreAdjFissClaim` class:

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

If a table has multiple primary key columns the plugin would know to also generate a java bean for the composite key.  For example:

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

Would generate code like this:

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

The current code base has a transformer class for each type of claim returned by the RDA API (i.e. `FissClaimTransformer` and `McsClaimTransformer`).
These classes contain code to copy RDA API data from protobuf messages into corresponding JPA entities.
Field values are copied using a `DataTransformation` object that provides methods for validating and parsing the individual field values.
There is a `DataTransformer` method for each type of field.
These methods support the conventions RDA API uses when mapping its data into protobuf.

The plugin generated code would continue to follow this same pattern.
It would generate a transformation class for every mapping that has a `transformerClassName` value.
The transformers would contain essentially the same sequence of method calls that a developer would currently write by hand.

Each transformation class has one public method that accepts an RDA API message object and a `DataTransformer` and returns a corresponding entity object.
The `DataTransformer` is provided by the caller since it collects any validation errors and the caller can query it afterwards to determine if there were any errors.

````java
public class FissClaimTransformer2 {
  private final Function<String, String> idHasher;

  // fields generated by transformer objects inserted here

  public FissClaimTransformer2(Function<String, String> idHasher,
      EnumStringExtractor.Factory enumExtractorFactory) {
    this.idHasher = idHasher;
    // initializers for fields generated by transformer objects inserted here
    }

  public PreAdjFissClaim transformMessage(
      FissClaim from, DataTransformer transformer, Instant now) {
    final PreAdjFissClaim to = transformMessageImpl(from, transformer, now, "");
    transformMessageArrays(from, to, transformer, now, "");
    return to;
  }

  // all the generated private methods to handle individual mappings
````

The `transformMessageImpl()` and `transformMessageArrays()` methods are private methods created for each mapping that the transformation class transforms.
Generally there is one `transformMessageImpl` method for the parent mapping plus one for each array mapping.
There will currently be only one `transformMessageArrays` method since the RDA API only uses arrays at the top level.
But the plugin would be capable of handling arrays in the child objects as well.

#### Field Transformations

A transformation takes data from one field in the RDA API message, validates it, and copies it to a destination field in the entity.

Each transformation is implemented as a Java class that implements an interface.
The interface contains three methods:
- One to get zero or more field definitions for any class level fields needed by the transformer.
- One to get initialization code for each such field for use in the generated transformer class' constructor.
- One to generate any java statements needed to perform the transformation.

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

In this example `destSetter` is a helper method in the abstract base class that returns a code block that sets the value of the destination (entity) field.

A more complex transformation would add a field with an `EnumStringExtractor` object, create code to initialize it appropriately, and code to invoke it to copy the enum's value into an entity field:

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

Arrays would be recognized and generate code to also transform the array elements appropriately to produce the detail objects for the JPA entities.


### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

Collect a list of action items to be resolved or officially deferred before this RFC is submitted for final comment, including:

* ?

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

Why should we *not* do this?

* Code generators can be somewhat complex.
  A case can be made that lots of hand written code can be more directly comprehensible than a code generator.
* Since RDA API is not yet in production might their conventions change substantially in the near future?


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

A design based around a maven plugin was chosen because it integrates easily into the current build pipeline.
Also IDEs can easily detect the presence of the plugin and recognize the generated classes so that they can be debugged.
The javapoet API was chosen since it is already in use with the RIF code generator and works well for the task.

A spreadsheet could be used for the DSL.
However we decided thata YAML file format has several advantages over a spreashseet for this process:
* Existing open source libraries such as jackson can directly convert YAML into java beans.
* RDA API data is inherently heirarchical and YAML naturally supports heirachical data.
* YAML is pure text it can be edited from within an IDE and diffs of the file can be reviewed as part of the github PR review process.

We considered using java annotation processing but decided that a maven plugin has some advantages:
* The maven plugin works directly within the maven build process rather than adding the complexity of java annotation processing.
* The same plugin can be invoked from multiple modules to generate different portions of the code exactly where it is needed.

We considered defining a full blown imperative DSL using groovy or something else but:
* Writing transformations in java fits more naturally into the BFD code base and team expertise.
* Declarative structure allows the plugin to guarantee adherance to the RDA API conventions and proper code review.
* Transformations in the plugin have a standard structure that makes them easier to develop and debug.

Continuing with the existing hard-written code would have a number of disadvantages:
* It would will make it more difficult to react to changes in the RDA API going forward.
* It would greatly complicate PR reviews for RDA API changes since so many files would have to be reviewed.
* Changes in conventions by the RDA API team would require changing logic in multiple places rather than just in the plugin.  An example of this would be if they changed how they map enums to strings in their responses.


## Prior Art
[Prior Art]: #prior-art

The RIF entities are currently generated using java annotation processing and reading metedata from a spreadsheet.

## Future Possibilities
[Future Possibilities]: #future-possibilities

FROM THE TEMPLATE:
Think about what the natural extension and evolution of your proposal would be and how it would affect the language and project as a whole in a holistic way.
Try to use this section as a tool to more fully consider all possible interactions with the project and language in your proposal.
Also consider how the this all fits into the roadmap for the project and of the relevant sub-team.

This is also a good place to "dump ideas", if they are out of scope for the RFC you are writing but otherwise related.

If you have tried and cannot think of any future possibilities, you may simply state that you cannot think of anything.

Note that having something written down in the future-possibilities section is not a reason to accept the current or a future RFC;
  such notes should be in the section on motivation or rationale in this or subsequent RFCs.
The section merely provides additional information.

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.
