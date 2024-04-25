package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.SpringConfiguration.CONFIG_LOADER_CONTEXT_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.ConfigLoaderSource;
import jakarta.servlet.ServletContext;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/** Unit test for {@link ServerInitializer}. */
public class ServerInitializerTest {
  /**
   * Verifies that {@link ServerInitializer#initializeSpringConfiguration} configures the spring
   * context properly to allow properties to be extracted from a {@link ConfigLoader}.
   */
  @Test
  void springPropertiesUseConfigLoader() {
    // Set up a mock ServletContext with just the methods we need.
    var servletContext = mock(ServletContext.class);

    // These will be called by the spring context when we refresh.
    doReturn(Collections.emptyEnumeration()).when(servletContext).getInitParameterNames();
    doReturn(Collections.emptyEnumeration()).when(servletContext).getAttributeNames();

    // Captures the ConfigLoader attribute into an AtomicReference
    var configLoader = new AtomicReference<ConfigLoader>();
    doAnswer(
            inv -> {
              configLoader.set(inv.getArgument(1, ConfigLoader.class));
              return null;
            })
        .when(servletContext)
        .setAttribute(eq(CONFIG_LOADER_CONTEXT_NAME), any());
    doAnswer(args -> configLoader.get())
        .when(servletContext)
        .getAttribute(CONFIG_LOADER_CONTEXT_NAME);

    // Initialize our spring context so we can verify properties where loaded.
    var springContext = new AnnotationConfigWebApplicationContext();
    var envVars = Map.of(TestConfiguration.PROP_PROPERTY_TEST, "hello");
    ServerInitializer.initializeSpringConfiguration(
        springContext,
        servletContext,
        TestConfiguration.class,
        ConfigLoaderSource.fromMap(envVars));

    // Check that our ConfigLoader and its property were used as expected.
    assertNotNull(configLoader.get());
    assertSame(configLoader.get(), springContext.getBean(ConfigLoader.class));
    var bean = springContext.getBean(PropertyTestBean.class);
    assertEquals("hello", bean.value());
  }

  /** Little bean that can be configured using a property value. */
  record PropertyTestBean(String value) {}

  /**
   * Little configuration class that just provides the {@link ConfigLoader} and a {@link
   * PropertyTestBean}.
   */
  static class TestConfiguration {
    /**
     * Property name used by a unit test to validate that spring is providing access to properties
     * correctly without initializing a database connection, etc.
     */
    private static final String PROP_PROPERTY_TEST = "PropertyTest";

    /**
     * Exposes our {@link ConfigLoader} instance as a singleton to components in the test.
     *
     * @param servletContext used to look for config loader attribute
     * @return the config object
     */
    @Bean
    ConfigLoader configLoader(@Autowired ServletContext servletContext) {
      return (ConfigLoader) servletContext.getAttribute(CONFIG_LOADER_CONTEXT_NAME);
    }

    /**
     * Bean used to verify that we have access to a particular property during a unit test.
     *
     * @param value value of the property
     * @return the bean
     */
    @Bean
    PropertyTestBean propertyTestBean(@Value("${" + PROP_PROPERTY_TEST + "}") String value) {
      return new PropertyTestBean(value);
    }
  }
}
