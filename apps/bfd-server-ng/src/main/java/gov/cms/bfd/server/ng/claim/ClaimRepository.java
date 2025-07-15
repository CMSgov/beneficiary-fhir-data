package gov.cms.bfd.server.ng.claim;

import ca.uhn.fhir.rest.param.TokenParam;
import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.claim.model.Claim;
import gov.cms.bfd.server.ng.claim.model.ClaimSourceId;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimRepository {
  private EntityManager entityManager;

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
    return withDateParams(
            entityManager.createQuery(
                String.format(
                    """
                    %s
                    WHERE c.claimUniqueId = :claimUniqueId
                    %s
                    """,
                    getClaimTables(), getDateFilters(claimThroughDate, lastUpdated)),
                Claim.class),
            claimThroughDate,
            lastUpdated)
        .setParameter("claimUniqueId", claimUniqueId)
        .getResultList()
        .stream()
        .findFirst();
  }

  /**
   * Returns claims for the given beneficiary.
   *
   * @param beneSk bene sk
   * @param claimThroughDate claim through date
   * @param lastUpdated last updated
   * @param limit limit
   * @param offset offset
   * @param tag tag
   * @return claims
   */
  public List<Claim> findByBeneXrefSk(
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      Optional<Integer> limit,
      Optional<Integer> offset,
      Optional<TokenParam> tag) {

    Optional<List<ClaimSourceId>> queryParameterValue = getSourceIdsForTagCode(tag);

    StringBuilder jpqlBuilder =
        new StringBuilder(
            String.format(
                """
                %s
                WHERE b.xrefSk = :beneSk
                """,
                getClaimTables()));

    jpqlBuilder.append(getDateFilters(claimThroughDate, lastUpdated));

    if (queryParameterValue.isPresent() && !queryParameterValue.get().isEmpty()) {
      jpqlBuilder.append(" AND c.claimSourceId IN :sourceIds");
    }

    // Add the final ordering.
    jpqlBuilder.append(" ORDER BY c.claimUniqueId");

    TypedQuery<Claim> query = entityManager.createQuery(jpqlBuilder.toString(), Claim.class);

    query.setParameter("beneSk", beneSk);

    queryParameterValue.ifPresent(
        sourceIds -> {
          if (!sourceIds.isEmpty()) {
            query.setParameter("sourceIds", sourceIds);
          }
        });

    TypedQuery<Claim> finalQuery = withDateParams(query, claimThroughDate, lastUpdated);

    return finalQuery
        .setMaxResults(limit.orElse(5000))
        .setFirstResult(offset.orElse(0))
        .getResultList();
  }

  /**
   * Returns the last updated timestamp for the claims data ingestion process.
   *
   * @return last updated timestamp
   */
  public ZonedDateTime claimLastUpdated() {
    return entityManager
        .createQuery(
            """
            SELECT MAX(p.batchCompletionTimestamp)
            FROM LoadProgress p
            WHERE p.tableName LIKE 'idr.claim%'
            """,
            ZonedDateTime.class)
        .getResultList()
        .stream()
        .findFirst()
        .orElse(DateUtil.MIN_DATETIME);
  }

  private String getClaimTables() {
    return """
    SELECT c
    FROM Claim c
    JOIN FETCH c.beneficiary b
    JOIN FETCH c.claimDateSignature AS cds
    JOIN FETCH c.claimLines AS cl
    JOIN FETCH c.claimProcedures cp
    LEFT JOIN FETCH c.claimInstitutional ci
    LEFT JOIN FETCH cl.claimLineInstitutional cli
    LEFT JOIN FETCH cli.ansiSignature a
    LEFT JOIN FETCH c.claimValues cv
    """;
  }

  private String getDateFilters(DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
    return String.format(
        """
        AND ((cast(:claimThroughDateLowerBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateLowerBound)
        AND ((cast(:claimThroughDateUpperBound AS LocalDate)) IS NULL OR c.billablePeriod.claimThroughDate %s :claimThroughDateUpperBound)
        AND ((cast(:lastUpdatedLowerBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedLowerBound)
        AND ((cast(:lastUpdatedUpperBound AS ZonedDateTime)) IS NULL OR c.meta.updatedTimestamp %s :lastUpdatedUpperBound)
        """,
        claimThroughDate.getLowerBoundSqlOperator(),
        claimThroughDate.getUpperBoundSqlOperator(),
        lastUpdated.getLowerBoundSqlOperator(),
        lastUpdated.getUpperBoundSqlOperator());
  }

  private <T> TypedQuery<T> withDateParams(
      TypedQuery<T> query, DateTimeRange claimThroughDate, DateTimeRange lastUpdated) {
    return query
        .setParameter(
            "claimThroughDateLowerBound", claimThroughDate.getLowerBoundDate().orElse(null))
        .setParameter(
            "claimThroughDateUpperBound", claimThroughDate.getUpperBoundDate().orElse(null))
        .setParameter("lastUpdatedLowerBound", lastUpdated.getLowerBoundDateTime().orElse(null))
        .setParameter("lastUpdatedUpperBound", lastUpdated.getUpperBoundDateTime().orElse(null));
  }

  /**
   * Maps a tag code ("Adjudicated" or "PartiallyAdjudicated") to the corresponding ClaimSourceId
   * enum values.
   *
   * @param tag The code from the _tag parameter.
   * @return A list of matching ClaimSourceId enums.
   */
  private Optional<List<ClaimSourceId>> getSourceIdsForTagCode(Optional<TokenParam> tag) {

    String tagCode = null;
    List<ClaimSourceId> matchingSources = new ArrayList<>();

    if (!tag.isEmpty()) {
      tagCode = tag.get().getValue();

      for (ClaimSourceId sourceId : ClaimSourceId.values()) {
        // Check if the sourceId's adjudicationStatus optional contains the requested code
        if (sourceId.getAdjudicationStatus().isPresent()
            && sourceId.getAdjudicationStatus().get().equalsIgnoreCase(tagCode)) {
          matchingSources.add(sourceId);
        }
      }
    }
    return Optional.of(matchingSources);
  }
}
