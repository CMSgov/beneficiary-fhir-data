package gov.cms.bfd.migrator.app;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Properties;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.cfgxml.spi.CfgXmlAccessService;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.selector.spi.StrategySelector;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Tests the functionality of the HibernateValidator. */
public class HibernateValidatorTest {

  HibernateValidator classUnderTest;

  @Mock HikariDataSource dataSource;

  @Mock SchemaValidator mockSchemaValidator;

  @Mock Configuration configuration;

  @Mock SessionFactory mockSessionFactory;

  /** Sets up mocks needed for each test. */
  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    setupHibernateMocks();
    /* Set the package to a spot that will find an annotated entity,
    to pass the validation that we have at least one class */
    List<String> packagesToScan = List.of("gov.cms.bfd.model.rif");
    classUnderTest = new HibernateValidator(dataSource, packagesToScan);
    classUnderTest.setSchemaValidator(mockSchemaValidator);
    classUnderTest.setHibernateConfiguration(configuration);
    when(configuration.buildSessionFactory(any())).thenReturn(mockSessionFactory);
  }

  /**
   * Sets up the Hibernate mocks needed to create a mock validator. Most of these are just for
   * getting past the NPEs, as we don't connect to a real DB as part of most of the tests and don't
   * need to actually set up most of the Hibernate internals.
   */
  private void setupHibernateMocks() {
    SessionFactoryOptions sessionFactoryOptions = mock(SessionFactoryOptions.class);
    when(mockSessionFactory.getSessionFactoryOptions()).thenReturn(sessionFactoryOptions);
    StandardServiceRegistry mockRegistry = mock(StandardServiceRegistry.class);
    when(sessionFactoryOptions.getServiceRegistry()).thenReturn(mockRegistry);
    when(configuration.getProperties()).thenReturn(new Properties());
    ConfigurationService configurationService = mock(ConfigurationService.class);
    when(configurationService.getSetting(
            any(), any(ConfigurationService.Converter.class), anyString()))
        .thenReturn(null);
    when(configurationService.getSetting(
            any(), any(ConfigurationService.Converter.class), anyBoolean()))
        .thenReturn(false);
    when(mockRegistry.getService(ConfigurationService.class)).thenReturn(configurationService);
    StrategySelector strategySelector = mock(StrategySelector.class);
    PhysicalNamingStrategy namingStrategy = mock(PhysicalNamingStrategy.class);
    when(strategySelector.resolveDefaultableStrategy(
            PhysicalNamingStrategy.class, null, PhysicalNamingStrategyStandardImpl.INSTANCE))
        .thenReturn(namingStrategy);
    when(mockRegistry.getService(StrategySelector.class)).thenReturn(strategySelector);
    ClassLoaderService classLoaderService = mock(ClassLoaderService.class);
    when(mockRegistry.getService(ClassLoaderService.class)).thenReturn(classLoaderService);
    CfgXmlAccessService cfxService = mock(CfgXmlAccessService.class);
    when(mockRegistry.getService(CfgXmlAccessService.class)).thenReturn(cfxService);
    JdbcServices jdbcService = mock(JdbcServices.class);
    when(mockRegistry.getService(JdbcServices.class)).thenReturn(jdbcService);
    Dialect dialect = mock(Dialect.class);
    when(jdbcService.getDialect()).thenReturn(dialect);
    JdbcEnvironment jdbcEnv = mock(JdbcEnvironment.class);
    when(mockRegistry.getService(JdbcEnvironment.class)).thenReturn(jdbcEnv);
  }

  /**
   * Test run hibernate validation when no classes are found for the given packages, expect failure.
   */
  @Test
  public void testRunHibernateValidationWhenNoClassesFoundExpectFailure() {
    // Given
    List<String> packagesToScan = List.of("package.doesnt.exist");
    classUnderTest = new HibernateValidator(dataSource, packagesToScan);
    classUnderTest.setSchemaValidator(mockSchemaValidator);
    classUnderTest.setHibernateConfiguration(configuration);
    when(configuration.buildSessionFactory(any())).thenReturn(mockSessionFactory);

    // When
    boolean result = classUnderTest.runHibernateValidation();

    // Then
    assertFalse(result);
  }

  /**
   * Test run hibernate validation when classes are found for the given packages, and those classes
   * are valid against the schema (i.e. are validated and throw no exceptions), expect the run
   * method returns true signaling success.
   */
  @Test
  public void testRunHibernateValidationWhenValidSchemaExpectSuccess() {
    // The default test setup should pass, so test that

    // When
    boolean result = classUnderTest.runHibernateValidation();

    // Then
    assertTrue(result);
  }

  /**
   * Test run hibernate validation when classes are found for the given packages, and there is a
   * validation failure (which throws a HibernateException) the run method returns false signaling
   * failure.
   */
  @Test
  public void testRunHibernateValidationWhenInvalidSchemaExpectFailure() {
    // Given
    doThrow(new HibernateException("bad validation")).when(mockSchemaValidator).validate(any());

    // When
    boolean result = classUnderTest.runHibernateValidation();

    // Then
    assertFalse(result);
  }

  /**
   * Test run hibernate validation when classes are found for the given packages, and there is an
   * unexpected exception in Hibernate or the Hibernate setup the run method returns false signaling
   * failure.
   */
  @Test
  public void testRunHibernateValidationWhenUnexpectedHibernateExceptionExpectFailure() {
    // Given
    doThrow(new RuntimeException("unexpected error")).when(mockSchemaValidator).validate(any());

    // When
    boolean result = classUnderTest.runHibernateValidation();

    // Then
    assertFalse(result);
  }
}
