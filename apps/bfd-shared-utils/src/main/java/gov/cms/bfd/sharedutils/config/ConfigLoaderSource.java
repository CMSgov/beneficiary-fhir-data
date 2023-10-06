package gov.cms.bfd.sharedutils.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;

public interface ConfigLoaderSource {
  Set<String> keySet();

  Collection<String> lookup(String key);

  static ConfigLoaderSource fromMap(Map<String, String> map) {

    return new MapSource(map);
  }

  static ConfigLoaderSource fromMultiMap(Map<String, ? extends Collection<String>> map) {
    return new MultiMapSource(map);
  }

  static ConfigLoaderSource fromEnv() {
    return new MapSource(System.getenv());
  }

  static ConfigLoaderSource fromProperties(Properties properties) {
    return new PropertiesSource(properties);
  }

  static ConfigLoaderSource fromPrioritizedSources(List<ConfigLoaderSource> sources) {
    return new CombinedSource(sources);
  }

  @Data
  class PropertiesSource implements ConfigLoaderSource {
    private final Properties properties;

    @Override
    public Set<String> keySet() {
      return properties.stringPropertyNames();
    }

    @Override
    public Collection<String> lookup(String key) {
      var value = properties.getProperty(key);
      return value == null ? null : List.of(value);
    }
  }

  @Data
  class MultiMapSource implements ConfigLoaderSource {
    private final Map<String, ? extends Collection<String>> map;

    @Override
    public Set<String> keySet() {
      return map.keySet();
    }

    @Override
    public Collection<String> lookup(String key) {
      return map.get(key);
    }
  }

  @Data
  class MapSource implements ConfigLoaderSource {
    private final Map<String, String> map;

    @Override
    public Set<String> keySet() {
      return map.keySet();
    }

    @Override
    public Collection<String> lookup(String key) {
      final String value = map.get(key);
      return value == null ? null : List.of(value);
    }
  }

  @Data
  class CombinedSource implements ConfigLoaderSource {
    private final List<ConfigLoaderSource> sources;

    public CombinedSource(List<ConfigLoaderSource> sources) {
      // Caller passes a list in increasing priority order but we need it in decreasing order so we
      // reverse it.
      var temp = new ArrayList<>(sources);
      Collections.reverse(temp);
      this.sources = List.copyOf(temp);
    }

    @Override
    public Set<String> keySet() {
      return sources.stream()
          .flatMap(source -> source.keySet().stream())
          .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Collection<String> lookup(String key) {
      for (ConfigLoaderSource source : sources) {
        Collection<String> values = source.lookup(key);
        if (values != null) {
          return values;
        }
      }
      return null;
    }
  }
}
