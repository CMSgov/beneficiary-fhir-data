package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.PluralAttribute;
import javax.persistence.metamodel.SingularAttribute;

import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3
 * {@link ExplanationOfBenefit} resources, derived from the CCW claims.
 */
@Component
public final class ExplanationOfBenefitResourceProvider implements IResourceProvider {
	/**
	 * A {@link Pattern} that will match the
	 * {@link ExplanationOfBenefit#getId()}s used in this application.
	 */
	private static final Pattern EOB_ID_PATTERN = Pattern.compile("(\\p{Alpha}+)-(\\p{Alnum}+)");

	private EntityManager entityManager;
	private MetricRegistry metricRegistry;

	/**
	 * @param entityManager
	 *            a JPA {@link EntityManager} connected to the application's
	 *            database
	 */
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/**
	 * @param metricRegistry
	 *            the {@link MetricRegistry} to use
	 */
	@Inject
	public void setMetricRegistry(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	/**
	 * @see ca.uhn.fhir.rest.server.IResourceProvider#getResourceType()
	 */
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return ExplanationOfBenefit.class;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "read" operation, for
	 * {@link ExplanationOfBenefit}s. The {@link Read} annotation indicates that
	 * this method supports the read operation.
	 * </p>
	 * <p>
	 * Read operations take a single parameter annotated with {@link IdParam},
	 * and should return a single resource instance.
	 * </p>
	 * 
	 * @param eobId
	 *            The read operation takes one parameter, which must be of type
	 *            {@link IdType} and must be annotated with the {@link IdParam}
	 *            annotation.
	 * @return Returns a resource matching the specified {@link IdDt}, or
	 *         <code>null</code> if none exists.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Read(version = false)
	public ExplanationOfBenefit read(@IdParam IdType eobId) {
		if (eobId == null)
			throw new IllegalArgumentException();
		if (eobId.getVersionIdPartAsLong() != null)
			throw new IllegalArgumentException();

		String eobIdText = eobId.getIdPart();
		if (eobIdText == null || eobIdText.trim().isEmpty())
			throw new IllegalArgumentException();

		Matcher eobIdMatcher = EOB_ID_PATTERN.matcher(eobIdText);
		if (!eobIdMatcher.matches())
			throw new ResourceNotFoundException(eobId);
		String eobIdTypeText = eobIdMatcher.group(1);
		Optional<ClaimType> eobIdType = ClaimType.parse(eobIdTypeText);
		if (!eobIdType.isPresent())
			throw new ResourceNotFoundException(eobId);
		String eobIdClaimIdText = eobIdMatcher.group(2);

		Timer.Context timerEobQuery = metricRegistry
				.timer(MetricRegistry.name(getClass().getSimpleName(), "query", "eob_by_id")).time();
		Class<?> entityClass = eobIdType.get().getEntityClass();
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery criteria = builder.createQuery(entityClass);
		Root root = criteria.from(entityClass);
		eobIdType.get().getEntityLazyAttributes().stream().forEach(a -> root.fetch(a));
		criteria.select(root);
		criteria.where(builder.equal(root.get(eobIdType.get().getEntityIdAttribute()), eobIdClaimIdText));

		Object claimEntity = null;
		try {
			claimEntity = entityManager.createQuery(criteria).getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException(eobId);
		} finally {
			timerEobQuery.stop();
		}

		ExplanationOfBenefit eob = eobIdType.get().getTransformer().apply(metricRegistry, claimEntity);

		// set the meta data on the resource for when this Beneficiary resource was last
		// updated

		TransformerUtils.setMetaData(builder, entityManager, eobIdType.get().toString(), null, eob);

		return eob;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for
	 * {@link ExplanationOfBenefit}s, allowing users to search by
	 * {@link ExplanationOfBenefit#getPatient()}.
	 * </p>
	 * <p>
	 * The {@link Search} annotation indicates that this method supports the
	 * search operation. There may be many different methods annotated with this
	 * {@link Search} annotation, to support many different search criteria.
	 * </p>
	 * 
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find
	 *            matches for {@link ExplanationOfBenefit}s
	 * @return Returns a {@link List} of {@link ExplanationOfBenefit}s, which
	 *         may contain multiple matching resources, or may also be empty.
	 */
	@Search
	public List<ExplanationOfBenefit> findByPatient(
			@RequiredParam(name = ExplanationOfBenefit.SP_PATIENT) ReferenceParam patient) {
		String beneficiaryId = patient.getIdPart();

		/*
		 * The way our JPA/SQL schema is setup, we have to run a separate search
		 * for each claim type, then combine the results. It's not super
		 * efficient, but it's also not so inefficient that it's worth fixing.
		 */
		List<ExplanationOfBenefit> eobs = new LinkedList<>();

		eobs.addAll(transformToEobs(ClaimType.CARRIER, findClaimTypeByPatient(ClaimType.CARRIER, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.DME, findClaimTypeByPatient(ClaimType.DME, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.HHA, findClaimTypeByPatient(ClaimType.HHA, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.HOSPICE, findClaimTypeByPatient(ClaimType.HOSPICE, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.INPATIENT, findClaimTypeByPatient(ClaimType.INPATIENT, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.OUTPATIENT, findClaimTypeByPatient(ClaimType.OUTPATIENT, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.PDE, findClaimTypeByPatient(ClaimType.PDE, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.SNF, findClaimTypeByPatient(ClaimType.SNF, beneficiaryId)));
		 
		return eobs;
	}

	/**
	 * @param claimType
	 *            the {@link ClaimType} to find
	 * @param patientId
	 *            the {@link Beneficiary#getBeneficiaryId()} to filter by
	 * @return the matching claim/event entities
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private <T> List<T> findClaimTypeByPatient(ClaimType claimType, String patientId) {
		Timer.Context timerEobQuery = metricRegistry
				.timer(MetricRegistry.name(getClass().getSimpleName(), "query", "eobs", claimType.name().toLowerCase()))
				.time();
		try {
			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery criteria = criteriaBuilder.createQuery((Class) claimType.getEntityClass());
			Root root = criteria.from((Class) claimType.getEntityClass());
			claimType.getEntityLazyAttributes().stream().forEach(a -> root.fetch((PluralAttribute) a));
			criteria.select(root).distinct(true);
			criteria.where(criteriaBuilder
					.equal(root.get((SingularAttribute) claimType.getEntityBeneficiaryIdAttribute()), patientId));

			List claimEntities = entityManager.createQuery(criteria).getResultList();
			return claimEntities;
		} finally {
			timerEobQuery.stop();
		}
	}

	/**
	 * @param claimType
	 *            the {@link ClaimType} being transformed
	 * @param claims
	 *            the claims/events to transform
	 * @return the transformed {@link ExplanationOfBenefit} instances, one for each
	 *         specified claim/event
	 */
	private List<ExplanationOfBenefit> transformToEobs(ClaimType claimType, List<?> claims) {

		List<ExplanationOfBenefit> eobList = claims.stream()
				.map(c -> claimType.getTransformer().apply(metricRegistry, c))
				.collect(Collectors.toList());
		  
		// set the meta data on the resource for when this Beneficiary resource was last
		// updated
		try {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			TransformerUtils.setMetaData(builder, entityManager, claimType.toString(), null, eobList.get(0));

			for (ExplanationOfBenefit eob : eobList) {
				eob.setMeta(new Meta().setLastUpdated(eobList.get(0).getMeta().getLastUpdated()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return eobList;
		 
	}
}
