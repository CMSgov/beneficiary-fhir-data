package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.model.PriorAuthorization;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for prior authorization. */
@Repository
@AllArgsConstructor
public class PriorAuthorizationRepository {
  @PersistenceContext private EntityManager entityManager;

  /**
   * Retrieves the prior authorizations from the mbi.
   *
   * @param mbi Medicare Beneficiary Identifier
   * @return all prior authorizations
   */
  public List<PriorAuthorization> getPriorAuthorizationFromMbi(String mbi) {
    return entityManager
        .createQuery(
            """
                 SELECT p
                 FROM PriorAuthorization p
                 JOIN FETCH p.beneficiary b
                 LEFT JOIN FETCH p.items i
                 WHERE b.latestTransactionFlag = 'Y'
                 AND p.id.mbi = :mbi
               """,
            PriorAuthorization.class)
        .setParameter("mbi", mbi)
        .getResultList();
  }
}
