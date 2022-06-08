# DSL Based Code Generation Maven Plugin

This project provides a Maven plugin that reads meta data in a YAML based DSL and generates java code for:

- JPA entity classes
- Data transformation classes to convert incoming message objects into entity objects.

Currently the project contains only the components that will be used by the plugin.
The actual plugin code will be added later and this file will be updated with details on their use.

The base package for the plugin is `gov.cms.model.dsl.codegen.plugin`.
The Maven Mojo files will reside in the base package.
Other packages provide component classes and interfaces:

| Package     | Description                                                                                                                                          |
|-------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| model       | Java bean classes implementing the DSL data model.                                                                                                   |
| accessor    | Component classes implementing code generators to create accessor methods (setters and getters) in specific ways.                                    |
| transformer | Component classes implementing code generates to create statements that transform data from the source object and copy it to the destination object. |

Key interfaces:

| Interface Name     | Description                                                                                                | Sample Class             |
|--------------------|------------------------------------------------------------------------------------------------------------|--------------------------|
| `Getter`           | Implementations generate code to get values from source objects.                                           | `GrpcGetter`             |
| `Setter`           | Implementations generate code to set values in destination objects.                                        | `OptionalSetter`         |
| `FieldTransformer` | Implementations generate code to transform and copy the data for a single field using a `DataTransformer`. | `AmountFieldTransformer` |
