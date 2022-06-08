package gov.cms.model.dsl.codegen.plugin.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.common.io.Files;
import com.squareup.javapoet.ClassName;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Unit tests for the {@link ModelUtil} class. */
public class ModelUtilTest {
  /** Test methods that parse class and package names from strings. */
  @Test
  public void testClassNameParsing() {
    assertEquals("java.lang", ModelUtil.packageName("java.lang.String"));
    assertEquals("String", ModelUtil.className("java.lang.String"));
    assertEquals(ClassName.get(String.class), ModelUtil.classType("java.lang.String"));
  }

  /**
   * Test the ability to load mappings from individual files as well as directories.
   *
   * @throws Exception included because the methods have checked exceptions
   */
  @Test
  public void testReadMappingsFromDisk() throws Exception {
    final var folder = new TemporaryFolder();
    try {
      folder.create();
      assertThrows(
          IOException.class,
          () ->
              ModelUtil.loadModelFromYamlFileOrDirectory(folder.getRoot().getPath() + "/nothing"));

      final var file1 = folder.newFile("a.yaml");
      Files.asCharSink(file1, StandardCharsets.UTF_8).write("mappings:\n  - id: a\n");

      final var file2 = folder.newFile("b.yaml");
      Files.asCharSink(file2, StandardCharsets.UTF_8).write("mappings:\n  - id: b\n");

      assertEquals(
          1,
          ModelUtil.loadModelFromYamlFileOrDirectory(file1.getAbsolutePath()).getMappings().size());
      assertEquals(
          1,
          ModelUtil.loadModelFromYamlFileOrDirectory(file2.getAbsolutePath()).getMappings().size());
      assertEquals(
          2,
          ModelUtil.loadModelFromYamlFileOrDirectory(folder.getRoot().getAbsolutePath())
              .getMappings()
              .size());
    } finally {
      folder.delete();
    }
  }
}
