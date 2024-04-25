package gov.cms.model.dsl.codegen.plugin;

import static gov.cms.model.dsl.codegen.plugin.PoetUtil.OptionalClassName;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.EnumTypeBean;
import gov.cms.model.dsl.codegen.plugin.model.JoinBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.ModelUtil;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.TableBean;
import gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil;
import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.hibernate.annotations.BatchSize;

/** A Maven Mojo that generates code for JPA entities. */
@Mojo(name = "entities", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateEntitiesFromDslMojo extends AbstractMojo {
  /** Value to use for the {@link BatchSize} annotation value on arrays. */
  static final int BATCH_SIZE_FOR_ARRAY_FIELDS = 100;

  /** Value to use for the ignoring @Builder annotations. */
  static final int NUM_FIELDS_TO_IGNORE_BUILDER_ANNOTATION = 100;

  /**
   * {@link FieldSpec} used to add a {@code serialVersionUID} static member variable to a composite
   * key class.
   */
  static final FieldSpec SerialVersionUIDField =
      FieldSpec.builder(
              long.class, "serialVersionUID", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
          .initializer("$L", 1L)
          .build();

  /** Path to a single mapping file or a directory containing one or more mapping files. */
  @Parameter(property = "mappingPath")
  private String mappingPath;

  /** Path to directory to contain generated code. */
  @Parameter(
      property = "entitiesDirectory",
      defaultValue = "${project.build.directory}/generated-sources/entities")
  private String entitiesDirectory;

  /**
   * Instance of {@link MavenProject} used to call {@link MavenProject#addCompileSourceRoot(String)}
   * to ensure our generated classes are compiled.
   */
  @Parameter(property = "project", readonly = true)
  private MavenProject project;

  /** Parameterless constructor used by Maven to instantiate the plugin. */
  public GenerateEntitiesFromDslMojo() {}

  /**
   * All fields constructor for use in unit tests.
   *
   * @param mappingPath path to file or directory containing mappings
   * @param entitiesDirectory path to directory to contain generated code
   * @param project instance of {@link MavenProject}
   */
  @VisibleForTesting
  GenerateEntitiesFromDslMojo(String mappingPath, String entitiesDirectory, MavenProject project) {
    this.mappingPath = mappingPath;
    this.entitiesDirectory = entitiesDirectory;
    this.project = project;
  }

  /**
   * Executed by maven to execute the mojo. Reads all mapping files and generates an entity class
   * for every {@link MappingBean}.
   *
   * @throws MojoExecutionException if the process fails due to some error
   */
  public void execute() throws MojoExecutionException {
    try {
      final File outputDir = MojoUtil.initializeOutputDirectory(entitiesDirectory);
      final RootBean root = ModelUtil.loadModelFromYamlFileOrDirectory(mappingPath);
      MojoUtil.validateModel(root);
      generateEnumClasses(outputDir, root);
      generateEntityClasses(outputDir, root);
      if (project != null) {
        project.addCompileSourceRoot(entitiesDirectory);
      }
    } catch (IOException ex) {
      throw new MojoExecutionException("I/O error during code generation", ex);
    }
  }

  /**
   * Writes a java class file for every entity defined in the {@link RootBean}'s mappings.
   *
   * @param outputDir directory to write the generated class file to
   * @param root {@link RootBean} containing all known mappings
   * @throws IOException if any problems arise
   */
  private void generateEntityClasses(File outputDir, RootBean root)
      throws MojoExecutionException, IOException {
    for (MappingBean mapping : root.getMappings()) {
      TypeSpec entitySpec = createEntityClassForMapping(root, mapping);
      JavaFile javaFile = JavaFile.builder(mapping.getEntityClassPackage(), entitySpec).build();
      javaFile.writeTo(outputDir);
    }
  }

  /**
   * Writes a java class file for every non-inner class enum defined in the {@link RootBean}'s
   * mappings.
   *
   * @param outputDir directory to write the generated class file to
   * @param root {@link RootBean} containing all known mappings
   * @throws IOException if any problems arise
   */
  private void generateEnumClasses(File outputDir, RootBean root) throws IOException {
    for (MappingBean mapping : root.getMappings()) {
      for (EnumTypeBean enumType : mapping.getEnumTypes()) {
        if (!enumType.isInnerClass()) {
          final TypeSpec enumSpec = createEnumTypeSpec(enumType);
          JavaFile javaFile = JavaFile.builder(enumType.getPackageName(), enumSpec).build();
          javaFile.writeTo(outputDir);
        }
      }
    }
  }

  /**
   * Creates a {@link TypeSpec} for an entity class for the specified {@link MappingBean}.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to create entity class for
   * @return the {@link TypeSpec}
   * @throws MojoExecutionException if any problems arise
   */
  private TypeSpec createEntityClassForMapping(RootBean root, MappingBean mapping)
      throws MojoExecutionException {
    if (!mapping.getTable().hasPrimaryKey()) {
      throw MojoUtil.createException(
          "mapping has no primary key fields: mapping=%s", mapping.getId());
    }

    final var interfaceNames =
        mapping.getEntityInterfaces().stream()
            .map(TransformerUtil::toClassName)
            .collect(Collectors.toList());
    final var innerClassEnumTypeSpecs = createInnerClassEnumTypeSpecs(mapping.getEnumTypes());
    final var fieldDefinitions = createAllFieldDefinitions(root, mapping);
    final var fieldSpecs =
        fieldDefinitions.stream().map(FieldDefinition::getFieldSpec).collect(Collectors.toList());
    final var accessorSpecs =
        fieldDefinitions.stream()
            .flatMap(f -> createMethodSpecsForAccessorSpec(mapping, f.accessorSpec).stream())
            .collect(Collectors.toList());
    final var joinPropertySpecs = createMethodSpecsForJoinProperties(mapping);
    final var annotationSpecs = createEntityClassAnnotationSpecs(mapping);
    final var primaryKeySpecs =
        fieldDefinitions.stream()
            .flatMap(f -> f.primaryKeyFieldSpec.stream())
            .collect(Collectors.toList());

    final var classBuilder = TypeSpec.classBuilder(mapping.getEntityClassSimpleName());
    classBuilder.addJavadoc(createJavadocCommentForMapping(mapping));
    classBuilder.addModifiers(Modifier.PUBLIC);
    classBuilder.addSuperinterfaces(interfaceNames);
    classBuilder.addTypes(innerClassEnumTypeSpecs);
    classBuilder.addFields(fieldSpecs);
    classBuilder.addMethods(accessorSpecs);
    classBuilder.addMethods(joinPropertySpecs);
    classBuilder.addMethods(createMethodSpecsForGroupedProperties(mapping));
    classBuilder.addAnnotations(annotationSpecs);
    if (primaryKeySpecs.size() > 1) {
      final var primaryKeyClassName = computePrimaryKeyClassName(mapping);
      final var primaryKeyClass =
          createTypeSpecForCompositePrimaryKeyClass(mapping, primaryKeyClassName, primaryKeySpecs);
      final var primaryKeyClassAnnotation = createIdClassAnnotation(primaryKeyClassName);
      classBuilder.addType(primaryKeyClass);
      classBuilder.addAnnotation(primaryKeyClassAnnotation);
    }
    if (mapping.getTable().getAdditionalFieldNames().size() > 0) {
      final var additionalFieldsClass =
          createAdditionalFieldsInnerClass(mapping, mapping.getTable().getAdditionalFieldNames());
      classBuilder.addType(additionalFieldsClass);
    }
    return classBuilder.build();
  }

  /**
   * Generates a javadoc comment for the given mapping's entity class. Uses a defined comment on the
   * {@link TableBean} if one is present otherwise uses a simple default value.
   *
   * @param mapping the {@link MappingBean} to process
   * @return the javadoc comment string
   */
  @VisibleForTesting
  String createJavadocCommentForMapping(MappingBean mapping) {
    if (mapping.getTable().hasComment()) {
      return mapping.getTable().getComment();
    } else {
      return String.format("JPA class for the {@code %s} table.", mapping.getTable().getName());
    }
  }

  /**
   * Creates a list containing one {@link AnnotationSpec} for each annotation that should be added
   * to the generated entity's class.
   *
   * @param mapping {@link MappingBean} defining the entity
   * @return the list of {@link AnnotationSpec}s
   */
  private List<AnnotationSpec> createEntityClassAnnotationSpecs(MappingBean mapping) {
    List<AnnotationSpec> annotationSpecs = new ArrayList<>();
    annotationSpecs.add(AnnotationSpec.builder(Entity.class).build());

    // lombok does not react well to classes with a huge number of fields.
    // This is a safety to omit adding the nice-to-have lombok annotations for such classes.
    if (mapping.getTable().getColumns().size() < NUM_FIELDS_TO_IGNORE_BUILDER_ANNOTATION) {
      annotationSpecs.add(AnnotationSpec.builder(Builder.class).build());
      annotationSpecs.add(AnnotationSpec.builder(AllArgsConstructor.class).build());
      annotationSpecs.add(AnnotationSpec.builder(NoArgsConstructor.class).build());
    } else {
      getLog()
          .info(
              String.format(
                  "Mapping field count prevented generation of builder or constructors: mapping=%s fieldCount=%d",
                  mapping.getId(), mapping.getTable().getColumns().size()));
    }

    if (mapping.getTable().isEqualsNeeded()) {
      annotationSpecs.add(createEqualsAndHashCodeAnnotation());
    }

    annotationSpecs.add(AnnotationSpec.builder(FieldNameConstants.class).build());
    annotationSpecs.add(createTableAnnotation(mapping.getTable()));

    return annotationSpecs;
  }

  /**
   * Creates a list containing one {@link TypeSpec} for every enum that needs to be created as an
   * inner class of the entity rather than as a stand alone class.
   *
   * @param enumMappings the {@link EnumTypeBean}s to create specs for
   * @return the list of {@link TypeSpec}
   */
  private List<TypeSpec> createInnerClassEnumTypeSpecs(List<EnumTypeBean> enumMappings) {
    List<TypeSpec> typeSpecs = new ArrayList<>();
    for (EnumTypeBean enumMapping : enumMappings) {
      if (enumMapping.isInnerClass()) {
        typeSpecs.add(createEnumTypeSpec(enumMapping));
      }
    }
    return typeSpecs;
  }

  /**
   * Creates a {@link TypeSpec} used to generate an enum class.
   *
   * @param enumType {@link EnumTypeBean} describing the enum to be created
   * @return the {@link TypeSpec}
   */
  private TypeSpec createEnumTypeSpec(EnumTypeBean enumType) {
    TypeSpec.Builder builder =
        TypeSpec.enumBuilder(enumType.getName()).addModifiers(Modifier.PUBLIC);
    for (String value : enumType.getValues()) {
      builder.addEnumConstant(value);
    }
    return builder.build();
  }

  /**
   * Creates a list containing one {@link FieldDefinition} for each field that needs to be included
   * in the generated entity class.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to create fields for
   * @return the list of {@link FieldDefinition}s
   * @throws MojoExecutionException is any problems arise
   */
  @Nonnull
  private List<FieldDefinition> createAllFieldDefinitions(RootBean root, MappingBean mapping)
      throws MojoExecutionException {
    final var primaryKeyFieldNames = ImmutableSet.copyOf(mapping.getTable().getPrimaryKeyColumns());
    final var nonArrayJoins = mapping.getNonArrayJoins();
    final var fields = new ArrayList<FieldDefinition>();
    fields.addAll(
        createFieldDefinitionsForPrimaryKeyJoins(
            root, mapping, nonArrayJoins, primaryKeyFieldNames));
    fields.addAll(createFieldDefinitionsForColumns(mapping, primaryKeyFieldNames));
    fields.addAll(createFieldDefinitionsForArrays(root, mapping, primaryKeyFieldNames.size()));
    fields.addAll(
        createFieldDefinitionsForOrdinaryJoins(root, mapping, nonArrayJoins, primaryKeyFieldNames));
    return fields;
  }

  /**
   * Creates a list containing one {@link FieldDefinition} for each join that is a primary key. The
   * {@link FieldDefinition} will also include a {@link FieldDefinition#primaryKeyFieldSpec} for the
   * extra field to be added to the primary key inner class.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} containing the joins
   * @param joins list of all of the {@link JoinBean}s to process
   * @param primaryKeyFieldNames collection of the field names for this entity's primary keys
   * @return the list of {@link FieldDefinition}s
   */
  @VisibleForTesting
  List<FieldDefinition> createFieldDefinitionsForPrimaryKeyJoins(
      RootBean root,
      MappingBean mapping,
      List<JoinBean> joins,
      Collection<String> primaryKeyFieldNames)
      throws MojoExecutionException {
    List<FieldDefinition> fieldSpecs = new ArrayList<>();
    for (JoinBean join : joins) {
      if (primaryKeyFieldNames.contains(join.getFieldName())) {
        var fieldDef = createFieldDefinitionForJoin(root, mapping, join);
        var primaryKeyFieldDef = createPrimaryKeyFieldSpecForJoin(root, mapping, join);
        fieldSpecs.add(fieldDef.withPrimaryKeyFieldSpec(primaryKeyFieldDef));
      }
    }
    return fieldSpecs;
  }

  /**
   * Creates a list containing one {@link FieldDefinition} for each join that is not a primary key.
   *
   * @param root {@link RootBean} containing all known {@link MappingBean}s
   * @param mapping {@link MappingBean} containing the joins
   * @param joins list of all of the {@link JoinBean}s to process
   * @param primaryKeyFieldNames collection containing the field name for all of the entity's
   *     primary keys
   * @return the list of {@link FieldDefinition}s
   */
  @VisibleForTesting
  List<FieldDefinition> createFieldDefinitionsForOrdinaryJoins(
      RootBean root,
      MappingBean mapping,
      List<JoinBean> joins,
      Collection<String> primaryKeyFieldNames)
      throws MojoExecutionException {
    List<FieldDefinition> fieldSpecs = new ArrayList<>();
    for (JoinBean join : joins) {
      if (!primaryKeyFieldNames.contains(join.getFieldName())) {
        fieldSpecs.add(createFieldDefinitionForJoin(root, mapping, join));
      }
    }
    return fieldSpecs;
  }

  /**
   * Creates a {@link FieldDefinition} for a {@link JoinBean} defined in the mapping.
   *
   * @param root {@link RootBean} containing all known {@link MappingBean}s
   * @param mapping {@link MappingBean} containing the join
   * @param join {@link JoinBean} the join
   * @return the {@link FieldDefinition}
   * @throws MojoExecutionException if the join does not contain a package name
   */
  @VisibleForTesting
  FieldDefinition createFieldDefinitionForJoin(RootBean root, MappingBean mapping, JoinBean join)
      throws MojoExecutionException {
    var entityClass = root.getEntityClassForJoin(join);
    if (entityClass.isEmpty() || !ModelUtil.isValidFullClassName(entityClass.get())) {
      throw MojoUtil.createException(
          "entityClass for join must include package: mapping=%s join=%s entityClass=%s",
          mapping.getId(), join.getFieldName(), entityClass);
    }
    TypeName fieldType = ModelUtil.classType(entityClass.get());
    if (join.getJoinType().isMultiValue()) {
      fieldType = ParameterizedTypeName.get(join.getCollectionType().getInterfaceName(), fieldType);
    }
    final var fieldSpec =
        FieldSpec.builder(fieldType, join.getFieldName()).addModifiers(Modifier.PRIVATE);
    if (mapping.getTable().isPrimaryKey(join)) {
      fieldSpec.addAnnotation(Id.class);
      if (mapping.getTable().isEqualsNeeded()) {
        fieldSpec.addAnnotation(EqualsAndHashCode.Include.class);
      }
    }
    if (join.hasComment()) {
      fieldSpec.addJavadoc(join.getComment());
    }
    fieldSpec.addAnnotation(createJoinTypeAnnotation(mapping, join));
    if (join.hasColumnName()) {
      fieldSpec.addAnnotation(createJoinColumnAnnotation(mapping, join));
    }
    if (join.hasOrderBy()) {
      fieldSpec.addAnnotation(
          AnnotationSpec.builder(OrderBy.class)
              .addMember("value", "$S", join.getOrderBy())
              .build());
    }
    if (join.getJoinType().isMultiValue()) {
      fieldSpec
          .initializer("new $T<>()", join.getCollectionType().getClassName())
          .addAnnotation(
              AnnotationSpec.builder(BatchSize.class)
                  .addMember("size", "$L", BATCH_SIZE_FOR_ARRAY_FIELDS)
                  .build());
      // We previously skipped adding the @Builder annotation to an entity that
      // has an excessive number of fields; the check here is to prevent spurious
      // Lombok warnings that correctly point out that since we are not adding the
      // @Builder annotation, we should also not add a @Builder.Default annotation.
      if (mapping.getTable().getColumns().size() < NUM_FIELDS_TO_IGNORE_BUILDER_ANNOTATION) {
        fieldSpec.addAnnotation(Builder.Default.class);
      }
    }
    final var accessorSpec =
        AccessorSpec.builder()
            .fieldName(join.getFieldName())
            .fieldType(fieldType)
            .accessorType(fieldType)
            .isNullableColumn(false)
            .isReadOnly(join.isReadOnly())
            .build();
    return new FieldDefinition(fieldSpec.build(), accessorSpec);
  }

  /**
   * Creates a list containing one {@link FieldDefinition} for every column in the {@link
   * MappingBean}'s table.
   *
   * @param mapping containing the columns
   * @param primaryKeyFieldNames column names for our primary keys
   * @return the list of {@link FieldDefinition}
   * @throws MojoExecutionException if any problems arise
   */
  @VisibleForTesting
  List<FieldDefinition> createFieldDefinitionsForColumns(
      MappingBean mapping, Collection<String> primaryKeyFieldNames) throws MojoExecutionException {
    List<FieldDefinition> fieldSpecs = new ArrayList<>();
    for (ColumnBean column : mapping.getTable().getColumns()) {
      // Do not generate fields for database only columns.
      if (column.isDbOnly()) {
        continue;
      }
      var fieldDefinition = createFieldDefinitionForColumn(mapping, column);
      if (primaryKeyFieldNames.contains(column.getName())) {
        var primaryKeyFieldSpec =
            createPrimaryKeyFieldSpecForColumn(mapping, column.getName(), column);
        fieldDefinition = fieldDefinition.withPrimaryKeyFieldSpec(primaryKeyFieldSpec);
      }
      fieldSpecs.add(fieldDefinition);
    }
    return fieldSpecs;
  }

  /**
   * Creates a {@link FieldDefinition} for the field that holds the value of the given column.
   *
   * @param mapping {@link MappingBean} containing the column
   * @param column {@link ColumnBean} the column
   * @return the {@link FieldDefinition}
   * @throws MojoExecutionException if any validity checks fail
   */
  @VisibleForTesting
  FieldDefinition createFieldDefinitionForColumn(MappingBean mapping, ColumnBean column)
      throws MojoExecutionException {
    final var equalsFields = mapping.getTable().getColumnsForEqualsMethod();
    final var fieldType = createFieldTypeForColumn(mapping, column);
    final var accessorType = createAccessorTypeForColumn(mapping, column);
    final var fieldSpec =
        FieldSpec.builder(fieldType, column.getName()).addModifiers(Modifier.PRIVATE);
    if (column.hasComment()) {
      fieldSpec.addJavadoc(column.getComment());
    }
    if (column.isEnum()) {
      fieldSpec.addAnnotation(createEnumeratedAnnotation(mapping, column));
    }
    fieldSpec.addAnnotations(createAnnotationsForColumn(mapping, column));
    if (equalsFields.contains(column.getName())) {
      fieldSpec.addAnnotation(EqualsAndHashCode.Include.class);
    }
    final var accessorSpec =
        AccessorSpec.builder()
            .fieldName(column.getName())
            .fieldType(fieldType)
            .accessorType(accessorType)
            .isNullableColumn(column.isNullable())
            .isReadOnly(false)
            .build();
    return new FieldDefinition(fieldSpec.build(), accessorSpec);
  }

  /**
   * Creates a list containing one {@link FieldDefinition} for each array defined in the mapping.
   *
   * @param root {@link RootBean} containing all known {@link MappingBean}s
   * @param mapping {@link MappingBean} possibly containing arrays
   * @param primaryKeyFieldCount number of fields in this mapping's primary key
   * @return list of {@link FieldDefinition}s
   * @throws MojoExecutionException if the mapping is invalid or array mapping does not exist
   */
  @VisibleForTesting
  List<FieldDefinition> createFieldDefinitionsForArrays(
      RootBean root, MappingBean mapping, int primaryKeyFieldCount) throws MojoExecutionException {
    List<FieldDefinition> fieldSpecs = new ArrayList<>();
    var arrays = mapping.getArrayJoins();
    if (arrays.size() > 0 && primaryKeyFieldCount != 1) {
      throw MojoUtil.createException(
          "classes with arrays must have a single primary key column but this one has %d: mapping=%s",
          primaryKeyFieldCount, mapping.getId());
    }
    for (JoinBean arrayBean : arrays) {
      if (root.findMappingWithId(arrayBean.getEntityMapping()).isEmpty()) {
        throw MojoUtil.createException(
            "array references unknown mapping: mapping=%s array=%s missing=%s",
            mapping.getId(), arrayBean.getFieldName(), arrayBean.getEntityMapping());
      }
      fieldSpecs.add(createFieldDefinitionForArray(root, mapping, arrayBean));
    }
    return fieldSpecs;
  }

  /**
   * Creates a {@link FieldDefinition} for the field that holds the values for a multi-value
   * (one-to-many or many-to-many) join corresponding to an array in the {@link MappingBean}.
   *
   * @param root {@link RootBean} containing all known {@link MappingBean}s
   * @param mapping {@link MappingBean} containing the array
   * @param join {@link JoinBean} containing the spec for elements of the array
   * @return {@link FieldDefinition} for the field
   * @throws MojoExecutionException if the join is single value
   */
  @VisibleForTesting
  FieldDefinition createFieldDefinitionForArray(RootBean root, MappingBean mapping, JoinBean join)
      throws MojoExecutionException {
    if (join.getJoinType().isSingleValue()) {
      throw MojoUtil.createException(
          "array mappings must have multi-value joins: array=%s joinType=%s",
          join.getFieldName(), join.getJoinType());
    }
    return createFieldDefinitionForJoin(root, mapping, join);
  }

  /**
   * Creates a {@link FieldSpec} for the field to be added to a primary key inner class to hold the
   * value of the column in the joined entity.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} containing the join
   * @param join {@link JoinBean} for the join that connects us to the joined entity
   * @return the {@link FieldSpec}
   * @throws MojoExecutionException if no mapping exists for the joined entity
   */
  @VisibleForTesting
  FieldSpec createPrimaryKeyFieldSpecForJoin(RootBean root, MappingBean mapping, JoinBean join)
      throws MojoExecutionException {
    var parentMapping = root.findMappingForJoinBean(join);
    if (parentMapping.isEmpty()) {
      throw MojoUtil.createException(
          "no mapping found for primary key join class: mapping=%s join=%s entityClass=%s entityMapping=%s",
          mapping.getId(), join.getFieldName(), join.getEntityClass(), join.getEntityMapping());
    }
    var keyColumn = parentMapping.get().getTable().findColumnByName(join.getJoinColumnName());
    return createPrimaryKeyFieldSpecForColumn(parentMapping.get(), join.getFieldName(), keyColumn);
  }

  /**
   * Creates a {@link FieldSpec} for the field to be added to a primary key inner class to hold the
   * value of the given column.
   *
   * @param mapping {@link MappingBean} containing the column
   * @param fieldName name to use for the field
   * @param column {@link ColumnBean} for the primary key column
   * @return the {@link FieldSpec}
   */
  @VisibleForTesting
  FieldSpec createPrimaryKeyFieldSpecForColumn(
      MappingBean mapping, String fieldName, ColumnBean column) {
    final TypeName fieldType = createFieldTypeForColumn(mapping, column);
    return FieldSpec.builder(fieldType, fieldName).addModifiers(Modifier.PRIVATE).build();
  }

  /**
   * Creates a list containing one {@link MethodSpec} for each setter and getter method required for
   * the given {@link AccessorSpec}.
   *
   * @param mapping {@link MappingBean} containing the field
   * @param accessorSpec {@link AccessorSpec} describing the type of accessors to create
   * @return the list of {@link MethodSpec}
   */
  @VisibleForTesting
  List<MethodSpec> createMethodSpecsForAccessorSpec(
      MappingBean mapping, AccessorSpec accessorSpec) {
    List<MethodSpec> methodSpecs = new ArrayList<>();
    if (accessorSpec.isNullableColumn
        && mapping.getNullableFieldAccessorType()
            == MappingBean.NullableFieldAccessorType.Optional) {
      methodSpecs.add(
          PoetUtil.createOptionalGetter(
              accessorSpec.fieldName, accessorSpec.fieldType, accessorSpec.accessorType));
      if (!accessorSpec.isReadOnly) {
        methodSpecs.add(
            PoetUtil.createOptionalSetter(
                accessorSpec.fieldName, accessorSpec.fieldType, accessorSpec.accessorType));
      }
    } else {
      methodSpecs.add(
          PoetUtil.createStandardGetter(
              accessorSpec.fieldName, accessorSpec.fieldType, accessorSpec.accessorType));
      if (!accessorSpec.isReadOnly) {
        methodSpecs.add(
            PoetUtil.createStandardSetter(
                accessorSpec.fieldName, accessorSpec.fieldType, accessorSpec.accessorType));
      }
    }
    return methodSpecs;
  }

  /**
   * Creates a list containing a {@link MethodSpec} for every join property getter defined in {@link
   * JoinBean} properties within the mapping.
   *
   * @param mapping {@link MappingBean} containing the joins
   * @return the list of {@link MethodSpec} objects
   * @throws MojoExecutionException if any types cannot be mapped
   */
  @VisibleForTesting
  List<MethodSpec> createMethodSpecsForJoinProperties(MappingBean mapping)
      throws MojoExecutionException {
    List<MethodSpec> methodSpecs = new ArrayList<>();
    for (JoinBean join : mapping.getNonArrayJoins()) {
      if (join.getJoinType().isSingleValue()) {
        for (JoinBean.Property property : join.getProperties()) {
          final TypeName fieldType =
              ModelUtil.mapJavaTypeToTypeName(property.getJavaType())
                  .orElseThrow(
                      () ->
                          MojoUtil.createException(
                              "invalid javaType %s for join property %s in mapping %s",
                              property.getJavaType(), property.getName(), mapping.getId()));
          methodSpecs.add(
              PoetUtil.createJoinPropertyGetter(
                  property.getName(), fieldType, join.getFieldName(), property.getFieldName()));
        }
      }
    }
    return methodSpecs;
  }

  /**
   * Creates a list containing one {@link AnnotationSpec} for each JPA annotation needed for the
   * given column.
   *
   * @param mapping {@link MappingBean} containing the column
   * @param column {@link ColumnBean} the column
   * @return the list of {@link AnnotationSpec}
   * @throws MojoExecutionException if any validity checks fail
   */
  @VisibleForTesting
  List<AnnotationSpec> createAnnotationsForColumn(MappingBean mapping, ColumnBean column)
      throws MojoExecutionException {
    final var annotationSpecs = new ArrayList<AnnotationSpec>();
    if (column.isIdentity() && column.hasSequence()) {
      throw MojoUtil.createException(
          "identity columns cannot have sequences: mapping=%s column=%s",
          mapping.getId(), column.getName());
    }

    if (column.getFieldType() == ColumnBean.FieldType.Transient
        && mapping.getTable().isPrimaryKey(column.getName())) {
      throw MojoUtil.createException(
          "transient columns cannot be primary keys: mapping=%s column=%s",
          mapping.getId(), column.getName());
    }
    if (column.getFieldType() == ColumnBean.FieldType.Transient) {
      annotationSpecs.add(AnnotationSpec.builder(Transient.class).build());
    } else {
      if (mapping.getTable().isPrimaryKey(column.getName())) {
        annotationSpecs.add(AnnotationSpec.builder(Id.class).build());
      }
      annotationSpecs.add(createColumnAnnotation(mapping.getTable(), column));
      if (column.isIdentity()) {
        annotationSpecs.add(
            AnnotationSpec.builder(GeneratedValue.class)
                .addMember("strategy", "$T.$L", GenerationType.class, GenerationType.IDENTITY)
                .build());
      } else if (column.hasSequence()) {
        annotationSpecs.add(
            AnnotationSpec.builder(GeneratedValue.class)
                .addMember("strategy", "$T.$L", GenerationType.class, GenerationType.SEQUENCE)
                .addMember("generator", "$S", column.getSequence().getName())
                .build());
        final var sequenceGenerator =
            AnnotationSpec.builder(SequenceGenerator.class)
                .addMember("name", "$S", column.getSequence().getName())
                .addMember("sequenceName", "$S", column.getSequence().getName())
                .addMember("allocationSize", "$L", column.getSequence().getAllocationSize());
        final var table = mapping.getTable();
        if (table.hasSchema()) {
          sequenceGenerator.addMember("schema", "$S", table.quoteName(table.getSchema()));
        }
        annotationSpecs.add(sequenceGenerator.build());
      }
    }
    return annotationSpecs;
  }

  /**
   * Creates an {@link AnnotationSpec} for an enum field.
   *
   * @param mapping {@link MappingBean} containing the column
   * @param column {@link ColumnBean} that stores the enum name
   * @return the {@link AnnotationSpec}
   * @throws MojoExecutionException if the column is of the wrong type
   */
  @VisibleForTesting
  AnnotationSpec createEnumeratedAnnotation(MappingBean mapping, ColumnBean column)
      throws MojoExecutionException {
    if (!column.isString()) {
      throw MojoUtil.createException(
          "enum columns must have String type but this one does not: mapping=%s column=%s",
          mapping.getId(), column.getName());
    }
    return AnnotationSpec.builder(Enumerated.class)
        .addMember("value", "$T.$L", EnumType.class, EnumType.STRING)
        .build();
  }

  /**
   * Creates a {@link ClassName} for a primary key inner class for an entity with a compound primary
   * key.
   *
   * @param mapping {@link MappingBean} that defines the entity
   * @return the {@link ClassName}
   */
  private ClassName computePrimaryKeyClassName(MappingBean mapping) {
    return ClassName.get(
        mapping.getEntityClassPackage(),
        mapping.getEntityClassSimpleName(),
        mapping.getTable().getCompositeKeyClassName());
  }

  /**
   * Creates a {@link AnnotationSpec} for an entity with a compound primary key.
   *
   * @param primaryKeyClassName {@link ClassName} to use for the generated class
   * @return the {@link AnnotationSpec}
   */
  private AnnotationSpec createIdClassAnnotation(ClassName primaryKeyClassName) {
    return AnnotationSpec.builder(IdClass.class)
        .addMember("value", "$T.class", primaryKeyClassName)
        .build();
  }

  /**
   * Creates a {@link AnnotationSpec} for a join field. The type of the annotation is based on the
   * {@link JoinBean} joinType but will be one of {@link jakarta.persistence.OneToMany}, {@link
   * jakarta.persistence.ManyToOne}, or {@link jakarta.persistence.OneToOne}.
   *
   * @param mapping {@link MappingBean} containing the join
   * @param join {@link JoinBean} the join
   * @return the {@link AnnotationSpec}
   * @throws MojoExecutionException if the join is invalid
   */
  @VisibleForTesting
  AnnotationSpec createJoinTypeAnnotation(MappingBean mapping, JoinBean join)
      throws MojoExecutionException {
    if (join.getJoinType() == null) {
      throw MojoUtil.createException(
          "missing joinType: mapping=%s joinFieldName=%s", mapping.getId(), join.getFieldName());
    }
    final var annotationClass = join.getJoinType().getAnnotationClass();
    final var builder = AnnotationSpec.builder(annotationClass);
    if (join.hasMappedBy()) {
      builder.addMember("mappedBy", "$S", join.getMappedBy());
    }
    if (join.isFetchTypeRequired()) {
      builder.addMember("fetch", "$T.$L", FetchType.class, join.getFetchType());
    }
    if (join.hasOrphanRemoval()) {
      builder.addMember("orphanRemoval", "$L", join.getOrphanRemoval());
    }
    for (CascadeType cascadeType : join.getCascadeTypes()) {
      builder.addMember("cascade", "$T.$L", CascadeType.class, cascadeType);
    }
    return builder.build();
  }

  /**
   * Creates a {@link JoinColumn} {@link AnnotationSpec} for a field that represents a join to
   * another table.
   *
   * @param mapping {@link MappingBean} containing the join
   * @param join {@link JoinBean} the join
   * @return the {@link AnnotationSpec}
   * @throws MojoExecutionException if the join is invalid
   */
  @VisibleForTesting
  AnnotationSpec createJoinColumnAnnotation(MappingBean mapping, JoinBean join)
      throws MojoExecutionException {
    if (!join.hasColumnName()) {
      throw MojoUtil.createException(
          "missing joinColumnName: mapping=%s join=%s", mapping.getId(), join.getFieldName());
    }
    final ColumnBean joinColumn =
        mapping
            .getTable()
            .getColumnByName(join.getJoinColumnName())
            .orElseThrow(
                () ->
                    MojoUtil.createException(
                        "joinColumnName must match a column in the same mapping: mapping=%s joinColumnName=%s",
                        mapping.getId(), join.getJoinColumnName()));
    AnnotationSpec.Builder builder =
        AnnotationSpec.builder(JoinColumn.class)
            .addMember("name", "$S", mapping.getTable().quoteName(joinColumn.getColumnName()));
    if (join.hasForeignKey()) {
      builder.addMember(
          "foreignKey",
          "$L",
          AnnotationSpec.builder(ForeignKey.class)
              .addMember("name", "$S", join.getForeignKey())
              .build());
    }
    return builder.build();
  }

  /**
   * Creates a {@link TypeSpec} for an inner class. Used in entities that have composite keys and
   * associated with an {@link IdClass} annotation on the entity class.
   *
   * @param mapping {@link MappingBean} for the entity class
   * @param primaryKeyClassName {@link ClassName} to use for the generated class
   * @param primaryKeyFieldSpecs {@link List} of primary key fields in the composite key
   * @return static inner class {@link TypeSpec}
   */
  @VisibleForTesting
  TypeSpec createTypeSpecForCompositePrimaryKeyClass(
      MappingBean mapping, ClassName primaryKeyClassName, List<FieldSpec> primaryKeyFieldSpecs) {
    TypeSpec.Builder pkClassBuilder =
        TypeSpec.classBuilder(primaryKeyClassName)
            .addSuperinterface(Serializable.class)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addAnnotation(Getter.class)
            .addAnnotation(EqualsAndHashCode.class)
            .addAnnotation(NoArgsConstructor.class)
            .addAnnotation(AllArgsConstructor.class)
            .addJavadoc("PK class for the $L table", mapping.getTable().getName())
            .addField(SerialVersionUIDField);
    for (FieldSpec fieldSpec : primaryKeyFieldSpecs) {
      pkClassBuilder.addField(fieldSpec);
    }
    return pkClassBuilder.build();
  }

  /**
   * Creates an {@link AnnotationSpec} for a lombok {@link EqualsAndHashCode} annotation for the
   * entity.
   *
   * @return an {@link AnnotationSpec}
   */
  private AnnotationSpec createEqualsAndHashCodeAnnotation() {
    return AnnotationSpec.builder(EqualsAndHashCode.class)
        .addMember("onlyExplicitlyIncluded", "$L", true)
        .build();
  }

  /**
   * Creates an {@link AnnotationSpec} for a JPA {@link Table} annotation for the given table.
   *
   * @param table {@link TableBean} containing the column
   * @return an {@link AnnotationSpec}
   */
  @VisibleForTesting
  AnnotationSpec createTableAnnotation(TableBean table) {
    AnnotationSpec.Builder builder =
        AnnotationSpec.builder(Table.class)
            .addMember("name", "$S", table.quoteName(table.getName()));
    if (table.hasSchema()) {
      builder.addMember("schema", "$S", table.quoteName(table.getSchema()));
    }
    return builder.build();
  }

  /**
   * Creates an {@link AnnotationSpec} for a JPA {@link Column} annotation for the given column.
   *
   * @param table {@link TableBean} containing the column
   * @param column {@link ColumnBean} defining the column
   * @return an {@link AnnotationSpec}
   */
  @VisibleForTesting
  AnnotationSpec createColumnAnnotation(TableBean table, ColumnBean column) {
    AnnotationSpec.Builder builder =
        AnnotationSpec.builder(Column.class)
            .addMember("name", "$S", table.quoteName(column.getColumnName()));
    builder.addMember("nullable", "$L", column.isNullable());
    if (!column.isUpdatable()) {
      builder.addMember("updatable", "$L", false);
    }
    if (column.isNumeric()) {
      builder.addMember("columnDefinition", "$S", column.getSqlType());
      var value = column.computePrecision();
      if (value > 0) {
        builder.addMember("precision", "$L", value);
      }
      value = column.computeScale();
      if (value > 0) {
        builder.addMember("scale", "$L", value);
      }
    }
    int length = column.computeLength();
    if (length > 0 && length < Integer.MAX_VALUE) {
      builder.addMember("length", "$L", length);
    }
    return builder.build();
  }

  /**
   * Creates an appropriate {@link TypeName} for the given column's field in the entity class. The
   * type is either an enum (possibly an inner class of the entity or a standalone enum class) or a
   * simple java type.
   *
   * @param mapping {@link MappingBean} containing the column
   * @param column {@link ColumnBean} defining the column
   * @return appropriate {@link TypeName}
   */
  @VisibleForTesting
  TypeName createFieldTypeForColumn(MappingBean mapping, ColumnBean column) {
    TypeName fieldType;
    if (column.isEnum()) {
      EnumTypeBean enumBean = mapping.findEnum(column.getEnumType());
      if (enumBean.isInnerClass()) {
        fieldType =
            ClassName.get(
                mapping.getEntityClassPackage(),
                mapping.getEntityClassSimpleName(),
                enumBean.getName());
      } else {
        fieldType = ClassName.get(enumBean.getPackageName(), enumBean.getName());
      }
    } else {
      fieldType = column.computeJavaType();
    }
    return fieldType;
  }

  /**
   * Creates an appropriate {@link TypeName} for the given column's setter return type and getter
   * parameter type. The type is either an enum (possibly an inner class of the entity or a
   * standalone enum class) or a simple java type.
   *
   * @param mapping {@link MappingBean} containing the column
   * @param column {@link ColumnBean} defining the column
   * @return appropriate {@link TypeName}
   */
  @VisibleForTesting
  TypeName createAccessorTypeForColumn(MappingBean mapping, ColumnBean column) {
    TypeName fieldType;
    if (column.hasDefinedAccessorType()) {
      fieldType = column.computeJavaAccessorType();
    } else {
      fieldType = createFieldTypeForColumn(mapping, column);
    }
    return fieldType;
  }

  /**
   * Create a {@link TypeSpec} for a static inner class named "Fields" to provide additional
   * constants for use in transformers.
   *
   * @param mapping {@link MappingBean} for the entity with the new fields
   * @param fields list of {@link
   *     gov.cms.model.dsl.codegen.plugin.model.TableBean.AdditionalFieldName} objects for the
   *     fields
   * @return the {@link TypeSpec}
   */
  @VisibleForTesting
  TypeSpec createAdditionalFieldsInnerClass(
      MappingBean mapping, List<TableBean.AdditionalFieldName> fields) {
    ClassName className =
        ClassName.get(
            mapping.getEntityClassPackage(), mapping.getEntityClassSimpleName(), "Fields");
    final var classBuilder =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addJavadoc(
                "Defines extra field names. Lombok will append all of the other fields to this class automatically.");
    for (TableBean.AdditionalFieldName field : fields) {
      FieldSpec fieldSpec =
          FieldSpec.builder(
                  PoetUtil.StringClassName,
                  field.getName(),
                  Modifier.PUBLIC,
                  Modifier.STATIC,
                  Modifier.FINAL)
              .initializer("$S", field.getFieldValue())
              .build();
      classBuilder.addField(fieldSpec);
    }
    return classBuilder.build();
  }

  /**
   * Generates an accessor method for fields of the same group to be returned together.
   *
   * @param mapping {@link MappingBean} for the entity with the new fields
   * @return the list of {@link MethodSpec}
   */
  @VisibleForTesting
  List<MethodSpec> createMethodSpecsForGroupedProperties(MappingBean mapping) {
    List<MethodSpec> methodSpecs = new LinkedList<>();
    Map<String, List<ColumnBean>> groupedColumns =
        mapping.getTable().getColumns().stream()
            .filter(columnBean -> columnBean.hasGroupName())
            .collect(Collectors.groupingBy(ColumnBean::getGroupName));
    for (Map.Entry<String, List<ColumnBean>> entry : groupedColumns.entrySet()) {
      final var columnBean = entry.getValue().get(0);
      final var propertyType =
          columnBean.isNullable()
              ? ParameterizedTypeName.get(OptionalClassName, columnBean.computeJavaAccessorType())
              : columnBean.computeJavaAccessorType();
      final var methodSpec =
          PoetUtil.createGroupedPropertiesGetter(
              entry.getKey(),
              entry.getValue().stream().map(c -> c.getName()).toList(),
              propertyType);
      methodSpecs.add(methodSpec);
    }
    return methodSpecs;
  }

  /**
   * Immutable {@link Record} holding all the information required to create accessor methods
   * (setter/getter) for a field.
   *
   * @param fieldName Name of the field.
   * @param fieldType Type of the field.
   * @param accessorType Type of the accessor method parameter or return value.
   * @param isNullableColumn True if the column is nullable.
   * @param isReadOnly True if the column is read only. (No setter should be generated.)
   * @param groupName The name of the group of fields to which the field belongs.
   */
  @Builder
  @VisibleForTesting
  record AccessorSpec(
      String fieldName,
      TypeName fieldType,
      TypeName accessorType,
      boolean isNullableColumn,
      boolean isReadOnly,
      String groupName) {}

  /** Wrapper for the properties of every field to be generated in the entity class. */
  @Data
  @AllArgsConstructor
  @VisibleForTesting
  static class FieldDefinition {
    /** The {@link FieldSpec} defining how to generate the field itself. */
    private final FieldSpec fieldSpec;

    /**
     * The {@link AccessorSpec} defining how to generate the setter and getter methods for the
     * field.
     */
    private final AccessorSpec accessorSpec;

    /**
     * Optional value to define how to generate the field within our primary key inner class. Only
     * populated for fields that are part of the primary key.
     */
    private final Optional<FieldSpec> primaryKeyFieldSpec;

    /**
     * Creates an instance with no primary key spec.
     *
     * @param fieldSpec the {@link FieldSpec}
     * @param accessorSpec the {@link AccessorSpec}
     */
    FieldDefinition(FieldSpec fieldSpec, AccessorSpec accessorSpec) {
      this.fieldSpec = fieldSpec;
      this.accessorSpec = accessorSpec;
      primaryKeyFieldSpec = Optional.empty();
    }

    /**
     * Creates a new instance containing our current values plus the given primary key spec.
     *
     * @param primaryKeyFieldSpec the {@link FieldSpec} for the primary key
     * @return the new instance
     */
    FieldDefinition withPrimaryKeyFieldSpec(FieldSpec primaryKeyFieldSpec) {
      Preconditions.checkArgument(
          this.primaryKeyFieldSpec.isEmpty(), "can only add a primary key spec once");
      return new FieldDefinition(fieldSpec, accessorSpec, Optional.of(primaryKeyFieldSpec));
    }
  }
}
