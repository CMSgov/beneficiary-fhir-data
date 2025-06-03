package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimProcedure;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class ClaimRepository {
  private EntityManager entityManager;

  public Optional<Claim> findById(long claimUniqueId) {
    return entityManager
        .createQuery(
            """
              SELECT c
              FROM Claim c
              where c.claimUniqueId = :claimUniqueId
              """,
            Claim.class)
        .setParameter("claimUniqueId", claimUniqueId)
        .getResultList()
        .stream()
        .findFirst();
  }

  public List<Claim> getClaimsFromXrefSk(long xrefSk) {
    return entityManager
        .createQuery(
            """
              SELECT c
              FROM Claim c
              JOIN Beneficiary b ON b.beneSk = c.beneSk
              WHERE b.xrefSk = :xrefSk
              """,
            Claim.class)
        .setParameter("xrefSk", xrefSk)
        .getResultList();
  }

  public List<ClaimProcedure> getClaimProcedures(List<Long> claimIds) {
    return entityManager
        .createQuery(
            """
            SELECT c
            FROM ClaimProcedure cp
            WHERE claimUniqueId IN :claimIds
            """,
            ClaimProcedure.class)
        .setParameter("claimIds", claimIds)
        .getResultList();
  }
}
