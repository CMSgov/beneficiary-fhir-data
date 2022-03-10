package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ConfigLoaderTest {

  private Map<String, String> values;
  private ConfigLoader loader;

  @BeforeEach
  public void setUp() {
    values = new HashMap<>();
    loader = spy(ConfigLoader.builder().addSingle(values::get).build());
  }

  @Test
  public void requiredStringValueFound() {
    values.put("a", "A");
    assertEquals("A", loader.stringValue("a"));
  }

  @Test
  public void requiredStringValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.stringValue("not-there");
        });
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
  public void requiredFloatValueFound() {
    values.put("a", "3.3");
    assertEquals(3.3f, loader.floatValue("a"));
  }

  @Test
  public void requiredFloatValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.floatValue("not-there");
        });
  }

  @Test
  public void invalidFloatValue() {
    values.put("a", "-not-a-number");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.floatValue("a");
        });
  }

  @Test
  public void optionalFloatValue() {
    values.put("a", "3.3");
    assertEquals(3.3f, loader.floatValue("a", -10.0f));
    assertEquals(-10.0f, loader.floatValue("z", -10.0f));
    assertEquals(Optional.of(3.3f), loader.floatOption("a"));
    assertEquals(Optional.empty(), loader.floatOption("z"));
  }

  @Test
  public void requiredIntValueFound() {
    values.put("a", "33");
    assertEquals(33, loader.intValue("a"));
  }

  @Test
  public void requiredIntValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.intValue("not-there");
        });
  }

  @Test
  public void invalidIntValue() {
    values.put("a", "-not-a-number");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.intValue("a");
        });
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
  public void optionalLongValue() {
    values.put("a", "33");
    assertEquals(Optional.of(33L), loader.longOption("a"));
    assertEquals(Optional.empty(), loader.longOption("z"));
  }

  @Test
  public void requiredEnumValueFound() {
    values.put("a", "First");
    assertEquals(TestEnum.First, loader.enumValue("a", TestEnum::valueOf));
  }

  @Test
  public void requiredEnumValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.enumValue("not-there", TestEnum::valueOf);
        });
  }

  @Test
  public void invalidEnumValue() {
    values.put("a", "-not-a-number");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.enumValue("a", TestEnum::valueOf);
        });
  }

  @Test
  public void readableFileSuccess() {
    // configure the mock to accept any file with the expected path
    doAnswer(invocation -> invocation.getArgument(1))
        .when(loader)
        .validateReadableFile("f", new File("path"));
    values.put("f", "path");
    assertEquals(new File("path"), loader.readableFile("f"));
  }

  @Test
  public void readableFileFailure() throws Exception {
    // configure the mock to reject any file with the expected path
    doThrow(new ConfigException("f", "not readable"))
        .when(loader)
        .validateReadableFile("f", new File("path"));
    values.put("f", "path");
    assertException("f", "not readable", () -> loader.readableFile("f"));
  }

  @Test
  public void validateReadableFileIsReadable() {
    File file = mock(File.class);
    doReturn(true).when(file).isFile();
    doReturn(true).when(file).canRead();
    assertSame(file, loader.validateReadableFile("a", file));
  }

  @Test
  public void validateReadableFileIsNotReadable() throws Exception {
    File file = mock(File.class);
    doReturn(true).when(file).isFile();
    doReturn(false).when(file).canRead();
    assertException("a", "file is not readable", () -> loader.validateReadableFile("a", file));
  }

  @Test
  public void validateReadableFileIsNotAFile() throws Exception {
    File file = mock(File.class);
    doReturn(false).when(file).isFile();
    assertException(
        "a",
        "object referenced by path is not a file",
        () -> loader.validateReadableFile("a", file));
  }

  @Test
  public void validateReadableFileThrows() throws Exception {
    File file = mock(File.class);
    doThrow(new ConfigException("a", "oops", new IOException())).when(file).isFile();
    assertException("a", "oops", () -> loader.validateReadableFile("a", file));
  }

  @Test
  public void writeableFileSuccess() {
    // configure the mock to accept any file with the expected path
    doAnswer(invocation -> invocation.getArgument(1))
        .when(loader)
        .validateWriteableFile("f", new File("path"));
    values.put("f", "path");
    assertEquals(new File("path"), loader.writeableFile("f"));
  }

  @Test
  public void writeableFileFailure() throws Exception {
    // configure the mock to reject any file with the expected path
    doThrow(new ConfigException("f", "not readable"))
        .when(loader)
        .validateWriteableFile("f", new File("path"));
    values.put("f", "path");
    assertException("f", "not readable", () -> loader.writeableFile("f"));
  }

  @Test
  public void validateWriteableFileIsNotAFile() throws Exception {
    File file = mock(File.class);
    doReturn(true).when(file).exists();
    doReturn(false).when(file).isFile();
    assertException(
        "a",
        "object referenced by path is not a file",
        () -> loader.validateWriteableFile("a", file));
  }

  @Test
  public void validateWriteableFileExists() {
    File file = mock(File.class);
    doReturn(true).when(file).exists();
    doReturn(true).when(file).isFile();
    doReturn(true).when(file).canWrite();
    assertSame(file, loader.validateWriteableFile("a", file));
  }

  @Test
  public void validateWriteableFileIsNotWriteable() throws Exception {
    File file = mock(File.class);
    doReturn(true).when(file).exists();
    doReturn(true).when(file).isFile();
    doReturn(false).when(file).canWrite();
    assertException("a", "file is not writeable", () -> loader.validateWriteableFile("a", file));
  }

  @Test
  public void validateWriteableFileIsMissingAndParentIsNotWriteable() throws Exception {
    File file = mock(File.class);
    File parent = mock(File.class);
    doReturn(file).when(file).getAbsoluteFile();
    doReturn(parent).when(file).getParentFile();
    doReturn(false).when(file).exists();
    doReturn(false).when(parent).canWrite();
    assertException(
        "a",
        "file does not exist and parent is not writeable",
        () -> loader.validateWriteableFile("a", file));
  }

  @Test
  public void validateWriteableFileIsMissingButParentIsWriteable() {
    File file = mock(File.class);
    File parent = mock(File.class);
    doReturn(file).when(file).getAbsoluteFile();
    doReturn(parent).when(file).getParentFile();
    doReturn(false).when(file).exists();
    doReturn(true).when(parent).canWrite();
    assertSame(file, loader.validateWriteableFile("a", file));
  }

  @Test
  public void validateWriteableFileThrows() throws Exception {
    File file = mock(File.class);
    doThrow(new ConfigException("a", "oops", new IOException())).when(file).exists();
    assertException("a", "oops", () -> loader.validateWriteableFile("a", file));
  }

  // SimplifiableAssert - Clearer failure message with assertEquals
  @SuppressWarnings("SimplifiableAssertion")
  @Test
  public void optionalBooleanValue() {
    values.put("a", "True");
    assertTrue(loader.booleanValue("a", false));
    assertFalse(loader.booleanValue("z", false));
  }

  @Test
  public void invalidBooleanValue() {
    values.put("a", "-not-a-boolean");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.booleanValue("a", false);
        });
  }

  @Test
  public void fromProperties() {
    Properties p = new Properties();
    p.setProperty("a", "A");
    ConfigLoader config = ConfigLoader.builder().addProperties(p).build();
    assertEquals("A", config.stringValue("a"));
  }

  @Test
  public void fromEnvironmentVariables() {
    final List<String> names = new ArrayList<>(System.getenv().keySet());
    final ConfigLoader config = ConfigLoader.builder().addEnvironmentVariables().build();
    for (String name : names) {
      assertEquals(System.getenv(name), config.stringValue(name, ""), "mismatch for " + name);
    }
  }

  @Test
  public void fromSystemProperties() {
    final Iterable<String> names = System.getProperties().stringPropertyNames();
    final ConfigLoader config = ConfigLoader.builder().addSystemProperties().build();
    for (String name : names) {
      assertEquals(
          System.getProperty(name, ""), config.stringValue(name, ""), "mismatch for " + name);
    }
  }

  @Test
  public void fromCommandLineArguments() {
    final ConfigLoader config =
        ConfigLoader.builder()
            .addKeyValueCommandLineArguments(new String[] {"a:1", "ignored", "bbb:222"})
            .build();
    assertEquals(Optional.of("1"), config.stringOption("a"));
    assertEquals(Optional.of(222), config.intOption("bbb"));
    assertEquals(Optional.empty(), config.intOption("ignored"));
  }

  @Test
  public void testFallback() {
    final Map<String, String> primary = ImmutableMap.of("in-primary", "A");
    final Map<String, String> fallback =
        ImmutableMap.of("in-primary", "hidden", "in-fallback", "B");
    final ConfigLoader config =
        ConfigLoader.builder().addSingle(fallback::get).addSingle(primary::get).build();
    assertEquals("A", config.stringValue("in-primary"));
    assertEquals("B", config.stringValue("in-fallback"));
  }

  private void assertException(String expectedName, String expectedMessage, Callable<?> test)
      throws Exception {
    try {
      test.call();
      fail(
          "expected a ConfigException with name "
              + expectedName
              + " and message "
              + expectedMessage);
    } catch (ConfigException ex) {
      assertEquals(expectedName, ex.getName());
      assertEquals(expectedMessage, ex.getMessage());
    }
  }

  public enum TestEnum {
    First,
    Last
  }
}
