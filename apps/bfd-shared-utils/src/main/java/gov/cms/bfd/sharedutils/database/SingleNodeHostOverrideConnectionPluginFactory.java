package gov.cms.bfd.sharedutils.database;

import java.util.Properties;
import software.amazon.jdbc.ConnectionPlugin;
import software.amazon.jdbc.ConnectionPluginFactory;
import software.amazon.jdbc.PluginService;

/** Plugin factory for {@link SingleNodeHostOverrideConnectionPlugin}. */
public class SingleNodeHostOverrideConnectionPluginFactory implements ConnectionPluginFactory {
  @Override
  public ConnectionPlugin getInstance(PluginService pluginService, Properties properties) {
    return new SingleNodeHostOverrideConnectionPlugin(pluginService);
  }
}
