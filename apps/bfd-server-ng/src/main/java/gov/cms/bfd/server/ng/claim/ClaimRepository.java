package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.model.Claim;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

/** Repository methods for claims. */
@Component
@AllArgsConstructor
public class ClaimRepository {
  private EntityManager entityManager;

  /**
   * Search for a claim by its ID.
   *
   * @param claimUniqueId claim ID
   * @return claim
   */
  public Optional<Claim> findById(long claimUniqueId) {
    return entityManager
        .createQuery(
            """
              SELECT c
              FROM Claim c
              JOIN c.claimLines cl
              JOIN c.claimDateSignature cds
              JOIN c.claimProcedures cp
              LEFT JOIN c.claimInstitutional ci
              LEFT JOIN cl.claimLineInstitutional cli
              LEFT JOIN cli.ansiSignature as
              LEFT JOIN c.claimValues cv
              WHERE c.claimUniqueId = :claimUniqueId
              """,
            Claim.class)
        .setParameter("claimUniqueId", claimUniqueId)
        .getResultList()
        .stream()
        .findFirst();
  }
}
