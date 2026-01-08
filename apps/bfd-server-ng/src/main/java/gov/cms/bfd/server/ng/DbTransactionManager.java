package gov.cms.bfd.server.ng;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** Custom transaction manager to set session variables. */
@Component
@RequiredArgsConstructor
public class DbTransactionManager extends JpaTransactionManager {

  private final EntityManager entityManager;

  @Override
  protected void prepareSynchronization(
      DefaultTransactionStatus status, TransactionDefinition definition) {
    super.prepareSynchronization(status, definition);
    if (status.isNewTransaction()) {
      // This is a temporary workaround to improve query performance until we reduce the number of
      // tables.
      final String query = "SET SESSION join_collapse_limit TO 30";
      entityManager.createNativeQuery(query).executeUpdate();
    }
  }
}
