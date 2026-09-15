package gov.cms.bfd.server.ng.loadprogress;

import gov.cms.bfd.server.ng.util.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.ZonedDateTime;
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
  @PersistenceContext private EntityManager entityManager;
  private final DateUtil dateUtil;

  /**
   * Returns the global max batch completion timestamp or UTC now if none.
   *
   * @return latest timestamp
   */
  public ZonedDateTime lastUpdated() {
    // We should only take into account data from the main pipeline
    // since other pipelines are likely being used for backfills
    final var defaultJobId = 1;
    // COALESCE is needed here in case no batches have been loaded.
    return entityManager
        .createQuery(
            """
            SELECT COALESCE(MAX(p.batchCompletionTimestamp), :defaultDate)
            FROM LoadProgress p
            WHERE p.jobId = :defaultJobId
            """,
            ZonedDateTime.class)
        // Per user request, it's desirable to default to the current timestamp here to show that no
        // data has changed
        .setParameter("defaultDate", dateUtil.nowUtc())
        .setParameter("defaultJobId", defaultJobId)
        .getSingleResult();
  }
}
