# DSL Based Code Generation Maven Plugin

This project provides a Maven plugin that parses PDF files to generate:

- One XML file per PDF containing all variables defined within the PDF.
- One java source file for an `enum` containing one value for each unique variable defined any XML.

This project replaces an annotation processor that did the same thing but had some
compatibility issues with IDEs.

The plugin is only executed at build time by maven so it is not needed at runtime by applications using code that it
generated.
However there are runtime components used by the generated code. These are in the `bfd-model-codebook-library` project.
That project's artifacts must be present at runtime for the generated code to work properly.

The plugin has a single mojo named `CodebookMojo` that does all of its work when the `codebook` goal is executed.
The Maven Mojo resides in the `gov.cms.model.codebook.extractor` package.

The PDF files to parse are defined as java resources in the plugin.
The source directory for those (relative to the `bfd-model-codebook-plugin` module directory) is `src/main/resources`.
One PDF file must be present for each value defined in the `SupportedCodebook` enum from
the `bfd-model-codebook-library`.

## Using the plugins:

The plugins are enabled just as any other plugin. One or more goals must be specified. Which ones to use depends on the
context.

```
<plugin>
    <!-- Generates XML files and enum class for codebook variables using codebook pdfs. -->
    <groupId>gov.cms.bfd</groupId>
    <artifactId>bfd-model-codebook-plugin</artifactId>
    <version>${project.version}</version>
    <configuration>
        <xmlFilesDirectory>${project.build.directory}/generated-resources</xmlFilesDirectory>
        <javaFilesDirectory>${project.build.directory}/generated-sources</javaFilesDirectory>
        <enumPackage>gov.cms.bfd.model.codebook.data</enumPackage>
        <enumClass>CcwCodebookVariable</enumClass>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>codebooks</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

The supported goals are:

| Goal Name | Mojo Class   | Description                                                   |
|-----------|--------------|---------------------------------------------------------------|
| codebook  | CodebookMojo | Generates one XML file per PDF plus a single java enum class. |

The configuration properties for the plugin are:

| Property           | Description                                | Default                                          |
|--------------------|--------------------------------------------|--------------------------------------------------|
| xmlFilesDirectory  | Directory to write generated XML files to. | `${project.build.directory}/generated-resources` |
| javaFilesDirectory | Directory to write generated java code to. | `${project.build.directory}/generated-sources`   |
| enumPackage        | Java package for enum class.               | `gov.cms.bfd.model.codebook`                     |
| enumClass          | Class name for enum class.                 | `CcwCodebookVariable`                            |
| warnAboutVariables | Boolean setting.  See below for more info. | `false`                                          |

The `PdfParser` class has an option to log a warning message for every codebook variable.
This is disabled by default. It is only useful when parsing a new PDF that might not parse correctly.
