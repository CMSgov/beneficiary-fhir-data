package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.base.Strings;
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

/** Ensures the ConfigLoader can read the various configuration type data. */
public class ConfigLoaderTest {

  /** The configuration values. */
  private Map<String, String> values;

  /** The config loader under test. */
  private ConfigLoader loader;

  /** Sets the tests up by initializing the configurations and loader. */
  @BeforeEach
  public void setUp() {
    values = new HashMap<>();
    loader = spy(ConfigLoader.builder().addSingle(values::get).build());
  }

  /** Validates that a required string value is found if it exists in the config data. */
  @Test
  public void requiredStringValueFound() {
    values.put("a", "A");
    assertEquals("A", loader.stringValue("a"));
  }

  /**
   * Validates that an exception is thrown if a required string value doesnt exist in the config
   * data.
   */
  @Test
  public void requiredStringValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.stringValue("not-there");
        });
  }

  /**
   * Validates that if a string value is optional, it can correctly fall back to a default, returns
   * if exists, and returns an empty optional if it doesnt exist in the config data.
   */
  @Test
  public void optionalStringValue() {
    values.put("a", "A");
    assertEquals("A", loader.stringValue("a", "---"));
    assertEquals("---", loader.stringValue("z", "---"));
    assertEquals(Optional.of("A"), loader.stringOption("a"));
    assertEquals(Optional.empty(), loader.stringOption("z"));
  }

  /** Validates that a required float value is found if it exists in the config data. */
  @Test
  public void requiredFloatValueFound() {
    values.put("a", "3.3");
    assertEquals(3.3f, loader.floatValue("a"));
  }

  /**
   * Validates that an exception is thrown if a required float value doesnt exist in the config
   * data.
   */
  @Test
  public void requiredFloatValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.floatValue("not-there");
        });
  }

  /**
   * Validates that if a float value in the configuration is not parsable, a {@link ConfigException}
   * is thrown.
   */
  @Test
  public void invalidFloatValue() {
    values.put("a", "-not-a-number");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.floatValue("a");
        });
  }

  /**
   * Validates that if a float value is optional, it can correctly fall back to a default, returns
   * if exists, and returns an empty optional if it doesnt exist in the config data.
   */
  @Test
  public void optionalFloatValue() {
    values.put("a", "3.3");
    assertEquals(3.3f, loader.floatValue("a", -10.0f));
    assertEquals(-10.0f, loader.floatValue("z", -10.0f));
    assertEquals(Optional.of(3.3f), loader.floatOption("a"));
    assertEquals(Optional.empty(), loader.floatOption("z"));
  }

  /** Validates that a required int value is found if it exists in the config data. */
  @Test
  public void requiredIntValueFound() {
    values.put("a", "33");
    assertEquals(33, loader.intValue("a"));
  }

  /**
   * Validates that an exception is thrown if a required int value doesnt exist in the config data.
   */
  @Test
  public void requiredIntValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.intValue("not-there");
        });
  }

  /**
   * Validates that if an int value in the configuration is not parsable, a {@link ConfigException}
   * is thrown.
   */
  @Test
  public void invalidIntValue() {
    values.put("a", "-not-a-number");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.intValue("a");
        });
  }

  /**
   * Validates that if an int value is optional, it can correctly fall back to a default, returns if
   * exists, and returns an empty optional if it doesnt exist in the config data.
   */
  @Test
  public void optionalIntValue() {
    values.put("a", "33");
    assertEquals(33, loader.intValue("a", -10));
    assertEquals(-10, loader.intValue("z", -10));
    assertEquals(Optional.of(33), loader.intOption("a"));
    assertEquals(Optional.empty(), loader.intOption("z"));
  }

  /**
   * Validates that if a long value is optional, it can correctly fall back to a default, returns if
   * exists, and returns an empty optional if it doesnt exist in the config data.
   */
  @Test
  public void optionalLongValue() {
    values.put("a", "33");
    assertEquals(Optional.of(33L), loader.longOption("a"));
    assertEquals(Optional.empty(), loader.longOption("z"));
  }

  /** Validates that a required enum value is found if it exists in the config data. */
  @Test
  public void requiredEnumValueFound() {
    values.put("a", "First");
    assertEquals(TestEnum.First, loader.enumValue("a", TestEnum::valueOf));
  }

  /**
   * Validates that an exception is thrown if a required enum value doesnt exist in the config data.
   */
  @Test
  public void requiredEnumValueNotFound() {
    assertThrows(
        ConfigException.class,
        () -> {
          loader.enumValue("not-there", TestEnum::valueOf);
        });
  }

  /**
   * Validates that if an enum value in the configuration is not parsable, a {@link ConfigException}
   * is thrown.
   */
  @Test
  public void invalidEnumValue() {
    values.put("a", "-not-a-number");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.enumValue("a", TestEnum::valueOf);
        });
  }

  /**
   * Validates that if a boolean value is optional, it can correctly fall back to a default, and
   * returns if exists.
   */
  // SimplifiableAssert - Clearer failure message with assertEquals
  @SuppressWarnings("SimplifiableAssertion")
  @Test
  public void optionalBooleanValue() {
    values.put("a", "True");
    assertTrue(loader.booleanValue("a", false));
    assertFalse(loader.booleanValue("z", false));
  }

  /**
   * Validates that if a boolean value in the configuration is not parsable, a {@link
   * ConfigException} is thrown.
   */
  @Test
  public void invalidBooleanValue() {
    values.put("a", "-not-a-boolean");
    assertThrows(
        ConfigException.class,
        () -> {
          loader.booleanValue("a", false);
        });
  }

  /**
   * Validates that a required File value is found if it exists in the config data and can be found.
   */
  @Test
  public void readableFileSuccess() {
    // configure the mock to accept any file with the expected path
    doAnswer(invocation -> invocation.getArgument(1))
        .when(loader)
        .validateReadableFile("f", new File("path"));
    values.put("f", "path");
    assertEquals(new File("path"), loader.readableFile("f"));
  }

  /** Validates an exception is thrown if a required File in the configuration is not found. */
  @Test
  public void readableFileFailure() throws Exception {
    // configure the mock to reject any file with the expected path
    doThrow(new ConfigException("f", "not readable"))
        .when(loader)
        .validateReadableFile("f", new File("path"));
    values.put("f", "path");
    assertException("f", "not readable", () -> loader.readableFile("f"));
  }

  /**
   * Validates a file is returned if a file passed to {@link ConfigLoader#validateReadableFile} is
   * readable.
   */
  @Test
  public void validateReadableFileIsReadable() {
    File file = mock(File.class);
    doReturn(true).when(file).isFile();
    doReturn(true).when(file).canRead();
    assertSame(file, loader.validateReadableFile("a", file));
  }

  /**
   * Validates an exception is thrown if a file passed to {@link ConfigLoader#validateReadableFile}
   * is not readable.
   */
  @Test
  public void validateReadableFileIsNotReadable() throws Exception {
    File file = mock(File.class);
    doReturn(true).when(file).isFile();
    doReturn(false).when(file).canRead();
    assertException("a", "file is not readable", () -> loader.validateReadableFile("a", file));
  }

  /**
   * Validates an exception is thrown if a file passed to {@link ConfigLoader#validateReadableFile}
   * is not a file.
   *
   * @throws Exception an unexpected exception thrown from the test
   */
  @Test
  public void validateReadableFileIsNotAFile() throws Exception {
    File file = mock(File.class);
    doReturn(false).when(file).isFile();
    assertException(
        "a",
        "object referenced by path is not a file",
        () -> loader.validateReadableFile("a", file));
  }

  /**
   * Validates an exception is thrown if {@link ConfigLoader#validateReadableFile} throws an {@link
   * IOException}.
   *
   * @throws Exception an unexpected exception thrown from the test
   */
  @Test
  public void validateReadableFileThrows() throws Exception {
    File file = mock(File.class);
    doThrow(new ConfigException("a", "oops", new IOException())).when(file).isFile();
    assertException("a", "oops", () -> loader.validateReadableFile("a", file));
  }

  /**
   * Validates that if a file passed to {@link ConfigLoader#writeableFile} exists and is writable, a
   * file is returned.
   */
  @Test
  public void writeableFileSuccess() {
    // configure the mock to accept any file with the expected path
    doAnswer(invocation -> invocation.getArgument(1))
        .when(loader)
        .validateWriteableFile("f", new File("path"));
    values.put("f", "path");
    assertEquals(new File("path"), loader.writeableFile("f"));
  }

  /**
   * Validates that if a file passed to {@link ConfigLoader#writeableFile} exists and is not
   * readable, an exception is thrown.
   */
  @Test
  public void writeableFileFailure() throws Exception {
    // configure the mock to reject any file with the expected path
    doThrow(new ConfigException("f", "not readable"))
        .when(loader)
        .validateWriteableFile("f", new File("path"));
    values.put("f", "path");
    assertException("f", "not readable", () -> loader.writeableFile("f"));
  }

  /**
   * Validates that if a file passed to {@link ConfigLoader#writeableFile} is not a file, an
   * exception is thrown.
   */
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

  /**
   * Validates that if a file passed to {@link ConfigLoader#validateWriteableFile} exists and is
   * writable, a file is returned.
   */
  @Test
  public void validateWriteableFileExists() {
    File file = mock(File.class);
    doReturn(true).when(file).exists();
    doReturn(true).when(file).isFile();
    doReturn(true).when(file).canWrite();
    assertSame(file, loader.validateWriteableFile("a", file));
  }

  /**
   * Validates that if a file passed to {@link ConfigLoader#validateWriteableFile} exists and is not
   * writable, an exception is thrown.
   *
   * @throws Exception an unexpected exception
   */
  @Test
  public void validateWriteableFileIsNotWriteable() throws Exception {
    File file = mock(File.class);
    doReturn(true).when(file).exists();
    doReturn(true).when(file).isFile();
    doReturn(false).when(file).canWrite();
    assertException("a", "file is not writeable", () -> loader.validateWriteableFile("a", file));
  }

  /**
   * Validates that if a file passed to {@link ConfigLoader#validateWriteableFile} does not exist
   * and the parent folder cannot be written to, an exception is thrown.
   *
   * @throws Exception an unexpected exception
   */
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

  /**
   * Validates that if a file passed to {@link ConfigLoader#validateWriteableFile} does not exist
   * and the parent folder can be written to, the file is returned.
   */
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

  /**
   * Validates when {@link ConfigLoader#validateWriteableFile} file throws an {@link IOException} it
   * is caught and a {@link ConfigException} is thrown.
   *
   * @throws Exception the exception
   */
  @Test
  public void validateWriteableFileThrows() throws Exception {
    File file = mock(File.class);
    doThrow(new ConfigException("a", "oops", new IOException())).when(file).exists();
    assertException("a", "oops", () -> loader.validateWriteableFile("a", file));
  }

  /** Validates a config value can be read from {@link Properties}. */
  @Test
  public void fromProperties() {
    Properties p = new Properties();
    p.setProperty("a", "A");
    ConfigLoader config = ConfigLoader.builder().addProperties(p).build();
    assertEquals("A", config.stringValue("a"));
  }

  /** Validates a configuration can be read from environment variables. */
  @Test
  public void fromEnvironmentVariables() {
    final List<String> names = new ArrayList<>(System.getenv().keySet());
    final ConfigLoader config = ConfigLoader.builder().addEnvironmentVariables().build();
    for (String name : names) {
      assertEquals(
          ConfigLoader.trim(Strings.nullToEmpty(System.getenv(name))),
          config.stringValue(name, ""),
          "mismatch for " + name);
    }
  }

  /** Validates a configuration can be read from system properties. */
  @Test
  public void fromSystemProperties() {
    final Iterable<String> names = System.getProperties().stringPropertyNames();
    final ConfigLoader config = ConfigLoader.builder().addSystemProperties().build();
    for (String name : names) {
      assertEquals(
          ConfigLoader.trim(System.getProperty(name, "")),
          config.stringValue(name, ""),
          "mismatch for " + name);
    }
  }

  /** Validates a configuration can be read from command link arguments. */
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

  /** Validates that hierarchical fallback configurations are supported. */
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

  /** Validates that leading and trailing whitespace are removed as expected. */
  @Test
  public void testTrimming() {
    assertEquals("", ConfigLoader.trim(""));
    assertEquals("x", ConfigLoader.trim("x"));
    assertEquals("x y", ConfigLoader.trim("x y"));
    assertEquals("x y", ConfigLoader.trim(" x y"));
    assertEquals("x y", ConfigLoader.trim("x y "));
    assertEquals("x y", ConfigLoader.trim("\tx y"));
    assertEquals("x y", ConfigLoader.trim("x y\t"));
    assertEquals("x y", ConfigLoader.trim(" x y\t"));
    assertEquals("x y", ConfigLoader.trim("\tx y "));
    assertEquals("x y", ConfigLoader.trim(" \tx y\t "));
  }

  /**
   * Asserts that an exception is thrown with the expected name and message.
   *
   * @param expectedName the expected name
   * @param expectedMessage the expected message
   * @param test the test to call which expects an exception
   * @throws Exception any unexpected exception (i.e. not a {@link ConfigException})
   */
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

  /** An enum used for testing configurations. */
  public enum TestEnum {
    /** The first test enum. */
    First,
    /** The last test enum. */
    Last
  }
}
