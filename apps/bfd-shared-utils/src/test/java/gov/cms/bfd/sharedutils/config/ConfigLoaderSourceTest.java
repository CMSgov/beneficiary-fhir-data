package gov.cms.bfd.sharedutils.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ConfigLoaderSource}. */
public class ConfigLoaderSourceTest {
  /** Verifies that sources created from {@link Map}s of strings work correctly. */
  @Test
  void testFromMap() {
    Map<String, String> values = Map.of("a", "A", "b", "B");
    ConfigLoaderSource source = ConfigLoaderSource.fromMap(values);

    assertEquals(Set.of("a", "b"), source.validNames());
    for (String key : values.keySet()) {
      assertEquals(List.of(values.get(key)), source.lookup(key));
    }
    assertNull(source.lookup("z"));

    // verify that the lombok generated equals method works
    assertEquals(source, ConfigLoaderSource.fromMap(values));
  }

  /** Verifies that sources created from {@link Map}s of collections of strings work correctly. */
  @Test
  void testFromMultiMap() {
    // a key with an empty collection should be ignored
    Map<String, Collection<String>> values =
        Map.of("a", Set.of("A"), "b", List.of("B", "BB"), "c", Set.of());
    ConfigLoaderSource source = ConfigLoaderSource.fromMultiMap(values);

    assertEquals(Set.of("a", "b"), source.validNames());
    assertEquals(Set.of("A"), source.lookup("a"));
    assertEquals(List.of("B", "BB"), source.lookup("b"));
    assertNull(source.lookup("c"));
    assertNull(source.lookup("z"));

    // verify that the lombok generated equals method works
    assertEquals(source, ConfigLoaderSource.fromMultiMap(values));
  }

  /** Verifies that sources created from system environment variables work correctly. */
  @Test
  void testFromEnv() {
    ConfigLoaderSource source = ConfigLoaderSource.fromEnv();

    assertEquals(System.getenv().keySet(), source.validNames());
    for (Map.Entry<String, String> entry : System.getenv().entrySet()) {
      assertEquals(List.of(entry.getValue()), source.lookup(entry.getKey()));
    }

    // verify that the lombok generated equals method works
    assertEquals(source, ConfigLoaderSource.fromEnv());
  }

  /** Verifies that sources created from {@link Properties}s work correctly. */
  @Test
  void testFromProperties() {
    Properties values = new Properties();
    values.setProperty("a", "A");
    values.setProperty("b", "B");
    ConfigLoaderSource source = ConfigLoaderSource.fromProperties(values);

    assertEquals(Set.of("a", "b"), source.validNames());
    for (String key : values.stringPropertyNames()) {
      assertEquals(List.of(values.getProperty(key)), source.lookup(key));
    }
    assertNull(source.lookup("z"));

    // verify that the lombok generated equals method works
    assertEquals(source, ConfigLoaderSource.fromProperties(values));
  }

  /** Verifies that sources created from prioritized lists of sources work correctly. */
  @Test
  void testPrioritizedSources() {
    ConfigLoaderSource lowPriority =
        ConfigLoaderSource.fromMap(Map.of("a", "1", "b", "2", "c", "3"));
    ConfigLoaderSource middlePriority = ConfigLoaderSource.fromMap(Map.of("a", "11", "b", "22"));
    ConfigLoaderSource highPriority = ConfigLoaderSource.fromMap(Map.of("b", "222"));
    ConfigLoaderSource source =
        ConfigLoaderSource.fromPrioritizedSources(
            List.of(lowPriority, middlePriority, highPriority));

    assertEquals(Set.of("a", "b", "c"), source.validNames());
    assertEquals(middlePriority.lookup("a"), source.lookup("a"));
    assertEquals(highPriority.lookup("b"), source.lookup("b"));
    assertEquals(lowPriority.lookup("c"), source.lookup("c"));

    // verify that the lombok generated equals method works
    assertEquals(
        source,
        ConfigLoaderSource.fromPrioritizedSources(
            List.of(lowPriority, middlePriority, highPriority)));
  }

  /** Verifies that name prefix is applied correctly. */
  @Test
  void testFromOtherUsingNamePrefix() {
    Properties values = new Properties();
    values.setProperty("a", "A");
    values.setProperty("b", "B");
    values.setProperty("x.c", "XC");
    values.setProperty("x.d", "XD");
    ConfigLoaderSource otherSource = ConfigLoaderSource.fromProperties(values);
    ConfigLoaderSource prefixedSource =
        ConfigLoaderSource.fromOtherUsingSsmToPropertyMapping("x.", otherSource);
    assertEquals(Set.of("c", "d"), prefixedSource.validNames());
    assertEquals(null, prefixedSource.lookup("a"));
    assertEquals(List.of("XC"), prefixedSource.lookup("c"));
    assertEquals(List.of("XD"), prefixedSource.lookup("d"));
  }

  /** Verifies that name prefix and character mapping are applied correctly. */
  @Test
  void testFromOtherUsingSsmToEnvVarMapping() {
    Properties values = new Properties();
    values.setProperty("a", "A");
    values.setProperty("BFD_X_ONE", "XC");
    ConfigLoaderSource otherSource = ConfigLoaderSource.fromProperties(values);
    ConfigLoaderSource prefixedSource =
        ConfigLoaderSource.fromOtherUsingSsmToEnvVarMapping("bfd_", otherSource);
    assertEquals(Set.of("X_ONE"), prefixedSource.validNames());
    assertEquals(null, prefixedSource.lookup("a"));
    assertEquals(List.of("XC"), prefixedSource.lookup("x/one"));
  }
}
