package gov.cms.bfd.sharedutils.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/** Implementations serve as sources of configuration values for use by a {@link ConfigLoader}. */
public abstract class ConfigLoaderSource {
  /**
   * Returns a {@link Set} containing all of the names that are known to have a value.
   *
   * @return set of names
   */
  @Nonnull
  public abstract Set<String> validNames();

  /**
   * Looks for a value mapped to the specified name. Returns null if there is no mapping or a {@link
   * Collection} containing all values if a mapping exists.
   *
   * @param name name to look up
   * @return null or all of the values mapped to the name
   */
  @Nullable
  public abstract Collection<String> lookup(String name);

  /**
   * Create an instance that uses the given {@link Map} as its source of values.
   *
   * @param map contains the values
   * @return the source that was created
   */
  public static ConfigLoaderSource fromMap(Map<String, String> map) {

    return new MapSource(map);
  }

  /**
   * Create an instance that uses the given {@link Map} as its source of values.
   *
   * @param map contains the values
   * @return the source that was created
   */
  public static ConfigLoaderSource fromMultiMap(Map<String, ? extends Collection<String>> map) {
    return new MultiMapSource(map);
  }

  /**
   * Create an instance that uses {@link System#getenv} as its source of values.
   *
   * @return the source that was created
   */
  public static ConfigLoaderSource fromEnv() {
    return new MapSource(System.getenv());
  }

  /**
   * Create an instance that uses the given {@link Properties} as its source of values.
   *
   * @param properties contains the values
   * @return the source that was created
   */
  public static ConfigLoaderSource fromProperties(Properties properties) {
    return new PropertiesSource(properties);
  }

  /**
   * Create an instance that uses all of the given {@link ConfigLoaderSource}s as its source of
   * values. When looking for a mapping for any given name the value returned will always be that
   * from the last source in the list that contains a value. That means the list should contain the
   * sources in order of increasing priority.
   *
   * @param sources all sources in order of increasing priority
   * @return the source that was created
   */
  public static ConfigLoaderSource fromPrioritizedSources(List<ConfigLoaderSource> sources) {
    return new CombinedSource(sources);
  }

  /** Implementation used by {@link ConfigLoaderSource#fromProperties}. */
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode(callSuper = false)
  private static class PropertiesSource extends ConfigLoaderSource {
    /** All of our values. */
    private final Properties properties;

    @Nonnull
    @Override
    public Set<String> validNames() {
      return properties.stringPropertyNames();
    }

    @Nullable
    @Override
    public Collection<String> lookup(String name) {
      var value = properties.getProperty(name);
      return value == null ? null : List.of(value);
    }

    @Override
    public String toString() {
      return String.format("%s with %d properties", getClass().getSimpleName(), properties.size());
    }
  }

  /** Implementation used by {@link ConfigLoaderSource#fromMultiMap}. */
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode(callSuper = false)
  private static class MultiMapSource extends ConfigLoaderSource {
    /** All of our values. */
    private final Map<String, ? extends Collection<String>> map;

    /**
     * Names that are in the map but have empty values are ignored.
     *
     * <p>{@inheritDoc}
     */
    @Nonnull
    @Override
    public Set<String> validNames() {
      return map.entrySet().stream()
          .filter(e -> !e.getValue().isEmpty())
          .map(Map.Entry::getKey)
          .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    @Override
    public Collection<String> lookup(String name) {
      Collection<String> value = map.get(name);
      return (value == null || value.isEmpty()) ? null : value;
    }

    @Override
    public String toString() {
      return String.format(
          "%s with %d keys: %s", getClass().getSimpleName(), map.size(), map.keySet());
    }
  }

  /** Implementation used by {@link ConfigLoaderSource#fromMap}. */
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  @EqualsAndHashCode(callSuper = false)
  private static class MapSource extends ConfigLoaderSource {
    /** All of our values. */
    private final Map<String, String> map;

    @Nonnull
    @Override
    public Set<String> validNames() {
      return map.keySet();
    }

    @Nullable
    @Override
    public Collection<String> lookup(String name) {
      final String value = map.get(name);
      return value == null ? null : List.of(value);
    }

    @Override
    public String toString() {
      return String.format(
          "%s with %d keys: %s", getClass().getSimpleName(), map.size(), map.keySet());
    }
  }

  /** Implementation used by {@link ConfigLoaderSource#fromPrioritizedSources}. */
  @EqualsAndHashCode(callSuper = false)
  private static class CombinedSource extends ConfigLoaderSource {
    /** All sources in order of decreasing priority. */
    private final List<ConfigLoaderSource> sources;

    /**
     * Initializes an instance. Ensures our list of sources is in order of decreasing priority so
     * that a simple loop can be used to find the highest priority value.
     *
     * @param sources all sources to use when looking up values
     */
    private CombinedSource(List<ConfigLoaderSource> sources) {
      // Caller passes a list in increasing priority order but we need it in decreasing order so we
      // create a copy with the order reversed.
      var temp = new ArrayList<>(sources);
      Collections.reverse(temp);
      this.sources = List.copyOf(temp);
    }

    @Nonnull
    @Override
    public Set<String> validNames() {
      return sources.stream()
          .flatMap(source -> source.validNames().stream())
          .collect(Collectors.toUnmodifiableSet());
    }

    @Nullable
    @Override
    public Collection<String> lookup(String name) {
      for (ConfigLoaderSource source : sources) {
        Collection<String> values = source.lookup(name);
        if (values != null && !values.isEmpty()) {
          return values;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      return String.format(
          "%s with %d sources: %s", getClass().getSimpleName(), sources.size(), sources);
    }
  }
}
