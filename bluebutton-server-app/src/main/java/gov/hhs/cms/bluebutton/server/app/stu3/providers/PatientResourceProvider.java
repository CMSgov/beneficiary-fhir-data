package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory_;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary_;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;

/**
 * This FHIR {@link IResourceProvider} adds support for STU3 {@link Patient}
 * resources, derived from the CCW beneficiaries.
 */
@Component
public final class PatientResourceProvider implements IResourceProvider {
	/**
	 * The {@link Identifier#getSystem()} values that are supported by
	 * {@link #searchByIdentifier(TokenParam)}.
	 */
	private static final List<String> SUPPORTED_HICN_HASH_IDENTIFIER_SYSTEMS = Arrays.asList(
			TransformerConstants.CODING_BBAPI_BENE_HICN_HASH, TransformerConstants.CODING_BBAPI_BENE_HICN_HASH_OLD);

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
		return Patient.class;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "read" operation, for {@link Patient}s. The
	 * {@link Read} annotation indicates that this method supports the read
	 * operation.
	 * </p>
	 * <p>
	 * Read operations take a single parameter annotated with {@link IdParam},
	 * and should return a single resource instance.
	 * </p>
	 * 
	 * @param patientId
	 *            The read operation takes one parameter, which must be of type
	 *            {@link IdType} and must be annotated with the {@link IdParam}
	 *            annotation.
	 * @return Returns a resource matching the specified {@link IdDt}, or
	 *         <code>null</code> if none exists.
	 */
	@Read(version = false)
	public Patient read(@IdParam IdType patientId) {
		if (patientId == null)
			throw new IllegalArgumentException();
		if (patientId.getVersionIdPartAsLong() != null)
			throw new IllegalArgumentException();

		String beneIdText = patientId.getIdPart();
		if (beneIdText == null || beneIdText.trim().isEmpty())
			throw new IllegalArgumentException();

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();

		Timer.Context timerBeneQuery = metricRegistry
				.timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_id")).time();
		CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
		Root<Beneficiary> root = criteria.from(Beneficiary.class);
		criteria.select(root);
		criteria.where(builder.equal(root.get(Beneficiary_.beneficiaryId), beneIdText));

		Beneficiary beneficiary = null;
		try {
			beneficiary = entityManager.createQuery(criteria).getSingleResult();
		} catch (NoResultException e) {
			throw new ResourceNotFoundException(patientId);
		} finally {
			timerBeneQuery.stop();
		}

		Patient patient = BeneficiaryTransformer.transform(metricRegistry, beneficiary);

		// set the meta data on the resource for when this Beneficiary resource was last
		// updated

		TransformerUtils.setMetaData(builder, entityManager,
				RifFileType.BENEFICIARY.toString(), patient, null);

		return patient;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for {@link Patient}s,
	 * allowing users to search by {@link Patient#getId()}.
	 * <p>
	 * The {@link Search} annotation indicates that this method supports the
	 * search operation. There may be many different methods annotated with this
	 * {@link Search} annotation, to support many different search criteria.
	 * </p>
	 * 
	 * @param logicalId
	 *            a {@link TokenParam} (with no system, per the spec) for the
	 *            {@link Patient#getId()} to try and find a matching
	 *            {@link Patient} for
	 * @return Returns a {@link List} of {@link Patient}s, which may contain
	 *         multiple matching resources, or may also be empty.
	 */
	@Search
	public List<Patient> searchByLogicalId(@RequiredParam(name = Patient.SP_RES_ID) TokenParam logicalId) {
		if (logicalId.getQueryParameterQualifier() != null)
			throw new InvalidRequestException(
					"Unsupported query parameter qualifier: " + logicalId.getQueryParameterQualifier());
		if (logicalId.getSystem() != null && !logicalId.getSystem().isEmpty())
			throw new InvalidRequestException("Unsupported query parameter system: " + logicalId.getSystem());
		if (logicalId.getValueNotNull().isEmpty())
			throw new InvalidRequestException("Unsupported query parameter value: " + logicalId.getValue());

		try {
			return Arrays.asList(read(new IdType(logicalId.getValue())));
		} catch (ResourceNotFoundException e) {
			return new LinkedList<>();
		}
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for {@link Patient}s,
	 * allowing users to search by {@link Patient#getIdentifier()}.
	 * Specifically, the following criteria are supported:
	 * </p>
	 * <ul>
	 * <li>Matching a {@link Beneficiary#getHicn()} hash value: when
	 * {@link TokenParam#getSystem()} matches one of the
	 * {@link #SUPPORTED_HICN_HASH_IDENTIFIER_SYSTEMS} entries.</li>
	 * </ul>
	 * <p>
	 * Searches that don't match one of the above forms are not supported.
	 * </p>
	 * <p>
	 * The {@link Search} annotation indicates that this method supports the
	 * search operation. There may be many different methods annotated with this
	 * {@link Search} annotation, to support many different search criteria.
	 * </p>
	 * 
	 * @param identifier
	 *            an {@link Identifier} {@link TokenParam} for the
	 *            {@link Patient#getIdentifier()} to try and find a matching
	 *            {@link Patient} for
	 * @return Returns a {@link List} of {@link Patient}s, which may contain
	 *         multiple matching resources, or may also be empty.
	 */
	@Search
	public List<Patient> searchByIdentifier(@RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam identifier) {
		if (identifier.getQueryParameterQualifier() != null)
			throw new InvalidRequestException(
					"Unsupported query parameter qualifier: " + identifier.getQueryParameterQualifier());

		if (!SUPPORTED_HICN_HASH_IDENTIFIER_SYSTEMS.contains(identifier.getSystem()))
			throw new InvalidRequestException("Unsupported identifier system: " + identifier.getSystem());

		try {
			return Arrays.asList(queryDatabaseByHicnHash(identifier.getValue()));
		} catch (NoResultException e) {
			return new LinkedList<>();
		}
	}

	/**
	 * @param hicnHash
	 *            the {@link Beneficiary#getHicn()} hash value to match
	 * @return a FHIR {@link Patient} for the CCW {@link Beneficiary} that
	 *         matches the specified {@link Beneficiary#getHicn()} hash value
	 * @throws NoResultException
	 *             A {@link NoResultException} will be thrown if no matching
	 *             {@link Beneficiary} can be found
	 */
	private Patient queryDatabaseByHicnHash(String hicnHash) {
		if (hicnHash == null || hicnHash.trim().isEmpty())
			throw new IllegalArgumentException();

		/*
		 * Beneficiaries' HICNs can change over time and those past HICNs may land in
		 * BeneficiaryHistory records. Accordingly, we need to search for matching HICNs
		 * in both the Beneficiary and the BeneficiaryHistory records. Once a match is
		 * found, we return the Beneficiary data for the matched `beneficiaryId`.
		 */

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		Set<String> matchingBeneficiaryIds = new HashSet<>();

		// Search the Beneficiary records for HICN matches.
		Timer.Context timerHicnQuery = metricRegistry
				.timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_hicn", "current")).time();
		CriteriaQuery<String> beneHicnQuery = builder.createQuery(String.class);
		Root<Beneficiary> beneHicnQueryRoot = beneHicnQuery.from(Beneficiary.class);
		beneHicnQuery.select(beneHicnQueryRoot.get(Beneficiary_.beneficiaryId));
		beneHicnQuery.where(builder.equal(beneHicnQueryRoot.get(Beneficiary_.hicn), hicnHash));
		matchingBeneficiaryIds.addAll(entityManager.createQuery(beneHicnQuery).getResultList());
		timerHicnQuery.stop();

		// Search the BeneficiaryHistory records for HICN matches.
		Timer.Context timerHicnHistoryQuery = metricRegistry
				.timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_hicn", "history")).time();
		CriteriaQuery<String> beneHistoryHicnQuery = builder.createQuery(String.class);
		Root<BeneficiaryHistory> beneHistoryHicnQueryRoot = beneHistoryHicnQuery.from(BeneficiaryHistory.class);
		beneHistoryHicnQuery.select(beneHistoryHicnQueryRoot.get(BeneficiaryHistory_.beneficiaryId));
		beneHistoryHicnQuery.where(builder.equal(beneHistoryHicnQueryRoot.get(BeneficiaryHistory_.hicn), hicnHash));
		matchingBeneficiaryIds.addAll(entityManager.createQuery(beneHistoryHicnQuery).getResultList());
		timerHicnHistoryQuery.stop();

		/*
		 * Because data is always dirty, we watch out for and throw an error if no
		 * matches are found or if more than one match is found.
		 */
		if (matchingBeneficiaryIds.size() <= 0) {
			throw new NoResultException();
		} else if (matchingBeneficiaryIds.size() > 1) {
			throw new NonUniqueResultException();
		}

		/*
		 * Try to pull the Beneficiary record for the (sole) matching beneficiaryId.
		 * Because the BeneficiaryHistory table doesn't have a FK to the Beneficiary
		 * table, we watch out for cases where a matching Beneficiary can't be found
		 * (again: data is always dirty).
		 */
		Beneficiary beneficiary = entityManager.find(Beneficiary.class, matchingBeneficiaryIds.iterator().next());
		if (beneficiary == null) {
			throw new NoResultException();
		}

		Patient patient = BeneficiaryTransformer.transform(metricRegistry, beneficiary);

		// set the meta data on the resource for when this Beneficiary resource was last
		// updated

		TransformerUtils.setMetaData(builder, entityManager, RifFileType.BENEFICIARY.toString(), patient, null);

		return patient;
	}
}
