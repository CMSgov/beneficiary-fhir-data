package gov.cms.bfd.server.war.utils;

import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.model.rif.schema.DatabaseTestUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.sql.DataSource;

public class RDATestUtils {

  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  private EntityManager entityManager;

  public void init() {
    final DataSource dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);

    final Map<String, Object> hibernateProperties =
        ImmutableMap.of(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);

    entityManager =
        Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
            .createEntityManager();
  }

  public void seedData(Collection<?> entities) {
    Iterator<?> i = entities.iterator();

    while (i.hasNext()) {
      doTransaction(em -> em.persist(i.next()));
    }
  }

  public void truncate(Class<?> entityClass) {
    doTransaction(
        em -> {
          em.createQuery("delete from " + entityClass.getSimpleName() + " f").executeUpdate();
        });
  }

  public void doTransaction(Consumer<EntityManager> transaction) {
    entityManager.getTransaction().begin();
    transaction.accept(entityManager);
    entityManager.getTransaction().commit();
  }
}
