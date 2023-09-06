package gov.cms.model.dsl.codegen.plugin;

import static gov.cms.model.dsl.codegen.plugin.GenerateEntitiesFromDslMojo.AccessorSpec;
import static gov.cms.model.dsl.codegen.plugin.GenerateEntitiesFromDslMojo.BATCH_SIZE_FOR_ARRAY_FIELDS;
import static gov.cms.model.dsl.codegen.plugin.GenerateEntitiesFromDslMojo.FieldDefinition;
import static gov.cms.model.dsl.codegen.plugin.GenerateEntitiesFromDslMojo.SerialVersionUIDField;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.EnumTypeBean;
import gov.cms.model.dsl.codegen.plugin.model.JoinBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.SequenceBean;
import gov.cms.model.dsl.codegen.plugin.model.TableBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.hibernate.annotations.BatchSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GenerateEntitiesFromDslMojo}. */
public class GenerateEntitiesFromDslMojoTest {
  /** The generation class. */
  private GenerateEntitiesFromDslMojo mojo;

  /** Sets the test up. */
  @BeforeEach
  void setUp() {
    mojo = spy(new GenerateEntitiesFromDslMojo());
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createJavadocCommentForMapping}. */
  @Test
  public void testCreateJavadocCommentForMapping() throws MojoExecutionException {
    MappingBean mapping =
        MappingBean.builder().table(TableBean.builder().name("my_table").build()).build();
    assertEquals(
        "JPA class for the {@code my_table} table.", mojo.createJavadocCommentForMapping(mapping));

    mapping =
        MappingBean.builder()
            .table(
                TableBean.builder()
                    .name("my_table")
                    .comment("This is my comment and I like it.")
                    .build())
            .build();
    assertEquals("This is my comment and I like it.", mojo.createJavadocCommentForMapping(mapping));
  }

  /**
   * Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionsForPrimaryKeyJoins}.
   * Verifies that non-primary key joins are skipped but primary key joins are processed.
   */
  @Test
  public void testCreateFieldDefinitionsForPrimaryKeyJoins() throws MojoExecutionException {
    MappingBean mapping = MappingBean.builder().build();
    JoinBean primary = JoinBean.builder().fieldName("primary").build();
    JoinBean nonPrimary = JoinBean.builder().fieldName("non").build();
    List<JoinBean> joins = List.of(nonPrimary, primary);
    Set<String> primaryKeyFieldNames = Set.of("primary");
    FieldDefinition fieldDef = mock(FieldDefinition.class);
    FieldDefinition fieldDefWithPrimaryKeySpec = mock(FieldDefinition.class);
    FieldSpec primaryKeyFieldSpec = mock(FieldSpec.class);
    RootBean root = RootBean.builder().mapping(mapping).build();

    doReturn(fieldDef).when(mojo).createFieldDefinitionForJoin(root, mapping, primary);
    doReturn(primaryKeyFieldSpec)
        .when(mojo)
        .createPrimaryKeyFieldSpecForJoin(root, mapping, primary);
    doReturn(fieldDefWithPrimaryKeySpec)
        .when(fieldDef)
        .withPrimaryKeyFieldSpec(primaryKeyFieldSpec);

    List<FieldDefinition> result =
        mojo.createFieldDefinitionsForPrimaryKeyJoins(root, mapping, joins, primaryKeyFieldNames);
    assertEquals(List.of(fieldDefWithPrimaryKeySpec), result);
  }

  /**
   * Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionsForOrdinaryJoins}. Verifies
   * that primary key joins are skipped but others are processed.
   */
  @Test
  public void testCreateFieldDefinitionsForOrdinaryJoins() throws MojoExecutionException {
    MappingBean mapping = MappingBean.builder().build();
    JoinBean primary1 = JoinBean.builder().fieldName("p1").build();
    JoinBean primary2 = JoinBean.builder().fieldName("p2").build();
    JoinBean nonPrimary = JoinBean.builder().fieldName("non").build();
    List<JoinBean> joins = List.of(primary1, nonPrimary, primary2);
    RootBean root = RootBean.builder().mapping(mapping).build();
    Set<String> primaryKeyFieldNames = Set.of("p1", "p2");
    FieldDefinition def1 = mock(FieldDefinition.class);
    doReturn(def1).when(mojo).createFieldDefinitionForJoin(root, mapping, nonPrimary);
    List<FieldDefinition> result =
        mojo.createFieldDefinitionsForOrdinaryJoins(root, mapping, joins, primaryKeyFieldNames);
    assertEquals(List.of(def1), result);
  }

  /**
   * Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionForJoin}. Tests a join with
   * one-to-one relationship and minimum of options.
   */
  @Test
  public void testCreateFieldDefinitionForJoin_Simple() throws MojoExecutionException {
    JoinBean join =
        JoinBean.builder()
            .fieldName("af")
            .entityClass("gov.cms.test.B")
            .joinType(JoinBean.JoinType.OneToOne)
            .build();
    ColumnBean column = ColumnBean.builder().name("ac").sqlType("varchar(10)").build();
    TableBean table =
        TableBean.builder()
            .quoteNames(false)
            .name("records")
            .join(join)
            .column(column)
            .primaryKeyColumn("af")
            .build();
    MappingBean mapping =
        MappingBean.builder().id("m").entityClassName("gov.cms.test.A").table(table).build();
    RootBean root = RootBean.builder().mapping(mapping).build();

    TypeName fieldType = ClassName.get("gov.cms.test", "B");
    assertEquals(
        new FieldDefinition(
            FieldSpec.builder(fieldType, "af", Modifier.PRIVATE)
                .addAnnotation(Id.class)
                .addAnnotation(EqualsAndHashCode.Include.class)
                .addAnnotation(OneToOne.class)
                .build(),
            AccessorSpec.builder()
                .fieldName("af")
                .fieldType(fieldType)
                .accessorType(fieldType)
                .isNullableColumn(false)
                .isReadOnly(false)
                .build()),
        mojo.createFieldDefinitionForJoin(root, mapping, join));
  }

  /**
   * Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionForJoin}. Tests a join with
   * one-to-many and all available options.
   */
  @Test
  public void testCreateFieldDefinitionForJoin_AllOptions() throws MojoExecutionException {
    JoinBean join =
        JoinBean.builder()
            .fieldName("af")
            .joinColumnName("ac")
            .entityClass("gov.cms.test.B")
            .joinType(JoinBean.JoinType.OneToMany)
            .comment("hello")
            .orderBy("ac ASC")
            .build();
    ColumnBean column = ColumnBean.builder().name("ac").sqlType("varchar(10)").build();
    TableBean table =
        TableBean.builder().quoteNames(false).name("records").join(join).column(column).build();
    MappingBean mapping =
        MappingBean.builder().id("m").entityClassName("gov.cms.test.A").table(table).build();
    RootBean root = RootBean.builder().mapping(mapping).build();

    TypeName fieldType =
        ParameterizedTypeName.get(ClassName.get(List.class), ClassName.get("gov.cms.test", "B"));
    TypeName initializerType = ClassName.get(LinkedList.class);
    assertEquals(
        new FieldDefinition(
            FieldSpec.builder(fieldType, "af", Modifier.PRIVATE)
                .addJavadoc("hello")
                .addAnnotation(OneToMany.class)
                .addAnnotation(
                    AnnotationSpec.builder(JoinColumn.class).addMember("name", "$S", "ac").build())
                .addAnnotation(
                    AnnotationSpec.builder(OrderBy.class)
                        .addMember("value", "$S", "ac ASC")
                        .build())
                .initializer("new $T<>()", initializerType)
                .addAnnotation(
                    AnnotationSpec.builder(BatchSize.class)
                        .addMember("size", "$L", BATCH_SIZE_FOR_ARRAY_FIELDS)
                        .build())
                .addAnnotation(Builder.Default.class)
                .build(),
            AccessorSpec.builder()
                .fieldName("af")
                .fieldType(fieldType)
                .accessorType(fieldType)
                .isNullableColumn(false)
                .isReadOnly(false)
                .build()),
        mojo.createFieldDefinitionForJoin(root, mapping, join));

    // missing package name throws
    join.setEntityClass("NoPackage");
    var exception =
        assertThrows(
            MojoExecutionException.class,
            () -> mojo.createFieldDefinitionForJoin(root, mapping, join));
    assertEquals(
        "entityClass for join must include package: mapping=m join=af entityClass=Optional[NoPackage]",
        exception.getMessage());
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionsForColumns}. */
  @Test
  public void testCreateFieldDefinitionsForColumns() throws MojoExecutionException {
    ColumnBean column = ColumnBean.builder().name("c").sqlType("varchar(10)").build();
    TableBean table =
        TableBean.builder()
            .quoteNames(false)
            .name("records")
            .primaryKeyColumn("key")
            .column(column)
            .build();
    MappingBean mapping = MappingBean.builder().id("m").table(table).build();

    assertEquals(
        List.of(mojo.createFieldDefinitionForColumn(mapping, column)),
        mojo.createFieldDefinitionsForColumns(mapping, List.of("key")));

    assertEquals(
        List.of(
            mojo.createFieldDefinitionForColumn(mapping, column)
                .withPrimaryKeyFieldSpec(
                    mojo.createPrimaryKeyFieldSpecForColumn(mapping, column.getName(), column))),
        mojo.createFieldDefinitionsForColumns(mapping, List.of("c")));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionForColumn}. */
  @Test
  public void testCreateFieldDefinitionForColumn() throws MojoExecutionException {
    TableBean table =
        TableBean.builder().quoteNames(false).name("records").primaryKeyColumn("key").build();
    EnumTypeBean enumType =
        EnumTypeBean.builder().name("E").packageName("gov.cms.test").value("C").value("D").build();
    ClassName enumClass = ClassName.get("gov.cms.test", "E");
    MappingBean mapping = MappingBean.builder().id("m").table(table).enumType(enumType).build();
    ColumnBean column = ColumnBean.builder().name("c").sqlType("varchar(10)").build();
    List<AnnotationSpec> columnAnnotations = mojo.createAnnotationsForColumn(mapping, column);
    ClassName fieldType = PoetUtil.StringClassName;

    // simple
    assertEquals(
        new FieldDefinition(
            FieldSpec.builder(fieldType, "c", Modifier.PRIVATE)
                .addAnnotations(columnAnnotations)
                .build(),
            AccessorSpec.builder()
                .fieldName("c")
                .fieldType(fieldType)
                .accessorType(fieldType)
                .isNullableColumn(true)
                .isReadOnly(false)
                .build()),
        mojo.createFieldDefinitionForColumn(mapping, column));

    // all the options
    column.setComment("hello world");
    column.setEnumType("E");
    table.setEqualsColumns(List.of("c"));
    assertEquals(
        new FieldDefinition(
            FieldSpec.builder(enumClass, "c", Modifier.PRIVATE)
                .addJavadoc("hello world")
                .addAnnotation(mojo.createEnumeratedAnnotation(mapping, column))
                .addAnnotations(columnAnnotations)
                .addAnnotation(EqualsAndHashCode.Include.class)
                .build(),
            AccessorSpec.builder()
                .fieldName("c")
                .fieldType(enumClass)
                .accessorType(enumClass)
                .isNullableColumn(true)
                .isReadOnly(false)
                .build()),
        mojo.createFieldDefinitionForColumn(mapping, column));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionsForArrays}. */
  @Test
  public void testCreateFieldDefinitionForArrays() throws MojoExecutionException {
    JoinBean.Array array1 =
        new JoinBean.Array().toBuilder().fieldName("a").entityMapping("ma").build();
    JoinBean.Array array2 =
        new JoinBean.Array().toBuilder().fieldName("b").entityMapping("mb").build();
    MappingBean array1Mapping =
        MappingBean.builder().id("ma").entityClassName("gov.cms.test.A").build();
    TableBean table =
        TableBean.builder()
            .quoteNames(false)
            .name("records")
            .join(array1)
            .join(array2)
            .primaryKeyColumn("x")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .id("m")
            .table(table)
            .transformation(
                TransformationBean.builder()
                    .to("a")
                    .transformer(TransformationBean.ArrayTransformName)
                    .build())
            .transformation(
                TransformationBean.builder()
                    .to("b")
                    .transformer(TransformationBean.ArrayTransformName)
                    .build())
            .build();
    RootBean root = RootBean.builder().mapping(mapping).mapping(array1Mapping).build();

    var exception =
        assertThrows(
            MojoExecutionException.class,
            () -> mojo.createFieldDefinitionsForArrays(root, mapping, 2));
    assertEquals(
        "classes with arrays must have a single primary key column but this one has 2: mapping=m",
        exception.getMessage());

    exception =
        assertThrows(
            MojoExecutionException.class,
            () -> mojo.createFieldDefinitionsForArrays(root, mapping, 1));
    assertEquals(
        "array references unknown mapping: mapping=m array=b missing=mb", exception.getMessage());

    // removed the bad array so it should generate a field
    table.setJoins(List.of(array1));
    assertEquals(
        mojo.createFieldDefinitionForJoin(root, mapping, array1),
        mojo.createFieldDefinitionForArray(root, mapping, array1));
    assertEquals(
        List.of(mojo.createFieldDefinitionForArray(root, mapping, array1)),
        mojo.createFieldDefinitionsForArrays(root, mapping, 1));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createFieldDefinitionForArray}. */
  @Test
  public void testCreateFieldDefinitionForArray() throws MojoExecutionException {
    JoinBean.Array array =
        new JoinBean.Array()
            .toBuilder()
                .fieldName("a")
                .joinType(JoinBean.JoinType.OneToOne)
                .entityMapping("ma")
                .build();
    MappingBean arrayMapping =
        MappingBean.builder().id("ma").entityClassName("gov.cms.test.A").build();
    TableBean table = TableBean.builder().quoteNames(false).name("records").join(array).build();
    MappingBean mapping =
        MappingBean.builder()
            .table(table)
            .transformation(
                TransformationBean.builder()
                    .to("a")
                    .transformer(TransformationBean.ArrayTransformName)
                    .build())
            .build();
    RootBean root = RootBean.builder().mapping(mapping).mapping(arrayMapping).build();

    var exception =
        assertThrows(
            MojoExecutionException.class,
            () -> mojo.createFieldDefinitionForArray(root, mapping, array));
    assertEquals(
        "array mappings must have multi-value joins: array=a joinType=OneToOne",
        exception.getMessage());

    array.setJoinType(JoinBean.JoinType.OneToMany);
    assertEquals(
        mojo.createFieldDefinitionForJoin(root, mapping, array),
        mojo.createFieldDefinitionForArray(root, mapping, array));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createPrimaryKeyFieldSpecForJoin}. */
  @Test
  public void testCreatePrimaryKeyFieldSpecForJoin() throws MojoExecutionException {
    JoinBean join =
        JoinBean.builder()
            .fieldName("af")
            .joinColumnName("ac")
            .entityClass("gov.cms.test.B")
            .build();
    ColumnBean column = ColumnBean.builder().name("ac").sqlType("varchar(10)").build();
    TableBean table =
        TableBean.builder().quoteNames(false).name("records").join(join).column(column).build();
    MappingBean mapping =
        MappingBean.builder().id("m").entityClassName("gov.cms.test.A").table(table).build();
    MappingBean parentMapping =
        MappingBean.builder().id("pm").table(table).entityClassName("gov.cms.test.B").build();
    RootBean root = RootBean.builder().mapping(mapping).mapping(parentMapping).build();

    assertEquals(
        FieldSpec.builder(PoetUtil.StringClassName, "af", Modifier.PRIVATE).build(),
        mojo.createPrimaryKeyFieldSpecForJoin(root, mapping, join));

    // unknown entity class name throws
    join.setEntityClass("gov.cms.test.NotThere");
    var exception =
        assertThrows(
            MojoExecutionException.class,
            () -> mojo.createPrimaryKeyFieldSpecForJoin(root, mapping, join));
    assertEquals(
        "no mapping found for primary key join class: mapping=m join=af entityClass=gov.cms.test.NotThere entityMapping=null",
        exception.getMessage());
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createMethodSpecsForAccessorSpec}. */
  @Test
  public void testCreateMethodSpecsForAccessorSpec() throws MojoExecutionException {
    final TypeName fieldType = PoetUtil.StringClassName;
    final TypeName accessorType = TypeName.LONG;
    final String fieldName = "f";
    MappingBean mapping = MappingBean.builder().id("m").build();

    // nullable using nulls
    assertEquals(
        List.of(
            PoetUtil.createStandardGetter(fieldName, fieldType, accessorType),
            PoetUtil.createStandardSetter(fieldName, fieldType, accessorType)),
        mojo.createMethodSpecsForAccessorSpec(
            mapping,
            AccessorSpec.builder()
                .fieldName(fieldName)
                .fieldType(fieldType)
                .accessorType(accessorType)
                .isNullableColumn(true)
                .isReadOnly(false)
                .build()));

    // read-only nullable using nulls
    assertEquals(
        List.of(PoetUtil.createStandardGetter(fieldName, fieldType, accessorType)),
        mojo.createMethodSpecsForAccessorSpec(
            mapping,
            AccessorSpec.builder()
                .fieldName(fieldName)
                .fieldType(fieldType)
                .accessorType(accessorType)
                .isNullableColumn(true)
                .isReadOnly(true)
                .build()));

    // not-nullable
    assertEquals(
        List.of(
            PoetUtil.createStandardGetter(fieldName, fieldType, accessorType),
            PoetUtil.createStandardSetter(fieldName, fieldType, accessorType)),
        mojo.createMethodSpecsForAccessorSpec(
            mapping,
            AccessorSpec.builder()
                .fieldName(fieldName)
                .fieldType(fieldType)
                .accessorType(accessorType)
                .isNullableColumn(false)
                .isReadOnly(false)
                .build()));

    // read-only not-nullable
    assertEquals(
        List.of(PoetUtil.createStandardGetter(fieldName, fieldType, accessorType)),
        mojo.createMethodSpecsForAccessorSpec(
            mapping,
            AccessorSpec.builder()
                .fieldName(fieldName)
                .fieldType(fieldType)
                .accessorType(accessorType)
                .isNullableColumn(false)
                .isReadOnly(true)
                .build()));

    // nullable using Optional
    mapping.setNullableFieldAccessorType(MappingBean.NullableFieldAccessorType.Optional);
    assertEquals(
        List.of(
            PoetUtil.createOptionalGetter(fieldName, fieldType, accessorType),
            PoetUtil.createOptionalSetter(fieldName, fieldType, accessorType)),
        mojo.createMethodSpecsForAccessorSpec(
            mapping,
            AccessorSpec.builder()
                .fieldName(fieldName)
                .fieldType(fieldType)
                .accessorType(accessorType)
                .isNullableColumn(true)
                .isReadOnly(false)
                .build()));

    // read-only nullable using Optional
    assertEquals(
        List.of(PoetUtil.createOptionalGetter(fieldName, fieldType, accessorType)),
        mojo.createMethodSpecsForAccessorSpec(
            mapping,
            AccessorSpec.builder()
                .fieldName(fieldName)
                .fieldType(fieldType)
                .accessorType(accessorType)
                .isNullableColumn(true)
                .isReadOnly(true)
                .build()));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createMethodSpecsForJoinProperties}. */
  @Test
  public void testCreateMethodSpecsForJoinProperties() throws MojoExecutionException {
    final TypeName fieldType = PoetUtil.StringClassName;
    JoinBean join =
        JoinBean.builder()
            .fieldName("j")
            .joinType(JoinBean.JoinType.OneToOne)
            .property(new JoinBean.Property("g", "p", "String"))
            .build();
    TableBean table = TableBean.builder().join(join).build();
    MappingBean mapping = MappingBean.builder().id("m").table(table).build();

    assertEquals(
        List.of(PoetUtil.createJoinPropertyGetter("g", fieldType, "j", "p")),
        mojo.createMethodSpecsForJoinProperties(mapping));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createEnumeratedAnnotation}. */
  @Test
  public void testCreateAnnotationsForColumn() throws MojoExecutionException {
    TableBean table =
        TableBean.builder().quoteNames(false).name("records").primaryKeyColumn("key").build();
    MappingBean mapping = MappingBean.builder().id("m").table(table).build();

    // identity fields with sequence not allowed
    ColumnBean column1 =
        ColumnBean.builder()
            .name("c")
            .identity(true)
            .sequence(SequenceBean.builder().build())
            .build();
    var exception =
        assertThrows(
            MojoExecutionException.class, () -> mojo.createAnnotationsForColumn(mapping, column1));
    assertEquals(
        "identity columns cannot have sequences: mapping=m column=c", exception.getMessage());

    // transient columns cannot be primary keys
    ColumnBean column2 =
        ColumnBean.builder().name("key").fieldType(ColumnBean.FieldType.Transient).build();
    exception =
        assertThrows(
            MojoExecutionException.class, () -> mojo.createAnnotationsForColumn(mapping, column2));
    assertEquals(
        "transient columns cannot be primary keys: mapping=m column=key", exception.getMessage());

    // simple
    ColumnBean column3 = ColumnBean.builder().name("c").sqlType("int").build();
    assertEquals(
        List.of(mojo.createColumnAnnotation(table, column3)),
        mojo.createAnnotationsForColumn(mapping, column3));

    // transient
    column3 = ColumnBean.builder().name("c").fieldType(ColumnBean.FieldType.Transient).build();
    assertEquals(
        List.of(AnnotationSpec.builder(Transient.class).build()),
        mojo.createAnnotationsForColumn(mapping, column3));

    // primary key and identity
    column3 = ColumnBean.builder().name("key").identity(true).sqlType("int").build();
    assertEquals(
        List.of(
            AnnotationSpec.builder(Id.class).build(),
            mojo.createColumnAnnotation(table, column3),
            AnnotationSpec.builder(GeneratedValue.class)
                .addMember("strategy", "$T.$L", GenerationType.class, GenerationType.IDENTITY)
                .build()),
        mojo.createAnnotationsForColumn(mapping, column3));

    // sequence
    column3 =
        ColumnBean.builder()
            .name("a")
            .sequence(SequenceBean.builder().name("s").allocationSize(8).build())
            .sqlType("int")
            .build();
    assertEquals(
        List.of(
            mojo.createColumnAnnotation(table, column3),
            AnnotationSpec.builder(GeneratedValue.class)
                .addMember("strategy", "$T.$L", GenerationType.class, GenerationType.SEQUENCE)
                .addMember("generator", "$S", "s")
                .build(),
            AnnotationSpec.builder(SequenceGenerator.class)
                .addMember("name", "$S", "s")
                .addMember("sequenceName", "$S", "s")
                .addMember("allocationSize", "$L", 8)
                .build()),
        mojo.createAnnotationsForColumn(mapping, column3));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createEnumeratedAnnotation}. */
  @Test
  public void testCreateEnumeratedAnnotation() throws MojoExecutionException {
    ColumnBean column = ColumnBean.builder().name("c").sqlType("int").build();
    MappingBean mapping = MappingBean.builder().id("m").build();

    // non-string column throws
    var exception =
        assertThrows(
            MojoExecutionException.class, () -> mojo.createEnumeratedAnnotation(mapping, column));
    assertEquals(
        "enum columns must have String type but this one does not: mapping=m column=c",
        exception.getMessage());

    // string column works
    column.setSqlType("varchar(10)");
    assertEquals(
        AnnotationSpec.builder(Enumerated.class)
            .addMember("value", "$T.$L", EnumType.class, EnumType.STRING)
            .build(),
        mojo.createEnumeratedAnnotation(mapping, column));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createJoinTypeAnnotation}. */
  @Test
  public void testCreateJoinTypeAnnotation() throws MojoExecutionException {
    JoinBean join = JoinBean.builder().fieldName("af").joinColumnName("ac").build();
    MappingBean mapping = MappingBean.builder().id("m").build();

    // no configured join type throws
    var exception =
        assertThrows(
            MojoExecutionException.class, () -> mojo.createJoinTypeAnnotation(mapping, join));
    assertEquals("missing joinType: mapping=m joinFieldName=af", exception.getMessage());

    // no optional stuff
    join.setJoinType(JoinBean.JoinType.OneToMany);
    assertEquals(
        AnnotationSpec.builder(OneToMany.class).build(),
        mojo.createJoinTypeAnnotation(mapping, join));

    // all the optional stuff
    join.setMappedBy("mb");
    join.setOrphanRemoval(true);
    join.setFetchType(FetchType.EAGER);
    join.setCascadeTypes(List.of(CascadeType.MERGE, CascadeType.REMOVE));
    assertEquals(
        AnnotationSpec.builder(OneToMany.class)
            .addMember("mappedBy", "$S", "mb")
            .addMember("fetch", "$T.$L", FetchType.class, FetchType.EAGER)
            .addMember("orphanRemoval", "$L", true)
            .addMember("cascade", "$T.$L", CascadeType.class, CascadeType.MERGE)
            .addMember("cascade", "$T.$L", CascadeType.class, CascadeType.REMOVE)
            .build(),
        mojo.createJoinTypeAnnotation(mapping, join));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createJoinColumnAnnotation}. */
  @Test
  public void testCreateJoinColumnAnnotation() throws MojoExecutionException {
    JoinBean join = JoinBean.builder().fieldName("af").build();
    TableBean table =
        TableBean.builder()
            .quoteNames(false)
            .name("records")
            .column(ColumnBean.builder().name("ac").build())
            .join(join)
            .build();
    MappingBean mapping = MappingBean.builder().id("m").table(table).build();

    var exception =
        assertThrows(
            MojoExecutionException.class, () -> mojo.createJoinColumnAnnotation(mapping, join));
    assertEquals("missing joinColumnName: mapping=m join=af", exception.getMessage());

    join.setJoinColumnName("ac");

    // no foreign key
    assertEquals(
        AnnotationSpec.builder(JoinColumn.class)
            .addMember("name", "$S", mapping.getTable().quoteName("ac"))
            .build(),
        mojo.createJoinColumnAnnotation(mapping, join));

    // with a foreign key
    join.setForeignKey("fk");
    assertEquals(
        AnnotationSpec.builder(JoinColumn.class)
            .addMember("name", "$S", mapping.getTable().quoteName("ac"))
            .addMember(
                "foreignKey",
                "$L",
                AnnotationSpec.builder(ForeignKey.class).addMember("name", "$S", "fk").build())
            .build(),
        mojo.createJoinColumnAnnotation(mapping, join));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createTypeSpecForCompositePrimaryKeyClass}. */
  @Test
  public void testCreateTypeSpecForCompositePrimaryKeyClass() {
    FieldSpec field = FieldSpec.builder(TypeName.INT, "id", Modifier.PRIVATE).build();
    TableBean table = TableBean.builder().quoteNames(false).name("records").build();
    MappingBean mapping = MappingBean.builder().table(table).build();
    ClassName className = ClassName.get("gov.cms.test", "PK");
    assertEquals(
        TypeSpec.classBuilder(className)
            .addSuperinterface(Serializable.class)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .addAnnotation(Getter.class)
            .addAnnotation(EqualsAndHashCode.class)
            .addAnnotation(NoArgsConstructor.class)
            .addAnnotation(AllArgsConstructor.class)
            .addJavadoc("PK class for the $L table", mapping.getTable().getName())
            .addField(SerialVersionUIDField)
            .addField(field)
            .build(),
        mojo.createTypeSpecForCompositePrimaryKeyClass(mapping, className, List.of(field)));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createTableAnnotation}. */
  @Test
  public void testCreateTableAnnotation() {
    TableBean table = TableBean.builder().quoteNames(false).name("records").build();
    assertEquals(
        AnnotationSpec.builder(Table.class).addMember("name", "$S", "records").build(),
        mojo.createTableAnnotation(table));

    table = TableBean.builder().quoteNames(true).name("records").schema("rda").build();
    assertEquals(
        AnnotationSpec.builder(Table.class)
            .addMember("name", "$S", "`records`")
            .addMember("schema", "$S", "`rda`")
            .build(),
        mojo.createTableAnnotation(table));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createColumnAnnotation}. */
  @Test
  public void testCreateColumnAnnotation() {
    TableBean table = TableBean.builder().quoteNames(false).build();
    ColumnBean column = ColumnBean.builder().name("simple").sqlType("varchar(10)").build();
    assertEquals(
        AnnotationSpec.builder(Column.class)
            .addMember("name", "$S", "simple")
            .addMember("nullable", "$L", true)
            .addMember("length", "$L", 10)
            .build(),
        mojo.createColumnAnnotation(table, column));

    column = ColumnBean.builder().name("blob").sqlType("varchar(max)").build();
    assertEquals(
        AnnotationSpec.builder(Column.class)
            .addMember("name", "$S", "blob")
            .addMember("nullable", "$L", true)
            .build(),
        mojo.createColumnAnnotation(table, column));

    column = ColumnBean.builder().name("dollars").sqlType("decimal(12,2)").nullable(false).build();
    assertEquals(
        AnnotationSpec.builder(Column.class)
            .addMember("name", "$S", "dollars")
            .addMember("nullable", "$L", false)
            .addMember("columnDefinition", "$S", "decimal(12,2)")
            .addMember("precision", "$L", 12)
            .addMember("scale", "$L", 2)
            .build(),
        mojo.createColumnAnnotation(table, column));

    column =
        ColumnBean.builder()
            .name("records")
            .sqlType("numeric(11)")
            .nullable(false)
            .updatable(false)
            .build();
    assertEquals(
        AnnotationSpec.builder(Column.class)
            .addMember("name", "$S", "records")
            .addMember("nullable", "$L", false)
            .addMember("updatable", "$L", false)
            .addMember("columnDefinition", "$S", "numeric(11)")
            .addMember("precision", "$L", 11)
            .build(),
        mojo.createColumnAnnotation(table, column));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createFieldTypeForColumn}. */
  @Test
  public void testCreateFieldTypeForColumn() {
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Data")
            .enumType(EnumTypeBean.builder().name("InnerEnum").value("A").value("B").build())
            .enumType(
                EnumTypeBean.builder()
                    .name("OuterEnum")
                    .packageName("gov.cms.test")
                    .value("C")
                    .value("D")
                    .build())
            .build();

    // simple java type
    ColumnBean column = ColumnBean.builder().javaType("String").build();
    assertEquals(ClassName.get(String.class), mojo.createFieldTypeForColumn(mapping, column));

    // an enum that is an inner class defined within the entity
    column = ColumnBean.builder().enumType("InnerEnum").build();
    assertEquals(
        ClassName.get("gov.cms.test", "Data", "InnerEnum"),
        mojo.createFieldTypeForColumn(mapping, column));

    // an enum that is a stand-alone enum class
    column = ColumnBean.builder().enumType("OuterEnum").build();
    assertEquals(
        ClassName.get("gov.cms.test", "OuterEnum"), mojo.createFieldTypeForColumn(mapping, column));
  }

  /** Tests for {@link GenerateEntitiesFromDslMojo#createAccessorTypeForColumn}. */
  @Test
  public void testCreateAccessorTypeForColumn() {
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Data")
            .enumType(EnumTypeBean.builder().name("InnerEnum").value("A").value("B").build())
            .build();

    // simple java type
    ColumnBean column = ColumnBean.builder().javaType("String").build();
    assertEquals(ClassName.get(String.class), mojo.createAccessorTypeForColumn(mapping, column));

    // separate accessor type for the setter/getter
    column = ColumnBean.builder().javaType("String").javaAccessorType("long").build();
    assertEquals(TypeName.LONG, mojo.createAccessorTypeForColumn(mapping, column));
  }

  /** Test for {@link GenerateEntitiesFromDslMojo#createMethodSpecsForGroupedProperties}. */
  @Test
  public void testCreateMethodSpecsForGroupedProperties() {
    TableBean table =
        TableBean.builder()
            .quoteNames(false)
            .name("records")
            .column(
                ColumnBean.builder()
                    .name("diagnosis1Code")
                    .groupName("diagnosisCodes")
                    .javaAccessorType("String")
                    .build())
            .column(
                ColumnBean.builder()
                    .name("diagnosis2Code")
                    .groupName("diagnosisCodes")
                    .javaAccessorType("String")
                    .build())
            .column(
                ColumnBean.builder()
                    .name("diagnosis3Code")
                    .groupName("diagnosisCodes")
                    .javaAccessorType("String")
                    .build())
            .build();

    MappingBean mappingBean = MappingBean.builder().table(table).build();
    assertEquals(
        List.of(
            PoetUtil.createGroupedPropertiesGetter(
                "diagnosisCodes",
                Arrays.asList("diagnosis1Code", "diagnosis2Code", "diagnosis3Code"),
                ParameterizedTypeName.get(PoetUtil.OptionalClassName, PoetUtil.StringClassName))),
        mojo.createMethodSpecsForGroupedProperties(mappingBean));
  }
}
