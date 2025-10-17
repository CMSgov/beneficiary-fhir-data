package gov.cms.model.dsl.codegen.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link PoetUtil}. */
public class PoetUtilTest {
  /** Unit test for {@link PoetUtil#createStandardGetter}. */
  @Test
  public void testCreateStandardGetter() {
    assertEquals(
        "public int getBucketCount() {\n" + "  return bucketCount;\n" + "}\n",
        PoetUtil.createStandardGetter("bucketCount", TypeName.INT, TypeName.INT).toString());

    assertEquals(
        "public java.lang.String getBucketName() {\n" + "  return bucketName;\n" + "}\n",
        PoetUtil.createStandardGetter(
                "bucketName", PoetUtil.StringClassName, PoetUtil.StringClassName)
            .toString());

    assertEquals(
        "public long getBucketName() {\n"
            + "  return java.lang.Long.parseLong(bucketName);\n"
            + "}\n",
        PoetUtil.createStandardGetter("bucketName", PoetUtil.StringClassName, TypeName.LONG)
            .toString());

    try {
      PoetUtil.createStandardGetter("bucketName", PoetUtil.StringClassName, PoetUtil.LongClassName);
      fail("should have thrown");
    } catch (IllegalArgumentException error) {
      assertEquals(
          "unsupported combination of java types for property: fieldType=java.lang.String accessorType=java.lang.Long property=bucketName",
          error.getMessage());
    }
  }

  /** Unit test for {@link PoetUtil#createStandardSetter}. */
  @Test
  public void testCreateStandardSetter() {
    assertEquals(
        "public void setBucketCount(int bucketCount) {\n"
            + "  this.bucketCount = bucketCount;\n"
            + "}\n",
        PoetUtil.createStandardSetter("bucketCount", TypeName.INT, TypeName.INT).toString());

    assertEquals(
        "public void setBucketName(java.lang.String bucketName) {\n"
            + "  this.bucketName = bucketName;\n"
            + "}\n",
        PoetUtil.createStandardSetter(
                "bucketName", PoetUtil.StringClassName, PoetUtil.StringClassName)
            .toString());

    assertEquals(
        "public void setBucketName(long bucketName) {\n"
            + "  this.bucketName = java.lang.String.valueOf(bucketName);\n"
            + "}\n",
        PoetUtil.createStandardSetter("bucketName", PoetUtil.StringClassName, TypeName.LONG)
            .toString());

    try {
      PoetUtil.createStandardSetter("bucketName", PoetUtil.StringClassName, PoetUtil.LongClassName);
      fail("should have thrown");
    } catch (IllegalArgumentException error) {
      assertEquals(
          "unsupported combination of java types for property: fieldType=java.lang.String accessorType=java.lang.Long property=bucketName",
          error.getMessage());
    }
  }

  /** Unit test for {@link PoetUtil#createOptionalGetter}. */
  @Test
  public void testCreateOptionalGetter() {
    assertEquals(
        "public java.util.Optional<java.lang.Long> getBucketCount() {\n"
            + "  return java.util.Optional.ofNullable(bucketCount);\n"
            + "}\n",
        PoetUtil.createOptionalGetter("bucketCount", PoetUtil.LongClassName, PoetUtil.LongClassName)
            .toString());

    assertEquals(
        "public java.util.Optional<java.lang.String> getBucketName() {\n"
            + "  return java.util.Optional.ofNullable(bucketName);\n"
            + "}\n",
        PoetUtil.createOptionalGetter(
                "bucketName", PoetUtil.StringClassName, PoetUtil.StringClassName)
            .toString());

    assertEquals(
        "public java.util.Optional<java.lang.Long> getBucketName() {\n"
            + "  return java.util.Optional.ofNullable(bucketName).map(java.lang.Long::parseLong);\n"
            + "}\n",
        PoetUtil.createOptionalGetter("bucketName", PoetUtil.StringClassName, TypeName.LONG)
            .toString());

    try {
      PoetUtil.createOptionalGetter("bucketName", PoetUtil.StringClassName, PoetUtil.LongClassName);
      fail("should have thrown");
    } catch (IllegalArgumentException error) {
      assertEquals(
          "unsupported combination of java types for property: fieldType=java.lang.String accessorType=java.lang.Long property=bucketName",
          error.getMessage());
    }
  }

  /** Unit test for {@link PoetUtil#createOptionalSetter}. */
  @Test
  public void testCreateOptionalSetter() {
    assertEquals(
        "public void setBucketCount(java.util.Optional<java.lang.Long> bucketCount) {\n"
            + "  this.bucketCount = bucketCount.orElse(null);\n"
            + "}\n",
        PoetUtil.createOptionalSetter("bucketCount", PoetUtil.LongClassName, PoetUtil.LongClassName)
            .toString());

    assertEquals(
        "public void setBucketName(java.util.Optional<java.lang.String> bucketName) {\n"
            + "  this.bucketName = bucketName.orElse(null);\n"
            + "}\n",
        PoetUtil.createOptionalSetter(
                "bucketName", PoetUtil.StringClassName, PoetUtil.StringClassName)
            .toString());

    assertEquals(
        "public void setBucketName(java.util.Optional<java.lang.Long> bucketName) {\n"
            + "  this.bucketName = bucketName.map(java.lang.String::valueOf).orElse(null);\n"
            + "}\n",
        PoetUtil.createOptionalSetter("bucketName", PoetUtil.StringClassName, TypeName.LONG)
            .toString());

    try {
      PoetUtil.createOptionalSetter("bucketName", PoetUtil.StringClassName, PoetUtil.LongClassName);
      fail("should have thrown");
    } catch (IllegalArgumentException error) {
      assertEquals(
          "unsupported combination of java types for property: fieldType=java.lang.String accessorType=java.lang.Long property=bucketName",
          error.getMessage());
    }
  }

  /** Unit test for {@link PoetUtil#createJoinPropertyGetter}. */
  @Test
  public void testCreateJoinPropertyGetter() {
    assertEquals(
        "public java.lang.String getMbi() {\n"
            + "  return mbiRecord == null ? null : mbiRecord.getMbi();\n"
            + "}\n",
        PoetUtil.createJoinPropertyGetter("mbi", PoetUtil.StringClassName, "mbiRecord", "mbi")
            .toString());
  }

  /** Unit test for {@link PoetUtil#createGroupedPropertiesGetter}. */
  @Test
  public void testCreateGroupedPropertiesGetter() {
    assertEquals(
"""
public java.util.Map<java.lang.String, java.util.Optional<java.lang.String>> getDiagnosisCodes() {
  java.util.Map<java.lang.String, java.util.Optional<java.lang.String>> diagnosisCodes = new java.util.HashMap<>();
  diagnosisCodes.put("diagnosis1Code", getDiagnosis1Code());
  diagnosisCodes.put("diagnosis2Code", getDiagnosis2Code());
  diagnosisCodes.put("diagnosis3Code", getDiagnosis3Code());
  return diagnosisCodes;
}
""",
        PoetUtil.createGroupedPropertiesGetter(
                "diagnosisCodes",
                Arrays.asList("diagnosis1Code", "diagnosis2Code", "diagnosis3Code"),
                ParameterizedTypeName.get(PoetUtil.OptionalClassName, PoetUtil.StringClassName))
            .toString());
  }
}
