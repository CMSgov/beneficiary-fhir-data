package gov.cms.bfd.sharedutils.config;

import static gov.cms.bfd.sharedutils.config.ConfigLoader.NOT_POSITIVE_INTEGER;
import static gov.cms.bfd.sharedutils.config.ConfigLoader.NOT_PROVIDED;
import static gov.cms.bfd.sharedutils.config.ConfigLoader.NOT_VALID_HEX;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
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
import javax.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/** Ensures the ConfigLoader can read the various configuration type data. */
public class ConfigLoaderTest {
  /** Expected error message when parsing invalid float value. */
  private static final String NOT_VALID_FLOAT =
      "not a valid Float value: exception=NumberFormatException message=For input string: \"***\"";

  /** Expected error message when parsing invalid integer value. */
  private static final String NOT_VALID_INTEGER =
      "not a valid Integer value: exception=NumberFormatException message=For input string: \"***\"";

  /** Expected error message when parsing invalid long value. */
  private static final String NOT_VALID_LONG =
      "not a valid Long value: exception=NumberFormatException message=For input string: \"***\"";

  /** Expected error message when parsing invalid boolean value. */
  private static final String NOT_VALID_BOOLEAN =
      "not a valid Boolean value: exception=IllegalArgumentException message=invalid boolean value";

  /** Expected error message when parsing invalid enum value. */
  private static final String NOT_VALID_ENUM =
      "not a valid TestEnum value: exception=IllegalArgumentException message=No enum constant gov.cms.bfd.sharedutils.config.ConfigLoaderTest.TestEnum.***";

  /** The configuration values. */
  private Map<String, String> values;

  /** The config loader under test. */
  private ConfigLoader loader;

  /** Sets the tests up by initializing the configurations and loader. */
  @BeforeEach
  public void setUp() {
    values = new HashMap<>();
    loader = spy(ConfigLoader.builder().addMap(values).build());
  }

  /** Validates that getting lists of strings work properly for multi-value sources. */
  @Test
  public void testMultipleValues() {
    var multiValues = new HashMap<String, List<String>>();
    multiValues.put("a", List.of());
    multiValues.put("b", List.of("", ""));
    multiValues.put("d", List.of("D"));
    loader = ConfigLoader.builder().addMultiMap(multiValues).build();
    assertThrows(ConfigException.class, () -> loader.stringValues("a"));
    assertThrows(ConfigException.class, () -> loader.stringValues("z"));
    assertEquals(List.of("x"), loader.stringValues("a", List.of("x")));
    assertEquals(List.of("x"), loader.stringValues("z", List.of("x")));
    assertEquals(List.of("", ""), loader.stringValues("b"));
    assertEquals(List.of("D"), loader.stringValues("d"));
    assertEquals(List.of("", ""), loader.stringValues("b", List.of("x")));
    assertEquals(List.of("D"), loader.stringValues("d", List.of("x")));
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
    assertThrows(ConfigException.class, () -> loader.stringValue("not-there"));
  }

  /**
   * Validates that if a string value is optional, it can correctly fall back to a default, returns
   * if exists, and returns an empty optional if it doesnt exist in the config data.
   */
  @Test
  public void optionalStringValue() {
    values.put("a", "A");
    values.put("b", "");
    assertEquals("A", loader.stringValue("a", "---"));
    assertEquals("---", loader.stringValue("z", "---"));
    assertEquals(Optional.of("A"), loader.stringOption("a"));
    assertEquals(Optional.empty(), loader.stringOption("b"));
    assertEquals(Optional.empty(), loader.stringOption("z"));
    assertEquals(Optional.of("A"), loader.stringOptionEmptyOK("a"));
    assertEquals(Optional.of(""), loader.stringOptionEmptyOK("b"));
    assertEquals(Optional.empty(), loader.stringOptionEmptyOK("z"));
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
    assertException("not-there", null, NOT_PROVIDED, () -> loader.floatValue("not-there"));
  }

  /**
   * Validates that if a float value in the configuration is not parsable, a {@link ConfigException}
   * is thrown.
   */
  @Test
  public void invalidFloatValue() {
    values.put("a", "-not-a-number");
    assertException("a", values, NOT_VALID_FLOAT, () -> loader.floatValue("a"));
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
    assertThrows(ConfigException.class, () -> loader.intValue("not-there"));
  }

  /**
   * Validates that if an int value in the configuration is not parsable, a {@link ConfigException}
   * is thrown.
   */
  @Test
  public void invalidIntValue() {
    values.put("a", "-not-a-number");
    assertThrows(ConfigException.class, () -> loader.intValue("a"));
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

  /** Validates all cases for positive int values. */
  @Test
  public void testPositiveInts() {
    values.put("a", "10");
    values.put("b", "0");
    values.put("c", "-1");
    values.put("d", "not-a-number");

    assertEquals(Optional.of(10), loader.positiveIntOption("a"));
    assertException("b", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntOption("b"));
    assertException("c", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntOption("c"));
    assertException("d", values, NOT_VALID_INTEGER, () -> loader.positiveIntOption("d"));
    assertEquals(Optional.empty(), loader.positiveIntOption("z"));

    assertEquals(10, loader.positiveIntValue("a"));
    assertException("b", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntValue("b"));
    assertException("c", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntValue("c"));
    assertException("d", values, NOT_VALID_INTEGER, () -> loader.positiveIntValue("d"));
    assertException("z", null, NOT_PROVIDED, () -> loader.positiveIntValue("z"));

    assertEquals(10, loader.positiveIntValue("a"));
    assertException("b", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntValue("b", 42));
    assertException("c", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntValue("c", 42));
    assertException("d", values, NOT_VALID_INTEGER, () -> loader.positiveIntValue("d", 42));
    assertEquals(42, loader.positiveIntValue("z", 42));
  }

  /** Validates all cases for positive int values when zero is allowed. */
  @Test
  public void testPositiveIntsZeroOK() {
    values.put("a", "10");
    values.put("b", "0");
    values.put("c", "-1");
    values.put("d", "not-a-number");

    assertEquals(Optional.of(10), loader.positiveIntOption("a"));
    assertEquals(Optional.of(0), loader.positiveIntOptionZeroOK("b"));
    assertException("c", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntOptionZeroOK("c"));
    assertException("d", values, NOT_VALID_INTEGER, () -> loader.positiveIntOptionZeroOK("d"));
    assertEquals(Optional.empty(), loader.positiveIntOption("z"));

    assertEquals(10, loader.positiveIntValue("a"));
    assertEquals(0, loader.positiveIntValueZeroOK("b"));
    assertException("c", values, NOT_POSITIVE_INTEGER, () -> loader.positiveIntValueZeroOK("c"));
    assertException("d", values, NOT_VALID_INTEGER, () -> loader.positiveIntValueZeroOK("d"));
    assertException("z", null, NOT_PROVIDED, () -> loader.positiveIntValueZeroOK("z"));
  }

  /** Validates all cases for long values. */
  @Test
  public void testLongs() {
    values.put("a", "33");
    values.put("b", "-20");
    values.put("c", "not-a-number");

    assertEquals(Optional.of(33L), loader.longOption("a"));
    assertEquals(Optional.of(-20L), loader.longOption("b"));
    assertException("c", values, NOT_VALID_LONG, () -> loader.longOption("c"));
    assertEquals(Optional.empty(), loader.longOption("z"));
  }

  /** Validates all cases for enum values. */
  @Test
  public void testEnums() {
    values.put("a", "First");
    values.put("b", "Eighth");

    assertEquals(Optional.of(TestEnum.First), loader.enumOption("a", TestEnum.class));
    assertException("b", values, NOT_VALID_ENUM, () -> loader.enumOption("b", TestEnum.class));
    assertEquals(Optional.empty(), loader.<TestEnum>enumOption("z", TestEnum.class));

    assertEquals(TestEnum.First, loader.enumValue("a", TestEnum.class));
    assertException("b", values, NOT_VALID_ENUM, () -> loader.enumValue("b", TestEnum.class));
    assertException(
        "z", values, NOT_PROVIDED, () -> loader.<TestEnum>enumValue("z", TestEnum.class));
  }

  /** Validates all cases for boolean values. */
  @Test
  public void testBooleans() {
    values.put("a", "True");
    values.put("b", "fAlSe");
    values.put("c", "yes");

    assertTrue(loader.booleanValue("a"));
    assertTrue(loader.booleanValue("a", false));
    assertFalse(loader.booleanValue("b"));
    assertFalse(loader.booleanValue("b", true));
    assertException("c", values, NOT_VALID_BOOLEAN, () -> loader.booleanValue("c"));
    assertException("c", values, NOT_VALID_BOOLEAN, () -> loader.booleanValue("c", true));
    assertException("z", values, NOT_PROVIDED, () -> loader.booleanValue("z"));
    assertFalse(loader.booleanValue("z", false));

    assertEquals(Optional.of(TRUE), loader.booleanOption("a"));
    assertEquals(Optional.of(FALSE), loader.booleanOption("b"));
    assertException("c", values, NOT_VALID_BOOLEAN, () -> loader.booleanOption("c"));
    assertEquals(Optional.empty(), loader.booleanOption("z"));
  }

  /** Validates all cases for hex values. */
  @Test
  public void testHex() {
    values.put("a", "0123456789abcdef");
    values.put("b", "not-hex");
    assertArrayEquals(new byte[] {1, 35, 69, 103, -119, -85, -51, -17}, loader.hexBytes("a"));
    assertException("b", values, NOT_VALID_HEX, () -> loader.hexBytes("b"));
    assertException("z", values, NOT_PROVIDED, () -> loader.hexBytes("z"));
  }

  /** Validates all cases for parsed values. */
  @Test
  public void testParsed() {
    values.put("a", "10");
    values.put("b", "not-a-number");

    assertEquals(10, loader.parsedValue("a", Integer.class, Integer::valueOf));
    assertException(
        "b",
        values,
        NOT_VALID_INTEGER,
        () -> loader.parsedValue("b", Integer.class, Integer::valueOf));
    assertException(
        "z", values, NOT_PROVIDED, () -> loader.parsedValue("z", Integer.class, Integer::valueOf));

    assertEquals(Optional.of(10), loader.parsedOption("a", Integer.class, Integer::valueOf));
    assertException(
        "b",
        values,
        NOT_VALID_INTEGER,
        () -> loader.parsedOption("b", Integer.class, Integer::valueOf));
    assertEquals(Optional.empty(), loader.parsedOption("z", Integer.class, Integer::valueOf));
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
    assertException("f", null, "not readable", () -> loader.readableFile("f"));
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
  public void validateReadableFileIsNotReadable() {
    File file = mock(File.class);
    doReturn(true).when(file).isFile();
    doReturn(false).when(file).canRead();
    assertException(
        "a", null, "file is not readable", () -> loader.validateReadableFile("a", file));
  }

  /**
   * Validates an exception is thrown if a file passed to {@link ConfigLoader#validateReadableFile}
   * is not a file.
   */
  @Test
  public void validateReadableFileIsNotAFile() {
    File file = mock(File.class);
    doReturn(false).when(file).isFile();
    assertException(
        "a",
        null,
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
    assertException("a", null, "oops", () -> loader.validateReadableFile("a", file));
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
    assertException("f", null, "not readable", () -> loader.writeableFile("f"));
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
        null,
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
    assertException(
        "a", null, "file is not writeable", () -> loader.validateWriteableFile("a", file));
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
        null,
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
    assertException("a", null, "oops", () -> loader.validateWriteableFile("a", file));
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
    final ConfigLoader config = ConfigLoader.builder().addMap(fallback).addMap(primary).build();
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
   * Asserts that an exception is thrown with the expected name and message. Optionally also ensures
   * that no config value leaks into thrown {@link ConfigException} messages.
   *
   * @param expectedName the expected name
   * @param valuesMap optional map containing config values
   * @param expectedDetail the expected error detail
   * @param action the lambda to call which expects an exception
   */
  private void assertException(
      String expectedName,
      @Nullable Map<String, String> valuesMap,
      String expectedDetail,
      Executable action) {
    final var ex = assertThrows(ConfigException.class, action);
    assertEquals(expectedName, ex.getName());
    assertEquals(expectedDetail, ex.getDetail());
    assertEquals(ConfigException.createMessage(expectedName, expectedDetail), ex.getMessage());
    if (valuesMap != null && valuesMap.containsKey(expectedName)) {
      var doNotLogValue = valuesMap.get(expectedName);
      Throwable t = ex;
      while (t != null) {
        if (t.getMessage().contains(doNotLogValue)) {
          fail("message contains config value: " + t.getMessage());
        }
        t = t.getCause();
      }
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
