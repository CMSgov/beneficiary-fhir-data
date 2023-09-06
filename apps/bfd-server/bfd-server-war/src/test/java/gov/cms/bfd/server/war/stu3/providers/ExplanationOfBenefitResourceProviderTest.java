package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenOrListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.param.TokenParamModifier;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.sql.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.SingularAttribute;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.IdType;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Meta;
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
 * Units tests for the {@link ExplanationOfBenefitResourceProvider} that do not require a full fhir
 * setup to validate. Anything we want to validate from the fhir client level should go in {@link
 * ExplanationOfBenefitResourceProviderE2E}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ExplanationOfBenefitResourceProviderTest {

  /** The class under test. */
  ExplanationOfBenefitResourceProvider eobProvider;
  /** ExecutorService for threading. */
  ExecutorService mockExecutorService = spy(Executors.newFixedThreadPool(2));

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

  /** The mock metric timer. */
  @Mock Timer mockTimer;
  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;

  /** The mock samhsa matcher. */
  @Mock Stu3EobSamhsaMatcher mockSamhsaMatcher;

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
  @Mock CarrierClaimTransformer mockCarrierClaimTransformer;
  /** The mock entity manager for CarrierClaim claims. */
  @Mock EntityManager carrierEntityManager;

  /** The transformer for dme claims. */
  @Mock DMEClaimTransformer mockDmeClaimTransformer;
  /** The mock entity manager for DMEClaim claims. */
  @Mock EntityManager dmeEntityManager;

  /** The transformer for Part D events. */
  @Mock PartDEventTransformer mockPdeTransformer;
  /** The mock entity manager for Part D events. */
  @Mock EntityManager pdeEntityManager;

  /** The NPI Org lookup. */
  @Mock NPIOrgLookup mockNpiOrgLookup;
  /** The FDA drug display lookup. */
  @Mock FdaDrugCodeDisplayLookup mockDrugDisplayLookup;

  /** The re-used valid bene id value. */
  public static final String BENE_ID = "123456789";

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    setupEntities();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(mockTimer);
    when(mockTimer.time()).thenReturn(mockTimerContext);
    // NPI and FDA drug mocking
    when(mockNpiOrgLookup.retrieveNPIOrgDisplay(Optional.empty())).thenReturn(Optional.of("JUNK"));
    when(mockDrugDisplayLookup.retrieveFDADrugCodeDisplay(Optional.empty())).thenReturn("JUNK");

    when(mockCarrierClaimTransformer.transform(any(), anyBoolean())).thenReturn(testEob);
    when(mockDmeClaimTransformer.transform(any(), anyBoolean())).thenReturn(testEob);
    when(mockPdeTransformer.transform(any(), anyBoolean())).thenReturn(testEob);

    // the EOB provider
    eobProvider =
        new ExplanationOfBenefitResourceProvider(
            appContext,
            metricRegistry,
            loadedFilterManager,
            executorService,
            mockCarrierClaimTransformer,
            mockDmeClaimTransformer,
            Mockito.mock(HHAClaimTransformer.class),
            Mockito.mock(HospiceClaimTransformer.class),
            Mockito.mock(InpatientClaimTransformer.class),
            Mockito.mock(OutpatientClaimTransformer.class),
            mockPdeTransformer,
            Mockito.mock(SNFClaimTransformer.class));

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
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS))
        .thenReturn("false");
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS))
        .thenReturn("false");
    // We dont use this anymore; set to false since everything should work regardless of
    // its value
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS))
        .thenReturn("false");
    when(requestDetails.getHeader("startIndex")).thenReturn("-1");

    Map<String, List<String>> headers = new HashMap<>();
    headers.put(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, List.of("false"));
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
    when(mockCriteria.subquery(any())).thenReturn(mockSubquery);
    when(mockCriteria.distinct(anyBoolean())).thenReturn(mockCriteria);
    when(mockSubquery.select(any())).thenReturn(mockSubquery);
    when(mockSubquery.from(any(Class.class))).thenReturn(root);
    when(eobEntityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
    eobProvider.setEntityManager(eobEntityManager);
  }

  /**
   * Verifies that {@link TokenAndListParam} can be parsed correctly.
   * ExplanationOfBenefitResourceProvider#parseTypeParam(TokenAndListParam)} works as expected.
   */
  @Test
  void parseTypeParam() {
    TokenAndListParam typeParamNull = null;
    Set<ClaimType> typesForNull =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamNull);
    assertEquals(ClaimType.values().length, typesForNull.size());

    TokenAndListParam typeParamSystemWildcard =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam()
                    .add(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, null));
    Set<ClaimType> typesForSystemWildcard =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSystemWildcard);
    assertEquals(ClaimType.values().length, typesForSystemWildcard.size());

    TokenAndListParam typeParamSingle =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam(
                    TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingle =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingle);
    assertEquals(1, typesForSingle.size());
    assertTrue(typesForSingle.contains(ClaimType.CARRIER));

    TokenAndListParam typeParamSingleNullSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleNullSystem =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleNullSystem);
    assertEquals(1, typesForSingleNullSystem.size());
    assertTrue(typesForSingleNullSystem.contains(ClaimType.CARRIER));

    TokenAndListParam typeParamSingleInvalidSystem =
        new TokenAndListParam().addAnd(new TokenOrListParam("foo", ClaimType.CARRIER.name()));
    Set<ClaimType> typesForSingleInvalidSystem =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleInvalidSystem);
    assertEquals(0, typesForSingleInvalidSystem.size());

    TokenAndListParam typeParamSingleInvalidCode =
        new TokenAndListParam().addAnd(new TokenOrListParam(null, "foo"));
    Set<ClaimType> typesForSingleInvalidCode =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamSingleInvalidCode);
    assertEquals(0, typesForSingleInvalidCode.size());

    TokenAndListParam typeParamTwoEntries =
        new TokenAndListParam()
            .addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name(), ClaimType.DME.name()))
            .addAnd(new TokenOrListParam(null, ClaimType.CARRIER.name()));
    Set<ClaimType> typesForTwoEntries =
        ExplanationOfBenefitResourceProvider.parseTypeParam(typeParamTwoEntries);
    assertEquals(1, typesForTwoEntries.size());
    assertTrue(typesForTwoEntries.contains(ClaimType.CARRIER));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#parseTypeParam(TokenAndListParam)}
   * throws an exception when query param modifiers are used, which are unsupported.
   */
  @Test
  void parseTypeParam_modifiers() {
    TokenAndListParam typeParam =
        new TokenAndListParam()
            .addAnd(
                new TokenOrListParam()
                    .add(
                        new TokenParam(
                                TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE,
                                ClaimType.CARRIER.name())
                            .setModifier(TokenParamModifier.ABOVE)));
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          ExplanationOfBenefitResourceProvider.parseTypeParam(typeParam);
        });
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#read} throws an exception for an
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
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
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
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
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
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
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
   * Verifies that {@link ExplanationOfBenefitResourceProvider#read} works as expected for an {@link
   * ExplanationOfBenefit} using a negative ID, which is used for synthetic data and should pass the
   * id regex.
   */
  @Test
  void testReadWhenNegativeIdExpectEobReturned() {
    when(eobId.getIdPart()).thenReturn("pde--123456789");
    when(mockQuery.getSingleResult()).thenReturn(testPdeClaim);
    when(mockPdeTransformer.transform(any(), anyBoolean())).thenReturn(testEob);
    ExplanationOfBenefit eob = eobProvider.read(eobId, requestDetails);
    assertNotNull(eob);
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#read} throws an exception when the
   * search id has an invalid id parameter.
   */
  @Test
  void testReadWhenInvalidIdExpectException() {
    when(eobId.getIdPart()).thenReturn("-1?234");
    assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * when the search id has an invalid id parameter.
   */
  @Test
  void testFindByPatientWhenInvalidIdExpectException() {
    when(patientParam.getIdPart()).thenReturn("-1?234");

    assertThrows(
        NumberFormatException.class,
        () ->
            eobProvider.findByPatient(
                patientParam, null, null, null, null, null, null, requestDetails));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} does not add paging
   * values when no paging is requested.
   */
  @Test
  void testFindByPatientWithPageSizeNotProvidedExpectNoPaging() {
    when(mockQuery.getResultList()).thenReturn(List.of(0));

    Bundle response =
        eobProvider.findByPatient(patientParam, null, null, null, null, null, null, requestDetails);

    assertNotNull(response);
    assertNull(response.getLink(Constants.LINK_NEXT));
    assertNull(response.getLink(Constants.LINK_PREVIOUS));
    assertNull(response.getLink(Constants.LINK_FIRST));
    assertNull(response.getLink(Constants.LINK_LAST));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
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
                patientParam, null, null, null, null, null, null, requestDetails));
  }

  /**
   * Tests that {@link ExplanationOfBenefitResourceProvider#findByPatient} returns a bundle of size
   * 0 if no claims are found.
   */
  @Test
  void testFindByPatientWhenNoClaimsFoundExpectEmptyBundle() {
    // mock no result when making JPA call
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);
    when(mockQuery.getResultList()).thenReturn(List.of(0));

    Bundle response =
        eobProvider.findByPatient(patientParam, null, null, null, null, null, null, requestDetails);

    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#parseTypeParam} ignores an invalid or
   * unspecified claim type.
   */
  @Test
  void testFindByPatientIgnoresInvalidClaimType() {
    TokenAndListParam listParam = createClaimsTokenAndListParam(Arrays.asList("carriers", "dme"));
    Set<ClaimType> result = ExplanationOfBenefitResourceProvider.parseTypeParam(listParam);
    assertEquals(1, result.size());
    assertFalse(result.contains(ClaimType.CARRIER)); // ignored carriers, should be carrier
    assertTrue(result.contains(ClaimType.DME));
    assertFalse(result.contains(ClaimType.SNF));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#parseTypeParam} supports null claim
   * type and returns all claim types in the EnumSet.
   */
  @Test
  void testFindByPatientSupportsNullRequestedClaimType() {
    Set<ClaimType> result = ExplanationOfBenefitResourceProvider.parseTypeParam(null);
    assertEquals(8, result.size());
    assertTrue(result.contains(ClaimType.CARRIER));
    assertTrue(result.contains(ClaimType.DME));
    assertTrue(result.contains(ClaimType.SNF));
    assertTrue(result.contains(ClaimType.PDE));
    assertTrue(result.contains(ClaimType.HHA));
    assertTrue(result.contains(ClaimType.HOSPICE));
    assertTrue(result.contains(ClaimType.INPATIENT));
    assertTrue(result.contains(ClaimType.OUTPATIENT));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} supports wildcard
   * claim type; returns empty Bundle since we return 0 for claims that have data in db.
   */
  @Test
  void testFindByPatientSupportsWildcardClaimType() {
    TokenAndListParam listParam =
        createClaimsTokenAndListParam(Arrays.asList("carriers", "dme", "*"));

    Bundle response =
        eobProvider.findByPatient(
            patientParam, listParam, null, null, null, null, null, requestDetails);

    assertNotNull(response);
    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} supports null claim
   * type; returns empty Bundle since we return 0 for claims that have data in db.
   */
  @Test
  void testFindByPatientSupportsNullClaimType() {
    Bundle response =
        eobProvider.findByPatient(patientParam, null, null, null, null, null, null, requestDetails);

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
