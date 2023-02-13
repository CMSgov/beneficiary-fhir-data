package gov.cms.model.dsl.codegen.plugin.model;

import static gov.cms.model.dsl.codegen.plugin.model.ModelUtil.mapJavaTypeToTypeName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Files;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Unit tests for the {@link ModelUtil} class. */
public class ModelUtilTest {
  /** Test methods that parse class and package names from strings. */
  @Test
  public void testClassNameParsing() {
    assertFalse(ModelUtil.isValidFullClassName(null));
    assertFalse(ModelUtil.isValidFullClassName(""));
    assertFalse(ModelUtil.isValidFullClassName("String"));
    assertTrue(ModelUtil.isValidFullClassName("java.lang.String"));
    assertEquals("java.lang", ModelUtil.packageName("java.lang.String"));
    assertEquals("String", ModelUtil.className("java.lang.String"));
    assertEquals(ClassName.get(String.class), ModelUtil.classType("java.lang.String"));
  }

  /**
   * Test the ability to load mappings from individual files as well as directories.
   *
   * @param folder the folder tor read from
   * @throws Exception included because the methods have checked exceptions
   */
  @Test
  public void testReadMappingsFromDisk(@TempDir File folder) throws Exception {
    assertThrows(
        IOException.class,
        () -> ModelUtil.loadModelFromYamlFileOrDirectory(folder.getPath() + "/nothing"));

    final var file1 = new File(folder, "a.yaml");
    Files.asCharSink(file1, StandardCharsets.UTF_8).write("mappings:\n  - id: a\n");

    final var file2 = new File(folder, "b.yaml");
    Files.asCharSink(file2, StandardCharsets.UTF_8).write("mappings:\n  - id: b\n");

    assertEquals(
        1,
        ModelUtil.loadModelFromYamlFileOrDirectory(file1.getAbsolutePath()).getMappings().size());
    assertEquals(
        1,
        ModelUtil.loadModelFromYamlFileOrDirectory(file2.getAbsolutePath()).getMappings().size());
    assertEquals(
        2,
        ModelUtil.loadModelFromYamlFileOrDirectory(folder.getAbsolutePath()).getMappings().size());
  }

  /** Validates that {@link ModelUtil#mapJavaTypeToTypeName} returns the correct type. */
  @Test
  public void testMapJavaTypeToTypeName() {
    assertEquals(Optional.of(TypeName.CHAR), mapJavaTypeToTypeName("char"));
    assertEquals(Optional.of(ClassName.get(Character.class)), mapJavaTypeToTypeName("Character"));
    assertEquals(Optional.of(TypeName.INT), mapJavaTypeToTypeName("int"));
    assertEquals(Optional.of(ClassName.get(Integer.class)), mapJavaTypeToTypeName("Integer"));
    assertEquals(Optional.of(TypeName.SHORT), mapJavaTypeToTypeName("short"));
    assertEquals(Optional.of(ClassName.get(Short.class)), mapJavaTypeToTypeName("Short"));
    assertEquals(Optional.of(TypeName.LONG), mapJavaTypeToTypeName("long"));
    assertEquals(Optional.of(ClassName.get(Long.class)), mapJavaTypeToTypeName("Long"));
    assertEquals(Optional.of(TypeName.INT), mapJavaTypeToTypeName("int"));
    assertEquals(Optional.of(ClassName.get(String.class)), mapJavaTypeToTypeName("String"));
    assertEquals(
        Optional.of(ClassName.get(ColumnBean.class)),
        mapJavaTypeToTypeName(ColumnBean.class.getName()));
    assertEquals(Optional.empty(), mapJavaTypeToTypeName("undefined"));
  }
}
