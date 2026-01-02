package gov.cms.bfd.server.war.stu3.providers;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.Beneficiary_;
import gov.cms.bfd.server.war.CanonicalOperation;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.LoggingUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.OpenAPIContentProvider;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.RetryOnFailoverOrConnectionException;
import gov.cms.bfd.server.war.commons.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3 {@link Coverage} resources, derived
 * from the CCW beneficiary enrollment data.
 */
@Component
public class CoverageResourceProvider implements IResourceProvider {
  /**
   * A {@link Pattern} that will match the {@link Coverage#getId()}s used in this application, e.g.
   * <code>part-a-1234</code> or <code>part-a--1234</code> (for negative IDs).
   */
  private static final Pattern COVERAGE_ID_PATTERN =
      Pattern.compile("(\\p{Alnum}+-\\p{Alnum})-(-?\\p{Digit}+)");

  private static final Logger LOGGER = LoggerFactory.getLogger(CoverageResourceProvider.class);

  /** The entity manager. */
  private EntityManager entityManager;

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The Loaded filter manager. */
  private final LoadedFilterManager loadedFilterManager;

  /** The coverage transformer. */
  private final CoverageTransformer coverageTransformer;

  /**
   * Instantiates a new {@link CoverageResourceProvider}.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param metricRegistry the metric registry
   * @param loadedFilterManager the loaded filter manager
   * @param coverageTransformer the coverage transformer
   */
  public CoverageResourceProvider(
      MetricRegistry metricRegistry,
      LoadedFilterManager loadedFilterManager,
      CoverageTransformer coverageTransformer) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.loadedFilterManager = requireNonNull(loadedFilterManager);
    this.coverageTransformer = requireNonNull(coverageTransformer);
  }

  /**
   * Sets the {@link #entityManager}.
   *
   * @param entityManager a JPA {@link EntityManager} connected to the application's database
   */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** {@inheritDoc} */
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
  @RetryOnFailoverOrConnectionException
  public Coverage read(@IdParam IdType coverageId) {
    if (coverageId == null) {
      throw new InvalidRequestException("Missing required coverage ID");
    }
    if (coverageId.getVersionIdPartAsLong() != null) {
      throw new InvalidRequestException("Coverage ID must not define a version");
    }

    String coverageIdText = coverageId.getIdPart();
    if (coverageIdText == null || coverageIdText.trim().isEmpty()) {
      throw new InvalidRequestException("Missing required coverage ID");
    }

    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_COVERAGE);
    operation.setOption("by", "id");
    operation.publishOperationName();

    Matcher coverageIdMatcher = COVERAGE_ID_PATTERN.matcher(coverageIdText);
    if (!coverageIdMatcher.matches()) {
      throw new InvalidRequestException(
          "Coverage ID pattern: '"
              + coverageIdText
              + "' does not match expected pattern: {alphaNumericString}-{singleCharacter}-{idNumber}");
    }

    String coverageIdSegmentText = coverageIdMatcher.group(1);
    Optional<MedicareSegment> coverageIdSegment =
        MedicareSegment.selectByUrlPrefix(coverageIdSegmentText);
    if (!coverageIdSegment.isPresent()) throw new ResourceNotFoundException(coverageId);
    String coverageIdBeneficiaryIdText = coverageIdMatcher.group(2);

    Long beneficiaryId =
        StringUtils.parseLongOrBadRequest(coverageIdBeneficiaryIdText, "Beneficiary ID");
    Beneficiary beneficiaryEntity;
    try {
      beneficiaryEntity = findBeneficiaryById(beneficiaryId, null);

      // Add bene_id to MDC logs
      LoggingUtils.logBeneIdToMdc(beneficiaryId);
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(1);
    } catch (NoResultException e) {
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);

      throw new ResourceNotFoundException(
          new IdDt(Beneficiary.class.getSimpleName(), coverageIdBeneficiaryIdText));
    }

    Coverage coverage = coverageTransformer.transform(coverageIdSegment.get(), beneficiaryEntity);
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
   * @param count an {@link OptionalParam} for the count used in pagination
   * @param lastUpdated an {@link OptionalParam} to filter the results based on the passed date
   *     range
   * @param requestDetails a {@link RequestDetails} containing the details of the request URL, used
   *     to parse out pagination values
   * @return Returns a {@link List} of {@link Coverage}s, which may contain multiple matching
   *     resources, or may also be empty.
   */
  @Search
  @RetryOnFailoverOrConnectionException
  public Bundle searchByBeneficiary(
      @RequiredParam(name = Coverage.SP_BENEFICIARY)
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_SP_RES_ID_SHORT,
              value = OpenAPIContentProvider.PATIENT_SP_RES_ID_VALUE)
          ReferenceParam beneficiary,
      @OptionalParam(name = "startIndex")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_START_INDEX_SHORT,
              value = OpenAPIContentProvider.PATIENT_START_INDEX_VALUE)
          String startIndex,
      @OptionalParam(name = Constants.PARAM_COUNT)
          @Description(
              shortDefinition = OpenAPIContentProvider.COUNT_SHORT,
              value = OpenAPIContentProvider.COUNT_VALUE)
          String count,
      @OptionalParam(name = "_lastUpdated")
          @Description(
              shortDefinition = OpenAPIContentProvider.PATIENT_LAST_UPDATED_SHORT,
              value = OpenAPIContentProvider.PATIENT_LAST_UPDATED_VALUE)
          DateRangeParam lastUpdated,
      RequestDetails requestDetails) {
    List<IBaseResource> coverages;
    Long beneficiaryId =
        StringUtils.parseLongOrBadRequest(beneficiary.getIdPart(), "Beneficiary ID");
    try {
      Beneficiary beneficiaryEntity = findBeneficiaryById(beneficiaryId, lastUpdated);
      coverages = coverageTransformer.transform(beneficiaryEntity);
    } catch (NoResultException e) {
      coverages = new LinkedList<IBaseResource>();
    }

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/Coverage?");

    CanonicalOperation operation = new CanonicalOperation(CanonicalOperation.Endpoint.V1_COVERAGE);
    operation.setOption("by", "beneficiary");
    operation.setOption("pageSize", paging.isPagingRequested() ? "" + paging.getPageSize() : "*");
    operation.setOption(
        "_lastUpdated", Boolean.toString(lastUpdated != null && !lastUpdated.isEmpty()));
    operation.publishOperationName();

    // Add bene_id to MDC logs
    LoggingUtils.logBeneIdToMdc(beneficiaryId);

    return TransformerUtils.createBundle(
        paging, coverages, loadedFilterManager.getTransactionTime());
  }

  /**
   * Finds beneficiary by id.
   *
   * @param beneId the {@link Beneficiary#getBeneficiaryId()} value to find a matching {@link
   *     Beneficiary} for
   * @param lastUpdatedRange the last updated range
   * @return the {@link Beneficiary} that matches the specified {@link
   *     Beneficiary#getBeneficiaryId()} value
   * @throws NoResultException A {@link NoResultException} will be thrown if no matching {@link
   *     Beneficiary} can be found in the database.
   */
  private Beneficiary findBeneficiaryById(Long beneId, DateRangeParam lastUpdatedRange)
      throws NoResultException {
    // Optimize when the lastUpdated parameter is specified and result set is empty
    if (loadedFilterManager.isResultSetEmpty(beneId, lastUpdatedRange)) {
      // Add bene_id to MDC logs when _lastUpdated filter is in effect
      LoggingUtils.logBeneIdToMdc(beneId);
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);

      throw new NoResultException();
    }
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
    Root<Beneficiary> root = criteria.from(Beneficiary.class);
    root.fetch(Beneficiary_.beneficiaryMonthlys, JoinType.LEFT);
    criteria.select(root);
    Predicate wherePredicate = builder.equal(root.get(Beneficiary_.beneficiaryId), beneId);
    if (lastUpdatedRange != null) {
      Predicate predicate = QueryUtils.createLastUpdatedPredicate(builder, root, lastUpdatedRange);
      wherePredicate = builder.and(wherePredicate, predicate);
    }
    criteria.where(wherePredicate);

    Beneficiary beneficiary = null;
    try (Timer.Context timerBeneQuery =
        CommonTransformerUtils.createMetricsTimer(
            metricRegistry, getClass().getSimpleName(), "query", "bene_by_id")) {
      try {
        beneficiary = entityManager.createQuery(criteria).getSingleResult();
      } finally {
        long beneByIdQueryNanoSeconds = timerBeneQuery.stop();
        CommonTransformerUtils.recordQueryInMdc(
            "bene_by_id_include_", beneByIdQueryNanoSeconds, beneficiary == null ? 0 : 1);
      }
      return beneficiary;
    }
  }
}
