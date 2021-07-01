package gov.cms.bfd.model.rda;

import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import org.hibernate.tool.schema.Action;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Test;

public class SchemaMigrationIT {
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  @Test
  public void createLoadAndVerifyEntities() {
    final JDBCDataSource dataSource = createInMemoryDataSource();
    DatabaseSchemaManager.createOrUpdateSchema(dataSource);

    final EntityManager entityManager = createEntityManager(dataSource);

    final PreAdjFissClaim claim = new PreAdjFissClaim();
    claim.setDcn("1");
    claim.setHicNo("h1");
    claim.setCurrStatus('1');
    claim.setCurrLoc1('A');
    claim.setCurrLoc2("1A");

    final PreAdjFissProcCode procCode = new PreAdjFissProcCode();
    procCode.setDcn("1");
    procCode.setPriority((short) 0);
    procCode.setProcCode("P");
    procCode.setProcFlag("F");
    procCode.setProcDate(LocalDate.now());
    procCode.setLastUpdated(Instant.now());
    claim.getProcCodes().add(procCode);

    entityManager.getTransaction().begin();
    entityManager.persist(claim);
    entityManager.getTransaction().commit();

    final TypedQuery<PreAdjFissClaim> query =
        entityManager.createQuery("select c from PreAdjFissClaim c", PreAdjFissClaim.class);
    List<PreAdjFissClaim> claims = query.getResultList();
    assertEquals(1, claims.size());

    final PreAdjFissClaim resultClaim = claims.get(0);
    assertEquals("h1", resultClaim.getHicNo());

    final List<PreAdjFissProcCode> resultCodes = ImmutableList.copyOf(resultClaim.getProcCodes());
    assertEquals(1, resultCodes.size());
    assertEquals("F", resultCodes.get(0).getProcFlag());
  }

  private EntityManager createEntityManager(JDBCDataSource dataSource) {
    final Map<String, Object> hibernateProperties =
        ImmutableMap.of(
            org.hibernate.cfg.AvailableSettings.DATASOURCE,
            dataSource,
            org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO,
            Action.VALIDATE,
            org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE,
            10);

    return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME, hibernateProperties)
        .createEntityManager();
  }

  private JDBCDataSource createInMemoryDataSource() {
    JDBCDataSource dataSource = new JDBCDataSource();
    dataSource.setUrl("jdbc:hsqldb:mem:unit-tests");
    return dataSource;
  }
}
