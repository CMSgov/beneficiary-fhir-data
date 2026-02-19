package gov.cms.bfd.server.ng.claim;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.filter.*;
import gov.cms.bfd.server.ng.claim.model.*;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.input.TagCriterion;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

/** Repository methods for claims. */
@Repository
@AllArgsConstructor
public class ClaimAsyncService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClaimAsyncService.class);
  @PersistenceContext private final EntityManager entityManager;

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
      List<ClaimTypeCode> claimTypeCodes,
      List<List<MetaSourceSk>> sources) {

    var filterBuilders =
        List.of(
            new BillablePeriodFilterParam(claimThroughDate),
            new LastUpdatedFilterParam(lastUpdated),
            new ClaimTypeCodeFilterParam(claimTypeCodes),
            new TagCriteriaFilterParam(tagCriteria),
            new SourceFilterParam(sources));
    var filters = getFilters(filterBuilders);

    var jpql =
        String.format(
            """
            %s
            WHERE b.xrefSk = :beneSk
            %s
            """,
            baseQuery, filters.filterClause());

    var result =
        withParams(entityManager.createQuery(jpql, claimClass), filters.params())
            .setParameter("beneSk", beneSk)
            .getResultList();

    return CompletableFuture.completedFuture(result);
  }

  <T extends DbFilterBuilder> DbFilter getFilters(List<T> builders) {
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
