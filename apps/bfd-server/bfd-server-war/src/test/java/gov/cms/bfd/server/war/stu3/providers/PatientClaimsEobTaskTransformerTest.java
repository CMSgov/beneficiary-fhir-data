package gov.cms.bfd.server.war.stu3.providers;

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
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Units tests for the {@link PatientClaimsEobTaskTransformerTest}. Basically verifying that all the
 * task transformers work and return an EOB as well as some pertinent info such as SAMHSA processing
 * information.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientClaimsEobTaskTransformerTest {
  /** The mock query, for mocking DB returns. */
  @Mock TypedQuery mockQuery;

  /** The mock metric registry. */
  @Mock MetricRegistry metricRegistry;

  /** The mock metric timer. */
  @Mock Timer mockTimer;

  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;

  /** The NPI Org lookup. */
  @Mock NPIOrgLookup mockNpiOrgLookup;

  /** The FDA drug display lookup. */
  @Mock FdaDrugCodeDisplayLookup mockDrugDisplayLookup;

  /** The mock samhsa matcher. */
  @Mock Stu3EobSamhsaMatcher mockSamhsaMatcher;

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

  /** The mock entity manager for mocking database calls. */
  @Mock EntityManager mockEntityManager;

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
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * CarrierClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs no SAMHSA
   * EOB filtering.
   */
  @Test
  void testTaskTransformerUsingCarrierClaimNoSamhsaFilter() throws IOException {
    CriteriaQuery<CarrierClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<CarrierClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.CARRIER, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new CarrierClaimTransformer(metricRegistry, mockDrugDisplayLookup, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.CARRIER, 1234L, Optional.empty(), Optional.empty(), false);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertFalse(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(-1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * CarrierClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs SAMHSA EOB
   * filtering.
   */
  @Test
  void testTaskTransformerUsingCarrierClaimWithSamhsa() throws IOException {
    CriteriaQuery<CarrierClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<CarrierClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.CARRIER, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new CarrierClaimTransformer(metricRegistry, mockDrugDisplayLookup, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.CARRIER, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * DMEClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs no SAMHSA EOB
   * filtering.
   */
  @Test
  void testTaskTransformerUsingDmeClaimNoSamhsaFilter() throws IOException {
    CriteriaQuery<DMEClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.DME, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new DMEClaimTransformer(metricRegistry, mockDrugDisplayLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.DME, 1234L, Optional.empty(), Optional.empty(), false);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertFalse(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(-1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * DMEClaim} entity into a {@link ExplanationOfBenefit} FHIR resource and performs SAMHSA EOB
   * filtering.
   */
  @Test
  void testTaskTransformerUsingDmeClaimWithSamhsa() throws IOException {
    CriteriaQuery<DMEClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.DME, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new DMEClaimTransformer(metricRegistry, mockDrugDisplayLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.DME, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * HHAClaim} entity into a {@link ExplanationOfBenefit} FHIR resource;performs SAMHSA filtering
   * flag when set to true.
   */
  @Test
  void testTaskTransformerUsingHhaClaimWithSamhsa() throws IOException {
    CriteriaQuery<HHAClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<HHAClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.HHA, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new HHAClaimTransformer(metricRegistry, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.HHA, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * HospiceClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA
   * filtering flag when set to true.
   */
  @Test
  void testTaskTransformerUsingHospiceClaimWithSamhsa() throws IOException {
    CriteriaQuery<HospiceClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<HospiceClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.HOSPICE, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new HospiceClaimTransformer(metricRegistry, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.HOSPICE, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * InpatientClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA
   * filtering flag when set to true.
   */
  @Test
  void testTaskTransformerUsingInpatientClaimWithSamhsa() throws IOException {
    CriteriaQuery<InpatientClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<InpatientClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.INPATIENT, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new InpatientClaimTransformer(metricRegistry, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.INPATIENT, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * OutpatientClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA
   * filtering flag when set to true.
   */
  @Test
  void testTaskTransformerUsingOutpatientClaimWithSamhsa() throws IOException {
    CriteriaQuery<OutpatientClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<OutpatientClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.OUTPATIENT, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new OutpatientClaimTransformer(metricRegistry, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.OUTPATIENT, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * PartDEvent} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA filtering
   * flag when set to true.
   */
  @Test
  void testTaskTransformerUsingPartDEventWithSamhsa() throws IOException {
    CriteriaQuery<PartDEvent> clmMockCriteria = mock(CriteriaQuery.class);
    Root<PartDEvent> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.PDE, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new PartDEventTransformer(metricRegistry, mockDrugDisplayLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.PDE, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} sucessfully transforms a {@link
   * SNFClaim} entity into a {@link ExplanationOfBenefit} FHIR resource; performs SAMHSA filtering
   * flag when set to true.
   */
  @Test
  void testTaskTransformerUsingSnfClaimWithSamhsa() throws IOException {
    CriteriaQuery<SNFClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<SNFClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.SNF, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new SNFClaimTransformer(metricRegistry, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.SNF, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertTrue(taskTransformer.ranSuccessfully());
    assertEquals(1, taskTransformer.fetchEOBs().size());
    assertTrue(taskTransformer.wasSamhsaFilteringPerformed());
    assertEquals(0, taskTransformer.eobsRemovedBySamhsaFilter());
    assertEquals(1, taskTransformer.eobsIgnoredBySamhsaFilter());
  }

  /**
   * Verify that the {@link PatientClaimsEobTaskTransformer} can handle internal processing {@link
   * Exception} and can return it to the caller via its {@link
   * PatientClaimsEobTaskTransformer#getFailure()} method. The test sets up a {@link DMEClaim}
   * entity but invokes the task setup to process the entity using a {@link SNFClaimTransformer};
   * this mis-match triggers a {@link BadCodeMonkeyException} which the caller can retrieve.
   */
  @Test
  void testGetFailureFromTaskTransformer() throws IOException {
    // purposely setup a DME clam
    CriteriaQuery<DMEClaim> clmMockCriteria = mock(CriteriaQuery.class);
    Root<DMEClaim> clmRoot = mock(Root.class);
    setupClaimEntity(mockEntityManager, ClaimType.DME, clmMockCriteria, clmRoot);

    ClaimTransformerInterface claimTransformer =
        new SNFClaimTransformer(metricRegistry, mockNpiOrgLookup);
    PatientClaimsEobTaskTransformer taskTransformer =
        new PatientClaimsEobTaskTransformer(
            metricRegistry, mockSamhsaMatcher, mockDrugDisplayLookup, mockNpiOrgLookup);

    // should ignore processing of NPI tax numbers even though it is set
    taskTransformer.setIncludeTaxNumbers(true);
    taskTransformer.setupTaskParams(
        claimTransformer, ClaimType.SNF, 1234L, Optional.empty(), Optional.empty(), true);
    taskTransformer.setEntityManager(mockEntityManager);

    PatientClaimsEobTaskTransformer rslt = taskTransformer.call();
    assertNotNull(rslt);
    assertFalse(taskTransformer.ranSuccessfully());
    assertTrue(taskTransformer.getFailure().get() instanceof BadCodeMonkeyException);
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
}
