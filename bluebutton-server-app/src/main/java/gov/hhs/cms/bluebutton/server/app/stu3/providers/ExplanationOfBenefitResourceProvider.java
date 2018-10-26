package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.stereotype.Component;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import ca.uhn.fhir.model.primitive.IdDt;
import ca.uhn.fhir.model.primitive.InstantDt;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.method.RequestDetails;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IBundleProvider;
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

	private static final Integer PAGESIZE = 50; // default value for pageSize
	private static final Integer OFFSET = 0; // default value for startIndex

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
		return eob;
	}

	/**
	 * <p>
	 * Adds support for the FHIR "search" operation for
	 * {@link ExplanationOfBenefit}s, allowing users to search by
	 * {@link ExplanationOfBenefit#getPatient()}.
	 * </p>
	 * <p>
	 * The {@link Search} annotation indicates that this method supports the search
	 * operation. There may be many different methods annotated with this
	 * {@link Search} annotation, to support many different search criteria.
	 * </p>
	 * 
	 * @param patient
	 *            a {@link ReferenceParam} for the
	 *            {@link ExplanationOfBenefit#getPatient()} to try and find matches
	 *            for {@link ExplanationOfBenefit}s
	 * @param startIndex
	 * 			  an {@link OptionalParam} for the startIndex (or offset) used to 
	 * 			  determine pagination
	 * @return Returns a {@link Bundle} of {@link ExplanationOfBenefit}s, which may
	 *         contain multiple matching resources, or may also be empty.
	 */
	@Search
	public Bundle findByPatient(
			@RequiredParam(name = ExplanationOfBenefit.SP_PATIENT) ReferenceParam patient,
			@OptionalParam(name = "startIndex") String startIndex,
			RequestDetails requestDetails) {
		/*
		 * startIndex is an optional parameter here because it must be handled in the
		 * event it is passed in. However, it is not being used here because it is also
		 * contained within requestDetails and parsed out along with other parameters
		 * later.
		 */

		String beneficiaryId = patient.getIdPart();

		List<IBaseResource> eobs = new ArrayList<IBaseResource>();
		/*
		 * The way our JPA/SQL schema is setup, we have to run a separate search for
		 * each claim type, then combine the results. It's not super efficient, but it's
		 * also not so inefficient that it's worth fixing.
		 */
		eobs.addAll(transformToEobs(ClaimType.CARRIER, findClaimTypeByPatient(ClaimType.CARRIER, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.DME, findClaimTypeByPatient(ClaimType.DME, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.HHA, findClaimTypeByPatient(ClaimType.HHA, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.HOSPICE, findClaimTypeByPatient(ClaimType.HOSPICE, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.INPATIENT, findClaimTypeByPatient(ClaimType.INPATIENT, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.OUTPATIENT, findClaimTypeByPatient(ClaimType.OUTPATIENT, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.PDE, findClaimTypeByPatient(ClaimType.PDE, beneficiaryId)));
		eobs.addAll(transformToEobs(ClaimType.SNF, findClaimTypeByPatient(ClaimType.SNF, beneficiaryId)));

		EobBundleProvider bundleProvider = new EobBundleProvider(eobs);

		/*
		 * Create the bundle to return.
		 */
		Bundle bundle = createBundle(bundleProvider, beneficiaryId, requestDetails, eobs.size());
		

		/**
		 * Return a bundle which can page through and return the resources.
		 */
		return bundle;
	}

	/**
	 * @param bundleProvider
	 *            the {@link EobBundleProvider} from which to return a bundle
	 * @param beneficiaryId
	 *            the {@link Beneficiary#getBeneficiaryId()} to include in the links
	 * @param requestDetails
	 *            the {@link RequestDetails} containing additional parameters for
	 *            the URL in need of parsing out
	 * @param numTotalResults
	 *            the number of total results returned by the query matching the
	 *            {@link Beneficiary#getBeneficiaryId()}, needed to help determine
	 *            pagination
	 * @return Returns a {@link Bundle} of {@link ExplanationOfBenefit}s, which may
	 *         contain multiple matching resources, or may also be empty.
	 */
	private Bundle createBundle(EobBundleProvider bundleProvider, String beneficiaryId, RequestDetails requestDetails,
			int numTotalResults) {
		/*
		 * Parse parameters from RequestDetails.
		 */
		Integer pageSize = parseParameterFromRequestDetails(requestDetails, "_count");
		if (pageSize == null) {
			pageSize = PAGESIZE;
		}

		Integer startIndex = parseParameterFromRequestDetails(requestDetails, "startIndex");
		if (startIndex == null) {
			startIndex = OFFSET;
		}

		String serverBase = requestDetails.getServerBaseForRequest();

		/*
		 * Determine which resources to return based on the pageSize and startIndex.
		 */
		int numToReturn;
		List<IBaseResource> resourceList;
		if (pageSize == null || pageSize.equals(Integer.valueOf(0))) {
			numToReturn = numTotalResults;
		} else {
			numToReturn = Math.min(pageSize, numTotalResults);
		}

		if (numToReturn > 0) {
			resourceList = bundleProvider.getResources(startIndex, numToReturn + startIndex);
		} else {
			resourceList = Collections.emptyList();
		}

		/*
		 * Create the bundle and add the resources.
		 */
		Bundle bundle = new Bundle();
		for (IBaseResource res : resourceList) {
			BundleEntryComponent entry = bundle.addEntry();
			entry.setResource((Resource) res);
		}

		/*
		 * Create the paging links and add them to the bundle.
		 */
		addPagingLinks(bundle, serverBase, beneficiaryId, startIndex, pageSize, numToReturn, numTotalResults);

		return bundle;
	}

	/**
	 * @param bundle
	 *            the {@link Bundle} to which links are being added
	 * @param serverBase
	 *            the serverBase for the request
	 * @param beneficiaryId
	 *            the {@link Beneficiary#getBeneficiaryId()} to include in the links
	 * @param startIndex
	 *            the index for the resources at which to begin the next page
	 * @param pageSize
	 *            the total number of resources contained on a page
	 * @param numToReturn
	 *            the number of resources to return for the current page
	 * @param numTotalResults
	 *            the number of total resources matching the
	 *            {@link Beneficiary#getBeneficiaryId()} *
	 */
	private void addPagingLinks(Bundle bundle, String serverBase, String beneficiaryId, Integer startIndex,
			Integer pageSize, int numToReturn, int numTotalResults) {

		// Link to the next page.
		if (startIndex + numToReturn < numTotalResults) {
			bundle.addLink(new BundleLinkComponent().setRelation(Bundle.LINK_NEXT)
					.setUrl(createPagingLink(serverBase, beneficiaryId, startIndex + numToReturn, numToReturn)));
		}

		// Link to the previous page.
		if (startIndex > 0) {
			int start = Math.max(0, startIndex - pageSize);
			bundle.addLink(new BundleLinkComponent().setRelation(Bundle.LINK_PREV)
					.setUrl(createPagingLink(serverBase, beneficiaryId, start, pageSize)));
		}

		/*
		 * TODO Create a link to the last page. Currently Bundle.LINK_LAST is not
		 * supported and it does not appear to like setRelation("last").
		 */
	}

	/**
	 * @param requestDetails
	 *            the {@link RequestDetails} containing additional parameters for
	 *            the URL in need of parsing out
	 * @param parameterToParse
	 *            the parameter to parse from requestDetails
	 * @return Returns the parsed parameter as an Integer, null if the parameter is
	 *         not found.
	 */
	private Integer parseParameterFromRequestDetails(RequestDetails requestDetails, String parameterToParse) {
		if (requestDetails.getParameters().containsKey(parameterToParse)) {
			return Integer.parseInt(requestDetails.getParameters().get(parameterToParse)[0]);
		}

		return null;
	}

	/**
	 * @return Returns the URL string for a paging link.
	 */
	private String createPagingLink(String theServerBase, String patientId, int startIndex, int theCount) {
		StringBuilder b = new StringBuilder();
		b.append(theServerBase + "/ExplanationOfBenefit?");
		b.append("_count=" + theCount);
		b.append("&startIndex=" + startIndex);
		b.append("&patient=" + patientId);

		return b.toString();
	}

	/**
	 * @param claimType
	 *            the {@link ClaimType} to find
	 * @param patientId
	 *            the {@link Beneficiary#getBeneficiaryId()} to filter by
	 * @return the matching claim/event entities
	 */
	@SuppressWarnings({ "rawtypes", "unchecked"})
	private <T> List<T> findClaimTypeByPatient(ClaimType claimType, String patientId) {
		Timer.Context timerEobQuery = metricRegistry.timer(MetricRegistry
				.name(metricRegistry.getClass().getSimpleName(), "query", "eobs", claimType.name().toLowerCase()))
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
		return claims.stream().map(c -> claimType.getTransformer().apply(metricRegistry, c))
				.collect(Collectors.toList());
	}

	/**
	 * This FHIR {@link IBundleProvider} contains all resources matching the search
	 * sorted by claim ID the by claim type. Returns sublist of those resources,
	 * useful for paging.
	 */
	final static class EobBundleProvider implements IBundleProvider {

		private List<IBaseResource> eobs = new ArrayList<IBaseResource>();
		private InstantDt published = InstantDt.withCurrentTime();

		public EobBundleProvider(List<IBaseResource> eobs) {

			this.eobs = eobs;

			/*
			 * In order for paging to be meaningful (and stable), the claims have to be
			 * consistently sorted across different app server instances (in case page 1
			 * comes from Server A but page 2 comes from Server B). Right now, we don't have
			 * anything "useful" to sort by, so we just sort by claim ID (subsorted by claim
			 * type). TODO once we have metadata from BLUEBUTTON-XXX on when each claim was
			 * first loaded into our DB, we should sort by that.
			 */
			eobs.sort(EobBundleProvider::compareByClaimIdThenClaimType);
		}
		
		private static int compareByClaimIdThenClaimType(IBaseResource res1, IBaseResource res2) {
			if (!(res1 instanceof ExplanationOfBenefit))
				throw new IllegalArgumentException();
			if (!(res2 instanceof ExplanationOfBenefit))
				throw new IllegalArgumentException();
			ExplanationOfBenefit eob1 = (ExplanationOfBenefit) res1;
			ExplanationOfBenefit eob2 = (ExplanationOfBenefit) res2;
			if (TransformerUtils.getUnprefixedClaimId(eob1) == TransformerUtils.getUnprefixedClaimId(eob2)) {
				return TransformerUtils.getClaimType(eob1).compareTo(TransformerUtils.getClaimType(eob2));
			} else {
				return TransformerUtils.getUnprefixedClaimId(eob1)
						.compareTo(TransformerUtils.getUnprefixedClaimId(eob2));
			}
		}

		@Override
		public String getUuid() {
			return null;
		}

		@Override
		public IPrimitiveType<Date> getPublished() {
			return published;
		}

		@Override
		public Integer preferredPageSize() {
			return 10;
		}

		@Override
		public Integer size() {
			return eobs.size();
		}

		@Override
		public List<IBaseResource> getResources(int theFromIndex, int theToIndex) {
			int end = Math.min(theToIndex, eobs.size());
			return eobs.subList(theFromIndex, end);
		}
	}
}
