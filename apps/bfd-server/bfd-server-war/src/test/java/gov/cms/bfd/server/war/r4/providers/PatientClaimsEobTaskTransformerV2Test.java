package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.SamhsaV2InterceptorShadow;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.SecurityTagsDao;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.SingularAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Units tests for the {@link PatientClaimsEobTaskTransformerV2Test}. Basically verifying that all
 * the task transformers work and return an EOB as well as some pertinent info such as SAMHSA
 * processing information.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientClaimsEobTaskTransformerV2Test {
  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The mock metric registry. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  /** The NPI Org lookup. */
  @Mock NPIOrgLookup mockNpiOrgLookup;

  /** The SamhsaSecurityTag lookup. */
  @Mock SecurityTagManager securityTagManager;

  /** The mock samhsa matcher. */
  @Mock R4EobSamhsaMatcher mockSamhsaMatcher;

  /** v2SamhsaConsentSimulation. */
  @Mock SamhsaV2InterceptorShadow samhsaV2InterceptorShadow;

  /** The Security Tags Dao. */
  @Mock SecurityTagsDao securityTagsDao;

  /** The carrier claim returned in tests. */
  CarrierClaim testCarrierClaim;

  /** The DME claim returned in tests. */
  DMEClaim testDmeClaim;

  /** The HHA claim returned in tests. */
  HHAClaim testHhaClaim;

  /** The Hospice claim returned in tests. */
  HospiceClaim testHospiceClaim;

  /** The Inpatient claim returned in tests. */
  InpatientClaim testInpatientClaim;

  /** The Outpatient claim returned in tests. */
  OutpatientClaim testOutpatientClaim;

  /** The Part D Event returned in tests. */
  PartDEvent testPdeClaim;

  /** The SNF claim returned in tests. */
  SNFClaim testSnfClaim;

  @Mock SingularAttribute mockAttr;

  /** The mock entity manager for mocking database calls. */
  @Mock EntityManager mockEntityManager;

  /** The mock npi lookup. */
  @Mock NPIOrgLookup mockNpiTaxonomyLookup;

  /** Sets up the test class. */
  @BeforeEach
  public void setup() {
    setupEntities();
    NPIData npiData =
        NPIData.builder()
            .npi("0000000000")
            .taxonomyCode("207X00000X")
            .taxonomyDisplay("Orthopaedic Surgery")
            .build();
    // metrics mocking
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);
    // used to get the claim type in transformer utils
    CodeableConcept mockConcept = mock(CodeableConcept.class);
    Coding mockCoding = mock(Coding.class);
    when(mockCoding.getSystem()).thenReturn(TransformerConstants.CODING_SYSTEM_BBAPI_EOB_TYPE);
    when(mockConcept.getCoding()).thenReturn(List.of(mockCoding));
    when(mockCoding.getCode()).thenReturn("CARRIER");
  }

  /** sets up claim entities. */
  private void setupEntities() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

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

    testHhaClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(HHAClaim.class::cast)
            .findFirst()
            .get();

    testHospiceClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(HospiceClaim.class::cast)
            .findFirst()
            .get();

    testInpatientClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(InpatientClaim.class::cast)
            .findFirst()
            .get();

    testOutpatientClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(OutpatientClaim.class::cast)
            .findFirst()
            .get();
    testPdeClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(PartDEvent.class::cast)
            .findFirst()
            .get();

    testSnfClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(SNFClaim.class::cast)
            .findFirst()
            .get();
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * CarrierClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs no SAMHSA
   * EOB filtering.
   */
  @Test
  void testTaskTransformerUsingCarrierClaimNoSamhsaFilter() {
    CriteriaQuery<CarrierClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<CarrierClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.CARRIER, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new CarrierClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.CARRIER, 1234L, Optional.empty(), Optional.empty(), false);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertFalse(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(-1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.CARRIER);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * CarrierClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs SAMHSA EOB
   * filtering.
   */
  @Test
  void testTaskTransformerUsingCarrierClaimWithSamhsa() {
    CriteriaQuery<CarrierClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<CarrierClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.CARRIER, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new CarrierClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.CARRIER, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.CARRIER);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * DMEClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs no SAMHSA EOB
   * filtering.
   */
  @Test
  void testTaskTransformerUsingDmeClaimNoSamhsaFilter() {
    CriteriaQuery<DMEClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.DME, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new DMEClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.DME, 1234L, Optional.empty(), Optional.empty(), false);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertFalse(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(-1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.DME);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * DMEClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs SAMHSA EOB
   * filtering.
   */
  @Test
  void testTaskTransformerUsingDmeClaimWithSamhsa() {
    CriteriaQuery<DMEClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.DME, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new DMEClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.DME, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.DME);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * HHAClaim} entity into a {@link ExplanationOfBenefit} FHIR resource;performs SAMHSA filtering
   * flag when set to true.
   */
  @Test
  void testTaskTransformerUsingHhaClaimWithSamhsa() {
    CriteriaQuery<HHAClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<HHAClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.HHA, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new HHAClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.HHA, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.HHA);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * HospiceClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA
   * filtering flag when set to true.
   */
  @Test
  void testTaskTransformerUsingHospiceClaimWithSamhsa() {
    CriteriaQuery<HospiceClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<HospiceClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.HOSPICE, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new HospiceClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.HOSPICE, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.HOSPICE);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * InpatientClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA
   * filtering flag when set to true.
   */
  @Test
  void testTaskTransformerUsingInpatientClaimWithSamhsa() {
    CriteriaQuery<InpatientClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<InpatientClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.INPATIENT, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new InpatientClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.INPATIENT, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.INPATIENT);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * OutpatientClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA
   * filtering flag when set to true.
   */
  @Test
  void testTaskTransformerUsingOutpatientClaimWithSamhsa() {
    CriteriaQuery<OutpatientClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<OutpatientClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.OUTPATIENT, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new OutpatientClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.OUTPATIENT, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.OUTPATIENT);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * PartDEvent} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA filtering
   * flag when set to true.
   */
  @Test
  void testTaskTransformerUsingPartDEventWithSamhsa() {
    CriteriaQuery<PartDEvent> clmMockCriteria = mock(CriteriaQuery.class);
    Root<PartDEvent> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.PDE, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new PartDEventTransformerV2(new MetricRegistry());
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.PDE, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.PDE);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} sucessfully transforms a {@link
   * SNFClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA filtering
   * flag when set to true.
   */
  @Test
  void testTaskTransformerUsingSnfClaimWithSamhsa() {
    CriteriaQuery<SNFClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<SNFClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.SNF, clmMockCriteria, clmRoot);

    // Ignore metrics registry calls on the claim transformer; its not under test here
    ClaimTransformerInterfaceV2 claimTransformer =
        new SNFClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.SNF, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
    verifyMetrics(ClaimType.SNF);
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformerV2} can handle internal processing {@link
   * Exception} and can return it to the caller via its {@link
   * PatientClaimsEobTaskTransformerV2#getFailure()} method. The test sets up a {@link DMEClaim}
   * entity but invokes the task setup to process the entity using a {@link SNFClaimTransformerV2};
   * this mis-match triggers a {@link BadCodeMonkeyException} which the caller can retrieve.
   */
  @Test
  void testGetFailureFromTaskTransformer() {
    // purposely setup a DME clam
    CriteriaQuery<DMEClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.DME, clmMockCriteria, clmRoot);

    ClaimTransformerInterfaceV2 claimTransformer =
        new SNFClaimTransformerV2(metricRegistry, securityTagManager, false);
    PatientClaimsEobTaskTransformerV2 taskTransformer =
        new PatientClaimsEobTaskTransformerV2(
            metricRegistry, mockSamhsaMatcher, samhsaV2InterceptorShadow, securityTagsDao, false);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.DME, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformerV2 rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertFalse(taskTransformer.ranSuccessfully());
    assertTrue(taskTransformer.getFailure().orElseThrow() instanceof BadCodeMonkeyException);
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
      case CARRIER -> {
        list = new ArrayList<CarrierClaim>();
        list.add(testCarrierClaim);
      }
      case DME -> {
        list = new ArrayList<DMEClaim>();
        list.add(testDmeClaim);
      }
      case HHA -> {
        list = new ArrayList<HHAClaim>();
        list.add(testHhaClaim);
      }
      case HOSPICE -> {
        list = new ArrayList<HospiceClaim>();
        list.add(testHospiceClaim);
      }
      case INPATIENT -> {
        list = new ArrayList<InpatientClaim>();
        list.add(testInpatientClaim);
      }
      case OUTPATIENT -> {
        list = new ArrayList<OutpatientClaim>();
        list.add(testOutpatientClaim);
      }
      case PDE -> {
        list = new ArrayList<PartDEvent>();
        list.add(testPdeClaim);
      }
      case SNF -> {
        list = new ArrayList<SNFClaim>();
        list.add(testSnfClaim);
      }
      default -> {}
    }
    when(clmMockQuery.getResultList()).thenReturn(list);
  }

  /**
   * Verify that the metric are started and stopped properly.
   *
   * @param claimType the claim type expected for this metric
   */
  private void verifyMetrics(ClaimType claimType) {
    /* FUTURE: Not sure if the registered class here _should_ be MetricsRegistry
    or if that was a mistake in the initial impl. Leaving it as-is, for now... */
    String expectedTimerName =
        "MetricRegistry.query.eobs_by_bene_id." + claimType.name().toLowerCase();
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).stop();
  }
}
