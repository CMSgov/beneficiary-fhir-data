package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Tests the {@link ColumnBean}. */
public class ColumnBeanTest {
  /** Test accessor for column name. */
  @Test
  public void testGetColumnName() {
    assertEquals("field", ColumnBean.builder().name("field").build().getColumnName());
    assertEquals("field", ColumnBean.builder().name("field").dbName("").build().getColumnName());
    assertEquals(
        "column", ColumnBean.builder().name("field").dbName("column").build().getColumnName());
  }

  /** Test computation of column length. */
  @Test
  public void testComputeLength() {
    assertEquals(1, ColumnBean.builder().sqlType("char").build().computeLength());
    assertEquals(1, ColumnBean.builder().sqlType("VARCHAR").build().computeLength());
    assertEquals(10, ColumnBean.builder().sqlType("char(10)").build().computeLength());
    assertEquals(20, ColumnBean.builder().sqlType("varchar(20)").build().computeLength());
    assertEquals(
        Integer.MAX_VALUE, ColumnBean.builder().sqlType("varchar(max)").build().computeLength());
    assertEquals(0, ColumnBean.builder().sqlType("").build().computeLength());
    assertEquals(0, ColumnBean.builder().build().computeLength());
    assertEquals(0, ColumnBean.builder().sqlType("numeric(10)").build().computeLength());
    assertEquals(1, ColumnBean.builder().build().computeMinLength(1));
    assertEquals(3, ColumnBean.builder().minLength(3).build().computeMinLength(1));
  }

  /** Test computation of java type. */
  @Test
  public void testComputeJavaType() {
    assertEquals(
        ClassName.get(String.class),
        ColumnBean.builder().sqlType("varchar(10)").build().computeJavaType());
    assertEquals(
        ClassName.get(Short.class),
        ColumnBean.builder().sqlType("smallint").build().computeJavaType());
    assertEquals(
        ClassName.get(Long.class),
        ColumnBean.builder().sqlType("bigint").build().computeJavaType());
    assertEquals(
        ClassName.get(Integer.class),
        ColumnBean.builder().sqlType("int").build().computeJavaType());
    assertEquals(
        ClassName.get(BigDecimal.class),
        ColumnBean.builder().sqlType("decimal(4,2)").build().computeJavaType());
    assertEquals(
        ClassName.get(BigDecimal.class),
        ColumnBean.builder().sqlType("numeric(10)").build().computeJavaType());
    assertEquals(
        ClassName.get(LocalDate.class),
        ColumnBean.builder().sqlType("date").build().computeJavaType());
    assertEquals(
        ClassName.get(Instant.class),
        ColumnBean.builder().sqlType("timestamp with time zone").build().computeJavaType());

    assertEquals(TypeName.CHAR, ColumnBean.builder().javaType("char").build().computeJavaType());
    assertEquals(
        ClassName.get(Character.class),
        ColumnBean.builder().javaType("Character").build().computeJavaType());
    assertEquals(TypeName.INT, ColumnBean.builder().javaType("int").build().computeJavaType());
    assertEquals(
        ClassName.get(Integer.class),
        ColumnBean.builder().javaType("Integer").build().computeJavaType());
    assertEquals(TypeName.SHORT, ColumnBean.builder().javaType("short").build().computeJavaType());
    assertEquals(
        ClassName.get(Short.class),
        ColumnBean.builder().javaType("Short").build().computeJavaType());
    assertEquals(TypeName.LONG, ColumnBean.builder().javaType("long").build().computeJavaType());
    assertEquals(
        ClassName.get(Long.class), ColumnBean.builder().javaType("Long").build().computeJavaType());
    assertEquals(TypeName.INT, ColumnBean.builder().javaType("int").build().computeJavaType());
    assertEquals(
        ClassName.get(String.class),
        ColumnBean.builder().javaType("String").build().computeJavaType());
    assertEquals(
        ClassName.get(ColumnBean.class),
        ColumnBean.builder().javaType(ColumnBean.class.getName()).build().computeJavaType());
  }

  /** Test computation of accessor type. */
  @Test
  public void testComputeJavaAccessorType() {
    // the accessor type takes precedence
    assertEquals(
        TypeName.CHAR,
        ColumnBean.builder()
            .javaType("int")
            .javaAccessorType("char")
            .build()
            .computeJavaAccessorType());
    // the java type is used if there is no accessor type
    assertEquals(
        TypeName.INT, ColumnBean.builder().javaType("int").build().computeJavaAccessorType());
  }

  /** Test the simple boolean accessor methods. */
  @Test
  public void testSimpleBooleanGetters() {
    assertFalse(ColumnBean.builder().build().hasDefinedAccessorType());
    assertTrue(ColumnBean.builder().javaAccessorType("int").build().hasDefinedAccessorType());

    assertFalse(ColumnBean.builder().build().hasComment());
    assertTrue(ColumnBean.builder().comment("informative!").build().hasComment());

    assertFalse(ColumnBean.builder().sqlType("int").build().isNumeric());
    assertTrue(ColumnBean.builder().sqlType("decimal").build().isNumeric());
    assertTrue(ColumnBean.builder().sqlType("decimal(10)").build().isNumeric());
    assertTrue(ColumnBean.builder().sqlType("decimal(10,2)").build().isNumeric());
    assertTrue(ColumnBean.builder().sqlType("numeric").build().isNumeric());
    assertTrue(ColumnBean.builder().sqlType("numeric(11)").build().isNumeric());
    assertTrue(ColumnBean.builder().sqlType("numeric(11,3)").build().isNumeric());

    assertFalse(ColumnBean.builder().build().isEnum());
    assertTrue(ColumnBean.builder().enumType("something").build().isEnum());

    assertFalse(ColumnBean.builder().javaType("int").sqlType("int").build().isString());
    assertFalse(ColumnBean.builder().javaType("String").sqlType("int").build().isString());
    assertFalse(ColumnBean.builder().javaType("int").sqlType("char(10)").build().isString());
    assertTrue(ColumnBean.builder().javaType("String").sqlType("char(10)").build().isString());

    assertFalse(ColumnBean.builder().javaType("Character").build().isChar());
    assertTrue(ColumnBean.builder().javaType("char").build().isChar());

    assertTrue(ColumnBean.builder().javaType("Character").build().isCharacter());
    assertFalse(ColumnBean.builder().javaType("char").build().isCharacter());

    assertTrue(ColumnBean.builder().javaType("int").sqlType("date").build().isInt());
    assertTrue(ColumnBean.builder().javaType("String").sqlType("int").build().isInt());
    assertTrue(ColumnBean.builder().javaType("String").sqlType("integer").build().isInt());
    assertFalse(ColumnBean.builder().javaType("char").sqlType("char(10)").build().isInt());

    assertFalse(ColumnBean.builder().sqlType("int").build().isDate());
    assertTrue(ColumnBean.builder().sqlType("date").build().isDate());
    assertTrue(ColumnBean.builder().sqlType("datetime").build().isDate());

    assertFalse(ColumnBean.builder().build().hasSequence());
    assertTrue(ColumnBean.builder().sequence(new SequenceBean()).build().hasSequence());
  }

  /** Test extraction of length for decimal/numeric columns. */
  @Test
  public void testComputePrecision() {
    assertEquals(0, ColumnBean.builder().sqlType("date").build().computePrecision());
    assertEquals(0, ColumnBean.builder().sqlType("decimal").build().computePrecision());
    assertEquals(10, ColumnBean.builder().sqlType("decimal(10)").build().computePrecision());
    assertEquals(10, ColumnBean.builder().sqlType("decimal(10,2)").build().computePrecision());
    assertEquals(0, ColumnBean.builder().sqlType("numeric").build().computePrecision());
    assertEquals(12, ColumnBean.builder().sqlType("numeric(12)").build().computePrecision());
    assertEquals(12, ColumnBean.builder().sqlType("numeric(12, 2)").build().computePrecision());
  }

  /** Test extraction of scale for decimal/numeric columns. */
  @Test
  public void testComputeScale() {
    assertEquals(0, ColumnBean.builder().sqlType("date").build().computeScale());
    assertEquals(0, ColumnBean.builder().sqlType("decimal").build().computeScale());
    assertEquals(0, ColumnBean.builder().sqlType("decimal(10)").build().computeScale());
    assertEquals(2, ColumnBean.builder().sqlType("decimal(10, 2)").build().computeScale());
    assertEquals(0, ColumnBean.builder().sqlType("numeric(12)").build().computeScale());
    assertEquals(2, ColumnBean.builder().sqlType("numeric(12,2)").build().computeScale());
  }
}
