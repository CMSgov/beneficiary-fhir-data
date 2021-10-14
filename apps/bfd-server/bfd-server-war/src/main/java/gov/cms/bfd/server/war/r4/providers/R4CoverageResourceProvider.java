package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.Beneficiary_;
import gov.cms.bfd.server.war.Operation;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.QueryUtils;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for R4 {@link Coverage} resources, derived from
 * the CCW beneficiary enrollment data.
 */
@Component
public final class R4CoverageResourceProvider implements IResourceProvider {
  /**
   * A {@link Pattern} that will match the {@link Coverage#getId()}s used in this application, e.g.
   * <code>part-a-1234</code> or <code>part-a--1234</code> (for negative IDs).
   */
  private static final Pattern COVERAGE_ID_PATTERN =
      Pattern.compile("(\\p{Alnum}+-\\p{Alnum})-(-?\\p{Alnum}+)");

  private static final Logger LOGGER = LoggerFactory.getLogger(R4CoverageResourceProvider.class);

  private EntityManager entityManager;
  private MetricRegistry metricRegistry;
  private LoadedFilterManager loadedFilterManager;

  /** @param entityManager a JPA {@link EntityManager} connected to the application's database */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** @param metricRegistry the {@link MetricRegistry} to use */
  @Inject
  public void setMetricRegistry(MetricRegistry metricRegistry) {
    this.metricRegistry = metricRegistry;
  }

  /** @param loadedFilterManager the {@link LoadedFilterManager} to use */
  @Inject
  public void setLoadedFilterManager(LoadedFilterManager loadedFilterManager) {
    this.loadedFilterManager = loadedFilterManager;
  }

  /** @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType() */
  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return Coverage.class;
  }

  /**
   * Adds support for the FHIR "read" operation, for {@link Coverage}s. The {@link Read} annotation
   * indicates that this method supports the read operation.
   *
   * <p>Read operations take a single parameter annotated with {@link IdParam}, and should return a
   * single resource instance.
   *
   * @param coverageId The read operation takes one parameter, which must be of type {@link IdType}
   *     and must be annotated with the {@link IdParam} annotation.
   * @return Returns a resource matching the specified {@link IdDt}, or <code>null</code> if none
   *     exists.
   */
  @Read(version = false)
  @Trace
  public Coverage read(@IdParam IdType coverageId) {
    if (coverageId == null) throw new IllegalArgumentException();
    if (coverageId.getVersionIdPartAsLong() != null) throw new IllegalArgumentException();

    String coverageIdText = coverageId.getIdPart();
    if (coverageIdText == null || coverageIdText.trim().isEmpty())
      throw new IllegalArgumentException();

    Operation operation = new Operation(Operation.Endpoint.V2_COVERAGE);
    operation.setOption("by", "id");
    operation.publishOperationName();

    Matcher coverageIdMatcher = COVERAGE_ID_PATTERN.matcher(coverageIdText);
    if (!coverageIdMatcher.matches())
      throw new IllegalArgumentException("Unsupported ID pattern: " + coverageIdText);

    String coverageIdSegmentText = coverageIdMatcher.group(1);
    Optional<MedicareSegment> coverageIdSegment =
        MedicareSegment.selectByUrlPrefix(coverageIdSegmentText);
    if (!coverageIdSegment.isPresent()) throw new ResourceNotFoundException(coverageId);
    String coverageIdBeneficiaryIdText = coverageIdMatcher.group(2);

    Beneficiary beneficiaryEntity;
    try {
      beneficiaryEntity = findBeneficiaryById(coverageIdBeneficiaryIdText, null);
    } catch (NoResultException e) {
      throw new ResourceNotFoundException(
          new IdDt(Beneficiary.class.getSimpleName(), coverageIdBeneficiaryIdText));
    }

    if (!beneficiaryEntity.getBeneEnrollmentReferenceYear().isPresent()) {
      throw new ResourceNotFoundException("Cannot find coverage for non present enrollment year");
    }

    Coverage coverage =
        CoverageTransformerV2.transform(metricRegistry, coverageIdSegment.get(), beneficiaryEntity);
    return coverage;
  }

  /**
   * Adds support for the FHIR "search" operation for {@link Coverage}s, allowing users to search by
   * {@link Coverage#getBeneficiary()}.
   *
   * <p>The {@link Search} annotation indicates that this method supports the search operation.
   * There may be many different methods annotated with this {@link Search} annotation, to support
   * many different search criteria.
   *
   * @param beneficiary a {@link ReferenceParam} for the {@link Coverage#getBeneficiary()} to try
   *     and find matches for
   * @param startIndex an {@link OptionalParam} for the startIndex (or offset) used to determine
   *     pagination
   * @param lastUpdated an {@link OptionalParam} to filter the results based on the passed date
   *     range
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link List} of {@link Coverage}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  @Search
  @Trace
  public Bundle searchByBeneficiary(
      @RequiredParam(name = Coverage.SP_BENEFICIARY)
          @Description(shortDefinition = "The patient identifier to search for")
          ReferenceParam beneficiary,
      @OptionalParam(name = "startIndex")
          @Description(shortDefinition = "The offset used for result pagination")
          String startIndex,
      @OptionalParam(name = "_lastUpdated")
          @Description(shortDefinition = "Include resources last updated in the given range")
          DateRangeParam lastUpdated,
      RequestDetails requestDetails) {
    List<IBaseResource> coverages;
    try {
      Beneficiary beneficiaryEntity = findBeneficiaryById(beneficiary.getIdPart(), lastUpdated);
      coverages = CoverageTransformerV2.transform(metricRegistry, beneficiaryEntity);
    } catch (NoResultException e) {
      coverages = new LinkedList<IBaseResource>();
    }

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/Coverage?");

    Operation operation = new Operation(Operation.Endpoint.V2_COVERAGE);
    operation.setOption("by", "beneficiary");
    operation.setOption("pageSize", paging.isPagingRequested() ? "" + paging.getPageSize() : "*");
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.publishOperationName();

    // Add bene_id to MDC logs
    MDC.put("bene_id", beneficiary.getIdPart());

    return TransformerUtilsV2.createBundle(
        paging, coverages, loadedFilterManager.getTransactionTime());
  }

  /**
   * @param beneficiaryId the {@link Beneficiary#getBeneficiaryId()} value to find a matching {@link
   *     Beneficiary} for
   * @return the {@link Beneficiary} that matches the specified {@link
   *     Beneficiary#getBeneficiaryId()} value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found in the database.
   */
  @Trace
  private Beneficiary findBeneficiaryById(String beneficiaryId, DateRangeParam lastUpdatedRange)
      throws NoResultException {
    // Optimize when the lastUpdated parameter is specified and result set is empty
    if (loadedFilterManager.isResultSetEmpty(beneficiaryId, lastUpdatedRange)) {
      throw new NoResultException();
    }
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> root = criteria.from(Beneficiary.class);
    root.fetch(Beneficiary_.beneficiaryMonthlys, JoinType.LEFT);
    criteria.select(root);
    Predicate wherePredicate = builder.equal(root.get(Beneficiary_.beneficiaryId), beneficiaryId);
    if (lastUpdatedRange != null) {
      Predicate predicate = QueryUtils.createLastUpdatedPredicate(builder, root, lastUpdatedRange);
      wherePredicate = builder.and(wherePredicate, predicate);
    }
    criteria.where(wherePredicate);

    Beneficiary beneficiary = null;
    Long beneByIdQueryNanoSeconds = null;
    Timer.Context timerBeneQuery =
        metricRegistry
            .timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_id"))
            .time();
    try {
      beneficiary = entityManager.createQuery(criteria).getSingleResult();
    } finally {
      beneByIdQueryNanoSeconds = timerBeneQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "bene_by_id.include_", beneByIdQueryNanoSeconds, beneficiary == null ? 0 : 1);
    }
    return beneficiary;
  }
}
