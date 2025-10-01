package gov.cms.bfd.server.ng.loadprogress;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Repository for computing the most recent {@code batchCompletionTimestamp} across all LoadProgress
 * rows. Using a shared repository keeps repository code small and prevents divergence in how the
 * fallback value is handled.
 */
@Repository
@AllArgsConstructor
public class LoadProgressRepository {
  private final EntityManager entityManager;

  /**
   * Returns the global max batch completion timestamp or {@link DateUtil#MIN_DATETIME} if none.
   *
   * @return latest timestamp
   */
  public ZonedDateTime lastUpdated() {
    return entityManager
        .createQuery(
            """
            SELECT MAX(p.batchCompletionTimestamp)
            FROM LoadProgress p
            """,
            ZonedDateTime.class)
        .getResultList()
        .stream()
        .filter(Objects::nonNull)
        .findFirst()
        .orElse(DateUtil.MIN_DATETIME);
  }
}
