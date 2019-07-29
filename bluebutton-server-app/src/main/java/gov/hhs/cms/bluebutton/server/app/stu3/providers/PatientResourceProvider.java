package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory_;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary_;

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
	public Patient read(@IdParam IdType patientId, RequestDetails requestDetails) {
		if (patientId == null)
			throw new IllegalArgumentException();
		if (patientId.getVersionIdPartAsLong() != null)
			throw new IllegalArgumentException();

		String beneIdText = patientId.getIdPart();
		if (beneIdText == null || beneIdText.trim().isEmpty())
			throw new IllegalArgumentException();

		Timer.Context timerBeneQuery = metricRegistry
				.timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_id")).time();

		IncludeIdentifiersMode includeIdentifiersMode = IncludeIdentifiersMode
				.determineIncludeIdentifiersMode(requestDetails);

		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
		Root<Beneficiary> root = criteria.from(Beneficiary.class);
		if (includeIdentifiersMode == IncludeIdentifiersMode.INCLUDE_HICNS_AND_MBIS) {
			// For efficiency, grab these relations in the same query.
			// For security, only grab them when needed.
			root.fetch(Beneficiary_.beneficiaryHistories);
			root.fetch(Beneficiary_.medicareBeneficiaryIdHistories);
		}
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

		// Null out the unhashed HICNs and MBIs if we're not supposed to be returning
		// those.
		if (includeIdentifiersMode != IncludeIdentifiersMode.INCLUDE_HICNS_AND_MBIS) {
			beneficiary.setHicnUnhashed(Optional.empty());
			beneficiary.setMedicareBeneficiaryId(Optional.empty());
		}

		Patient patient = BeneficiaryTransformer.transform(metricRegistry, beneficiary, includeIdentifiersMode);
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
	 * @param startIndex
	 *            an {@link OptionalParam} for the startIndex (or offset) used to
	 *            determine pagination
	 * @param requestDetails
	 *            a {@link RequestDetails} containing the details of the request
	 *            URL, used to parse out pagination values
	 * @return Returns a {@link List} of {@link Patient}s, which may contain
	 *         multiple matching resources, or may also be empty.
	 */
	@Search
	public Bundle searchByLogicalId(@RequiredParam(name = Patient.SP_RES_ID) TokenParam logicalId,
			@OptionalParam(name = "startIndex") String startIndex, RequestDetails requestDetails) {
		if (logicalId.getQueryParameterQualifier() != null)
			throw new InvalidRequestException(
					"Unsupported query parameter qualifier: " + logicalId.getQueryParameterQualifier());
		if (logicalId.getSystem() != null && !logicalId.getSystem().isEmpty())
			throw new InvalidRequestException("Unsupported query parameter system: " + logicalId.getSystem());
		if (logicalId.getValueNotNull().isEmpty())
			throw new InvalidRequestException("Unsupported query parameter value: " + logicalId.getValue());

		List<IBaseResource> patients;
		try {
			patients = Arrays.asList(read(new IdType(logicalId.getValue()), requestDetails));
		} catch (ResourceNotFoundException e) {
			patients = new LinkedList<>();
		}

		PagingArguments pagingArgs = new PagingArguments(requestDetails);
		Bundle bundle = TransformerUtils.createBundle(pagingArgs, "/Patient?", Patient.SP_RES_ID, logicalId.getValue(),
				patients);
		return bundle;
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
	 * @param startIndex
	 *            an {@link OptionalParam} for the startIndex (or offset) used to
	 *            determine pagination
	 * @param requestDetails
	 *            a {@link RequestDetails} containing the details of the request
	 *            URL, used to parse out pagination values
	 * @return Returns a {@link List} of {@link Patient}s, which may contain
	 *         multiple matching resources, or may also be empty.
	 */
	@Search
	public Bundle searchByIdentifier(@RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam identifier,
			@OptionalParam(name = "startIndex") String startIndex, RequestDetails requestDetails) {
		if (identifier.getQueryParameterQualifier() != null)
			throw new InvalidRequestException(
					"Unsupported query parameter qualifier: " + identifier.getQueryParameterQualifier());

		if (!SUPPORTED_HICN_HASH_IDENTIFIER_SYSTEMS.contains(identifier.getSystem()))
			throw new InvalidRequestException("Unsupported identifier system: " + identifier.getSystem());

		List<IBaseResource> patients;
		try {
			patients = Arrays.asList(queryDatabaseByHicnHash(identifier.getValue(), requestDetails));
		} catch (NoResultException e) {
			patients = new LinkedList<>();
		}

		PagingArguments pagingArgs = new PagingArguments(requestDetails);
		Bundle bundle = TransformerUtils.createBundle(pagingArgs, "/Patient?", Patient.SP_IDENTIFIER,
				identifier.getValue(), patients);
		return bundle;
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
	private Patient queryDatabaseByHicnHash(String hicnHash, RequestDetails requestDetails) {
		if (hicnHash == null || hicnHash.trim().isEmpty())
			throw new IllegalArgumentException();

		/*
		 * Beneficiaries' HICNs can change over time and those past HICNs may land in
		 * BeneficiaryHistory records. Accordingly, we need to search for matching HICNs
		 * in both the Beneficiary and the BeneficiaryHistory records. Once a match is
		 * found, we return the Beneficiary data for the matched `beneficiaryId`.
		 *
		 * We want to run this query:
		 *
		 * SELECT
		 *     *   -- Retrieved columns are dynamic, based on JPA fetch groups and IncludeIdentifiers.
		 *   FROM "Beneficiaries"
		 *   LEFT JOIN "BeneficiariesHistory" ON "Beneficiaries"."beneficiaryId" = "BeneficiariesHistory"."beneficiaryId"
		 *   LEFT JOIN "MedicareBeneficiaryIdHistory" ON "Beneficiaries"."beneficiaryId" = "MedicareBeneficiaryIdHistory"."beneficiaryId"  -- Might be omitted, if JPA is smart enough.
		 *   WHERE
		 *     "Beneficiaries"."hicn" = $1
		 *     OR "BeneficiariesHistory"."hicn" = $1
		 *
		 *  And then, in app logic:
		 *  * Verify that it only contains a single distinct "beneficiaryId".
		 *  * Null out the unhashed identifiers if IncludeIdentifiers isn't enabled.
		 */
		
		// First, find all matching Beneficiary records.
		IncludeIdentifiersMode includeIdentifiersMode = IncludeIdentifiersMode
				.determineIncludeIdentifiersMode(requestDetails);
		Timer.Context timerHicnQuery = metricRegistry
				.timer(MetricRegistry.name(getClass().getSimpleName(), "query", "bene_by_hicn")).time();
		CriteriaBuilder builder = entityManager.getCriteriaBuilder();
		CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
		Root<Beneficiary> rootBenes = criteria.from(Beneficiary.class);
		SetJoin<Beneficiary, BeneficiaryHistory> joinHistory = rootBenes.join(Beneficiary_.beneficiaryHistories,
				JoinType.LEFT);
		if (includeIdentifiersMode == IncludeIdentifiersMode.INCLUDE_HICNS_AND_MBIS) {
			// For efficiency, grab these relations in the same query.
			// For security, only grab them when needed.
			rootBenes.fetch(Beneficiary_.beneficiaryHistories);
			rootBenes.fetch(Beneficiary_.medicareBeneficiaryIdHistories);
		}
		criteria.select(rootBenes);
		Predicate beneHicnMatches = builder.equal(rootBenes.get(Beneficiary_.hicn), hicnHash);
		Predicate beneHistoryHicnMatches = builder.equal(joinHistory.get(BeneficiaryHistory_.hicn), hicnHash);
		criteria.where(builder.or(beneHicnMatches, beneHistoryHicnMatches));
		List<Beneficiary> beneficiaries;
		try {
			beneficiaries = entityManager.createQuery(criteria).getResultList();
		} finally {
			timerHicnQuery.stop();
		}
		
		// Then, if we found more than one distinct BENE_ID, or 0, throw an error.
		long distinctBeneIds = beneficiaries.stream().map(b->b.getBeneficiaryId()).distinct().count();
		if (distinctBeneIds <= 0) {
			throw new NoResultException();
		} else if (distinctBeneIds > 1) {
			throw new NonUniqueResultException();
		}
		
		// Then, null out the HICNs and MBIs if we're not supposed to be returning
		// those.
		Beneficiary beneficiary = beneficiaries.get(0);
		if (includeIdentifiersMode != IncludeIdentifiersMode.INCLUDE_HICNS_AND_MBIS) {
			beneficiary.setHicnUnhashed(Optional.empty());
			beneficiary.setMedicareBeneficiaryId(Optional.empty());
		}

		Patient patient = BeneficiaryTransformer.transform(metricRegistry, beneficiary, includeIdentifiersMode);
		return patient;
	}

	/**
	 * Enumerates the supported "should we include unique beneficiary identifiers"
	 * options.
	 */
	public static enum IncludeIdentifiersMode {
		INCLUDE_HICNS_AND_MBIS,

		OMIT_HICNS_AND_MBIS;

		/**
		 * The header key used to determine which {@link IncludeIdentifiersMode} mode should
		 * be used. See {@link #determineIncludeIdentifiersMode(RequestDetails)} for
		 * details.
		 */
		public static final String HEADER_NAME_INCLUDE_IDENTIFIERS = "IncludeIdentifiers";

		static IncludeIdentifiersMode determineIncludeIdentifiersMode(RequestDetails requestDetails) {
			String includeIdentifiersValue = requestDetails.getHeader(HEADER_NAME_INCLUDE_IDENTIFIERS);
			if (Boolean.parseBoolean(includeIdentifiersValue) == true) {
				return INCLUDE_HICNS_AND_MBIS;
			} else {
				return OMIT_HICNS_AND_MBIS;
			}
		}
	}
}
