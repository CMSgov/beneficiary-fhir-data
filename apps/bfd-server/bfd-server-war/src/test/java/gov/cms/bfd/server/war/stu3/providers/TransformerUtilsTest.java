package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link TransformerUtils}. Not to be confused with {@link TransformerTestUtils},
 * which are test utilities.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class TransformerUtilsTest {

  /** The securityTagManager. */
  @Mock private SecurityTagManager securityTagManager;

  Set<String> securityTags = new HashSet<>();

  /** Verifies that {@link TransformerUtils#createExtensionCoding} works as expected. */
  @Test
  public void createExtensionCoding() {
    Patient patientA = new Patient();
    patientA.setId("12345");

    Extension extension =
        TransformerUtils.createExtensionCoding(patientA, CcwCodebookVariable.RACE, "4");
    assertNotNull(extension);
    assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", extension.getUrl());
    assertTrue(extension.getValue() instanceof Coding);

    Coding coding = (Coding) extension.getValue();
    assertNotNull(coding);
    assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
    assertEquals("4", coding.getCode());
  }

  /** Verifies that {@link TransformerUtils#createCodeableConcept} works as expected. */
  @Test
  public void createCodeableConcept() {
    Patient patientA = new Patient();
    patientA.setId("12345");

    CodeableConcept concept =
        TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "4");
    assertNotNull(concept);
    assertEquals(1, concept.getCoding().size());

    Coding coding = concept.getCodingFirstRep();
    assertNotNull(coding);
    assertEquals(TransformerConstants.BASE_URL_CCW_VARIABLES + "/race", coding.getSystem());
    assertEquals("4", coding.getCode());
  }

  /**
   * Verifies that {@link TransformerUtils#createCodeableConcept} sets {@link Coding#getDisplay()}
   * correctly.
   */
  @Test
  public void createCodeableConcept_display() {
    Patient patientA = new Patient();
    patientA.setId("12345");

    CodeableConcept raceConcept_4 =
        TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "4");
    assertEquals("Asian", raceConcept_4.getCodingFirstRep().getDisplay());

    // This code isn't valid and shouldn't end up with a matching display.
    CodeableConcept raceConcept_12 =
        TransformerUtils.createCodeableConcept(patientA, CcwCodebookVariable.RACE, "12");
    assertNull(raceConcept_12.getCodingFirstRep().getDisplay());

    /*
     * The REV_CNTR_PMT_MTHD_IND_CD Variable has value collisions. Verify that those
     * are handled correctly.
     */
    CodeableConcept paymentMethodConcept_1 =
        TransformerUtils.createCodeableConcept(
            patientA, CcwCodebookVariable.REV_CNTR_PMT_MTHD_IND_CD, "1");
    assertNull(paymentMethodConcept_1.getCodingFirstRep().getDisplay());
  }

  /** Tests that {@link TransformerUtils#addCareTeamPractitioner} adds the care team as expected. */
  @Test
  public void addCareTeamPractitioner() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.PRIMARY);
    assertEquals(1, eob.getCareTeam().size(), "Expect there to be one care team member");
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.ASSIST);
    assertEquals(2, eob.getCareTeam().size(), "Expect there to be two care team members");
    TransformerUtils.addCareTeamPractitioner(eob, null, "system", "123", ClaimCareteamrole.ASSIST);
    assertEquals(2, eob.getCareTeam().size(), "Expect there to be two care team members");
  }

  /**
   * Verifies that {@link TransformerUtils#createIdentifierReference} sets {@link Reference}
   * correctly.
   */
  @Test
  public void createReferenceTest() {
    String identifierSystem = "identifierSystem";
    String identifierValue = "identifierValue";

    Reference reference =
        TransformerUtils.createIdentifierReference(identifierSystem, identifierValue);

    assertEquals(identifierSystem, reference.getIdentifier().getSystem());
    assertEquals(identifierValue, reference.getIdentifier().getValue());
    assertTrue(isCodingListNullOrEmpty(reference.getIdentifier().getType().getCoding()));
  }

  /**
   * Verifies that {@link TransformerUtils#careTeamHasMatchingExtension} verifies if an extension is
   * found.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsTrueWhenFound() {
    String referenceUrl = "http://test.url";
    String codeValue = "code";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertTrue(returnResult);
  }

  /**
   * Verifies that {@link TransformerUtils#careTeamHasMatchingExtension} verifies it returns false
   * when a reference url is empty.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithEmptyReferenceUrl() {
    String referenceUrl = "";
    String codeValue = "code";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link TransformerUtils#careTeamHasMatchingExtension} verifies it returns false
   * when a reference url is null.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithNullReferenceUrl() {
    String referenceUrl = null;
    String codeValue = "code";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link TransformerUtils#careTeamHasMatchingExtension} verifies it returns false
   * when a code value is empty.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithEmptyCodeValue() {
    String referenceUrl = "http://test.url";
    String codeValue = "";
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link TransformerUtils#careTeamHasMatchingExtension} verifies it returns false
   * when a code value is null.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithNullCodeValue() {
    String referenceUrl = "http://test.url";
    String codeValue = null;
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtils.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Tests that addCareTeamQualification does not add a qualification if the input Optional is
   * empty.
   */
  @Test
  public void addCareTeamQualificationWhenEmptyCodeExpectNoQualificationCodeAdded() {

    CareTeamComponent careTeam = new CareTeamComponent();
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    CcwCodebookVariable codebookVariable = CcwCodebookVariable.CCW_PRSCRBR_ID;
    Optional<String> value = Optional.empty();

    TransformerUtils.addCareTeamQualification(careTeam, eob, codebookVariable, value);

    // ensure careteam qualification isnt populated
    // (hasQualification does some checks on coding and text existing)
    assertFalse(careTeam.hasQualification());
  }

  /**
   * Tests that addCareTeamQualification adds a qualification if the input Optional is not empty.
   */
  @Test
  public void addCareTeamQualificationWhenNotEmptyCodeExpectQualificationAdded() {

    CareTeamComponent careTeam = new CareTeamComponent();
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    CcwCodebookVariable codebookVariable = CcwCodebookVariable.CCW_PRSCRBR_ID;
    String expectedValue = "ABC-TESTID";
    Optional<String> value = Optional.of(expectedValue);
    String expectedUrl = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CCW_PRSCRBR_ID);

    TransformerUtils.addCareTeamQualification(careTeam, eob, codebookVariable, value);

    // careTeam object should have the qualification added with expected values
    assertTrue(careTeam.hasQualification());
    assertTrue(careTeam.getQualification().hasCoding());
    assertEquals(expectedUrl, careTeam.getQualification().getCoding().get(0).getSystem());
    assertEquals(expectedValue, careTeam.getQualification().getCoding().get(0).getCode());
  }

  /**
   * Tests that addCareTeamExtension correctly adds an extension when the value Optional and its
   * string value is not empty.
   */
  @Test
  public void addCareTeamExtensionWhenNotEmptyOptionalExpectExtensionAdded() {

    CareTeamComponent careTeam = new CareTeamComponent();
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    CcwCodebookVariable codebookVariable = CcwCodebookVariable.CCW_PRSCRBR_ID;
    String expectedValue = "DDD-TESTID";
    Optional<String> value = Optional.of(expectedValue);
    String expectedUrl = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CCW_PRSCRBR_ID);

    TransformerUtils.addCareTeamExtension(codebookVariable, value, careTeam, eob);

    assertTrue(careTeam.hasExtension(expectedUrl));
    Extension extension = careTeam.getExtensionByUrl(expectedUrl);
    String extensionValue = ((Coding) extension.getValue()).getCode();
    assertEquals(expectedValue, extensionValue);
  }

  /**
   * Tests that addCareTeamExtension correctly adds an extension when the value is required and a
   * char.
   */
  @Test
  public void addCareTeamExtensionWhenRequiredCharExpectExtensionAdded() {

    CareTeamComponent careTeam = new CareTeamComponent();
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    CcwCodebookVariable codebookVariable = CcwCodebookVariable.CCW_PRSCRBR_ID;
    char expectedValue = 'V';
    String expectedUrl = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CCW_PRSCRBR_ID);

    TransformerUtils.addCareTeamExtension(codebookVariable, expectedValue, careTeam, eob);

    assertTrue(careTeam.hasExtension(expectedUrl));
    Extension extension = careTeam.getExtensionByUrl(expectedUrl);
    String extensionValue = ((Coding) extension.getValue()).getCode();
    assertEquals(String.valueOf(expectedValue), extensionValue);
  }

  /**
   * Tests that addCareTeamExtension does not add an extension when the value Optional string value
   * is empty.
   */
  @Test
  public void addCareTeamExtensionWhenOptionalEmptyStringExpectNoExtensionAdded() {

    CareTeamComponent careTeam = new CareTeamComponent();
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    CcwCodebookVariable codebookVariable = CcwCodebookVariable.CCW_PRSCRBR_ID;
    String expectedValue = "";
    Optional<String> value = Optional.of(expectedValue);
    String expectedUrl = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CCW_PRSCRBR_ID);

    TransformerUtils.addCareTeamExtension(codebookVariable, value, careTeam, eob);

    assertFalse(careTeam.hasExtension(expectedUrl));
  }

  /** Tests that addCareTeamExtension does not add an extension when the value Optional is empty. */
  @Test
  public void addCareTeamExtensionWhenEmptyOptionalExpectNoExtensionAdded() {

    CareTeamComponent careTeam = new CareTeamComponent();
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    CcwCodebookVariable codebookVariable = CcwCodebookVariable.CCW_PRSCRBR_ID;
    Optional<String> value = Optional.empty();
    String expectedUrl = CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CCW_PRSCRBR_ID);

    TransformerUtils.addCareTeamExtension(codebookVariable, value, careTeam, eob);

    assertFalse(careTeam.hasExtension(expectedUrl));
  }

  /**
   * Verifies that {@link TransformerUtils#createIdentifierReference} sets {@link Reference}
   * correctly.
   */
  @Test
  public void createReferenceForIdentifierTypeTest() {
    String identifierValue = "identifierValue";
    IdentifierType identifierType = IdentifierType.NPI;
    Reference reference =
        TransformerUtils.createIdentifierReference(identifierType, identifierValue);

    assertEquals(identifierType.getSystem(), reference.getIdentifier().getSystem());
    assertEquals(identifierValue, reference.getIdentifier().getValue());
    assertEquals(
        identifierType.getCode(), reference.getIdentifier().getType().getCoding().get(0).getCode());
    assertEquals(
        identifierType.getDisplay(),
        reference.getIdentifier().getType().getCoding().get(0).getDisplay());
    assertEquals(
        identifierType.getSystem(),
        reference.getIdentifier().getType().getCoding().get(0).getSystem());
  }

  /** Verifies that {@link TransformerUtils#createBundle} sets bundle size correctly. */
  @Test
  public void createBundleWithoutPaging() throws IOException {

    RequestDetails requestDetails = mock(RequestDetails.class);
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    HHAClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(HHAClaim.class::cast)
            .findFirst()
            .get();
    claim.setLastUpdated(Instant.now());

    FhirContext fhirContext = FhirContext.forDstu3();
    ClaimTransformerInterface claimTransformerInterface =
        new HHAClaimTransformer(new MetricRegistry(), securityTagManager, false);
    ExplanationOfBenefit genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(claim, securityTags));
    TransformerUtils.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    List<IBaseResource> eobs = new ArrayList<IBaseResource>();
    eobs.add(parser.parseResource(ExplanationOfBenefit.class, json));

    Bundle bundle = TransformerUtils.createBundle(paging, eobs, Instant.now());
    assertEquals(1, bundle.getTotal());
    assertEquals(1, Integer.parseInt(BfdMDC.get("resources_returned_count")));
  }

  /** Verifies that {@link TransformerUtils#createBundle} sets bundle size correctly. */
  @Test
  public void createBundleWithoutPagingWithZeroEobs() throws IOException {

    RequestDetails requestDetails = mock(RequestDetails.class);
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    List<IBaseResource> eobs = new ArrayList<IBaseResource>();

    Bundle bundle = TransformerUtils.createBundle(paging, eobs, Instant.now());
    assertEquals(0, bundle.getTotal());
    assertEquals(0, Integer.parseInt(BfdMDC.get("resources_returned_count")));
  }

  /**
   * Verifies that {@link TransformerUtils#createBundle} sets bundle with paging size of 2
   * correctly.
   */
  @Test
  public void createBundleWithPagingWithASizeOf2() throws IOException {
    RequestDetails requestDetails = mock(RequestDetails.class);
    Map<String, String[]> pagingParams = new HashMap<String, String[]>();
    pagingParams.put(Constants.PARAM_COUNT, new String[] {"2"});
    pagingParams.put("startIndex", new String[] {"1"});

    when(requestDetails.getParameters()).thenReturn(pagingParams);

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    HHAClaim hhaClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(HHAClaim.class::cast)
            .findFirst()
            .get();
    hhaClaim.setLastUpdated(Instant.now());

    FhirContext fhirContext = FhirContext.forDstu3();
    MetricRegistry metricRegistry = new MetricRegistry();
    ClaimTransformerInterface claimTransformerInterface =
        new HHAClaimTransformer(metricRegistry, securityTagManager, false);
    ExplanationOfBenefit genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(hhaClaim, securityTags));
    TransformerUtils.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    List<IBaseResource> eobs = new ArrayList<IBaseResource>();
    eobs.add(parser.parseResource(ExplanationOfBenefit.class, json));

    HospiceClaim hospiceClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(HospiceClaim.class::cast)
            .findFirst()
            .get();
    hospiceClaim.setLastUpdated(Instant.now());

    claimTransformerInterface =
        new HospiceClaimTransformer(metricRegistry, securityTagManager, false);
    genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(hospiceClaim, securityTags));
    TransformerUtils.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    parser = fhirContext.newJsonParser();
    json = parser.encodeResourceToString(genEob);

    eobs.add(parser.parseResource(ExplanationOfBenefit.class, json));

    DMEClaim dmeClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof DMEClaim)
            .map(DMEClaim.class::cast)
            .findFirst()
            .get();
    dmeClaim.setLastUpdated(Instant.now());

    claimTransformerInterface = new DMEClaimTransformer(metricRegistry, securityTagManager, false);
    genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(dmeClaim, securityTags));
    TransformerUtils.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    parser = fhirContext.newJsonParser();
    json = parser.encodeResourceToString(genEob);

    eobs.add(parser.parseResource(ExplanationOfBenefit.class, json));

    InpatientClaim inpatientClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(InpatientClaim.class::cast)
            .findFirst()
            .get();
    inpatientClaim.setLastUpdated(Instant.now());

    claimTransformerInterface =
        new InpatientClaimTransformer(metricRegistry, securityTagManager, false);
    genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(inpatientClaim, securityTags));
    TransformerUtils.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    parser = fhirContext.newJsonParser();
    json = parser.encodeResourceToString(genEob);

    eobs.add(parser.parseResource(ExplanationOfBenefit.class, json));

    Bundle bundle = TransformerUtils.createBundle(paging, eobs, Instant.now());
    assertEquals(4, bundle.getTotal());
    assertEquals(2, Integer.parseInt(BfdMDC.get("resources_returned_count")));
  }

  /**
   * Verifies that {@link TransformerUtils#createBundle} returns an empty bundle when no eob items
   * are present and pagination is requested.
   */
  @Test
  public void createBundleWithNoResultsAndPagingExpectEmptyBundle() {

    RequestDetails requestDetails = mock(RequestDetails.class);
    Map<String, String[]> pagingParams = new HashMap<>();
    pagingParams.put(Constants.PARAM_COUNT, new String[] {"2"});
    pagingParams.put("startIndex", new String[] {"1"});

    when(requestDetails.getParameters()).thenReturn(pagingParams);

    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    List<IBaseResource> eobs = new ArrayList<>();

    Bundle bundle = TransformerUtils.createBundle(paging, eobs, Instant.now());
    assertEquals(0, bundle.getTotal());
  }

  /**
   * Verifies that creating a bundle with a start index greater than the resource count throws a
   * {@link InvalidRequestException}.
   */
  @Test
  public void createBundleWithStartIndexGreaterThanResourceCountExpectException() {

    RequestDetails requestDetails = mock(RequestDetails.class);
    Map<String, String[]> pagingParams = new HashMap<>();
    pagingParams.put(Constants.PARAM_COUNT, new String[] {"2"});
    pagingParams.put("startIndex", new String[] {"12"});
    when(requestDetails.getParameters()).thenReturn(pagingParams);
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    List<IBaseResource> eobs = new ArrayList<>();
    // Add three resources
    eobs.add(new ExplanationOfBenefit());
    eobs.add(new ExplanationOfBenefit());
    eobs.add(new ExplanationOfBenefit());

    InvalidRequestException expectedException =
        assertThrows(
            InvalidRequestException.class,
            () -> TransformerUtils.createBundle(paging, eobs, Instant.now()));
    assertEquals(
        "Value for startIndex (12) must be less than than result size (3)",
        expectedException.getMessage());
  }

  /**
   * Verifies that creating a bundle with a start index equal to the resource count throws a {@link
   * InvalidRequestException} (its 0-indexed).
   */
  @Test
  public void createBundleWithStartIndexEqualsResourceCountExpectException() {

    RequestDetails requestDetails = mock(RequestDetails.class);
    Map<String, String[]> pagingParams = new HashMap<>();
    pagingParams.put(Constants.PARAM_COUNT, new String[] {"2"});
    pagingParams.put("startIndex", new String[] {"3"});
    when(requestDetails.getParameters()).thenReturn(pagingParams);
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    List<IBaseResource> eobs = new ArrayList<>();
    // Add three resources
    eobs.add(new ExplanationOfBenefit());
    eobs.add(new ExplanationOfBenefit());
    eobs.add(new ExplanationOfBenefit());

    InvalidRequestException expectedException =
        assertThrows(
            InvalidRequestException.class,
            () -> TransformerUtils.createBundle(paging, eobs, Instant.now()));
    assertEquals(
        "Value for startIndex (3) must be less than than result size (3)",
        expectedException.getMessage());
  }

  /**
   * Verifies coding list is null or empty.
   *
   * @param coding the coding
   * @return {@code true} if the coding list is null or empty
   */
  private boolean isCodingListNullOrEmpty(List<Coding> coding) {
    return (coding == null || coding.isEmpty() || coding.size() == 0);
  }

  /**
   * Verifies that providing a EnumSet of {@link ClaimType} and a bit mask integer denoting claim
   * types that have data, the results is a filtered EnumSet.
   */
  @Test
  public void verifyEnumSetFromListOfClaimTypesAndDatabaseBitmaskOfData() {
    EnumSet<ClaimType> allClaimSet = EnumSet.allOf(ClaimType.class);

    // resultant set only includes claim types that have data.
    int testVal = QueryUtils.V_DME_HAS_DATA | QueryUtils.V_SNF_HAS_DATA | QueryUtils.V_HHA_HAS_DATA;
    EnumSet<ClaimType> availSet = TransformerUtils.fetchClaimsAvailability(allClaimSet, testVal);

    assertTrue(availSet.contains(ClaimType.HHA));
    assertTrue(availSet.contains(ClaimType.SNF));
    assertTrue(availSet.contains(ClaimType.DME));
    assertFalse(availSet.contains(ClaimType.INPATIENT));

    // check efficacy of EnumSet filter vs. bit mask of data.
    EnumSet<ClaimType> someClaimSet = EnumSet.noneOf(ClaimType.class);
    someClaimSet.add(ClaimType.CARRIER);
    someClaimSet.add(ClaimType.PDE);

    availSet = TransformerUtils.fetchClaimsAvailability(someClaimSet, testVal);
    assertFalse(availSet.contains(ClaimType.HHA));
    assertFalse(availSet.contains(ClaimType.SNF));
    assertFalse(availSet.contains(ClaimType.DME));
    assertFalse(availSet.contains(ClaimType.CARRIER));
    // adjust data bit mask and try again
    testVal = testVal | QueryUtils.V_CARRIER_HAS_DATA;
    availSet = TransformerUtils.fetchClaimsAvailability(someClaimSet, testVal);
    assertTrue(availSet.contains(ClaimType.CARRIER));
  }
}
