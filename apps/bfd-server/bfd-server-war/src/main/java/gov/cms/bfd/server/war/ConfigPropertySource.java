package gov.cms.bfd.server.war;

import gov.cms.bfd.sharedutils.config.ConfigLoader;
import javax.annotation.Nonnull;
import org.springframework.core.env.PropertySource;

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
}
