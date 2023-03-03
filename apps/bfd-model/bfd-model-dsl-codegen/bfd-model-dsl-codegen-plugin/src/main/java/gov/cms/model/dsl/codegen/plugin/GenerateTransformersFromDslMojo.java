package gov.cms.model.dsl.codegen.plugin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.model.dsl.codegen.library.EnumStringExtractor;
import gov.cms.model.dsl.codegen.library.ExternalTransformation;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.OptionalSetter;
import gov.cms.model.dsl.codegen.plugin.accessor.RifGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.ExternalTransformationBean;
import gov.cms.model.dsl.codegen.plugin.model.JoinBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import gov.cms.model.dsl.codegen.plugin.transformer.FieldTransformer;
import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import javax.lang.model.element.Modifier;
import lombok.AllArgsConstructor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * A Maven Mojo that generates code to copy and transform data from RDA API message objects into JPA
 * entity objects. The generated transformer class consists of the following parts:
 *
 * <ul>
 *   <li>Private final field declarations section containing one field for each {@link
 *       FieldTransformer} that requires one.
 *   <li>Private final field declaration for a lambda function to convert strings into hashed
 *       strings if one is required by the {@link TransformationBean}.
 *   <li>Private final field declaration for each {@link ExternalTransformation} lambda function
 *       required by the {@link TransformationBean}.
 *   <li>A public constructor that accepts any arguments needed to initialize the private final
 *       fields.
 *   <li>A simple public {code transformMessage()} method for each message class defined by the root
 *       {@link MappingBean}. The simple method accepts just a message class instance and either
 *       returns a valid entity class instance or throws a {@link
 *       DataTransformer.TransformationException} if there were any validation errors.
 *   <li>A more verbose public {@code transformMessage()} method for each message class defined by
 *       the root * {@link MappingBean}. The verbose method accepts instances of {@link
 *       DataTransformer} and {@link Instant} to use for the transformation process. The method
 *       always returns an entity class instance. The entity may be incomplete if there are any data
 *       transformation errors. The caller is required to check the {@link DataTransformer} object
 *       to determine if any errors were detected.
 *   <li>One private {@code transformMessageToX()} method for each message class. Each of these
 *       methods produce an entity instance with all of its single fields populated.
 *   <li>One private {@code transformMessageArraysToX()} method for each message class that contains
 *       at least one array of sub-messages. Each of these methods accepts an existing entity
 *       instance and populates any collections in the instance.
 * </ul>
 */
@Mojo(name = "transformers", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateTransformersFromDslMojo extends AbstractMojo {
  /** Name of the public method generated for top level message transformation method. */
  private static final String TRANSFORM_MESSAGE_METHOD_NAME = "transformMessage";

  /**
   * Base name for private methods generated for each message class involved in the transformation
   * process. The message name is appended to the end of this base to form the unique method name.
   */
  private static final String TRANSFORM_MAPPING_METHOD_NAME_BASE = "transformMessageTo";

  /**
   * Base name for private methods generated for each message class containing arrays. The message
   * name is appended to the end of this base to form the unique method name.
   */
  private static final String TRANSFORM_ARRAYS_METHOD_NAME_BASE = "transformMessageArraysTo";

  /** Path to a single mapping file or a directory containing one or more mapping files. */
  @Parameter(property = "mappingPath")
  private String mappingPath;

  /** Path to directory to contain generated code. */
  @Parameter(
      property = "transformersDirectory",
      defaultValue = "${project.build.directory}/generated-sources/transformers")
  private String transformersDirectory;

  /**
   * Instance of {@link MavenProject} used to call {@link MavenProject#addCompileSourceRoot(String)}
   * to ensure our generated classes are compiled.
   */
  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /** Parameterless constructor used by Maven to instantiate the plugin. */
  public GenerateTransformersFromDslMojo() {}

  /**
   * All fields constructor for use in unit tests.
   *
   * @param mappingPath path to file or directory containing mappings
   * @param transformersDirectory path to directory to contain generated code
   * @param project instance of {@link MavenProject}
   */
  @VisibleForTesting
  GenerateTransformersFromDslMojo(
      String mappingPath, String transformersDirectory, MavenProject project) {
    this.mappingPath = mappingPath;
    this.transformersDirectory = transformersDirectory;
    this.project = project;
  }

  /**
   * Executed by maven to execute the mojo. Reads all mapping files and generates a transformer
   * class for every {@link MappingBean} that has a non-empty {@link
   * MappingBean#transformerClassName} value.
   *
   * @throws MojoExecutionException if the process fails due to some error
   */
  public void execute() throws MojoExecutionException {
    try {
      final File outputDir = MojoUtil.initializeOutputDirectory(transformersDirectory);
      final RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingPath);
      MojoUtil.validateModel(root);
      for (MappingBean mapping : root.getMappings()) {
        if (mapping.hasTransformer()) {
          TypeSpec rootEntity = createTransformerClassForMapping(root, mapping);
          JavaFile javaFile = JavaFile.builder(mapping.transformerPackage(), rootEntity).build();
          javaFile.writeTo(outputDir);
        }
      }
      project.addCompileSourceRoot(transformersDirectory);
    } catch (IOException ex) {
      throw new MojoExecutionException("I/O error during code generation", ex);
    }
  }

  /**
   * Creates a {@link TypeSpec} defining a transformer class for the given {@link MappingBean}. The
   * public {@code transformMessage()} method of the class processes one {@link
   * MappingBean#messageClassName} instance to produce a corresponding {@link
   * MappingBean#entityClassName} instance.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to create transformer class for
   * @return the {@link TypeSpec}
   * @throws MojoExecutionException if any problems arise
   */
  private TypeSpec createTransformerClassForMapping(RootBean root, MappingBean mapping)
      throws MojoExecutionException {
    final List<MappingBean> allMappings = findAllMappingsForRootMapping(root, mapping);
    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(mapping.transformerSimpleName()).addModifiers(Modifier.PUBLIC);
    for (MappingBean aMapping : allMappings) {
      for (FieldSpec field : createFieldsForMapping(aMapping)) {
        classBuilder.addField(field);
      }
    }
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    if (TransformerUtil.anyMappingRequiresIdHasher(allMappings.stream())) {
      classBuilder.addField(
          ParameterizedTypeName.get(Function.class, String.class, String.class),
          FieldTransformer.HASHER_VAR,
          Modifier.PRIVATE,
          Modifier.FINAL);
      constructor
          .addParameter(
              ParameterizedTypeName.get(Function.class, String.class, String.class),
              FieldTransformer.HASHER_VAR)
          .addStatement("this.$L = $L", FieldTransformer.HASHER_VAR, FieldTransformer.HASHER_VAR);
    }
    if (TransformerUtil.mappingRequiresEnumExtractor(mapping)) {
      constructor.addParameter(
          EnumStringExtractor.Factory.class, FieldTransformer.ENUM_FACTORY_VAR);
    }
    for (MappingBean aMapping : allMappings) {
      for (CodeBlock initializer : createFieldInitializersForMapping(aMapping)) {
        constructor.addCode(initializer);
      }
      if (aMapping.hasExternalTransformations()) {
        addExternalTransformationFieldsForMapping(aMapping, classBuilder);
        addExternalTransformationConstructorParametersForMapping(aMapping, constructor);
      }
    }
    classBuilder.addMethod(constructor.build());
    classBuilder.addMethod(createSimplifiedTransformMessageMethod(mapping));
    classBuilder.addMethod(createTransformRootMessageMethod(mapping));
    for (MappingBean aMapping : allMappings) {
      classBuilder.addMethod(createTransformMethodForMapping(aMapping));
      if (aMapping.hasArrayTransformations()) {
        classBuilder.addMethod(createTransformArraysMethodForMapping(root, aMapping));
      }
    }
    return classBuilder.build();
  }

  /**
   * Finds all {@link MappingBean}s reachable from the specified root mapping by recursively
   * following all of its array mappings. The result is all of the mappings that this transformer
   * class will need to generate code for.
   *
   * @param root {@link RootBean} containing all known mappings.
   * @param firstMapping Base {@link MappingBean} that to use as starting point in search for
   *     mappings.
   * @return List of all mappings reachable from {@code firstMapping} (including {@code
   *     firstMapping}).
   */
  private List<MappingBean> findAllMappingsForRootMapping(RootBean root, MappingBean firstMapping)
      throws MojoExecutionException {
    final ImmutableList.Builder<MappingBean> answer = ImmutableList.builder();
    final Set<String> visited = new HashSet<>();
    final List<MappingBean> queue = new ArrayList<>();

    queue.add(firstMapping);
    while (queue.size() > 0) {
      MappingBean mapping = queue.remove(0);
      visited.add(mapping.getId());
      answer.add(mapping);
      for (ArrayTransformSpec spec : getAllArrayTransforms(mapping)) {
        Optional<MappingBean> elementMapping = root.findMappingForJoinBean(spec.join);
        if (elementMapping.isPresent() && !visited.contains(elementMapping.get().getId())) {
          queue.add(elementMapping.get());
        }
      }
    }
    return answer.build();
  }

  /**
   * Creates a {@link FieldSpec} for each private field of the generated transformer class. The
   * actual fields to be created are dictated by the {@link FieldTransformer#generateFieldSpecs}
   * method of all of the {@link FieldTransformer}s used by the {@link MappingBean#transformations}.
   *
   * @param mapping the {@link MappingBean} to generate fields for
   * @return list of required fields
   */
  private List<FieldSpec> createFieldsForMapping(MappingBean mapping) {
    return mapping.getTransformations().stream()
        .filter(t -> !t.isArray())
        .flatMap(
            transformation -> {
              final ColumnBean column = mapping.getTable().findColumnByName(transformation.getTo());
              return TransformerUtil.selectTransformerForField(column, transformation)
                  .map(
                      transformer ->
                          transformer.generateFieldSpecs(mapping, column, transformation))
                  .orElse(Collections.emptyList()).stream();
            })
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Creates an initializer statement to be added to the generated transformer's constructor for
   * each private field of the generated transformer class. The actual field initializers to be
   * created are dictated by the {@link FieldTransformer#generateFieldInitializers} method of all of
   * the {@link FieldTransformer}s used by the {@link MappingBean#transformations}.
   *
   * @param mapping the {@link MappingBean} to generate field initializers for
   * @return list of required field initializations
   */
  private List<CodeBlock> createFieldInitializersForMapping(MappingBean mapping) {
    return mapping.getTransformations().stream()
        .filter(t -> !t.isArray())
        .flatMap(
            transformation -> {
              final ColumnBean column = mapping.getTable().findColumnByName(transformation.getTo());
              return TransformerUtil.selectTransformerForField(column, transformation)
                  .map(
                      transformer ->
                          transformer.generateFieldInitializers(mapping, column, transformation))
                  .orElse(Collections.emptyList()).stream();
            })
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Updates the provided {@link MethodSpec.Builder} object by adding a parameter and an
   * initialization statement for all {@link ExternalTransformation} used by the {@link
   * MappingBean}'s {@link TransformationBean}s.
   *
   * @param mapping the {@link MappingBean} to generate code for
   * @param constructor the {@link MethodSpec.Builder} for the transformer's constructor
   */
  private void addExternalTransformationConstructorParametersForMapping(
      MappingBean mapping, MethodSpec.Builder constructor) {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final ClassName interfaceType = ClassName.get(ExternalTransformation.class);
    final TypeName parameterType =
        ParameterizedTypeName.get(interfaceType, messageClassType, entityClassType);
    for (ExternalTransformationBean externalTransformation : mapping.getExternalTransformations()) {
      final var parameterName = externalTransformation.getName();
      constructor.addParameter(parameterType, parameterName);
      constructor.addStatement("this.$L = $L", parameterName, parameterName);
    }
  }

  /**
   * Updates the provided {@link TypeSpec.Builder} object by adding a field for all {@link
   * ExternalTransformation} used by the {@link MappingBean}'s {@link TransformationBean}s. Each
   * field will hold an {@link ExternalTransformation} instance.
   *
   * @param mapping the {@link MappingBean} to generate code for
   * @param classBuilder the {@link TypeSpec.Builder} for the generated transformer class
   */
  private void addExternalTransformationFieldsForMapping(
      MappingBean mapping, TypeSpec.Builder classBuilder) {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final ClassName interfaceType = ClassName.get(ExternalTransformation.class);
    final TypeName fieldType =
        ParameterizedTypeName.get(interfaceType, messageClassType, entityClassType);
    for (ExternalTransformationBean externalTransformation : mapping.getExternalTransformations()) {
      classBuilder.addField(
          FieldSpec.builder(
                  fieldType, externalTransformation.getName(), Modifier.PRIVATE, Modifier.FINAL)
              .build());
    }
  }

  /**
   * Creates a method {@link MethodSpec} for a public method that transforms a message instance into
   * an entity instance for a given {@link MappingBean}. All properties and arrays of the message
   * are processed to produce the resulting entity. The generated method creates instances of {@link
   * DataTransformer} and {@link Instant} every time it is called. Once transformation is complete
   * the generated method checks for any transformation errors and throws an exception rather than
   * returning if there are any.
   *
   * @param mapping {@link MappingBean} for message/entity to be processed
   * @return the {@link MethodSpec}
   */
  private MethodSpec createSimplifiedTransformMessageMethod(MappingBean mapping) {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(TRANSFORM_MESSAGE_METHOD_NAME)
            .returns(entityClassType)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(messageClassType, FieldTransformer.SOURCE_VAR);
    builder.addStatement(
        "final $T $L = new $T()",
        DataTransformer.class,
        FieldTransformer.TRANSFORMER_VAR,
        DataTransformer.class);
    builder.addStatement(
        "final $T $L = $L($L, $L, Instant.now())",
        entityClassType,
        FieldTransformer.DEST_VAR,
        TRANSFORM_MESSAGE_METHOD_NAME,
        FieldTransformer.SOURCE_VAR,
        FieldTransformer.TRANSFORMER_VAR);
    CodeBlock.Builder errorCheck =
        CodeBlock.builder()
            .beginControlFlow("if ($L.getErrors().size() > 0)", FieldTransformer.TRANSFORMER_VAR)
            .addStatement(
                "throw new $T($S, $L.getErrors())",
                DataTransformer.TransformationException.class,
                "data transformation failed",
                FieldTransformer.TRANSFORMER_VAR)
            .endControlFlow();
    builder.addCode(errorCheck.build());
    builder.addStatement("return $L", FieldTransformer.DEST_VAR);
    return builder.build();
  }

  /**
   * Creates a method {@link MethodSpec} for a public method that transforms a message instance into
   * an entity instance for a given {@link MappingBean}. All properties and arrays of the message
   * are processed to produce the resulting entity. The generated method uses instances of {@link
   * DataTransformer} and {@link Instant} that are passed to the method by the caller. This allows
   * the caller to reuse these if desired any/or to implement additional error logic rather than
   * just catching an exception.
   *
   * @param mapping {@link MappingBean} for message/entity to be processed
   * @return the {@link MethodSpec}
   */
  private MethodSpec createTransformRootMessageMethod(MappingBean mapping) {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(TRANSFORM_MESSAGE_METHOD_NAME)
            .returns(entityClassType)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(messageClassType, FieldTransformer.SOURCE_VAR)
            .addParameter(DataTransformer.class, FieldTransformer.TRANSFORMER_VAR)
            .addParameter(Instant.class, FieldTransformer.NOW_VAR);
    builder.addStatement(
        "final $T $L = $L($L,$L,$L,$S)",
        entityClassType,
        FieldTransformer.DEST_VAR,
        createTransformMethodNameForMapping(mapping),
        FieldTransformer.SOURCE_VAR,
        FieldTransformer.TRANSFORMER_VAR,
        FieldTransformer.NOW_VAR,
        "");
    if (mapping.hasArrayTransformations()) {
      builder.addStatement(
          "$L($L,$L,$L,$L,$S)",
          createTransformArraysMethodNameForMapping(mapping),
          FieldTransformer.SOURCE_VAR,
          FieldTransformer.DEST_VAR,
          FieldTransformer.TRANSFORMER_VAR,
          FieldTransformer.NOW_VAR,
          "");
    }
    builder.addStatement("return $L", FieldTransformer.DEST_VAR);
    return builder.build();
  }

  /** Used as a return value for {@link GenerateTransformersFromDslMojo#getAllArrayTransforms}. */
  @AllArgsConstructor
  @VisibleForTesting
  static class ArrayTransformSpec {
    /** Copy of {@link TransformationBean#from} value. */
    private final String from;
    /** Matching {#link {@link JoinBean} to transform. */
    private final JoinBean join;
  }

  /**
   * Finds all {@link TransformationBean} in the mapping that specify a join to be transformed as an
   * array and returns them in a list of {@link ArrayTransformSpec} objects.
   *
   * @param mapping {@link MappingBean} for message/entity to be processed
   * @return the list of {@link ArrayTransformSpec}
   * @throws MojoExecutionException if a mapping is invalid
   */
  List<ArrayTransformSpec> getAllArrayTransforms(MappingBean mapping)
      throws MojoExecutionException {
    List<ArrayTransformSpec> specs = new ArrayList<>();
    for (TransformationBean transformation : mapping.getTransformations()) {
      if (transformation.isArray()) {
        Optional<JoinBean> join = mapping.findJoinByFieldName(transformation.getTo());
        if (join.isPresent()) {
          specs.add(new ArrayTransformSpec(transformation.getFrom(), join.get()));
        } else {
          throw MojoUtil.createException(
              "array transform references field with no matching join: mapping=%s from=%s to=%s",
              mapping.getId(), transformation.getFrom(), transformation.getTo());
        }
      }
    }
    return List.copyOf(specs);
  }

  /**
   * Creates a method {@link MethodSpec} for a method that transforms a message instance into an
   * entity instance for a given {@link MappingBean}. Only the properties of the message are
   * transformed by the generated method. A separate generated method transforms any arrays the
   * message may contain.
   *
   * @param mapping {@link MappingBean} for message/entity to be processed
   * @return the {@link MethodSpec}
   */
  private MethodSpec createTransformMethodForMapping(MappingBean mapping)
      throws MojoExecutionException {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(createTransformMethodNameForMapping(mapping))
            .returns(entityClassType)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(messageClassType, FieldTransformer.SOURCE_VAR)
            .addParameter(DataTransformer.class, FieldTransformer.TRANSFORMER_VAR)
            .addParameter(Instant.class, FieldTransformer.NOW_VAR)
            .addParameter(String.class, FieldTransformer.NAME_PREFIX_VAR)
            .addStatement(
                "final $T $L = new $T()",
                entityClassType,
                FieldTransformer.DEST_VAR,
                entityClassType);
    final var fromCodeGenerator =
        mapping.getSourceType() == MappingBean.SourceType.RifCsv
            ? RifGetter.Instance
            : GrpcGetter.Instance;
    final var toCodeGenerator =
        mapping.getNullableFieldAccessorType() == MappingBean.NullableFieldAccessorType.Standard
            ? StandardSetter.Instance
            : OptionalSetter.Instance;
    for (TransformationBean transformation : mapping.getTransformations()) {
      // array transformations are handled separately so skip any that we encounter here
      if (transformation.isArray()) {
        continue;
      }
      final ColumnBean column = mapping.getTable().findColumnByName(transformation.getTo());
      final CodeBlock transformationCode =
          TransformerUtil.selectTransformerForField(column, transformation)
              .map(
                  generator ->
                      generator.generateCodeBlock(
                          mapping, column, transformation, fromCodeGenerator, toCodeGenerator))
              .orElseThrow(
                  () ->
                      MojoUtil.createException(
                          "No known transformation found: mapping=%s from=%s to=%s",
                          mapping.getId(), transformation.getFrom(), transformation.getTo()));
      builder.addCode(transformationCode);
    }
    if (mapping.hasExternalTransformations()) {
      for (ExternalTransformationBean externalTransformation :
          mapping.getExternalTransformations()) {
        builder.addStatement(
            "$L.transformField($L, $L, $L, $L)",
            externalTransformation.getName(),
            FieldTransformer.TRANSFORMER_VAR,
            FieldTransformer.NAME_PREFIX_VAR,
            FieldTransformer.SOURCE_VAR,
            FieldTransformer.DEST_VAR);
      }
    }
    builder.addStatement("return $L", FieldTransformer.DEST_VAR);
    return builder.build();
  }

  /**
   * Creates a method {@link MethodSpec} for a method that transforms all objects in all arrays
   * within a given message. The generated method loops over all arrays in the mapping and calls the
   * appropriate transform method for instances of objects in each array of the message and copies
   * the resulting objects to the corresponding array in the entity.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} for message/entity containing the arrays to be processed
   * @return the {@link MethodSpec}
   * @throws MojoExecutionException if any errors are encountered
   */
  private MethodSpec createTransformArraysMethodForMapping(RootBean root, MappingBean mapping)
      throws MojoExecutionException {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(createTransformArraysMethodNameForMapping(mapping))
            .addModifiers(Modifier.PUBLIC)
            .addParameter(messageClassType, FieldTransformer.SOURCE_VAR)
            .addParameter(entityClassType, FieldTransformer.DEST_VAR)
            .addParameter(DataTransformer.class, FieldTransformer.TRANSFORMER_VAR)
            .addParameter(Instant.class, FieldTransformer.NOW_VAR)
            .addParameter(String.class, FieldTransformer.NAME_PREFIX_VAR);
    for (ArrayTransformSpec spec : getAllArrayTransforms(mapping)) {
      final MappingBean elementMapping =
          root.findMappingForJoinBean(spec.join)
              .orElseThrow(
                  () ->
                      MojoUtil.createException(
                          "array element of %s references undefined mapping %s",
                          mapping.getId(), spec.join.getEntityMapping()));
      CodeBlock.Builder loop =
          CodeBlock.builder()
              .beginControlFlow(
                  "for (short index = 0; index < $L.get$LCount(); ++index)",
                  FieldTransformer.SOURCE_VAR,
                  TransformerUtil.capitalize(spec.from));
      loop.addStatement(
              "final String itemNamePrefix = $L + $S + \"-\" + index + \"-\"",
              FieldTransformer.NAME_PREFIX_VAR,
              spec.join.getFieldName())
          .addStatement(
              "final $T itemFrom = $L.get$L(index)",
              TransformerUtil.toClassName(elementMapping.getMessageClassName()),
              FieldTransformer.SOURCE_VAR,
              TransformerUtil.capitalize(spec.from))
          .addStatement(
              "final $T itemTo = $L(itemFrom,$L,$L,itemNamePrefix)",
              TransformerUtil.toClassName(elementMapping.getEntityClassName()),
              createTransformMethodNameForMapping(elementMapping),
              FieldTransformer.TRANSFORMER_VAR,
              FieldTransformer.NOW_VAR);
      for (TransformationBean elementField : elementMapping.getTransformations()) {
        final String elementFrom = elementField.getFrom();
        if (elementFrom.equals(TransformerUtil.IndexFromName)) {
          // set the column to the current array element's index within the array
          loop.addStatement(
              "itemTo.set$L(index)", TransformerUtil.capitalize(elementField.getTo()));
        } else if (elementFrom.equals(TransformerUtil.ParentFromName)) {
          // copy the same column from parent into the array element field
          loop.addStatement(
              "itemTo.set$L($L.get$L())",
              TransformerUtil.capitalize(elementField.getTo()),
              FieldTransformer.DEST_VAR,
              TransformerUtil.capitalize(elementField.getTo()));
        }
      }
      // When mappedBy is a primary key the field is a value not an object so we don't initialize it
      // using to.
      if (spec.join.hasMappedBy() && !mapping.getTable().isPrimaryKey(spec.join.getMappedBy())) {
        loop.addStatement("itemTo.set$L(to)", TransformerUtil.capitalize(spec.join.getMappedBy()));
      }
      if (elementMapping.hasArrayTransformations()) {
        loop.addStatement(
            "$L(itemFrom,itemTo,$L,$L,itemNamePrefix)",
            createTransformArraysMethodNameForMapping(mapping),
            FieldTransformer.TRANSFORMER_VAR,
            FieldTransformer.NOW_VAR);
      }
      loop.addStatement(
              "$L.get$L().add(itemTo)",
              FieldTransformer.DEST_VAR,
              TransformerUtil.capitalize(spec.join.getFieldName()))
          .endControlFlow();
      builder.addCode(loop.build());
    }
    return builder.build();
  }

  /**
   * Creates a unique method name for the generated method that transforms single objects of a type.
   *
   * @param mapping {@link MappingBean} for the object being transformed
   * @return unique method name
   */
  private String createTransformMethodNameForMapping(MappingBean mapping) {
    return TRANSFORM_MAPPING_METHOD_NAME_BASE + mapping.getId();
  }

  /**
   * Creates a unique method name for the generated method that transforms arrays of objects.
   *
   * @param mapping {@link MappingBean} for the objects in the array
   * @return unique method name
   */
  private String createTransformArraysMethodNameForMapping(MappingBean mapping) {
    return TRANSFORM_ARRAYS_METHOD_NAME_BASE + mapping.getId();
  }
}
