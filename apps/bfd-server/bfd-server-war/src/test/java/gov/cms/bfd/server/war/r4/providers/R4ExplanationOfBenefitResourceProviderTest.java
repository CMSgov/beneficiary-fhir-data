package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.param.ReferenceParam;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  /** The mock samhsa matcher. */
  @Mock R4EobSamhsaMatcher samhsaMatcher;
  /** The mock loaded filter manager. */
  @Mock LoadedFilterManager loadedFilterManager;
  /** The mock NPI Org lookup. */
  @Mock NPIOrgLookup npiOrgLookup;
  /** The mock FDA drug display lookup. */
  @Mock FdaDrugCodeDisplayLookup drugDisplayLookup;
  /** The mock executor service. */
  @Mock ExecutorService executorService;

  /** The mock entity manager for mocking database calls. */
  @Mock EntityManager entityManager;

  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The test data bene. */
  Beneficiary testBene;

  /** The mock EOB returned from a transformer. */
  @Mock ExplanationOfBenefit testEob;

  /** The mock metric timer. */
  @Mock Timer mockTimer;

  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;

  // Mock transformers
  /** The mock transformer for carrier claims. */
  @Mock CarrierClaimTransformerV2 carrierClaimTransformer;

  /** The carrier claim returned in tests. */
  @Mock CarrierClaim testCarrierClaim;

  /** The mock transformer for dme claims. */
  @Mock DMEClaimTransformerV2 dmeClaimTransformer;

  /** The carrier claim returned in tests. */
  @Mock DMEClaim testDmeClaim;

  /** The mock transformer for hha claims. */
  @Mock HHAClaimTransformerV2 hhaClaimTransformer;

  /** The HHA claim returned in tests. */
  @Mock HHAClaim testHhaClaim;

  /** The mock transformer for hospice claims. */
  @Mock HospiceClaimTransformerV2 hospiceClaimTransformer;

  /** The hospice claim returned in tests. */
  @Mock HospiceClaim testHospiceClaim;

  /** The mock transformer for inpatient claims. */
  @Mock InpatientClaimTransformerV2 inpatientClaimTransformer;

  /** The inpatient claim returned in tests. */
  @Mock InpatientClaim testInpatientClaim;

  /** The mock transformer for outpatient claims. */
  @Mock OutpatientClaimTransformerV2 outpatientClaimTransformer;

  /** The outpatient claim returned in tests. */
  @Mock OutpatientClaim testOutpatientClaim;

  /** The mock transformer for part D events claims. */
  @Mock PartDEventTransformerV2 partDEventTransformer;

  /** The PDE claim returned in tests. */
  @Mock PartDEvent testPdeClaim;

  /** The mock transformer for snf claims. */
  @Mock SNFClaimTransformerV2 snfClaimTransformer;

  /** The SNF claim returned in tests. */
  @Mock SNFClaim testSnfClaim;

  /** The re-used valid bene id value. */
  public static String BENE_ID = "123456789";

  /** The mock EobTaskTransformer. */
  @Mock PatientClaimsEobTaskTransformerV2 taskTransformer;

  /** The mock concurrent task future. */
  @Mock Future<PatientClaimsEobTaskTransformerV2> futureTask;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
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

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    testBene =
        parsedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();

    // entity manager mocking
    mockEntityManager();

    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(mockTimer);
    when(mockTimer.time()).thenReturn(mockTimerContext);

    // transformer mocking
    when(carrierClaimTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);
    when(inpatientClaimTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);
    when(outpatientClaimTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);
    when(partDEventTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);
    when(hhaClaimTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);
    when(snfClaimTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);
    when(hospiceClaimTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);
    when(dmeClaimTransformer.transform(any(), Optional.ofNullable(anyBoolean())))
        .thenReturn(testEob);

    setupLastUpdatedMocks();

    mockHeaders();
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

    ExplanationOfBenefit eob = eobProvider.read(eobId, requestDetails);

    assertEquals(testEob, eob);
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

    // the bitmask determines which claims get returned for the test, so return three
    when(mockQuery.getResultList())
        .thenReturn(
            List.of(
                QueryUtils.V_CARRIER_HAS_DATA
                    + QueryUtils.V_SNF_HAS_DATA
                    + QueryUtils.V_DME_HAS_DATA));

    eobProvider.findByPatient(patientParam, null, null, "true", null, null, null, requestDetails);

    // we returned three claims, so we should see three samhsa filter tests
    verify(samhsaMatcher, times(3)).test(testEob);
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} with <code>
   * excludeSAMHSA=false</code> does not call the filter for SAMHSA-related claims.
   */
  @Test
  public void testFindByPatientWhenExcludeSamshaFalseExpectNoFiltering() {
    eobProvider.findByPatient(patientParam, null, null, "false", null, null, null, requestDetails);

    verify(samhsaMatcher, never()).test(testEob);
  }

  /**
   * Verifies that {@link R4ExplanationOfBenefitResourceProvider#findByPatient} passes the
   * includeTaxNumbers value to the transformer for each claim type that uses the value (currently
   * only carrier and DME).
   */
  @Test
  public void testFindByPatientIncludeTaxNumberPassedToTransformer() {

    when(requestDetails.getHeader(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS))
        .thenReturn("true");
    when(requestDetails.getHeaders(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS))
        .thenReturn(List.of("true"));

    eobProvider.findByPatient(patientParam, null, null, null, null, null, null, requestDetails);

    verify(carrierClaimTransformer, times(1)).transform(any(), eq(Optional.ofNullable(true)));
    verify(dmeClaimTransformer, times(1)).transform(any(), eq(Optional.ofNullable(true)));
  }
}
