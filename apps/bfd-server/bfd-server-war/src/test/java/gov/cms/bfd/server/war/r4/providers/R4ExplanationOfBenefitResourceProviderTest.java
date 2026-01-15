package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.SecurityTagsDao;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

/**
 * Units tests for the {@link R4ExplanationOfBenefitResourceProvider} that do not require a full
 * fhir setup to validate. Anything we want to validate from the fhir client level should go in
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class R4ExplanationOfBenefitResourceProviderTest {

  /** The class under test. */
  R4ExplanationOfBenefitResourceProvider eobProvider;

  /** The mocked request details. */
  @Mock ServletRequestDetails requestDetails;

  /** The mocked input id value. */
  @Mock IdType eobId;

  /** The mock spring application context. */
  @Mock ApplicationContext appContext;

  /** The mocked reference param used for search by patient. */
  @Mock ReferenceParam patientParam;

  /** The mock metric registry. */
  @Mock MetricRegistry metricRegistry;

  /** The mock loaded filter manager. */
  @Mock LoadedFilterManager loadedFilterManager;

  /** The mock executor service. */
  @Mock ExecutorService executorService;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The mock EOB returned from a transformer. */
  @Mock ExplanationOfBenefit testEob;

  /** The mock Bundle returned from a transformer. */
  Bundle testBundle;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The test data bene. */
  Beneficiary testBene;

  /** The carrier claim returned in tests. */
  CarrierClaim testCarrierClaim;

  /** The carrier claim returned in tests. */
  DMEClaim testDmeClaim;

  /** The carrier claim returned in tests. */
  PartDEvent testPdeClaim;

  /** The mock entity manager for mocking database calls. */
  @Mock EntityManager eobEntityManager;

  /** The transformer for carrier claims. */
  @Mock CarrierClaimTransformerV2 mockCarrierClaimTransformer;

  /** The transformer for dme claims. */
  @Mock DMEClaimTransformerV2 mockDmeClaimTransformer;

  /** The transformer for Part D events. */
  @Mock PartDEventTransformerV2 mockPdeTransformer;

  /** The NPI Org lookup. */
  @Mock NPIOrgLookup mockNpiOrgLookup;

  /** The re-used valid bene id value. */
  public static final String BENE_ID = "123456789";

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    setupEntities();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    when(mockCarrierClaimTransformer.transform(any())).thenReturn(testEob);
    when(mockDmeClaimTransformer.transform(any())).thenReturn(testEob);
    when(mockPdeTransformer.transform(any())).thenReturn(testEob);

    when(requestDetails.getAttribute(CommonTransformerUtils.SHOULD_FILTER_SAMHSA)).thenReturn(true);

    // the EOB provider
    eobProvider =
        new R4ExplanationOfBenefitResourceProvider(
            appContext,
            metricRegistry,
            loadedFilterManager,
            executorService,
            mockCarrierClaimTransformer,
            mockDmeClaimTransformer,
            Mockito.mock(HHAClaimTransformerV2.class),
            Mockito.mock(HospiceClaimTransformerV2.class),
            Mockito.mock(InpatientClaimTransformerV2.class),
            Mockito.mock(OutpatientClaimTransformerV2.class),
            mockPdeTransformer,
            Mockito.mock(SNFClaimTransformerV2.class),
            Mockito.mock(SecurityTagsDao.class),
            RDATestUtils.createTestNpiOrgLookup(),
            RDATestUtils.createFdaDrugCodeDisplayLookup());

    // entity manager mocking
    mockEntityManager();

    lenient().when(eobId.getVersionIdPartAsLong()).thenReturn(null);
    when(eobId.getIdPart()).thenReturn("carrier-" + BENE_ID);
    Identifier mockId = mock(Identifier.class);
    when(mockId.getSystem()).thenReturn("clm_id");
    when(mockId.getValue()).thenReturn(BENE_ID);
    when(testEob.getIdentifier()).thenReturn(List.of(mockId));

    // used to get the claim type in transformer utils
    CodeableConcept mockConcept = mock(CodeableConcept.class);
    Coding mockCoding = mock(Coding.class);
    when(mockCoding.getSystem()).thenReturn(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE);
    when(mockConcept.getCoding()).thenReturn(List.of(mockCoding));
    when(mockCoding.getCode()).thenReturn("CARRIER");

    when(testEob.getType()).thenReturn(mockConcept);
    when(patientParam.getIdPart()).thenReturn(BENE_ID);

    Bundle mockBundle = mock(Bundle.class);

    // Sets up the last updated mocks.
    Meta mockMeta = mock(Meta.class);
    when(mockMeta.getLastUpdated())
        .thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    when(testEob.getMeta()).thenReturn(mockMeta);
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());

    mockHeaders();
  }

  /** sets up claim entities. */
  private void setupEntities() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    testBene =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(Beneficiary.class::cast)
            .findFirst()
            .get();

    testCarrierClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();

    testDmeClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(DMEClaim.class::cast)
            .findFirst()
            .get();

    testPdeClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(PartDEvent.class::cast)
            .findFirst()
            .get();
  }

  /** Mocks the default header values. */
  private void mockHeaders() {
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS))
        .thenReturn("false");
    // We dont use this anymore; set to false since everything should work regardless of
    // its value
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS))
        .thenReturn("false");
    when(requestDetails.getHeader("startIndex")).thenReturn("-1");

    Map<String, List<String>> headers = new HashMap<>();
    headers.put(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, List.of("false"));
    headers.put(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS, List.of("false"));
    when(requestDetails.getHeaders()).thenReturn(headers);
    when(requestDetails.getCompleteUrl()).thenReturn("test");
  }

  /** Sets up the default entity manager mocks. */
  private void mockEntityManager() {
    CriteriaBuilder criteriaBuilder = mock(CriteriaBuilder.class);
    CriteriaQuery<Beneficiary> mockCriteria = mock(CriteriaQuery.class);
    Root<Beneficiary> root = mock(Root.class);
    Path mockPath = mock(Path.class);
    Subquery mockSubquery = mock(Subquery.class);

    when(eobEntityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    doReturn(mockCriteria).when(criteriaBuilder).createQuery(any());
    when(mockCriteria.select(any())).thenReturn(mockCriteria);
    when(mockCriteria.from(any(Class.class))).thenReturn(root);
    when(root.get(isNull(SingularAttribute.class))).thenReturn(mockPath);
    when(eobEntityManager.createQuery(mockCriteria)).thenReturn(mockQuery);

    when(mockQuery.setHint(any(), anyBoolean())).thenReturn(mockQuery);
    when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
    when(mockQuery.getSingleResult()).thenReturn(testBene);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

    // Used for the check to see if claim data exists; needs a list of bitmask data
    when(mockQuery.getResultList()).thenReturn(List.of(0));
    when(mockCriteria.subquery(any(Class.class))).thenReturn(mockSubquery);
    when(mockCriteria.distinct(anyBoolean())).thenReturn(mockCriteria);
    when(mockSubquery.select(any())).thenReturn(mockSubquery);
    when(mockSubquery.from(any(Class.class))).thenReturn(root);
    when(eobEntityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
    eobProvider.setEntityManager(eobEntityManager);
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#read} throws an exception for an
   * {@link org.hl7.fhir.dstu3.model.IdType} that has an unsupported claim type.
   */
  @Test
  void testEobReadWhereInvalidClaimTypeInIdExpectException() {
    when(eobId.getIdPart()).thenReturn("foo-1234");

    assertThrows(ResourceNotFoundException.class, () -> eobProvider.read(eobId, requestDetails));
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#read} throws an exception for an
   * {@link IdType} that has an invalidly formatted eobId parameter.
   */
  @Test
  void testEobReadWhereInvalidIdExpectException() {
    when(eobId.getIdPart()).thenReturn("1234");

    // Parameter is invalid, should throw exception
    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals(
        "ExplanationOfBenefit ID pattern: '1234' does not match expected pattern: {alphaString}-{idNumber}",
        exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link IdType} that has a null eobId parameter.
   */
  @Test
  void testEobReadWhereNullIdExpectException() {
    when(eobId.getIdPart()).thenReturn(null);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link IdType} that has a missing eobId parameter.
   */
  @Test
  void testEobReadWhereEmptyIdExpectException() {
    when(eobId.getIdPart()).thenReturn("");

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link IdType} that has a version supplied with the eobId parameter, as our read
   * requests do not support versioned requests.
   */
  @Test
  void testEobReadWhereVersionedIdExpectException() {
    when(eobId.getVersionIdPartAsLong()).thenReturn(1234L);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals(
        "ExplanationOfBenefit ID must not define a version", exception.getLocalizedMessage());
  }

  /**
   * Test when reading a claim, if the claim is not found, a {@link ResourceNotFoundException} is
   * thrown.
   */
  @Test
  void testReadWhenNoClaimsFoundExpectException() {
    // mock no result when making JPA call
    when(mockQuery.getSingleResult()).thenThrow(new NoResultException());

    assertThrows(ResourceNotFoundException.class, () -> eobProvider.read(eobId, requestDetails));
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#read} works as expected for an
   * {@link ExplanationOfBenefit} using a negative ID, which is used for synthetic data and should
   * pass the id regex.
   */
  @Test
  void testReadWhenNegativeIdExpectEobReturned() {
    when(eobId.getIdPart()).thenReturn("pde--123456789");
    when(mockQuery.getSingleResult()).thenReturn(testPdeClaim);
    when(mockPdeTransformer.transform(any())).thenReturn(testEob);
    ExplanationOfBenefit eob = eobProvider.read(eobId, requestDetails);
    assertNotNull(eob);
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#read} starts and stops its metrics.
   */
  @Test
  void testReadWhenValidIdExpectMetrics() {
    when(eobId.getIdPart()).thenReturn("pde-123456789");
    when(mockQuery.getSingleResult()).thenReturn(testPdeClaim);
    when(mockPdeTransformer.transform(any())).thenReturn(testEob);
    eobProvider.read(eobId, requestDetails);

    String expectedTimerName = eobProvider.getClass().getSimpleName() + ".query.eob_by_id";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#read} throws an exception when the
   * search id has an invalid id parameter.
   */
  @Test
  void testReadWhenInvalidIdExpectException() {
    when(eobId.getIdPart()).thenReturn("-1?234");
    assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * when the search id has an invalid id parameter.
   */
  @Test
  void testFindByPatientWhenInvalidIdExpectException() {
    when(patientParam.getIdPart()).thenReturn("-1?234");

    assertThrows(
        InvalidRequestException.class,
        () ->
            eobProvider.findByPatient(
                patientParam, null, null, null, null, null, null, null, requestDetails));
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} does not add paging
   * values when no paging is requested.
   */
  @Test
  void testFindByPatientWithPageSizeNotProvidedExpectNoPaging() {
    when(mockQuery.getResultList()).thenReturn(List.of(0));

    Bundle response =
        eobProvider.findByPatient(
            patientParam, null, null, null, null, null, null, null, requestDetails);

    assertNotNull(response);
    assertNull(response.getLink(Constants.LINK_NEXT));
    assertNull(response.getLink(Constants.LINK_PREVIOUS));
    assertNull(response.getLink(Constants.LINK_FIRST));
    assertNull(response.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * when using negative values for page size and start index parameters. This test expects to
   * receive a InvalidRequestException, as negative values should result in an HTTP 400.
   */
  @Test
  void testFindByPatientWithNegativeStartIndexExpectException() {
    // Set paging params to page size 0
    Map<String, String[]> params = new HashMap<>();
    params.put("startIndex", new String[] {"-1"});
    when(requestDetails.getParameters()).thenReturn(params);

    assertThrows(
        InvalidRequestException.class,
        () ->
            eobProvider.findByPatient(
                patientParam, null, null, null, null, null, null, null, requestDetails));
  }

  /**
   * Tests that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} returns a bundle of
   * size 0 if no claims are found.
   */
  @Test
  void testFindByPatientWhenNoClaimsFoundExpectEmptyBundle() {
    // mock no result when making JPA call
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);
    when(mockQuery.getResultList()).thenReturn(List.of(0));

    Bundle response =
        eobProvider.findByPatient(
            patientParam, null, null, null, null, null, null, null, requestDetails);

    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} supports wildcard
   * claim type; returns empty Bundle since we return 0 for claims that have data in db.
   */
  @Test
  void testFindByPatientSupportsWildcardClaimTypeV2() {
    TokenAndListParam listParam =
        createClaimsTokenAndListParam(Arrays.asList("carriers", "dme", "*"));

    Bundle response =
        eobProvider.findByPatient(
            patientParam, listParam, null, null, null, null, null, null, requestDetails);

    assertNotNull(response);
    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} supports null claim
   * type; returns empty Bundle since we return 0 for claims that have data in db.
   */
  @Test
  void testFindByPatientSupportsNullClaimTypeV2() {
    Bundle response =
        eobProvider.findByPatient(
            patientParam, null, null, null, null, null, null, null, requestDetails);

    assertNotNull(response);
    assertEquals(0, response.getTotal());
  }

  /**
   * Sets up mock query of a given claim type.
   *
   * @param em the {@link EntityManager} claim data to mock.
   * @param claimType the {@link ClaimType} claim data to mock.
   * @param clmMockCriteria the {@link CriteriaQuery} claim query criteria being mocked.
   * @param clmRoot the {@link Root} claim root being mocked.
   */
  private void setupClaimEntity(
      EntityManager em, ClaimType claimType, CriteriaQuery clmMockCriteria, Root clmRoot) {
    CriteriaBuilder clmCriteriaBuilder = mock(CriteriaBuilder.class);
    Path clmMockPath = mock(Path.class);
    TypedQuery clmMockQuery = mock(TypedQuery.class);

    when(em.getCriteriaBuilder()).thenReturn(clmCriteriaBuilder);
    doReturn(clmMockCriteria).when(clmCriteriaBuilder).createQuery(any());
    when(clmMockCriteria.select(any())).thenReturn(clmMockCriteria);
    when(clmMockCriteria.from(any(Class.class))).thenReturn(clmRoot);
    when(clmRoot.get(isNull(SingularAttribute.class))).thenReturn(clmMockPath);
    when(em.createQuery(clmMockCriteria)).thenReturn(clmMockQuery);
    when(clmMockQuery.setHint(any(), anyBoolean())).thenReturn(clmMockQuery);
    when(clmMockQuery.setMaxResults(anyInt())).thenReturn(clmMockQuery);
    when(clmMockQuery.setParameter(anyString(), any())).thenReturn(clmMockQuery);
    when(clmMockCriteria.distinct(anyBoolean())).thenReturn(clmMockCriteria);

    List list = null;
    switch (claimType) {
      case CARRIER:
        list = new ArrayList<CarrierClaim>();
        list.add(testCarrierClaim);
        break;
      case DME:
        list = new ArrayList<DMEClaim>();
        list.add(testDmeClaim);
        break;
      case PDE:
        list = new ArrayList<PartDEvent>();
        list.add(testPdeClaim);
        break;
      default:
        break;
    }
    when(clmMockQuery.getResultList()).thenReturn(list);
  }

  /**
   * Helper routine to build a FHIR {@link TokenAndListParam}.
   *
   * @param claimsList a {@link List} of claim {@link String} identifiers
   * @return a {@link TokenAndListParam}
   */
  private TokenAndListParam createClaimsTokenAndListParam(List<String> claimsList) {
    TokenAndListParam listParam = new TokenAndListParam();
    if (claimsList.isEmpty()) {
      return listParam;
    }
    TokenOrListParam orParam =
        new TokenOrListParam(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE);
    for (String val : claimsList) {
      orParam.add(val.toLowerCase());
    }
    listParam.addAnd(orParam);
    return listParam;
  }
}
