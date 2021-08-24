package gov.cms.bfd.pipeline.rda.grpc.sink;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import gov.cms.bfd.pipeline.rda.grpc.ThrowableConsumer;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.junit.Before;
import org.junit.Test;

public class ConfigLoaderTest {
  private Map<String, String> values;
  private ConfigLoader loader;

  @Before
  public void setUp() {
    values = new HashMap<>();
    loader = new ConfigLoader(values::get);
  }

  @Test
  public void requiredStringValueFound() {
    values.put("a", "A");
    assertEquals("A", loader.stringValue("a"));
  }

  @Test(expected = ConfigException.class)
  public void requiredStringValueNotFound() {
    loader.stringValue("not-there");
  }

  @Test
  public void optionalStringValue() {
    values.put("a", "A");
    assertEquals("A", loader.stringValue("a", "---"));
    assertEquals("---", loader.stringValue("z", "---"));
    assertEquals(Optional.of("A"), loader.stringOption("a"));
    assertEquals(Optional.empty(), loader.stringOption("z"));
  }

  @Test
  public void requiredIntValueFound() {
    values.put("a", "33");
    assertEquals(33, loader.intValue("a"));
  }

  @Test(expected = ConfigException.class)
  public void requiredIntValueNotFound() {
    loader.intValue("not-there");
  }

  @Test(expected = ConfigException.class)
  public void invalidIntValue() {
    values.put("a", "-not-a-number");
    loader.intValue("a");
  }

  @Test
  public void optionalIntValue() {
    values.put("a", "33");
    assertEquals(33, loader.intValue("a", -10));
    assertEquals(-10, loader.intValue("z", -10));
    assertEquals(Optional.of(33), loader.intOption("a"));
    assertEquals(Optional.empty(), loader.intOption("z"));
  }

  @Test
  public void requiredEnumValueFound() {
    values.put("a", "First");
    assertEquals(TestEnum.First, loader.enumValue("a", TestEnum::valueOf));
  }

  @Test(expected = ConfigException.class)
  public void requiredEnumValueNotFound() {
    loader.enumValue("not-there", TestEnum::valueOf);
  }

  @Test(expected = ConfigException.class)
  public void invalidEnumValue() {
    values.put("a", "-not-a-number");
    loader.enumValue("a", TestEnum::valueOf);
  }

  @Test
  public void readableFileExists() throws Exception {
    runWithTempFile(
        file -> {
          Files.touch(file);
          values.put("a", file.getPath());
          assertEquals(file, loader.readableFile("a"));
        });
  }

  @Test(expected = ConfigException.class)
  public void readableFileNotReadable() throws Exception {
    runWithTempFile(
        file -> {
          Files.touch(file);
          file.setReadable(false);
          values.put("a", file.getPath());
          assertEquals(file, loader.readableFile("a"));
        });
  }

  @Test(expected = ConfigException.class)
  public void readableFileMissing() throws Exception {
    runWithTempFile(
        file -> {
          file.delete();
          values.put("a", file.getPath());
          assertEquals(file, loader.readableFile("a"));
        });
  }

  @Test(expected = ConfigException.class)
  public void writeableFileIsNotAFile() throws Exception {
    runWithTempFile(
        file -> {
          file.delete();
          file.mkdir();
          values.put("a", file.getPath());
          assertEquals(file, loader.writeableFile("a"));
        });
  }

  @Test
  public void writeableFileExists() throws Exception {
    runWithTempFile(
        file -> {
          Files.touch(file);
          values.put("a", file.getPath());
          assertEquals(file, loader.writeableFile("a"));
        });
  }

  @Test(expected = ConfigException.class)
  public void writeableFileNotWriteable() throws Exception {
    runWithTempFile(
        file -> {
          Files.touch(file);
          file.setWritable(false);
          values.put("a", file.getPath());
          assertEquals(file, loader.writeableFile("a"));
        });
  }

  @Test
  public void writeableFileMissing() throws Exception {
    runWithTempFile(
        file -> {
          file.delete();
          values.put("a", file.getPath());
          // passes because the parent directory is writeable
          assertEquals(file, loader.writeableFile("a"));
        });
  }

  @Test
  public void optionalBooleanValue() {
    values.put("a", "True");
    assertEquals(true, loader.booleanValue("a", false));
    assertEquals(false, loader.booleanValue("z", false));
  }

  @Test(expected = ConfigException.class)
  public void invalidBooleanValue() {
    values.put("a", "-not-a-boolean");
    loader.booleanValue("a", false);
  }

  @Test
  public void fromPropertiesFile() throws Exception {
    runWithTempFile(
        propFile -> {
          Properties p = new Properties();
          p.setProperty("a", "A");
          try (Writer out = new FileWriter(propFile)) {
            p.store(out, "");
          }
          ConfigLoader config = ConfigLoader.builder().addPropertiesFile(propFile).build();
          assertEquals("A", config.stringValue("a"));
        });
  }

  @Test
  public void fromEnvironmentVariables() {
    final List<String> names = new ArrayList<>(System.getenv().keySet());
    final ConfigLoader config = ConfigLoader.builder().addEnvironmentVariables().build();
    for (String name : names) {
      assertTrue("mismatch for " + name, System.getenv(name).equals(config.stringValue(name, "")));
    }
  }

  @Test
  public void fromSystemProperties() {
    final Iterable<String> names = System.getProperties().stringPropertyNames();
    final ConfigLoader config = ConfigLoader.builder().addSystemProperties().build();
    for (String name : names) {
      assertTrue(
          "mismatch for " + name,
          System.getProperty(name, "").equals(config.stringValue(name, "")));
    }
  }

  @Test
  public void testFallback() {
    final Map<String, String> primary = ImmutableMap.of("in-primary", "A");
    final Map<String, String> fallback =
        ImmutableMap.of("in-primary", "hidden", "in-fallback", "B");
    final ConfigLoader config = ConfigLoader.builder().add(fallback::get).add(primary::get).build();
    assertEquals("A", config.stringValue("in-primary"));
    assertEquals("B", config.stringValue("in-fallback"));
  }

  private void runWithTempFile(ThrowableConsumer<File> test) throws Exception {
    final File tempFile = File.createTempFile("ConfigLoaderTest", ".properties");
    try {
      test.accept(tempFile);
    } finally {
      tempFile.delete();
    }
  }

  public enum TestEnum {
    First,
    Last
  }
}
