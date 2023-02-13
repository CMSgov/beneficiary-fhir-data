package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

/** Tests the {@link MappingBean}. */
public class MappingBeanTest {
  /** Verify that entity class name can be parsed into package and simple names. */
  @Test
  public void testEntityClassNameParsing() {
    MappingBean mapping =
        MappingBean.builder().entityClassName("gov.cms.model.dsl.codegen.TestEntity").build();
    assertEquals("gov.cms.model.dsl.codegen", mapping.getEntityClassPackage());
    assertEquals("TestEntity", mapping.getEntityClassSimpleName());
  }

  /** Verify that methods properly indicate whether certain values are present or not. */
  @Test
  public void testAccessorsThatDetermineIfValuesAreDefined() {
    MappingBean mapping = new MappingBean();
    assertFalse(mapping.hasTransformer());
    assertFalse(mapping.hasExternalTransformations());
    assertFalse(mapping.hasEntityInterfaces());

    mapping.setTransformerClassName("");
    assertFalse(mapping.hasTransformer());
    mapping.setTransformerClassName("a");
    assertTrue(mapping.hasTransformer());

    mapping.getExternalTransformations().add(new ExternalTransformationBean());
    assertTrue(mapping.hasExternalTransformations());

    mapping.getEntityInterfaces().add("a");
    assertTrue(mapping.hasEntityInterfaces());
  }

  /** Verify that finding an enum by name works correctly. */
  @Test
  public void testFindEnum() {
    MappingBean mapping =
        MappingBean.builder()
            .enumType(EnumTypeBean.builder().name("a").build())
            .enumType(EnumTypeBean.builder().name("b").build())
            .build();
    assertEquals("a", mapping.findEnum("a").getName());
    assertThrows(IllegalArgumentException.class, () -> mapping.findEnum("x"));
  }

  /** Verify finding a join by field name works correctly. */
  @Test
  public void testFindJoinByFieldName() {
    MappingBean mapping =
        MappingBean.builder()
            .table(TableBean.builder().join(JoinBean.builder().fieldName("a").build()).build())
            .build();
    assertTrue(mapping.findJoinByFieldName("a").isPresent());
    assertFalse(mapping.findJoinByFieldName("x").isPresent());
  }

  /** Verify that joins associated with array fields are properly filtered. */
  @Test
  public void testGetNonArrayJoins() {
    JoinBean joinA = JoinBean.builder().fieldName("a").build();
    JoinBean joinB = JoinBean.builder().fieldName("b").build();
    JoinBean joinC = JoinBean.builder().fieldName("c").build();
    MappingBean mapping =
        MappingBean.builder()
            .table(TableBean.builder().join(joinA).join(joinB).join(joinC).build())
            .transformation(
                TransformationBean.builder()
                    .to("b")
                    .transformer(TransformationBean.ArrayTransformName)
                    .build())
            .build();
    assertEquals(ImmutableList.of(joinB), mapping.getArrayJoins());
    assertEquals(ImmutableList.of(joinA, joinC), mapping.getNonArrayJoins());
  }
}
