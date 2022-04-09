package gov.cms.model.dsl.codegen.plugin;

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
import gov.cms.model.dsl.codegen.plugin.model.ArrayElement;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.ExternalTransformationBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import gov.cms.model.dsl.codegen.plugin.transformer.AbstractFieldTransformer;
import gov.cms.model.dsl.codegen.plugin.transformer.GrpcFromCodeGenerator;
import gov.cms.model.dsl.codegen.plugin.transformer.OptionalToCodeGenerator;
import gov.cms.model.dsl.codegen.plugin.transformer.RifFromCodeGenerator;
import gov.cms.model.dsl.codegen.plugin.transformer.StandardToCodeGenerator;
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
import lombok.SneakyThrows;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * A Maven Mojo that generates code to copy and transform data from RDA API message objects into JPA
 * entity objects.
 */
@Mojo(name = "transformers", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class RdaTransformerCodeGenMojo extends AbstractMojo {
  private static final String PUBLIC_TRANSFORM_METHOD_NAME = "transformMessage";
  private static final String PRIVATE_TRANSFORM_METHOD_NAME_BASE = "transformMessageTo";
  private static final String PRIVATE_TRANSFORM_ARRAYS_METHOD_NAME = "transformMessageArrays";

  @Parameter(property = "mappingFile")
  private String mappingFile;

  @Parameter(
      property = "outputDirectory",
      defaultValue = "${project.build.directory}/generated-sources/rda-entities")
  private String outputDirectory;

  @Parameter(property = "project", readonly = true)
  private MavenProject project;
  // endregion

  @SneakyThrows(IOException.class)
  public void execute() throws MojoExecutionException {
    final File outputDir = MojoUtil.initializeOutputDirectory(outputDirectory);
    final RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingFile);
    for (MappingBean mapping : root.getMappings()) {
      if (mapping.hasTransformer()) {
        TypeSpec rootEntity = createTransformerClassForMapping(mapping, root::findMappingWithId);
        JavaFile javaFile = JavaFile.builder(mapping.transformerPackage(), rootEntity).build();
        javaFile.writeTo(outputDir);
      }
    }
    project.addCompileSourceRoot(outputDirectory);
  }

  private TypeSpec createTransformerClassForMapping(
      MappingBean mapping, Function<String, Optional<MappingBean>> mappingFinder)
      throws MojoExecutionException {
    final List<MappingBean> allMappings = findAllMappingsForRootMapping(mapping, mappingFinder);
    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(mapping.transformerSimpleName()).addModifiers(Modifier.PUBLIC);
    for (MappingBean aMapping : allMappings) {
      for (FieldSpec field : createFieldsForMapping(aMapping)) {
        classBuilder.addField(field);
      }
    }
    MethodSpec.Builder constructor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    if (TransformerUtil.mappingRequiresIdHasher(mapping)) {
      classBuilder.addField(
          ParameterizedTypeName.get(Function.class, String.class, String.class),
          AbstractFieldTransformer.HASHER_VAR,
          Modifier.PRIVATE,
          Modifier.FINAL);
      constructor
          .addParameter(
              ParameterizedTypeName.get(Function.class, String.class, String.class),
              AbstractFieldTransformer.HASHER_VAR)
          .addStatement(
              "this.$L = $L",
              AbstractFieldTransformer.HASHER_VAR,
              AbstractFieldTransformer.HASHER_VAR);
    }
    if (TransformerUtil.mappingRequiresEnumExtractor(mapping)) {
      constructor.addParameter(
          EnumStringExtractor.Factory.class, AbstractFieldTransformer.ENUM_FACTORY_VAR);
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
    classBuilder.addMethod(createPublicSimplifiedTransformMessageMethod(mapping));
    classBuilder.addMethod(createPublicTransformMessageMethod(mapping));
    for (MappingBean aMapping : allMappings) {
      classBuilder.addMethod(createTransformMessageMethodForMapping(aMapping));
      if (aMapping.hasArrayElements()) {
        classBuilder.addMethod(
            createTransformMessageArraysMethodForMapping(aMapping, mappingFinder));
      }
    }
    return classBuilder.build();
  }

  /**
   * Finds all MappingBeans reachable from the specified root mapping by recursively following all
   * of its array mappings. The result is all of the mappings that this transformer class will need
   * to generate code for.
   *
   * @param root Base mapping that we are creating transformer for.
   * @param mappingFinder Lookup method to find mappings by their id.
   * @return List of all mappings reachable from root (including root).
   */
  private List<MappingBean> findAllMappingsForRootMapping(
      MappingBean root, Function<String, Optional<MappingBean>> mappingFinder) {
    final ImmutableList.Builder<MappingBean> answer = ImmutableList.builder();
    final Set<String> visited = new HashSet<>();
    final List<MappingBean> queue = new ArrayList<>();

    queue.add(root);
    while (queue.size() > 0) {
      MappingBean mapping = queue.remove(0);
      visited.add(mapping.getId());
      answer.add(mapping);
      for (ArrayElement element : mapping.getArrays()) {
        Optional<MappingBean> elementMapping = mappingFinder.apply(element.getMapping());
        if (elementMapping.isPresent() && !visited.contains(element.getMapping())) {
          queue.add(elementMapping.get());
        }
      }
    }
    return answer.build();
  }

  private List<FieldSpec> createFieldsForMapping(MappingBean mapping) {
    return mapping.getTransformations().stream()
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

  private List<CodeBlock> createFieldInitializersForMapping(MappingBean mapping) {
    return mapping.getTransformations().stream()
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

  private MethodSpec createPublicSimplifiedTransformMessageMethod(MappingBean mapping) {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(PUBLIC_TRANSFORM_METHOD_NAME)
            .returns(entityClassType)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(messageClassType, AbstractFieldTransformer.SOURCE_VAR);
    builder.addStatement(
        "final $T $L = new $T();",
        DataTransformer.class,
        AbstractFieldTransformer.TRANSFORMER_VAR,
        DataTransformer.class);
    builder.addStatement(
        "final $T $L = $L($L, $L, Instant.now())",
        entityClassType,
        AbstractFieldTransformer.DEST_VAR,
        PUBLIC_TRANSFORM_METHOD_NAME,
        AbstractFieldTransformer.SOURCE_VAR,
        AbstractFieldTransformer.TRANSFORMER_VAR);
    CodeBlock.Builder errorCheck =
        CodeBlock.builder()
            .beginControlFlow(
                "if ($L.getErrors().size() > 0)", AbstractFieldTransformer.TRANSFORMER_VAR)
            .addStatement(
                "throw new $T($S, $L.getErrors())",
                DataTransformer.TransformationException.class,
                "data transformation failed",
                AbstractFieldTransformer.TRANSFORMER_VAR)
            .endControlFlow();
    builder.addCode(errorCheck.build());
    builder.addStatement("return $L", AbstractFieldTransformer.DEST_VAR);
    return builder.build();
  }

  private MethodSpec createPublicTransformMessageMethod(MappingBean mapping) {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(PUBLIC_TRANSFORM_METHOD_NAME)
            .returns(entityClassType)
            .addModifiers(Modifier.PUBLIC)
            .addParameter(messageClassType, AbstractFieldTransformer.SOURCE_VAR)
            .addParameter(DataTransformer.class, AbstractFieldTransformer.TRANSFORMER_VAR)
            .addParameter(Instant.class, AbstractFieldTransformer.NOW_VAR);
    builder.addStatement(
        "final $T $L = $L($L,$L,$L,$S)",
        entityClassType,
        AbstractFieldTransformer.DEST_VAR,
        createPrivateTransformMethodNameForMapping(mapping),
        AbstractFieldTransformer.SOURCE_VAR,
        AbstractFieldTransformer.TRANSFORMER_VAR,
        AbstractFieldTransformer.NOW_VAR,
        "");
    if (mapping.hasArrayElements()) {
      builder.addStatement(
          "$L($L,$L,$L,$L,$S)",
          PRIVATE_TRANSFORM_ARRAYS_METHOD_NAME,
          AbstractFieldTransformer.SOURCE_VAR,
          AbstractFieldTransformer.DEST_VAR,
          AbstractFieldTransformer.TRANSFORMER_VAR,
          AbstractFieldTransformer.NOW_VAR,
          "");
    }
    builder.addStatement("return $L", AbstractFieldTransformer.DEST_VAR);
    return builder.build();
  }

  private MethodSpec createTransformMessageMethodForMapping(MappingBean mapping)
      throws MojoExecutionException {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(createPrivateTransformMethodNameForMapping(mapping))
            .returns(entityClassType)
            .addModifiers(Modifier.PRIVATE)
            .addParameter(messageClassType, AbstractFieldTransformer.SOURCE_VAR)
            .addParameter(DataTransformer.class, AbstractFieldTransformer.TRANSFORMER_VAR)
            .addParameter(Instant.class, AbstractFieldTransformer.NOW_VAR)
            .addParameter(String.class, AbstractFieldTransformer.NAME_PREFIX_VAR)
            .addStatement(
                "final $T $L = new $T()",
                entityClassType,
                AbstractFieldTransformer.DEST_VAR,
                entityClassType);
    final var fromCodeGenerator =
        mapping.getSourceType() == MappingBean.SourceType.RifCsv
            ? RifFromCodeGenerator.Instance
            : GrpcFromCodeGenerator.Instance;
    final var toCodeGenerator =
        mapping.getNullableFieldAccessorType() == MappingBean.NullableFieldAccessorType.Standard
            ? StandardToCodeGenerator.Instance
            : OptionalToCodeGenerator.Instance;
    for (TransformationBean transformation : mapping.getTransformations()) {
      final ColumnBean column = mapping.getTable().findColumnByName(transformation.getTo());
      TransformerUtil.selectTransformerForField(column, transformation)
          .map(
              generator ->
                  generator.generateCodeBlock(
                      mapping, column, transformation, fromCodeGenerator, toCodeGenerator))
          .ifPresent(builder::addCode);
    }
    if (mapping.hasExternalTransformations()) {
      for (ExternalTransformationBean externalTransformation :
          mapping.getExternalTransformations()) {
        builder.addStatement(
            "$L.transformField($L, $L, $L, $L)",
            externalTransformation.getName(),
            AbstractFieldTransformer.TRANSFORMER_VAR,
            AbstractFieldTransformer.NAME_PREFIX_VAR,
            AbstractFieldTransformer.SOURCE_VAR,
            AbstractFieldTransformer.DEST_VAR);
      }
    }
    builder.addStatement("return $L", AbstractFieldTransformer.DEST_VAR);
    return builder.build();
  }

  private MethodSpec createTransformMessageArraysMethodForMapping(
      MappingBean mapping, Function<String, Optional<MappingBean>> mappingFinder)
      throws MojoExecutionException {
    final TypeName messageClassType = ModelUtil.classType(mapping.getMessageClassName());
    final TypeName entityClassType = ModelUtil.classType(mapping.getEntityClassName());
    final MethodSpec.Builder builder =
        MethodSpec.methodBuilder(PRIVATE_TRANSFORM_ARRAYS_METHOD_NAME)
            .addModifiers(Modifier.PRIVATE)
            .addParameter(messageClassType, AbstractFieldTransformer.SOURCE_VAR)
            .addParameter(entityClassType, AbstractFieldTransformer.DEST_VAR)
            .addParameter(DataTransformer.class, AbstractFieldTransformer.TRANSFORMER_VAR)
            .addParameter(Instant.class, AbstractFieldTransformer.NOW_VAR)
            .addParameter(String.class, AbstractFieldTransformer.NAME_PREFIX_VAR);
    for (ArrayElement arrayElement : mapping.getArrays()) {
      final MappingBean elementMapping =
          mappingFinder
              .apply(arrayElement.getMapping())
              .orElseThrow(
                  () ->
                      failure(
                          "array element of %s references undefined mapping %s",
                          mapping.getId(), arrayElement.getMapping()));
      CodeBlock.Builder loop =
          CodeBlock.builder()
              .beginControlFlow(
                  "for (short index = 0; index < $L.get$LCount(); ++index)",
                  AbstractFieldTransformer.SOURCE_VAR,
                  TransformerUtil.capitalize(arrayElement.getFrom()));
      loop.addStatement(
              "final String itemNamePrefix = $L + $S + \"-\" + index + \"-\"",
              AbstractFieldTransformer.NAME_PREFIX_VAR,
              arrayElement.getNamePrefix())
          .addStatement(
              "final $T itemFrom = $L.get$L(index)",
              PoetUtil.toClassName(elementMapping.getMessageClassName()),
              AbstractFieldTransformer.SOURCE_VAR,
              TransformerUtil.capitalize(arrayElement.getFrom()))
          .addStatement(
              "final $T itemTo = $L(itemFrom,$L,$L,itemNamePrefix)",
              PoetUtil.toClassName(elementMapping.getEntityClassName()),
              createPrivateTransformMethodNameForMapping(elementMapping),
              AbstractFieldTransformer.TRANSFORMER_VAR,
              AbstractFieldTransformer.NOW_VAR);
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
              AbstractFieldTransformer.SOURCE_VAR,
              TransformerUtil.capitalize(elementField.getTo()));
        }
      }
      if (arrayElement.hasParentField()) {
        loop.addStatement(
            "itemTo.set$L(to)", TransformerUtil.capitalize(arrayElement.getParentField()));
      }
      if (elementMapping.hasArrayElements()) {
        loop.addStatement(
            "$L(itemFrom,itemTo,$L,$L,itemNamePrefix)",
            PRIVATE_TRANSFORM_ARRAYS_METHOD_NAME,
            AbstractFieldTransformer.TRANSFORMER_VAR,
            AbstractFieldTransformer.NOW_VAR);
      }
      loop.addStatement(
              "$L.get$L().add(itemTo)",
              AbstractFieldTransformer.DEST_VAR,
              TransformerUtil.capitalize(arrayElement.getTo()))
          .endControlFlow();
      builder.addCode(loop.build());
    }
    return builder.build();
  }

  private String createPrivateTransformMethodNameForMapping(MappingBean mapping) {
    return PRIVATE_TRANSFORM_METHOD_NAME_BASE + mapping.getId();
  }

  private MojoExecutionException failure(String formatString, Object... args) {
    String message = String.format(formatString, args);
    return new MojoExecutionException(message);
  }

  private void fail(String formatString, Object... args) throws MojoExecutionException {
    throw failure(formatString, args);
  }
}
