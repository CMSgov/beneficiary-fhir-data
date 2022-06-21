package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RootBean}. */
public class RootBeanTest {
  /** Verify that mappings can be added to {@link RootBean}s and found using the find methods. */
  @Test
  public void testAddingAndFinding() {
    MappingBean mappingA = MappingBean.builder().id("a").entityClassName("test.a").build();
    MappingBean mappingB = MappingBean.builder().id("b").entityClassName("test.b").build();

    final var root1 = RootBean.builder().mapping(mappingA).build();
    final var root2 = RootBean.builder().mapping(mappingB).build();
    final var root3 = new RootBean();
    root3.addMappingsFrom(root1);
    root3.addMappingsFrom(root2);

    assertEquals(Optional.of(mappingA), root1.findMappingWithId("a"));
    assertEquals(Optional.empty(), root2.findMappingWithId("a"));
    assertEquals(Optional.of(mappingA), root3.findMappingWithId("a"));

    assertEquals(Optional.empty(), root1.findMappingWithEntityClassName("test.b"));
    assertEquals(Optional.of(mappingB), root2.findMappingWithEntityClassName("test.b"));
    assertEquals(Optional.of(mappingB), root3.findMappingWithEntityClassName("test.b"));
  }
}
