package gov.cms.bfd.model.rif.schema;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.boot.Metadata;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;

/** Uses Hibernate's HBM2DDL {@link SchemaExport} utility to generate SQL database schemas. */
public final class HibernateSchemaPrinter {
  /**
   * A small application driver that calls {@link #printHibernateSchemaToStdout(String, Class)}.
   *
   * @param args (unused)
   */
  public static void main(String[] args) {
    printHibernateSchemaToStdout("gov.cms.bfd", PostgreSQLDialect.class);
  }

  /**
   * Prints the Hibernate-/HDM2DDL- auto-generated SQL schema to {@link System#out}.
   *
   * @param persistenceUnitName the name of the JPA persistence unit to generate the schema for
   * @param dialectType the Hibernate {@link Dialect} type to generate the schema for, e.g. {@link
   *     PostgreSQLDialect}
   */
  public static void printHibernateSchemaToStdout(
      String persistenceUnitName, Class<? extends Dialect> dialectType) {
    Map<Object, Object> properties = new HashMap<>();
    properties.put(AvailableSettings.DIALECT, dialectType.getName());

    /*
     * Use a Hibernate EntityManagerFactoryBuilderImpl to create a JPA
     * EntityManagerFactory, then grab the (now populated) Hibernate
     * Metadata instance out of it.
     */
    EntityManagerFactoryBuilderImpl entityManagerFactoryBuilder =
        new CustomHibernatePersistenceProvider()
            .getEntityManagerFactoryBuilder(persistenceUnitName, properties);
    entityManagerFactoryBuilder.build();
    Metadata metadata = entityManagerFactoryBuilder.getMetadata();

    SchemaExport schemaExport = new SchemaExport();
    schemaExport.setHaltOnError(true);
    schemaExport.setFormat(true);
    schemaExport.setDelimiter(";");
    schemaExport.execute(EnumSet.of(TargetType.STDOUT), SchemaExport.Action.CREATE, metadata);
  }

  /**
   * A small hack, needed to extract the {@link EntityManagerFactoryBuilderImpl} from {@link
   * HibernatePersistenceProvider}. Taken from the Hibernate Ant task here: <a href=
   * "https://github.com/hibernate/hibernate-tools/blob/321dba082f0cd11a2295063e0cbcf4f34a5b8bdd/main/src/java/org/hibernate/tool/ant/JPAConfigurationTask.java">
   * JPAConfigurationTask.java</a>.
   */
  private static final class CustomHibernatePersistenceProvider
      extends HibernatePersistenceProvider {
    /**
     * (See overridden method; we're just making it <code>public</code>).
     *
     * @param persistenceUnit (see overridden method)
     * @param properties (see overridden method)
     * @return (see overridden method)
     */
    public EntityManagerFactoryBuilderImpl getEntityManagerFactoryBuilder(
        String persistenceUnit, Map<Object, Object> properties) {
      return (EntityManagerFactoryBuilderImpl)
          getEntityManagerFactoryBuilderOrNull(persistenceUnit, properties);
    }
  }
}
