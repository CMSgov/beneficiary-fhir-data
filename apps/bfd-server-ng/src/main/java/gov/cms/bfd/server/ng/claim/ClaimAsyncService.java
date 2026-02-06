package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.filter.BillablePeriodFilterParam;
import gov.cms.bfd.server.ng.claim.filter.ClaimTypeCodeFilterParam;
import gov.cms.bfd.server.ng.claim.filter.LastUpdatedFilterParam;
import gov.cms.bfd.server.ng.claim.filter.TagCriteriaFilterParam;
import gov.cms.bfd.server.ng.claim.model.ClaimBase;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.input.TagCriterion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Repository methods for claims. */
// NOTE: @Transactional is needed to ensure our custom transaction manager is used
@Transactional(readOnly = true)
@Repository
@AllArgsConstructor
public class ClaimAsyncService {

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ClaimAsyncService.class);
  private final EntityManager entityManager;

  @Async
  protected <T extends ClaimBase> CompletableFuture<Optional<T>> findByIdInClaimType(
      String baseQuery, Class<T> claimClass, long claimUniqueId, DbFilter params) {

    LOGGER.info("VIRTUAL THREAD'{}'", Thread.currentThread());
    var jpql =
        String.format(
            """
                  %s
                  WHERE c.claimUniqueId = :claimUniqueId
                  %s
                  """,
            baseQuery, params.filterClause());

    var result =
        withParams(entityManager.createQuery(jpql, claimClass), params.params())
            .setParameter("claimUniqueId", claimUniqueId)
            .getResultList()
            .stream()
            .findFirst();

    return CompletableFuture.completedFuture(result);
  }

  @Async
  protected <T extends ClaimBase> CompletableFuture<List<T>> fetchClaims(
      String baseQuery,
      Class<T> claimClass,
      long beneSk,
      DateTimeRange claimThroughDate,
      DateTimeRange lastUpdated,
      List<List<TagCriterion>> tagCriteria,
      List<ClaimTypeCode> claimTypeCodes) {

    LOGGER
        .atInfo()
        .setMessage("VIRTUAL THREAD")
        .addKeyValue("THREAD", Thread.currentThread())
        .log();
    var filterBuilders =
        List.of(
            new BillablePeriodFilterParam(claimThroughDate),
            new LastUpdatedFilterParam(lastUpdated),
            new ClaimTypeCodeFilterParam(claimTypeCodes),
            new TagCriteriaFilterParam(tagCriteria));
    var filters = getFilters(filterBuilders);

    var jpql =
        String.format(
            """
                    WITH benes AS (
                        SELECT b.beneSk beneSk, b.effectiveTimestamp effectiveTimestamp
                        FROM Beneficiary b
                        WHERE b.xrefSk = :beneSk AND b.latestTransactionFlag = 'Y'
                    )
                    %s
                    WHERE EXISTS (
                        SELECT 1 FROM benes b2
                        WHERE b2.beneSk = b.beneSk
                        AND b2.effectiveTimestamp = b.effectiveTimestamp
                    )
                    %s
                    ORDER BY c.claimUniqueId
                    """,
            baseQuery, filters.filterClause());

    return CompletableFuture.completedFuture(
        withParams(entityManager.createQuery(jpql, claimClass), filters.params())
            .setParameter("beneSk", beneSk)
            .getResultList());
  }

  public <T extends DbFilterBuilder> DbFilter getFilters(List<T> builders) {
    var sb = new StringBuilder();
    var queryParams = new ArrayList<DbFilterParam>();
    for (var builder : builders) {
      var params = builder.getFilters("c");
      sb.append(params.filterClause());
      queryParams.addAll(params.params());
    }
    return new DbFilter(sb.toString(), queryParams);
  }

  private <T> TypedQuery<T> withParams(TypedQuery<T> query, List<DbFilterParam> params) {
    for (var param : params) {
      query.setParameter(param.name(), param.value());
    }
    return query;
  }
}
