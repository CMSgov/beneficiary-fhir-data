package gov.cms.bfd.server.war.r4.providers;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.param.DateRangeParam;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.server.war.commons.QueryUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Java Callable class that will create a list of patient claims for a given claim type. There are
 * some pertinent info for developers to grasp especially in understanding that each instance of
 * this class will most likely be running concurrently with other claim tasks. This class is a
 * non-singleton spring-managed bean; each task requires its own unique instance of a {@link
 * EntityManager} since processing requires fetching data from a database and requires its own db
 * connection to operate independently of other concurrent claim tasks. All BFD db connections are
 * injected by a Spring-managed {@link com.zaxxer.hikari.HikariDataSource}.
 *
 * <p>To achieve a unique {@link EntityManager}, this class defines its scope as "prototype", which
 * identifies its need have its own unique instance of the {@link EntityManager}.
 *
 * <p>Once Spring has created the class, the caller then sets up the task parameters that define the
 * operating constraints {@link PatientClaimsEobTaskTransformerV2#setupTaskParams}. In addition,
 * some claim tasks require an additional property denoting how NPI tax number is processed; this
 * property is set using the {@link PatientClaimsEobTaskTransformerV2#setIncludeTaxNumbers}; the
 * default property value for inclusion of NPI tax info is FALSE (do not include NPI tax info).
 */
@Component
@Scope("prototype")
public class PatientClaimsEobTaskTransformerV2 implements Callable {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(PatientClaimsEobTaskTransformerV2.class);

  // +++++++++++++++++++++++++++++++++++
  // bean injected parameter values
  // +++++++++++++++++++++++++++++++++++

  /** capture performance metrics. */
  private final MetricRegistry metricRegistry;
  /** drug description lookup table. */
  private final FdaDrugCodeDisplayLookup drugCodeDisplayLookup;
  /** NPI lookup table. */
  private final NPIOrgLookup npiOrgLookup;
  /** The samhsa matcher. */
  private final R4EobSamhsaMatcher samhsaMatcher;
  /** Database entity manager. */
  private EntityManager entityManager;

  // +++++++++++++++++++++++++++++++++++
  // setup parameter values
  // +++++++++++++++++++++++++++++++++++

  /** the ClaimTransformer interface for execution. */
  private ClaimTransformerInterfaceV2 claimTransformer = null;
  /** the claim type to retreive data for. */
  private ClaimTypeV2 claimType;
  /** beneficiary identifier to process. */
  private Long id = 0L;
  /** date range that lastUpdate falls within. */
  private Optional<DateRangeParam> lastUpdated = Optional.empty();
  /** date range that clm_thru_dt falls within. */
  private Optional<DateRangeParam> serviceDate = Optional.empty();
  /** whether to return tax numbers. */
  private boolean includeTaxNumbers = false;
  /** whether to exclude SAMHSA claims. */
  private boolean excludeSamhsa = false;

  // +++++++++++++++++++++++++++++++++++
  // task properties
  // +++++++++++++++++++++++++++++++++++

  /** capture exception if thrown. */
  private Exception taskException = null;
  /** keep track of EOBs that were not removed (ignored) by SAMHSA iterations. */
  private final AtomicInteger samhsaIgnoredCount = new AtomicInteger(-1);
  /** keep track of SAMHSA removals. */
  private final AtomicInteger samhsaRemovedCount = new AtomicInteger(0);
  /** the list of EOBs that we'll return. */
  private final List<ExplanationOfBenefit> eobs = new ArrayList<ExplanationOfBenefit>();

  /**
   * Constructor for TransformPatientClaimsToEobTask.
   *
   * <p>Spring will wire this class during the initial component scan, so this constructor should
   * only be explicitly called by tests.
   *
   * @param metricRegistry the metric registry bean //* @param loadedFilterManager the loaded filter
   *     manager bean
   * @param samhsaMatcher the samhsa matcher bean
   * @param drugCodeDisplayLookup the drug code display lookup bean
   * @param npiOrgLookup the npi org lookup bean
   */
  public PatientClaimsEobTaskTransformerV2(
      MetricRegistry metricRegistry,
      R4EobSamhsaMatcher samhsaMatcher,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup,
      NPIOrgLookup npiOrgLookup) {
    this.metricRegistry = requireNonNull(metricRegistry);
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
   * @param lastUpdated the date range that lastUpdate falls within.
   * @param serviceDate the date range that clm_thru_dt falls within.
   * @param excludeSamhsa if true excludes SAMHSA claims.
   */
  public void setupTaskParams(
      ClaimTransformerInterfaceV2 claimTransformer,
      ClaimTypeV2 claimType,
      Long id,
      Optional<DateRangeParam> lastUpdated,
      Optional<DateRangeParam> serviceDate,
      boolean excludeSamhsa) {
    this.claimTransformer = requireNonNull(claimTransformer);
    this.claimType = requireNonNull(claimType);
    this.id = requireNonNull(id);
    this.lastUpdated = lastUpdated;
    this.serviceDate = serviceDate;
    this.excludeSamhsa = excludeSamhsa;
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
   * Sets the {@link #includeTaxNumbers} which will turn on processing of NPI tax number info.
   *
   * @param includeTaxNumbers {@link boolean} to enable/disable NPI tax number processing.
   */
  public void setIncludeTaxNumbers(boolean includeTaxNumbers) {
    this.includeTaxNumbers = includeTaxNumbers;
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
      eobs.addAll(transformToEobs(findClaimTypeByPatient()));
      if (excludeSamhsa) {
        filterSamhsa(eobs);
      }
    } catch (NoResultException e) {
      LOGGER.warn(e.getMessage(), e);
      setException(e);
    } catch (Exception e) {
      // keep track of the Exception so we can provide to caller.
      LOGGER.error(e.getMessage(), e);
      setException(e);
    }
    return this;
  }

  /**
   * Transform a list of claims to a list of {@link ExplanationOfBenefit} objects.
   *
   * @param claims the claims/events to transform
   * @return the {@link ExplanationOfBenefit} instances, one per claim/event
   */
  @Trace
  private List<ExplanationOfBenefit> transformToEobs(List<?> claims) {
    return claims.stream().map(c -> transformEobClaim(c)).collect(Collectors.toList());
  }

  /**
   * Transforms the eob claim to the specified type.
   *
   * @param claimEntity the claim entity to transform
   * @return the {@link ExplanationOfBenefit} result
   */
  @VisibleForTesting
  private ExplanationOfBenefit transformEobClaim(Object claimEntity) {
    return claimTransformer.transform(claimEntity, includeTaxNumbers);
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
   * Fetch count of EOBs that were not removed by SAMHSA filter.
   *
   * @return number of EOBs that SAMHSA filtering ignored.
   */
  public int eobsIgnoredBySamhsaFilter() {
    return samhsaIgnoredCount.get();
  }

  /**
   * Fetch count of EOBs that were removed by SAMHSA filter.
   *
   * @return number of EOBs removed by SAMHSA filter.
   */
  public int eobsRemovedBySamhsaFilter() {
    return samhsaRemovedCount.get();
  }

  /**
   * Store a {@link Exception} if the task ecnountered one.
   *
   * @param exception ecountered by task transformer.
   */
  public synchronized void setException(Exception exception) {
    taskException = exception;
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
   * Fetch the claim type string.
   *
   * @return {@link String} identifying the {@link ClaimTypeV2} of task
   */
  @Override
  public String toString() {
    return claimType.toString();
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

    if (claimEntities != null && !serviceDate.isEmpty()) {
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
   * Removes all SAMHSA-related claims from the specified {@link List} of {@link
   * ExplanationOfBenefit} resources.
   *
   * @param eobs the {@link List} of {@link ExplanationOfBenefit} resources (i.e. claims) to filter
   */
  private void filterSamhsa(List<ExplanationOfBenefit> eobs) {
    ListIterator<ExplanationOfBenefit> eobsIter = eobs.listIterator();
    // init to zero if doing SAMHSA filtering
    samhsaIgnoredCount.getAndIncrement();
    while (eobsIter.hasNext()) {
      ExplanationOfBenefit eob = eobsIter.next();
      if (samhsaMatcher.test(eob)) {
        eobsIter.remove();
        samhsaRemovedCount.getAndIncrement();
      } else {
        samhsaIgnoredCount.getAndIncrement();
      }
    }
  }
}
