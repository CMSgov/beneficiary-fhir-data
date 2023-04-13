package gov.cms.model.dsl.codegen.plugin.transformer;

import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.Base64TransformerName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.EnumValueTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.IdHashTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.IndexFromName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.IntStringTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.LongStringTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.MessageEnumTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.NoMappingFromName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.ParentFromName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.RifTimestampTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.ShortStringTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.TimestampTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.UintToShortTransformName;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.anyMappingRequiresIdHasher;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.createFieldNameForErrorReporting;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.createPropertyAccessCodeBlock;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.mappingRequiresEnumExtractor;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.mappingRequiresIdHasher;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.selectTransformerForField;
import static gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.toClassName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Unit test for {@link TransformerUtil}. */
public class TransformerUtilTest {
  /**
   * Verify that {@link TransformerUtil#mappingRequiresIdHasher} can find a transform that requires
   * an IdHasher if one is present.
   */
  @Test
  public void testMappingRequiresIdHasher() {
    MappingBean mapping = new MappingBean();
    assertFalse(mappingRequiresIdHasher(mapping));

    mapping
        .getTransformations()
        .add(TransformationBean.builder().from("x").transformer(IntStringTransformName).build());
    assertFalse(mappingRequiresIdHasher(mapping));

    mapping
        .getTransformations()
        .add(TransformationBean.builder().from("y").transformer(IdHashTransformName).build());
    assertTrue(mappingRequiresIdHasher(mapping));
  }

  /**
   * Verify that {@link TransformerUtil#anyMappingRequiresIdHasher} can find a transform that
   * requires an IdHasher if one is present.
   */
  @Test
  public void testAnyMappingRequiresIdHasher() {
    MappingBean noHashMapping = new MappingBean();
    noHashMapping
        .getTransformations()
        .add(TransformationBean.builder().from("x").transformer(IntStringTransformName).build());
    assertFalse(mappingRequiresIdHasher(noHashMapping));

    MappingBean hashMapping = new MappingBean();
    hashMapping
        .getTransformations()
        .add(TransformationBean.builder().from("y").transformer(IdHashTransformName).build());

    assertFalse(anyMappingRequiresIdHasher(Stream.of(noHashMapping)));
    assertTrue(anyMappingRequiresIdHasher(Stream.of(hashMapping)));
    assertTrue(anyMappingRequiresIdHasher(Stream.of(noHashMapping, hashMapping)));
  }

  /** Unit test for {@link TransformerUtil#mappingRequiresEnumExtractor}. */
  @Test
  public void testMappingRequiresEnumExtractor() {
    MappingBean mapping = new MappingBean();
    assertFalse(mappingRequiresIdHasher(mapping));

    mapping
        .getTransformations()
        .add(TransformationBean.builder().from("x").transformer(IntStringTransformName).build());
    assertFalse(mappingRequiresEnumExtractor(mapping));

    mapping
        .getTransformations()
        .add(TransformationBean.builder().from("y").transformer(MessageEnumTransformName).build());
    assertTrue(mappingRequiresEnumExtractor(mapping));
  }

  /** Unit test for {@link TransformerUtil#capitalize}. */
  @Test
  public void testCapitalize() {
    assertEquals("A", TransformerUtil.capitalize("a"));
    assertEquals("A", TransformerUtil.capitalize("A"));
    assertEquals("AbcDef", TransformerUtil.capitalize("abcDef"));
  }

  /** Verify the transformation name can be used to map the expected {@link FieldTransformer}. */
  @Test
  public void testSelectTransformerForFieldUsingTransformer() {
    ColumnBean column = ColumnBean.builder().build();
    TransformationBean transformation = new TransformationBean();

    transformation.setTransformer("undefined");
    assertEquals(Optional.empty(), selectTransformerForField(column, transformation));

    transformation.setTransformer(IdHashTransformName);
    assertTransformerInstanceOf(column, transformation, IdHashFieldTransformer.class);

    transformation.setTransformer(TimestampTransformName);
    assertTransformerInstanceOf(column, transformation, TimestampFieldTransformer.class);

    transformation.setTransformer(MessageEnumTransformName);
    assertTransformerInstanceOf(column, transformation, MessageEnumFieldTransformer.class);

    transformation.setTransformer(EnumValueTransformName);
    assertTransformerInstanceOf(column, transformation, EnumValueIfPresentTransformer.class);

    transformation.setTransformer(RifTimestampTransformName);
    assertTransformerInstanceOf(column, transformation, RifTimestampFieldTransformer.class);

    transformation.setTransformer(IntStringTransformName);
    assertTransformerInstanceOf(column, transformation, IntStringFieldTransformer.class);

    transformation.setTransformer(ShortStringTransformName);
    assertTransformerInstanceOf(column, transformation, ShortStringFieldTransformer.class);

    transformation.setTransformer(LongStringTransformName);
    assertTransformerInstanceOf(column, transformation, LongStringFieldTransformer.class);

    transformation.setTransformer(UintToShortTransformName);
    assertTransformerInstanceOf(column, transformation, UintToShortFieldTransformer.class);

    transformation.setTransformer(Base64TransformerName);
    assertTransformerInstanceOf(column, transformation, Base64FieldTransformer.class);
  }

  /** Verify the java and/or sql type can be used to map the expected {@link FieldTransformer}. */
  @Test
  public void testSelectTransformerForFieldUsingColumn() {
    ColumnBean column = mock(ColumnBean.class);
    TransformationBean transformation = new TransformationBean();
    transformation.setFrom("x");

    assertEquals(Optional.empty(), selectTransformerForField(column, transformation));

    doReturn(true).when(column).isEnum();
    assertEquals(Optional.empty(), selectTransformerForField(column, transformation));

    reset(column);
    doReturn(true).when(column).isChar();
    assertTransformerInstanceOf(column, transformation, CharFieldTransformer.class);

    reset(column);
    doReturn(true).when(column).isCharacter();
    assertTransformerInstanceOf(column, transformation, CharFieldTransformer.class);

    reset(column);
    doReturn(true).when(column).isString();
    assertTransformerInstanceOf(column, transformation, StringFieldTransformer.class);

    reset(column);
    doReturn(true).when(column).isInt();
    assertTransformerInstanceOf(column, transformation, IntFieldTransformer.class);

    reset(column);
    doReturn(true).when(column).isLong();
    assertTransformerInstanceOf(column, transformation, LongFieldTransformer.class);

    reset(column);
    doReturn(true).when(column).isNumeric();
    assertTransformerInstanceOf(column, transformation, AmountFieldTransformer.class);

    reset(column);
    doReturn(true).when(column).isDate();
    assertTransformerInstanceOf(column, transformation, DateFieldTransformer.class);
  }

  /** Verify the special from names prevent a transformer being returned. */
  @Test
  public void testSelectTransformerForFieldRespectsNoMappingName() {
    ColumnBean column = mock(ColumnBean.class);
    TransformationBean transformation = new TransformationBean();
    transformation.setFrom("x");

    // verify a transformer would normally be selected
    doReturn(true).when(column).isChar();
    assertTransformerInstanceOf(column, transformation, CharFieldTransformer.class);

    // now verify the special names prevent the transformer being returned
    for (String fromName : List.of(NoMappingFromName, ParentFromName, IndexFromName)) {
      transformation.setFrom(fromName);
      assertTransformerInstanceOf(column, transformation, NoCodeFieldTransformer.class);
    }
  }

  /** Verify the correct lambda is called depending on whether name is simple or nested. */
  @Test
  public void testCreatePropertyAccessCodeBlock() {
    Function<String, CodeBlock> simpleProperty = s -> CodeBlock.of("$L", s);
    BiFunction<String, String, CodeBlock> nestedProperty = (a, b) -> CodeBlock.of("$L-$L", a, b);

    TransformationBean transformation = new TransformationBean();
    transformation.setFrom("simple");
    assertEquals(
        "Simple",
        createPropertyAccessCodeBlock(transformation, simpleProperty, nestedProperty).toString());

    transformation.setFrom("field.property");
    assertEquals(
        "Field-Property",
        createPropertyAccessCodeBlock(transformation, simpleProperty, nestedProperty).toString());
  }

  /** Verify correct expression is created to generate a field name at runtime. */
  @Test
  public void testCreateFieldNameForErrorReporting() {
    MappingBean mapping = new MappingBean();
    ColumnBean column = new ColumnBean();

    mapping.setEntityClassName(TransformerUtil.class.getName());
    column.setName("x");
    assertEquals(
        CodeBlock.of(
            "namePrefix + gov.cms.model.dsl.codegen.plugin.transformer.TransformerUtil.Fields.x"),
        createFieldNameForErrorReporting(mapping, column));
  }

  /** Verify class names are extracted correctly. */
  @Test
  public void testToClassName() {
    assertThrows(IllegalArgumentException.class, () -> toClassName("String"));
    assertEquals(ClassName.get("java.lang", "String"), toClassName("java.lang.String"));
  }

  /**
   * Helper method to call {@link TransformerUtil#selectTransformerForField} and verify an instance
   * of the expected class was returned.
   *
   * @param column model object describing the database column
   * @param transformation model object describing the transformation to apply
   * @param klass expected class for the result
   */
  private void assertTransformerInstanceOf(
      ColumnBean column, TransformationBean transformation, Class<?> klass) {
    final Optional<FieldTransformer> result = selectTransformerForField(column, transformation);
    assertTrue(result.isPresent(), "expected transformer to be found");
    assertTrue(
        klass.isInstance(result.get()),
        String.format(
            "expected instance of %s but was %s",
            klass.getName(), result.get().getClass().getName()));
  }
}
