# DSL Based Code Generation Maven Plugin

This project provides a Maven plugin that reads meta data in a YAML based DSL and generates java code for:

- JPA entity classes
- Data transformation classes to convert incoming message objects into entity objects.
- Sample SQL for use in creating Flyway migrations.
- `CsvWriter` classes for use with RIF pipeline.

This project is the follow up
to [RFC 0014-dsl-driven-rda-code-generation.md](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/docs/rfcs/0014-dsl-driven-rda-code-generation.md)
. This code is based on that concept but some features have evolved since that RFC was written.
The plugin is used for both RDA and CCW/RIF data models.

The plugin is only executed at build time by maven so it is not needed at runtime by applications using code that it generated.
However there are runtime components used by the generated code.  These are in the `bfd-model-dsl-codegen-library` project.
That project's artifacts must be present at runtime for the generated code to work properly.

The base package for the plugin is `gov.cms.model.dsl.codegen.plugin`.
The Maven Mojo files reside in the base package.

Other packages provide component classes and interfaces:

| Package          | Description                                                                                                                                           |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| model            | Java bean classes implementing the DSL data model.                                                                                                    |
| model.validation | Hibernate validator support classes used when validating mappings.                                                                                    |
| accessor         | Component classes implementing code generators to create accessor methods (setters and getters) in specific ways.                                     |
| transformer      | Component classes implementing code generators to create statements that transform data from the source object and copy it to the destination object. |

## Key interfaces:

| Interface Name   | Description                                                                                                | Sample Class           |
|------------------|------------------------------------------------------------------------------------------------------------|------------------------|
| Getter           | Implementations generate code to get values from source objects.                                           | GrpcGetter             |
| Setter           | Implementations generate code to set values in destination objects.                                        | OptionalSetter         |
| FieldTransformer | Implementations generate code to transform and copy the data for a single field using a `DataTransformer`. | AmountFieldTransformer |

## Using the plugins:

The plugins are enabled just as any other plugin. One or more goals must be specified. Which ones to use depends on the
context.

```
<plugin>
    <groupId>gov.cms.bfd</groupId>
    <artifactId>bfd-model-dsl-codegen-plugin</artifactId>
    <version>${project.version}</version>
    <configuration>
        <mappingPath>${project.basedir}/mappings</mappingPath>
        <outputDirectory>${project.build.directory}/generated-sources/dsl</outputDirectory>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>entities</goal>
                <goal>sql</goal>
                <goal>transformers</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The supported goals are:

| Goal Name    | Mojo Class                      | Description                                                                                                                                 |
|--------------|---------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| entities     | GenerateEntitiesFromDslMojo     | Generates one JPA entity class per mapping found at `mappingPath`. Classes are written to `entitiesDirectory`.                              |
| sql          | GenerateSqlFromDslMojo          | Generates a text file containing sample SQL for each mapping found at `mappingPath`. SQL is written to `sqlFile`.                           |
| transformers | GenerateTransformersFromDslMojo | Generates one transformer class for each mapping that has a defined `transformerClassName`. Classes are written to `transformersDirectory`. |
| csv-writers  | GenerateCsvWritersFromDslMojo   | Generates one CsvWriter class for each mapping that has a defined `transformerClassName`.  Classes are written to `csvWritersDirectory`.    |

Generally the `entities` and `sql` goals will be used together in a model project to create a jar containing entities.
The `transformers` goal would be used in whichever maven project should include the classes used to transform incoming
messages into entity objects. This might be a model project (e.g. `bfd-model-rif`) or a pipeline project
(e.g. `bfd-pipeline-rda-grpc`).
The `csv-writers` goal is only intended for use in the `bfd-model-rif` project.

The configuration properties for the plugins are:

| Property              | Goals                                    | Description                                                                            |
|-----------------------|------------------------------------------|----------------------------------------------------------------------------------------|
| mappingPath           | entities, sql, transformers, csv-writers | Path to either a single mapping file or a directory containing multiple mapping files. |
| entitiesDirectory     | entities                                 | Directory to write generated class files to.                                           |
| sqlFile               | sql                                      | File to write generated SQL code to.                                                   |
| transformersDirectory | transformers                             | Directory to write generated class files to.                                           |
| csvWritersDirectory   | csv-writers                              | Directory to write generated class files to.                                           |

## DSL Syntax Guide

DSL is stored in one or more YAML files.
Each YAML file contains an array of mappings.
Each mapping defines one database table, JPA entity, and (optionally) RDA API message object or CSV/RIF record layout.
All objects in the DSL map directly to classes in the `gov.cms.model.dsl.codegen.plugin.model` package.

### mapping

Top level properties in each mapping are:

- id: Unique identifier (name) of the mapping.
- messageClassName: Name of the protobuf message class as defined in the RDA API proto files or the CSV adaptor class
  for RIF data.
- entityClassName: Name of the generated JPA entity class to store objects of this type.
- transformerClassName: Name of the generated class to implement the transformation of protobuf message objects into JPA
  entities.
- sourceType: Specifies how to access values from message objects. Possible values are `Grpc` or `RifCsv`.
- nullableFieldAccessorType: Specifies how nullable values are passed to and from field accessor methods. Possible
  values are `Standard` or `Optional`.
- table: Object defining details about the database table that the JPA entity maps to.
- minStringLength: Default minimum string length for non-null string fields. (defaults to 1)
- enumTypes: Array of objects defining any `enum` classes that the generator should create for use in the
  transformation.
- transformations: Array of objects defining what transformations to apply to an RDA API message to copy its data into a
  JPA entity.
- externalTransformations: Array of objects defining external transformations provided to the generated transformer
  object's constructor.
- entityInterfaces: Array of interface names that entity class should implement.

### mapping.enumTypes

The `enumType` objects are sometimes used as flags in mapping messages to entities.
For example the RDA API sends data in one of two possible sub-objects for payer details in FissPayer messages and the
transformer copies data from whichever one has data and sets an enum column in the entity to indicate which of the two
it copied the data from.
They are also used in RIF mappings to trigger creation of the column enums.
The properties of these objects in the DSL are:

- name: Name of the enum class in java.
- packageName: Package to contain the enum if it is not an inner class of the entity. (default is empty string to
  trigger enum as an inner class of the JPA entity)
- values: Array of enum value names added to the enum in the java code.

### mapping.externalTransformations

The `externalTransformation` objects are sometimes used to allow the class which will call the generated transformer to
pass one or more lambdas to call on each transformed object.
These can be used to perform some post processing or complex/unique validation on the entities.
The properties of these objects in the DSL are:

- name: Name to use for the field that will hold a lambda used to trigger the transformation.

The generated transformer code will expect the client to pass a lanbda function for each `externalTransformation`.
The lambda must implement the `ExternalTransformation` interface.

### mapping.table

The `table` objects contain the following properties:

- name: Name of the table as it appears in the database.
- schema: Name of the schema containing the table (optional).
- comment: Text to insert into a javadoc for the JPA entity generated for this table.
- quoteNames: When true indicates schema, database, and column names should be quoted in JPA entities. (defaults
  to `false`)
- equalsNeeded: When true indicates entity classes need to include equals/hashCode methods. (defaults to `true`)
- compositeKeyClassName: Name of static inner class used for composite keys. (defaults to `PK`)
- primaryKeyColumns: Array of column names for the entity's primary keys.
- equalsColumns: Array of column names to compare in the entity's generated equals method. (defaults to same values
  as `primaryKeyColumns`)
- columns: Array of objects defining the columns in the table.
- joins: Array of objects defining specific join relationships between this entity and some other entity.
- additionalFieldNames: Array of objects defining additional fields to add to the JPA entity class' lombok
  generated `Fields` class.

### table.columns

The `column` objects have the following properties:

- name: Name of the column as it appears in the database and entity class.
- dbName: Name of the column in the database (if different than the field name in the entity).
- sqlType: Type of the column as it would appear in SQL DDL defining the column.
- javaType: Type of the field in the JPA entity class that holds this column's value.
- javaAccessorType: Type of the field's accessor methods if it differs frm field type (for long/String in RIF).
- enumType: Name of an enum defined in the same mapping as this column.
- comment: Text to insert into a javadoc for the field generated for this column in the JPA entity.
- nullable: Boolean indicating if the column is nullable. (default is `true`)
- identity: Indicates whether or not the column in the database is an IDENTITY column. (default is `false`)
- updatable: Indicates whether to add the updatable argument to the `Column` annotation. (default is `true`)
- fieldType: Indicates what type of field should be generated. Either `Column` or `Transient`.  (default is `Column`)
- minLength: Minimum allowed length for a non-null string value in this field. Default is to use the value in
  the `table`.
- dbOnly: When true the column will be added to generated SQL but not added as a field in entities.  Default value is false.  Intended for use with joinColumnName.
- sequence: Object defining the database sequence to use to populate this column.

### column.fieldType

This enum has two possible values:

- Column: field is a normal database column.
- Transient: field is present in the entity but is is not a column in the database.

### mapping.transformations

The `transformation` objects define how to copy data from a field in an RDA API message to a field in the JPA entity.
Generally each field has one transformation applied to it, but the DSL allows multiple transformations to be applied.
A simple example of needing multiple transformations is a the `mbi` field.
It requires two transformations: one to copy the field as is and another to store its hashed value.
The properties of `transformation` objects in the DSL are:

- from: Name of the field to transform as it appears in an RDA API message. This can be a two part compound name if
  necessary for gRPC messages.
- to: Name of the field in the JPA entity to store the the transformed value into.
- optional: Boolean indicating whether a missing field value in the RDA API message is allowed.
- optionalComponents: Specifies which parts of the `from` can be optional. Possible values are `FieldOnly`
  , `PropertyOnly`, `FieldAndProperty` (the default), or `None`.
- transformer: Specifies the name of the transformation to apply. These are predefined in the code generator.
- transformerOptions: Optional objects containing key/value pairs of configuration options for the transformer. These
  settings are specific to the particular transformer.

### table.joins

The `join` objects contain the following properties:

- class: Optional.  One of `array`, `child`, or `parent`.  See below for details. 
- fieldName: Name of the java field in the entity class.
- entityClass: Optional.  Full (package plus class) class name of the entity being joined. Used when the entity is not defined
  within the model.Either entityClass or entityMapping must be defined but not both.
- entityMapping: Optional.  ID of the entity's mapping. Used when the entity is defined within the model. Either entityClass or
  entityMapping must be defined but not both.
- joinColumnName: Which dbOnly column is used to map parent primary key.  For use with child joins.
- joinType: Type of JPA join annotation to use (OneToMany, ManyToOne, OneToOne).
- fetchType: value for fetchType parameter in JPA annotation.
- comment: Optional comment string to be added to the join field in the entity class.
- collectionType: Type of collection to use for field.  Possible values are `List` or `Set`.
- mappedBy: value for mappedBy parameter in JPA annotation.
- orphanRemoval: value for orphanRemoval parameter in JPA annotation.
- cascadeTypes: array of values for cascade parameter in JPA annotation.
- orderBy: Optional SQL to use in ORDER BY clause during join.
- foreignKey: Optional name of a foreign key constraint
- readOnly: When true no setter is generated for the field.
- properties: Optional array of objects defining properties of the joined entity that we want to expose as getters 
  in this entity.

### joins.class

The `class` property of a `join` can be used to set default values and special validity constraints depending on the type of relationship the join represents.

| class  | Relationship                                    | Defaults                                                                                                                 | Required Properties               |
|--------|-------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|-----------------------------------|
| array  | RDA API style array of unordered child objects. | `joinType=OneToMany`, `collectionType=Set`, `fetchType=EAGER`, `orphanRemoval=true`, `cascadeTypes=ALL`                  | `entityMapping`                   |
| child  | Child side of parent-child relationship.        | `joinType=ManyToOne`                                                                                                     | `entityMapping`, `joinColumnName` |
| parent | Parent side of parent-child relationship.       | `joinType=OneToMany`, `collectionType=List`, `fetchType=LAZY`, `readOnly=true`, `orphanRemoval=true`, `cascadeTypes=ALL` | `entityMapping`, `mappedBy`       | 

### joins.properties

The `property` objects include information required to generate a getter method in the entity that returns the value of a property in the joined entity.
For example if this entity links to the `MbiCache` table via a foreign key `mbiId` and you want this entity to have a `getMbi()` method that
returns the actual MBI string from the `MbiCache` record you would do that by adding a `property`.

Each `property` has the following fields:

| field     | Description                                                                                                     |
|-----------|-----------------------------------------------------------------------------------------------------------------|
| name      | Property name used to a create an appropriate getter method name. (e.g. `mbi` would create a `getMbi()` method) |
| fieldName | Name of the field in the joined entity that contains the value for the getter to return.                        |
| javaType  | The java type of the field being returned.                                                                      |


### mapping.transformers

Each `transformer` defines how a field in the message object should be transformed and copied into the entity by the generated
transformer class.  In order to use `transformers`the `mapping` must have a valid value in its `transformerClassName` property.

The `transformer` objects have the following properties:

- from: The name of the field in the source object to transform.
- to: Optional.  Name of the field in the destination object to receive the transformed value.  Defaults to value of `from`.
- optionalComponents: Specifies which components (if any) of the `from` field are optional.  See below for details.
- transformer: Name of the transformation to apply.
- defaultValue: Optional default value to use if `from` has an empty string value.
- transformerOptions: Optional.  Zero or more objects containing key/value pair of `transformer` specific configuration options.

### transformers.optionalComponents

Since property names can be compound (e.g. `detailRecords.detailId`) the transformers have be deal with multiple possible 
missing values when trying to access a property on the `from` object at runtime.

- The first part (field) could be null/missing (e.g. `detailRecords`)
- The second part (property) could be null/missing (e.g. `detailId`)

The `optionalComponents` setting defines which parts can be missing at runtime without triggering an error.
Possible values are:

| Value            | Description                                                                                               |
|------------------|-----------------------------------------------------------------------------------------------------------|
| FieldOnly        | Only the first component (field) is optional.  If the field is present the property must also be present. |
| PropertyOnly     | Only the second component (property) is optional.  The field must always be present.                      |
| FieldAndProperty | Both the first and second components are optional.  Either or both can be missing at runtime.             |
| None             | Both the first and second components are required to be present at runtime.                               |

### transformers.transformer

The valid `transformer` names for use in a `transformation` are:

| Name               | Description                                                                                       |
|--------------------|---------------------------------------------------------------------------------------------------|
| Array              | Transforms all child objects within a collection.  Used with array joins.                         |
| IdHash             | Hashes a string before storing it into the `to` field.                                            |
| Now                | Stores the current timestamp into the `to` field.                                                 |
| MessageEnum        | Uses an `EnumStringExtractor` instance to extract a string value from an enum value.              |
| EnumValueIfPresent | Stores a specific enum in the `to` field if the `from` field has any value.                       |
| RifTimestamp       | Parses a RIF timestamp string in `from` and copies the result to `to`.                            |
| IntString          | Parses an int string in `from` and copies the result to `to`.                                     |
| ShortString        | Parses a short string in `from` and copies the result to `to`.                                    |
| LongString         | Parses a long string in `from` and copies the result to `to`.                                     |
| UintToShort        | Copies a valid signed 16 bit integer from an unsigned long and copies it to `to` as a java short. |
| Base64             | Base64 encodes a given string in the `from` and copies the results to `to`.                       |

When `transformer` is omitted the transformation to use can be inferred from the `from` name.

| Name   | Description                                                                                     |
|--------|-------------------------------------------------------------------------------------------------|
| NOW    | Alias for `Now`.                                                                                |
| NONE   | No transformation is done at all. (used during development)                                     |
| PARENT | Used with array elements. Populates `to` with the value of the same field in the parent object. |
| INDEX  | Used with array elements. Populates `to` with the array index of the element.                   |

When `transformer` is omitted and no transformer corresponds to the `from` name one may still be inferred based on the column type.
Supported types for this inference are: char, string, int, long, numeric, or date.

### transformers.transformerOptions

The `Base64` transformer accepts these options:

- decodedLength: Length of the value before it is Base64 encoded, to be used for length validation checks.

The `EnumValueIfPresent` transformer accepts these options:

- enumName: Name of the enum (as defined in the mapping's `enumTypes` array) that defines the value.
- enumValue: Name of the specific enum value to assign to the `to` field (must be one of the ones listed in the `enumType`).

The `MessageEnum` transformer uses an `EnumStringExtractor` instance to extract a string value from an RDA API
`oneof` containing an enum field and a string field. The latter is used for unrecognized values.
By convention these fields have names ending in `Enum` and `Unrecognized`, respectively.
`MessageEnum` accepts these options:

- enumClass: The java class for the RDA API enum.
- hasUnrecognized: Boolean (true or false) indicating whether the message object has an additional field for an unrecognized value. (default is `true`)
- unsupportedEnumValues: Comma separated names of specific enum values that should be rejected as invalid.
- extractorOptions: Comma separated names of `EnumStringExtractor.Options` values to use when creating the `EnumStringExtractor`.
- enumNameSuffix: Suffix appended to the raw `from` field name to find the enum value field. (default is `Enum`)
- unrecognizedNameSuffix: Suffix appended to the raw `from` field name to find the unrecognized value field. (default is `Unrecognized`)

The implicit string transformer accepts a single option, `ignoreEmptyString`, which defaults to `false`.
When the option is specified as `true` any empty string value in the message will be ignored (i.e. no value will be stored in the `to` field).
Otherwise empty string values will be stored in the `to` field.
