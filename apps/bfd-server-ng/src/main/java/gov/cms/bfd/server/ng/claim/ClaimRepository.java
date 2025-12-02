package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.LogUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimRepository {

  private final EntityManager entityManager;

  private static final String CLAIM_TABLES_BASE =
      """
        SELECT c
        FROM Claim c
        JOIN FETCH c.beneficiary b
        JOIN FETCH c.claimDateSignature AS cds
        JOIN FETCH c.claimItems AS cl
        LEFT JOIN FETCH c.claimInstitutional ci
        LEFT JOIN FETCH c.claimProfessional cp
        LEFT JOIN FETCH cl.claimLineInstitutional cli
        LEFT JOIN FETCH cl.claimLineProfessional clp
        LEFT JOIN FETCH c.claimFiss cf
        LEFT JOIN FETCH cli.ansiSignature a
        LEFT JOIN FETCH cl.claimLineRx clr
        LEFT JOIN FETCH c.contract ct
        LEFT JOIN FETCH c.serviceProviderHistory p
        LEFT JOIN FETCH c.attendingProviderHistory ap
        LEFT JOIN FETCH c.operatingProviderHistory orp
        LEFT JOIN FETCH c.otherProviderHistory otp
        LEFT JOIN FETCH c.renderingProviderHistory rp
        LEFT JOIN FETCH c.prescribingProviderHistory pp
        LEFT JOIN FETCH c.billingProviderHistory bp
        LEFT JOIN FETCH c.referringProviderHistory rph
      """;

  /**
   * Search for a claim by its ID.
   *
   * @param claimUniqueId claim ID
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @return claim
   */
  public Optional<Claim> findById(
      long claimUniqueId, DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
    var jpql =
        String.format(
            """
              %s
              WHERE c.claimUniqueId = :claimUniqueId
              %s
            """,
            CLAIM_TABLES_BASE, getFilters(claimThroughDate, lastUpdated));
    var results =
        withParams(
                entityManager.createQuery(jpql, Claim.class),
                claimThroughDate,
                lastUpdated,
                new ArrayList<>(),
                new ArrayList<>())
            .setParameter("claimUniqueId", claimUniqueId)
            .getResultList();

    var optionalClaim = results.stream().findFirst();
    optionalClaim.ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
    return optionalClaim;
  }

  /**
   * Returns claims for the given beneficiary.
   *
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param limit limit
   * @param offset offset
   * @param sourceIds claim sourceIds
   * @param claimTypeCodes claimTypeCodes
   * @return claims
   */
  public List<Claim> findByBeneXrefSk(
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      Optional<Integer> limit,
      Optional<Integer> offset,
      List<ClaimSourceId> sourceIds,
      List<ClaimTypeCode> claimTypeCodes) {
    // JPQL doesn't support LIMIT/OFFSET unfortunately, so we have to load this separately.
    // setMaxResults will only limit the results in memory rather than at the database level.

    // We need to get a distinct list of bene_sk values here because there will be duplicates
    // since this is a history table.
    var claimIds =
        entityManager
            .createNativeQuery(
                """
                WITH benes AS (
                    SELECT DISTINCT b.bene_sk
                    FROM idr.beneficiary b
                    WHERE b.bene_xref_efctv_sk_computed = :beneSk
                )
                SELECT c.clm_uniq_id
                FROM idr.claim c
                JOIN benes b ON b.bene_sk = c.bene_sk
                ORDER BY c.clm_uniq_id
                LIMIT :limit
                OFFSET :offset
                """,
                Long.class)
            .setParameter("beneSk", beneSk)
            .setParameter("limit", limit.orElse(5000))
            .setParameter("offset", offset.orElse(0))
            .getResultList();
    var jpql =
        String.format(
            """
            %s
            WHERE c.claimUniqueId IN (:claimIds)
            %s
            """,
            CLAIM_TABLES_BASE, getFilters(claimThroughDate, lastUpdated));
    var claims =
        withParams(
                entityManager.createQuery(jpql, Claim.class),
                claimThroughDate,
                lastUpdated,
                sourceIds,
                claimTypeCodes)
            .setParameter("claimIds", claimIds)
            .getResultList();

    claims.stream()
        .findFirst()
        .ifPresent(claim -> LogUtil.logBeneSk(claim.getBeneficiary().getBeneSk()));
    return claims;
  }

  private String getFilters(DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
    return String.format(
        """
        AND ((cast(:claimThroughDateLowerBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateLowerBound)
        AND ((cast(:claimThroughDateUpperBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateUpperBound)
        AND ((cast(:lastUpdatedLowerBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedLowerBound)
        AND ((cast(:lastUpdatedUpperBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedUpperBound)
        AND (:hasSourceIds = false OR c.claimSourceId IN :sourceIds)
        AND (:hasClaimTypeCodes = false OR c.claimTypeCode IN :claimTypeCodes)
        """,
        claimThroughDate.getLowerBoundSqlOperator(),
        claimThroughDate.getUpperBoundSqlOperator(),
        lastUpdated.getLowerBoundSqlOperator(),
        lastUpdated.getUpperBoundSqlOperator());
  }

  private <T> TypedQuery<T> withParams(
      TypedQuery<T> query,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      List<ClaimSourceId> sourceIds,
      List<ClaimTypeCode> claimTypeCodes) {
    return query
        .setParameter("claimThroughDateLowerBound", claimThroughDate.getLowerBoundDate())
        .setParameter("claimThroughDateUpperBound", claimThroughDate.getUpperBoundDate())
        .setParameter("lastUpdatedLowerBound", lastUpdated.getLowerBoundDateTime().orElse(null))
        .setParameter("lastUpdatedUpperBound", lastUpdated.getUpperBoundDateTime().orElse(null))
        .setParameter("hasSourceIds", !sourceIds.isEmpty())
        .setParameter("sourceIds", sourceIds)
        .setParameter("hasClaimTypeCodes", !claimTypeCodes.isEmpty())
        .setParameter("claimTypeCodes", claimTypeCodes);
  }
}
