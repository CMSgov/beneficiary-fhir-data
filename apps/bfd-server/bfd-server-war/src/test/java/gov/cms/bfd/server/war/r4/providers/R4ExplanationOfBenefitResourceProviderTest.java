package gov.cms.bfd.server.war.r4.providers;

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
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.DMEClaim;
import gov.cms.bfd.model.rif.HHAClaim;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.commons.LoadedFilterManager;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
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
import java.util.concurrent.Future;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.persistence.metamodel.SingularAttribute;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;

/**
 * Units tests for the {@link R4ExplanationOfBenefitResourceProvider} that do not require a full
 * fhir setup to validate. Anything we want to validate from the fhir client level should go in
 * {@link R4ExplanationOfBenefitResourceProviderE2E}.
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

  /** The mock entity manager for mocking database calls. */
  @Mock EntityManager entityManager;
  /** The mock entity manager for CarrierClaim calls. */
  @Mock EntityManager carrierEntityManager;
  /** The mock entity manager for SNFClaim calls. */
  @Mock EntityManager snfEntityManager;
  /** The mock entity manager for DMEClaim calls. */
  @Mock EntityManager dmeEntityManager;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The mock EOB returned from a transformer. */
  @Mock ExplanationOfBenefit testEob;

  /** The mock metric timer. */
  @Mock Timer mockTimer;

  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;

  /** The mock samhsa matcher. */
  R4EobSamhsaMatcher samhsaMatcher;

  /** The test data bene. */
  Beneficiary testBene;
  /** The carrier claim returned in tests. */
  CarrierClaim testCarrierClaim;
  /** The carrier claim returned in tests. */
  DMEClaim testDmeClaim;
  /** The HHA claim returned in tests. */
  HHAClaim testHhaClaim;
  /** The hospice claim returned in tests. */
  HospiceClaim testHospiceClaim;
  /** The inpatient claim returned in tests. */
  InpatientClaim testInpatientClaim;
  /** The outpatient claim returned in tests. */
  OutpatientClaim testOutpatientClaim;
  /** The PDE claim returned in tests. */
  PartDEvent testPdeClaim;
  /** The SNF claim returned in tests. */
  SNFClaim testSnfClaim;

  /** The transformer for carrier claims. */
  CarrierClaimTransformerV2 carrierClaimTransformer;
  /** The transformer for dme claims. */
  DMEClaimTransformerV2 dmeClaimTransformer;
  /** The transformer for hha claims. */
  HHAClaimTransformerV2 hhaClaimTransformer;
  /** The transformer for hospice claims. */
  HospiceClaimTransformerV2 hospiceClaimTransformer;
  /** The transformer for inpatient claims. */
  InpatientClaimTransformerV2 inpatientClaimTransformer;
  /** The transformer for outpatient claims. */
  OutpatientClaimTransformerV2 outpatientClaimTransformer;
  /** The transformer for part D events claims. */
  PartDEventTransformerV2 partDEventTransformer;
  /** The transformer for snf claims. */
  SNFClaimTransformerV2 snfClaimTransformer;

  /** The NPI Org lookup. */
  @Mock NPIOrgLookup npiOrgLookup;
  /** The FDA drug display lookup. */
  static FdaDrugCodeDisplayLookup drugDisplayLookup;

  /** The re-used valid bene id value. */
  public static String BENE_ID = "123456789";

  /** The mock concurrent task future. */
  @Mock Future<PatientClaimsEobTaskTransformerV2> futureTask;

  /** one-time setup for entire test class. */
  @BeforeAll
  public static void setupFda() {
    drugDisplayLookup = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
  }

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    setupTransformers();

    samhsaMatcher = new R4EobSamhsaMatcher();

    eobProvider =
        new R4ExplanationOfBenefitResourceProvider(
            appContext,
            metricRegistry,
            loadedFilterManager,
            samhsaMatcher,
            drugDisplayLookup,
            npiOrgLookup,
            executorService,
            carrierClaimTransformer,
            dmeClaimTransformer,
            hhaClaimTransformer,
            hospiceClaimTransformer,
            inpatientClaimTransformer,
            outpatientClaimTransformer,
            partDEventTransformer,
            snfClaimTransformer);

    eobProvider.setEntityManager(entityManager);

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

    setupEntities();

    // entity manager mocking
    mockEntityManager();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(mockTimer);
    when(mockTimer.time()).thenReturn(mockTimerContext);

    // NPI and FDA drug mocking
    when(npiOrgLookup.retrieveNPIOrgDisplay(Optional.empty())).thenReturn(Optional.empty());

    setupLastUpdatedMocks();

    mockHeaders();
  }

  /** Sets up claim transformers. */
  private void setupTransformers() {
    carrierClaimTransformer =
        new CarrierClaimTransformerV2(metricRegistry, drugDisplayLookup, npiOrgLookup);
    partDEventTransformer = new PartDEventTransformerV2(metricRegistry, drugDisplayLookup);
    dmeClaimTransformer = new DMEClaimTransformerV2(metricRegistry, drugDisplayLookup);
    hhaClaimTransformer = new HHAClaimTransformerV2(metricRegistry, npiOrgLookup);
    hospiceClaimTransformer = new HospiceClaimTransformerV2(metricRegistry, npiOrgLookup);
    inpatientClaimTransformer = new InpatientClaimTransformerV2(metricRegistry, npiOrgLookup);
    outpatientClaimTransformer =
        new OutpatientClaimTransformerV2(metricRegistry, drugDisplayLookup, npiOrgLookup);
    snfClaimTransformer = new SNFClaimTransformerV2(metricRegistry, npiOrgLookup);
  }

  /** sets up claim entities. */
  private void setupEntities() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    testBene =
        (Beneficiary)
            parsedRecords.stream()
                .filter(r -> r instanceof Beneficiary)
                .map(r -> r)
                .findFirst()
                .get();
    testCarrierClaim =
        (CarrierClaim)
            parsedRecords.stream()
                .filter(r -> r instanceof CarrierClaim)
                .map(r -> r)
                .findFirst()
                .get();
    testDmeClaim =
        (DMEClaim)
            parsedRecords.stream().filter(r -> r instanceof DMEClaim).map(r -> r).findFirst().get();
    testHhaClaim =
        (HHAClaim)
            parsedRecords.stream().filter(r -> r instanceof HHAClaim).map(r -> r).findFirst().get();
    testHospiceClaim =
        (HospiceClaim)
            parsedRecords.stream()
                .filter(r -> r instanceof HospiceClaim)
                .map(r -> r)
                .findFirst()
                .get();
    testInpatientClaim =
        (InpatientClaim)
            parsedRecords.stream()
                .filter(r -> r instanceof InpatientClaim)
                .map(r -> r)
                .findFirst()
                .get();
    testOutpatientClaim =
        (OutpatientClaim)
            parsedRecords.stream()
                .filter(r -> r instanceof OutpatientClaim)
                .map(r -> r)
                .findFirst()
                .get();
    testPdeClaim =
        (PartDEvent)
            parsedRecords.stream()
                .filter(r -> r instanceof PartDEvent)
                .map(r -> r)
                .findFirst()
                .get();
    testSnfClaim =
        (SNFClaim)
            parsedRecords.stream().filter(r -> r instanceof SNFClaim).map(r -> r).findFirst().get();
  }

  /** Sets up the last updated mocks. */
  private void setupLastUpdatedMocks() {
    Meta mockMeta = mock(Meta.class);
    when(mockMeta.getLastUpdated())
        .thenReturn(Date.from(Instant.now().minus(1, ChronoUnit.SECONDS)));
    when(testEob.getMeta()).thenReturn(mockMeta);
    when(loadedFilterManager.getTransactionTime()).thenReturn(Instant.now());
  }

  /** Mocks the default header values. */
  private void mockHeaders() {
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS))
        .thenReturn("false");
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_ADDRESS_FIELDS))
        .thenReturn("false");
    // We dont use this anymore on v2, so set it to false since everything should work regardless of
    // its value
    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_IDENTIFIERS))
        .thenReturn("false");
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

    when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
    doReturn(mockCriteria).when(criteriaBuilder).createQuery(any());
    when(mockCriteria.select(any())).thenReturn(mockCriteria);
    when(mockCriteria.from(any(Class.class))).thenReturn(root);
    when(root.get(isNull(SingularAttribute.class))).thenReturn(mockPath);
    when(entityManager.createQuery(mockCriteria)).thenReturn(mockQuery);

    when(mockQuery.setHint(any(), anyBoolean())).thenReturn(mockQuery);
    when(mockQuery.setMaxResults(anyInt())).thenReturn(mockQuery);
    when(mockQuery.getSingleResult()).thenReturn(testBene);
    when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

    // Used for the check to see if claim data exists; needs a list of bitmask data
    when(mockQuery.getResultList())
        .thenReturn(
            List.of(
                QueryUtils.V_CARRIER_HAS_DATA
                    + QueryUtils.V_SNF_HAS_DATA
                    + QueryUtils.V_DME_HAS_DATA
                    + QueryUtils.V_HHA_HAS_DATA
                    + QueryUtils.V_HOSPICE_HAS_DATA
                    + QueryUtils.V_INPATIENT_HAS_DATA
                    + QueryUtils.V_OUTPATIENT_HAS_DATA
                    + QueryUtils.V_PART_D_HAS_DATA));
    when(mockCriteria.subquery(any())).thenReturn(mockSubquery);
    when(mockCriteria.distinct(anyBoolean())).thenReturn(mockCriteria);
    when(mockSubquery.select(any())).thenReturn(mockSubquery);
    when(mockSubquery.from(any(Class.class))).thenReturn(root);
    when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
    eobProvider.setEntityManager(entityManager);
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#read} throws an exception for an
   * {@link org.hl7.fhir.r4.model.IdType} that has an invalidly formatted eobId parameter.
   */
  @Test
  public void testEobReadWhereInvalidIdExpectException() {
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
   * for an {@link org.hl7.fhir.r4.model.IdType} that has a null eobId parameter.
   */
  @Test
  public void testEobReadWhereNullIdExpectException() {
    when(eobId.getIdPart()).thenReturn(null);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link org.hl7.fhir.r4.model.IdType} that has a missing eobId parameter.
   */
  @Test
  public void testEobReadWhereEmptyIdExpectException() {
    when(eobId.getIdPart()).thenReturn("");

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
    assertEquals("Missing required ExplanationOfBenefit ID", exception.getLocalizedMessage());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} throws an exception
   * for an {@link org.hl7.fhir.r4.model.IdType} that has a version supplied with the eobId
   * parameter, as our read requests do not support versioned requests.
   */
  @Test
  public void testEobReadWhereVersionedIdExpectException() {
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
  public void testReadWhenNoClaimsFoundExpectException() {
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
  public void testReadWhenNegativeIdExpectEobReturned() {
    when(eobId.getIdPart()).thenReturn("pde--123456789");
    when(mockQuery.getSingleResult()).thenReturn(testPdeClaim);

    ExplanationOfBenefit eob = eobProvider.read(eobId, requestDetails);

    assertNotNull(eob);
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#read} throws an exception when the
   * search id has an invalid id parameter.
   */
  @Test
  public void testReadWhenInvalidIdExpectException() {
    when(eobId.getIdPart()).thenReturn("-1?234");

    assertThrows(InvalidRequestException.class, () -> eobProvider.read(eobId, requestDetails));
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#findByPatient} does not add paging
   * values when no paging is requested.
   */
  @Test
  public void testFindByPatientWithPageSizeNotProvidedExpectNoPaging() {
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
  public void testFindByPatientWithNegativeStartIndexExpectException() {
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
  public void testFindByPatientWhenNoClaimsFoundExpectEmptyBundle() {
    // mock no result when making JPA call
    when(mockQuery.getSingleResult()).thenThrow(NoResultException.class);
    when(mockQuery.getResultList()).thenReturn(List.of(0));

    Bundle response =
        eobProvider.findByPatient(patientParam, null, null, null, null, null, null, requestDetails);

    assertEquals(0, response.getTotal());
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} with <code>
   * excludeSAMHSA=true</code> calls the samhsa matcher to determine if items should be removed.
   */
  @Test
  public void testFindByPatientWhenExcludeSamshaTrueExpectFilterMatcherCalled() {
    // mock CarrierClaim data
    CriteriaQuery<CarrierClaim> carrierMockCriteria = mock(CriteriaQuery.class);
    Root<CarrierClaim> carrierRoot = mock(Root.class);
    setupClaimEntity(carrierEntityManager, ClaimTypeV2.CARRIER, carrierMockCriteria, carrierRoot);

    // mock DMEClaim data
    CriteriaQuery<DMEClaim> dmeMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> dmeRoot = mock(Root.class);
    setupClaimEntity(dmeEntityManager, ClaimTypeV2.DME, dmeMockCriteria, dmeRoot);

    // mock SNFClaim data
    CriteriaQuery<SNFClaim> snfMockCriteria = mock(CriteriaQuery.class);
    Root<SNFClaim> snfRoot = mock(Root.class);
    setupClaimEntity(snfEntityManager, ClaimTypeV2.SNF, snfMockCriteria, snfRoot);

    PatientClaimsEobTaskTransformerV2 carrierTask =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, samhsaMatcher, drugDisplayLookup, npiOrgLookup);
    carrierTask.setupTaskParams(
        carrierClaimTransformer,
        ClaimTypeV2.CARRIER,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.ofNullable(true));
    carrierTask.setEntityManager(carrierEntityManager);
    carrierTask.call();
    assertTrue(carrierTask.ranSuccessfully());
    assertTrue(carrierTask.eobsRemovedBySamhsaFilter() < 1);

    PatientClaimsEobTaskTransformerV2 dmeTask =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, samhsaMatcher, drugDisplayLookup, npiOrgLookup);
    dmeTask.setupTaskParams(
        dmeClaimTransformer,
        ClaimTypeV2.DME,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.ofNullable(true));
    dmeTask.setEntityManager(dmeEntityManager);
    dmeTask.call();
    assertTrue(dmeTask.ranSuccessfully());
    assertTrue(dmeTask.eobsRemovedBySamhsaFilter() < 1);

    PatientClaimsEobTaskTransformerV2 snfTask =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, samhsaMatcher, drugDisplayLookup, npiOrgLookup);
    snfTask.setupTaskParams(
        snfClaimTransformer,
        ClaimTypeV2.SNF,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.ofNullable(true));
    snfTask.setEntityManager(snfEntityManager);
    snfTask.call();
    assertTrue(snfTask.ranSuccessfully());
    assertTrue(snfTask.eobsRemovedBySamhsaFilter() < 1);
  }

  /**
   * Verifies that processing multiple {@link PatientClaimsEobTaskTransformerV2} tasks with <code>
   * excludeSAMHSA=false</code> does not call the filter for SAMHSA-related claims.
   */
  @Test
  public void testFindByPatientWhenExcludeSamshaFalseExpectNoFiltering() {
    CriteriaQuery<CarrierClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<CarrierClaim> clmRoot = mock(Root.class);
    setupClaimEntity(carrierEntityManager, ClaimTypeV2.CARRIER, clmMockCriteria, clmRoot);

    CriteriaQuery<DMEClaim> dmeMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> dmeRoot = mock(Root.class);
    setupClaimEntity(dmeEntityManager, ClaimTypeV2.DME, dmeMockCriteria, dmeRoot);

    PatientClaimsEobTaskTransformerV2 carrierTask =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, samhsaMatcher, drugDisplayLookup, npiOrgLookup);
    carrierTask.setupTaskParams(
        carrierClaimTransformer,
        ClaimTypeV2.CARRIER,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.empty());
    carrierTask.setEntityManager(carrierEntityManager);
    carrierTask.call();
    assertTrue(carrierTask.ranSuccessfully());
    assertTrue(carrierTask.eobsRemovedBySamhsaFilter() == 0);

    PatientClaimsEobTaskTransformerV2 dmeTask =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, samhsaMatcher, drugDisplayLookup, npiOrgLookup);
    dmeTask.setupTaskParams(
        dmeClaimTransformer,
        ClaimTypeV2.DME,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.empty());
    dmeTask.setEntityManager(dmeEntityManager);
    dmeTask.call();
    assertTrue(dmeTask.ranSuccessfully());
    assertTrue(dmeTask.eobsRemovedBySamhsaFilter() == 0);
  }

  /**
   * Verifies that {@link PatientClaimsEobTaskTransformerV2} tasks successfully processes
   * includeTaxNumbers value to the transformer for each claim type that uses the value (currently
   * only carrier and DME).
   */
  @Test
  public void testFindByPatientIncludeTaxNumberPassedToTransformer() {
    CriteriaQuery<CarrierClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<CarrierClaim> clmRoot = mock(Root.class);
    setupClaimEntity(carrierEntityManager, ClaimTypeV2.CARRIER, clmMockCriteria, clmRoot);

    PatientClaimsEobTaskTransformerV2 carrierTask =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, samhsaMatcher, drugDisplayLookup, npiOrgLookup);

    carrierTask.setupTaskParams(
        carrierClaimTransformer,
        ClaimTypeV2.CARRIER,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.ofNullable(true));
    carrierTask.setEntityManager(carrierEntityManager);
    carrierTask.call();
    assertTrue(carrierTask.ranSuccessfully());

    CriteriaQuery<DMEClaim> dmeMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> dmeRoot = mock(Root.class);
    setupClaimEntity(dmeEntityManager, ClaimTypeV2.DME, dmeMockCriteria, dmeRoot);

    PatientClaimsEobTaskTransformerV2 dmeTask =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, samhsaMatcher, drugDisplayLookup, npiOrgLookup);
    dmeTask.setupTaskParams(
        dmeClaimTransformer,
        ClaimTypeV2.DME,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.ofNullable(true));
    dmeTask.setEntityManager(dmeEntityManager);
    dmeTask.call();
    assertTrue(dmeTask.ranSuccessfully());

    // test asserting that invalid task params results in a failed task
    dmeTask.setupTaskParams(
        carrierClaimTransformer,
        ClaimTypeV2.DME,
        Long.parseLong(BENE_ID),
        Optional.empty(),
        Optional.empty(),
        Optional.ofNullable(true),
        Optional.ofNullable(true));
    dmeTask.setEntityManager(dmeEntityManager);
    dmeTask.call();
    assertFalse(dmeTask.ranSuccessfully());
  }

  /**
   * Sets up mock query of a given claim type.
   *
   * @param em the {@link EntityManager} claim data to mock.
   * @param claimType the {@link ClaimTypeV2} claim data to mock.
   * @param clmMockCriteria the {@link CriteriaQuery} claim query criteria being mocked.
   * @param clmRoot the {@link Root} claim root being mocked.
   */
  private void setupClaimEntity(
      EntityManager em, ClaimTypeV2 claimType, CriteriaQuery clmMockCriteria, Root clmRoot) {
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
      case SNF:
        list = new ArrayList<SNFClaim>();
        list.add(testSnfClaim);
        break;
      default:
        break;
    }
    when(clmMockQuery.getResultList()).thenReturn(list);
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#parseTypeParam} returns a {@link
   * Set} of valid {@link ClaimTypeV2} values.
   */
  @Test
  public void testClaimTypesRequestAndMaskProcessing() {
    TokenAndListParam listParam = createClaimsTokenAndListParam(Arrays.asList("carrier", "dme"));

    Set<ClaimTypeV2> result = R4ExplanationOfBenefitResourceProvider.parseTypeParam(listParam);
    assertEquals(2, result.size());
    assertTrue(result.contains(ClaimTypeV2.CARRIER));
    assertTrue(result.contains(ClaimTypeV2.DME));
    assertFalse(result.contains(ClaimTypeV2.SNF));

    listParam = createClaimsTokenAndListParam(Arrays.asList("carrier", "dme", "snf"));
    result = R4ExplanationOfBenefitResourceProvider.parseTypeParam(listParam);
    assertEquals(3, result.size());
    assertTrue(result.contains(ClaimTypeV2.SNF));
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
