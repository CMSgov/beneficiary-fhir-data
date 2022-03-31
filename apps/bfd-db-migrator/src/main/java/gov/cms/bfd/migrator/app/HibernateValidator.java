package gov.cms.bfd.migrator.app;

import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.Entity;
import javax.sql.DataSource;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.tool.hbm2ddl.SchemaValidator;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Class for containing logic related to Hibernate validation. */
public class HibernateValidator {

  private final Logger LOGGER = LoggerFactory.getLogger(HibernateValidator.class);

  /**
   * Set this to <code>true</code> to have Hibernate log a ton of info on the SQL statements being
   * run and each session's performance. This should eventually be an application setting of some
   * kind.
   */
  private final boolean HIBERNATE_DETAILED_LOGGING = false;

  private final HikariDataSource dataSource;

  private SchemaValidator schemaValidator;

  Configuration hibernateConfiguration;

  List<String> modelPackagesToScan = new ArrayList<>();

  /**
   * Instantiates a new Hibernate validator.
   *
   * @param dataSource the data source to use
   * @param modelPackagesToScan the model packages to scan
   */
  public HibernateValidator(HikariDataSource dataSource, List<String> modelPackagesToScan) {
    this.dataSource = dataSource;
    this.modelPackagesToScan.addAll(modelPackagesToScan);
    this.schemaValidator = new SchemaValidator();
    this.hibernateConfiguration = new Configuration();
  }

  /**
   * Sets the schema validator, primarily for testing.
   *
   * @param schemaValidator the schema validator
   */
  public void setSchemaValidator(SchemaValidator schemaValidator) {
    this.schemaValidator = schemaValidator;
  }

  /**
   * Sets the hibernate configuration, primarily for testing.
   *
   * @param configuration the configuration
   */
  public void setHibernateConfiguration(Configuration configuration) {
    this.hibernateConfiguration = configuration;
  }

  /**
   * Runs hibernate validation and reports if it succeeded.
   *
   * @return {@code true} if the validation succeeded
   */
  public boolean runHibernateValidation() {
    try {
      // Add the models to scan for (used in validation)
      Set<Class<?>> scannedClasses = new HashSet<>();

      for (String packagePath : modelPackagesToScan) {
        scannedClasses.addAll(getEntityClassesFromPackage(packagePath));
      }

      if (scannedClasses.isEmpty()) {
        LOGGER.error("Found no classes to validate.");
        return false;
      }
      LOGGER.debug("Added {} classes to be validated.", scannedClasses.size());

      SessionFactory sessionFactory = createHibernateSessionFactory(dataSource, scannedClasses);
      // Validate the metadata
      StandardServiceRegistry registry =
          sessionFactory.getSessionFactoryOptions().getServiceRegistry();
      MetadataSources sources = new MetadataSources(registry);
      Metadata metadata = sources.buildMetadata(registry);
      // This will throw an exception if validation fails
      schemaValidator.validate(metadata);
    } catch (HibernateException hx) {
      LOGGER.error("Hibernate validation failed due to: ", hx);
      return false;
    } catch (Exception ex) {
      LOGGER.error("Hibernate validation failed due to unexpected exception: ", ex);
      return false;
    }
    return true;
  }

  /**
   * Creates a Hibernate session factory, needed for obtaining the metadata used in validation.
   *
   * @param dataSource the data source which contains the database connection
   * @param classesToValidate the classes to validate
   * @return the session factory
   */
  private SessionFactory createHibernateSessionFactory(
      DataSource dataSource, Set<Class<?>> classesToValidate) {

    for (Class<?> clazz : classesToValidate) {
      hibernateConfiguration.addAnnotatedClass(clazz);
    }

    // Set hibernate to validate the models on startup
    hibernateConfiguration.setProperty(AvailableSettings.HBM2DDL_AUTO, "validate");
    if (HIBERNATE_DETAILED_LOGGING) {
      hibernateConfiguration.setProperty(AvailableSettings.FORMAT_SQL, "true");
      hibernateConfiguration.setProperty(AvailableSettings.USE_SQL_COMMENTS, "true");
      hibernateConfiguration.setProperty(AvailableSettings.SHOW_SQL, "true");
      hibernateConfiguration.setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
    }

    // Build the session factory with the datasource
    return hibernateConfiguration.buildSessionFactory(
        new StandardServiceRegistryBuilder()
            .applySetting(Environment.DATASOURCE, dataSource)
            .applySettings(hibernateConfiguration.getProperties())
            .build());
  }

  /**
   * Gets the {code @Entity} annotated classes from the listed package.
   *
   * @param packageName the package name to find Entity classes in
   * @return the Entity annotated classes from the package
   */
  private Set<Class<?>> getEntityClassesFromPackage(String packageName) {
    Reflections reflections = new Reflections(packageName);
    return reflections.getTypesAnnotatedWith(Entity.class);
  }
}
