package gov.cms.bfd.server.war.r4.providers;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.LoggingUtils;
import gov.cms.bfd.server.war.commons.QueryUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Java Callable class that will create a list of patient claims for a given claim type. */
@Component
@Scope("prototype")
public class PatientClaimsEobTaskTransformerV2 implements Callable {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatientClaimsEobTaskTransformerV2.class);

  // +++++++++++++++++++++++++++++++++++
  // bean injected parameter values
  // +++++++++++++++++++++++++++++++++++

  /** capture performance metrics. */
  @Autowired private final MetricRegistry metricRegistry;
  /** The loaded filter manager. */
  @Autowired private final LoadedFilterManager loadedFilterManager;
  /** drug description lookup table. */
  @Autowired private final FdaDrugCodeDisplayLookup drugCodeDisplayLookup;
  /** NPI lookup table. */
  @Autowired private final NPIOrgLookup npiOrgLookup;
  /** The samhsa matcher. */
  @Autowired private final R4EobSamhsaMatcher samhsaMatcher;
  /** Database entity manager. */
  private EntityManager entityManager;

  // +++++++++++++++++++++++++++++++++++
  // setup parameter values
  // +++++++++++++++++++++++++++++++++++

  /** the ClaimTransformer interface for execution. */
  private ClaimTransformerInterfaceV2 claimTransformer;
  /** the claim type to retreive data for. */
  private ClaimTypeV2 claimType;
  /** beneficiary identifier to process. */
  private Long id;
  /** the id represents a single patient read. */
  private boolean singlePatientRead;
  /** date range that lastUpdate falls within. */
  private Optional<DateRangeParam> lastUpdated;
  /** date range that clm_thru_dt falls within. */
  private Optional<DateRangeParam> serviceDate;
  /** whether to return tax numbers. */
  private Optional<Boolean> includeTaxNumbers;
  /** whether to exclude SAMHSA claims. */
  private Boolean excludeSamhsa;
  /** ExecutorService count down latch. */
  private CountDownLatch countDownLatch = null;

  // +++++++++++++++++++++++++++++++++++
  // task properties
  // +++++++++++++++++++++++++++++++++++

  /** capture exception if thrown. */
  private Exception taskException = null;
  /** the list of EOBs that we'll return. */
  private List<ExplanationOfBenefit> eobs = new ArrayList<ExplanationOfBenefit>();

  /**
   * Constructor for TransformPatientClaimsToEobTask.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param metricRegistry the metric registry bean
   * @param loadedFilterManager the loaded filter manager bean
   * @param samhsaMatcher the samhsa matcher bean
   * @param drugCodeDisplayLookup the drug code display lookup bean
   * @param npiOrgLookup the npi org lookup bean
   */
  public PatientClaimsEobTaskTransformerV2(
      MetricRegistry metricRegistry,
      LoadedFilterManager loadedFilterManager,
      R4EobSamhsaMatcher samhsaMatcher,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup,
      NPIOrgLookup npiOrgLookup) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.loadedFilterManager = requireNonNull(loadedFilterManager);
    this.samhsaMatcher = requireNonNull(samhsaMatcher);
    this.drugCodeDisplayLookup = requireNonNull(drugCodeDisplayLookup);
    this.npiOrgLookup = requireNonNull(npiOrgLookup);
  }

  /**
   * Setup the task processing parameters.
   *
   * @param claimTransformer the {@link ClaimTransformerInterfaceV2} to run.
   * @param claimType the claim type to process.
   * @param id the beneficiary or claim identifier to process.
   * @param singlePatientRead the id param is beneficiary ID.
   * @param lastUpdated the date range that lastUpdate falls within.
   * @param serviceDate the date range that clm_thru_dt falls within.
   * @param includeTaxNumbers whether to return tax numbers.
   * @param excludeSamhsa if true excludes SAMHSA claims.abstract
   * @param countDownLatch the ExecutorService count down latch.
   */
  public void setupTaskParams(
      ClaimTransformerInterfaceV2 claimTransformer,
      ClaimTypeV2 claimType,
      Long id,
      boolean singlePatientRead,
      Optional<DateRangeParam> lastUpdated,
      Optional<DateRangeParam> serviceDate,
      Optional<Boolean> includeTaxNumbers,
      Boolean excludeSamhsa,
      CountDownLatch countDownLatch) {
    this.claimTransformer = requireNonNull(claimTransformer);
    this.claimType = requireNonNull(claimType);
    this.id = requireNonNull(id);
    this.countDownLatch = requireNonNull(countDownLatch);
    this.singlePatientRead = singlePatientRead;
    this.lastUpdated = lastUpdated;
    this.serviceDate = serviceDate;
    this.excludeSamhsa = excludeSamhsa;
    this.includeTaxNumbers = includeTaxNumbers;
  }

  /**
   * Sets the {@link #entityManager}; defined as scope=prototype to get unique JPA {@link
   * EntityManager} per thread.
   *
   * @param entityManager a JPA {@link EntityManager} connected to the application's database
   */
  @PersistenceContext
  @Scope("prototype")
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * ExecutorService will invoke the task.
   *
   * @return the results for the task.
   */
  @Override
  public PatientClaimsEobTaskTransformerV2 call() {
    LOGGER.debug("TransformPatientClaimsToEobTaskV2.call() started for {}", id);
    try {
      if (singlePatientRead) {
        patientRead();
      } else {
        eobs.addAll(transformToEobs(findClaimTypeByPatient()));
        /*
         * FIX THIS
         * if (excludeSamhsa) {
         * filterSamsha(eobs);
         * }
         */
      }
    } catch (Exception e) {
      // keep track of the Exception so we can provide to caller.
      LOGGER.error((taskException = e).getMessage(), e);
    }
    // the countdown latch waits for all threads to complete/fail.
    countDownLatch.countDown();
    return this;
  }

  /**
   * Transforms the eob claim to the specified type.
   *
   * @param claimEntity the claim entity to transform
   * @return the transformed explanation of benefit
   */
  private ExplanationOfBenefit transformEobClaim(Object claimEntity) {
    return claimTransformer.transform(claimEntity, includeTaxNumbers);
  }

  /** Do a db lookup using a claim type entity object. */
  private void patientRead() {
    Class<?> entityClass = claimType.getEntityClass();
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery(entityClass);
    Root root = criteria.from(entityClass);
    claimType.getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root);
    criteria.where(builder.equal(root.get(claimType.getEntityIdAttribute()), id));

    Object claimEntity = null;
    Long eobByIdQueryNanoSeconds = null;
    Timer.Context timerEobQuery =
        metricRegistry
            .timer(MetricRegistry.name(getClass().getSimpleName(), "query", "eob_by_id"))
            .time();
    try {
      claimEntity = entityManager.createQuery(criteria).getSingleResult();

      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(1);
    } catch (NoResultException e) {
      // Add number of resources to MDC logs
      LoggingUtils.logResourceCountToMdc(0);
    } finally {
      eobByIdQueryNanoSeconds = timerEobQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          "eob_by_id", eobByIdQueryNanoSeconds, claimEntity == null ? 0 : 1);
    }
  }

  /**
   * Check if the task encountered an {@link Exception}.
   *
   * @return false if the task encountered an exception, true otherwise.
   */
  public boolean ranSuccessfully() {
    return (taskException == null);
  }

  /**
   * Fetch the {@link Exception} if the task encountered one.
   *
   * @return Exception if the task encountered one, Optional.Empty() otherwise.
   */
  public Optional<Exception> getFailure() {
    return ranSuccessfully() ? Optional.empty() : Optional.of(taskException);
  }

  /**
   * Fetch the list of {@link ExplanationOfBenefit} that were created.
   *
   * @return {@link List} of {@link ExplanationOfBenefit}
   */
  public List<ExplanationOfBenefit> fetchEOBs() {
    return eobs;
  }

  /**
   * Find claim type by patient list.
   *
   * @param <T> the type parameter
   * @return the matching claim/event entities
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  @Trace
  private <T> List<T> findClaimTypeByPatient() {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery criteria = builder.createQuery((Class) claimType.getEntityClass());
    Root root = criteria.from(claimType.getEntityClass());
    claimType.getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
    criteria.select(root).distinct(true);

    Predicate wherePredicate =
        builder.equal(root.get(claimType.getEntityBeneficiaryIdAttribute()), id);

    if (lastUpdated.isPresent()) {
      Predicate predicate = QueryUtils.createLastUpdatedPredicate(builder, root, lastUpdated.get());
      wherePredicate = builder.and(wherePredicate, predicate);
    }
    criteria.where(wherePredicate);

    List<T> claimEntities = null;
    Long eobsByBeneIdQueryNanoSeconds = null;
    Timer.Context timerEobQuery =
        metricRegistry
            .timer(
                MetricRegistry.name(
                    metricRegistry.getClass().getSimpleName(),
                    "query",
                    "eobs_by_bene_id",
                    claimType.name().toLowerCase()))
            .time();
    try {
      claimEntities = entityManager.createQuery(criteria).getResultList();
    } finally {
      eobsByBeneIdQueryNanoSeconds = timerEobQuery.stop();
      TransformerUtilsV2.recordQueryInMdc(
          String.format("eobs_by_bene_id_%s", claimType.name().toLowerCase()),
          eobsByBeneIdQueryNanoSeconds,
          claimEntities == null ? 0 : claimEntities.size());
    }

    if (claimEntities != null && serviceDate != null && !serviceDate.isEmpty()) {
      final Instant lowerBound =
          serviceDate.get().getLowerBoundAsInstant() != null
              ? serviceDate.get().getLowerBoundAsInstant().toInstant()
              : null;
      final Instant upperBound =
          serviceDate.get().getUpperBoundAsInstant() != null
              ? serviceDate.get().getUpperBoundAsInstant().toInstant()
              : null;
      final java.util.function.Predicate<LocalDate> lowerBoundCheck =
          lowerBound == null
              ? (date) -> true
              : (date) ->
                  TransformerUtilsV2.compareLocalDate(
                      date,
                      lowerBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.get().getLowerBound().getPrefix());
      final java.util.function.Predicate<LocalDate> upperBoundCheck =
          upperBound == null
              ? (date) -> true
              : (date) ->
                  TransformerUtilsV2.compareLocalDate(
                      date,
                      upperBound.atZone(ZoneId.systemDefault()).toLocalDate(),
                      serviceDate.get().getUpperBound().getPrefix());

      return claimEntities.stream()
          .filter(
              entity ->
                  lowerBoundCheck.test(claimType.getServiceEndAttributeFunction().apply(entity))
                      && upperBoundCheck.test(
                          claimType.getServiceEndAttributeFunction().apply(entity)))
          .collect(Collectors.toList());
    }
    return claimEntities;
  }

  /**
   * Transform a list of claims to a list of {@link org.hl7.fhir.r4.model.ExplanationOfBenefit}
   * objects.
   *
   * @param claims the claims/events to transform
   * @return the transformed {@link ExplanationOfBenefit} instances, one for each specified
   *     claim/event
   */
  @Trace
  private List<ExplanationOfBenefit> transformToEobs(List<?> claims) {
    return claims.stream()
        .map(c -> claimTransformer.transform(c, includeTaxNumbers))
        .collect(Collectors.toList());
  }

  /**
   * Removes all SAMHSA-related claims from the specified {@link List} of {@link
   * ExplanationOfBenefit} resources.
   *
   * @param eobs the {@link List} of {@link ExplanationOfBenefit} resources (i.e. claims) to filter
   */
  private void filterSamhsa(List<IBaseResource> eobs) {
    ListIterator<IBaseResource> eobsIter = eobs.listIterator();
    while (eobsIter.hasNext()) {
      ExplanationOfBenefit eob = (ExplanationOfBenefit) eobsIter.next();
      if (samhsaMatcher.test(eob)) {
        eobsIter.remove();
      }
    }
  }
}
