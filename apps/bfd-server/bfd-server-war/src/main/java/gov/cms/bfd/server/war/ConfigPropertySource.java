package gov.cms.bfd.server.war;

import gov.cms.bfd.sharedutils.config.ConfigLoader;
import java.io.IOException;
import java.util.Properties;
import javax.annotation.Nonnull;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

/** A {@link PropertySource} that gets properties from a {@link ConfigLoader}. */
public class ConfigPropertySource extends PropertySource<ConfigLoader> {
  /**
   * Initializes an instance.
   *
   * @param name name of this instance
   * @param source source of properties
   */
  public ConfigPropertySource(String name, ConfigLoader source) {
    super(name, source);
  }

  @Override
  public Object getProperty(@Nonnull String name) {
    return getSource().stringOption(name).orElse(null);
  }

  /** Factory to create instances of {@link ConfigPropertySource}. */
  public static class Factory implements PropertySourceFactory {
    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource)
        throws IOException {
      if (name == null) {
        name = ConfigPropertySource.class.getName() + "-" + System.identityHashCode(this);
      }
      var properties = new Properties();
      try (var reader = resource.getReader()) {
        properties.load(reader);
      }
      ConfigLoader baseConfig =
          ConfigLoader.builder()
              .addProperties(properties)
              .addEnvironmentVariables()
              .addSystemProperties()
              .build();
      ConfigLoader.Builder realConfig = ConfigLoader.builder();

      String ssmPath = baseConfig.stringValue("aws.ssm.path", "");
      if (ssmPath.length() > 0) {}

      realConfig.addProperties(properties);
      realConfig.addEnvironmentVariables();
      realConfig.addSystemProperties();
      return new ConfigPropertySource(name, realConfig.build());
    }
  }
}
