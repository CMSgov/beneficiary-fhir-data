package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.C4BBInstutionalClaimSubtypes;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.OffsetLinkBuilder;
import gov.cms.bfd.server.war.commons.Profile;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.QueryUtils;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudicationStatus;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the utility methods within the {@link TransformerUtilsV2}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TransformerUtilsV2Test {
  /** The SamhsaSecurityTag lookup. */
  @Mock SecurityTagManager securityTagManager;

  Set<String> securityTags = new HashSet<>();

  /**
   * Ensures the revenue status code is correctly mapped to an item's revenue as an extension when
   * the input statusCode is present.
   */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenStatusCodeExistsExpectExtensionOnItem() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent();
    eob.addItem(item);

    Optional<String> statusCode = Optional.of("1");
    String expectedExtensionUrl =
        "https://bluebutton.cms.gov/resources/variables/rev_cntr_stus_ind_cd";

    TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(item, eob, statusCode);

    assertNotNull(item);
    assertNotNull(item.getRevenue());
    assertNotNull(item.getRevenue().getExtension());
    assertEquals(1, item.getRevenue().getExtension().size());
    Extension ext = item.getRevenue().getExtensionByUrl(expectedExtensionUrl);
    assertNotNull(ext);
    assertEquals(expectedExtensionUrl, ext.getUrl());
    assertTrue(ext.getValue() instanceof Coding);
    assertEquals(statusCode.get(), ((Coding) ext.getValue()).getCode());
  }

  /**
   * Verifies the item revenue status code is not mapped to an extension when the revenue status
   * code field is not present (empty optional).
   */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenStatusCodeDoesNotExistExpectNoExtensionOnItem() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent();
    eob.addItem(item);
    CodeableConcept revenue = new CodeableConcept();
    item.setRevenue(revenue);

    Optional<String> statusCode = Optional.empty();

    TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(item, eob, statusCode);

    assertNotNull(item);
    assertNotNull(item.getRevenue());
    assertNotNull(item);
    assertNotNull(item.getRevenue());
    assertNotNull(item.getRevenue().getExtension());
    assertEquals(0, item.getRevenue().getExtension().size());
  }

  /** Verifies an exception is thrown when the item is passed in as null. */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenNullItemExpectException() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    Optional<String> statusCode = Optional.of("1");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(null, eob, statusCode);
        });
  }

  /**
   * Verifies an exception is thrown when the eob is passed in as null.
   *
   * <p>Ideally a null eob would not cause issues since it's just used for debugging, but downstream
   * requires it to exist for now
   */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenNullEobExpectException() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent();
    eob.addItem(item);

    Optional<String> statusCode = Optional.of("1");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(item, null, statusCode);
        });
  }

  /**
   * Ensures the fi_num is correctly mapped to an eob as an extension when the input
   * fiscalIntermediaryNumber is present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenFiNumberExistsExpectExtensionOnEob() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    String fiNum = "12534";
    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_num";

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.empty(),
        Optional.empty(),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.of(fiNum),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.empty(),
        Profile.C4BB);

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiNumExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNotNull(fiNumExtension);
    assertEquals(fiNum, ((Coding) fiNumExtension.getValue()).getCode());
  }

  /**
   * Ensures the fi_num is correctly mapped to an eob as an extension when the input
   * fiscalIntermediaryNumber is present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenNpiOrgExistsExpectItOnEob() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.of(RDATestUtils.FAKE_NPI_NUMBER),
        Optional.of(RDATestUtils.FAKE_NPI_ORG_NAME),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.empty(),
        Profile.C4BB);

    Optional<Resource> organization =
        eob.getContained().stream()
            .filter(o -> o.getResourceType().equals(ResourceType.Organization))
            .findFirst();

    Organization org = (Organization) organization.get();
    Optional<Identifier> identifier =
        org.getIdentifier().stream()
            .filter(i -> i.getValue().equals(RDATestUtils.FAKE_NPI_NUMBER))
            .findFirst();
    assertEquals(RDATestUtils.FAKE_NPI_NUMBER, identifier.get().getValue());
    assertEquals(RDATestUtils.FAKE_NPI_ORG_NAME, org.getName());
  }

  /**
   * Ensures the fi_num is not mapped to an eob as an extension when the input
   * fiscalIntermediaryNumber is not present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenNoFiNumberExpectNoFiNumExtension() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_num";

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.empty(),
        Optional.empty(),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.empty(),
        Profile.C4BB);

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiNumExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNull(fiNumExtension);
  }

  /** Ensures the organiation contained resource is mapped correctly in the eob. */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenEOBHasAContainedOrganization() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    Instant instant = Instant.now();

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.of(RDATestUtils.FAKE_NPI_NUMBER),
        Optional.of(RDATestUtils.FAKE_NPI_ORG_NAME),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.empty(),
        Optional.of(instant),
        Optional.empty(),
        Optional.empty(),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.empty(),
        Profile.C4BB);

    assertEquals(1, eob.getContained().size());
    Organization actualEobContainedOrganizationResource = (Organization) eob.getContained().get(0);
    assertEquals("provider-org", actualEobContainedOrganizationResource.getId());
    assertEquals(true, actualEobContainedOrganizationResource.getActive());
    assertEquals(RDATestUtils.FAKE_NPI_ORG_NAME, actualEobContainedOrganizationResource.getName());
    assertTrue(
        actualEobContainedOrganizationResource.getIdentifier().stream()
            .filter(s -> s.getSystem().equals(TransformerConstants.CODING_NPI_US))
            .findFirst()
            .isPresent());
    assertTrue(
        actualEobContainedOrganizationResource.getMeta().getProfile().stream()
            .filter(o -> o.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL))
            .findFirst()
            .isPresent());
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2#careTeamHasMatchingExtension} verifies
   * if an extension is found.
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
        TransformerUtilsV2.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertTrue(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2#careTeamHasMatchingExtension} verifies
   * it returns false when a reference url is null.
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
        TransformerUtilsV2.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2#careTeamHasMatchingExtension} verifies
   * it returns false when a reference url is empty.
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
        TransformerUtilsV2.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2#careTeamHasMatchingExtension} verifies
   * it returns false when a code value is null.
   */
  @Test
  public void careTeamHasMatchingExtensionReturnsFalseWithNullOrEmptyCodeValue() {
    String referenceUrl = "http://test.url";
    String codeValue = null;
    Coding coding = new Coding();
    coding.setCode(codeValue);
    Extension extension = new Extension(referenceUrl);
    extension.setValue(coding);
    CareTeamComponent careTeamComponent = new CareTeamComponent();
    careTeamComponent.addExtension(extension);

    boolean returnResult =
        TransformerUtilsV2.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, codeValue);

    assertFalse(returnResult);
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.r4.providers.TransformerUtilsV2#careTeamHasMatchingExtension} verifies
   * it returns false when a code value is empty.
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
        TransformerUtilsV2.careTeamHasMatchingExtension(careTeamComponent, referenceUrl, null);

    assertFalse(returnResult);
  }

  /**
   * Ensures the fiClmActnCd is correctly mapped to an eob as an extension when the input
   * fiscalIntermediaryClaimActionCode is present.
   */
  @Test
  public void mapEobCommonGroupInpSNFWhenFiClmActnCdExistsExpectExtensionOnEob() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    Character fiClmActnCd = '1';
    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_clm_actn_cd";

    TransformerUtilsV2.addCommonEobInformationInpatientSNF(
        eob,
        ' ',
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(fiClmActnCd));

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiClmActnCdExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);

    assertNotNull(fiClmActnCdExtension);
    assertEquals(fiClmActnCd.toString(), ((Coding) fiClmActnCdExtension.getValue()).getCode());
  }

  /**
   * Ensures the fiClmActnCd is not mapped to an eob as an extension when the input
   * fiscalIntermediaryClaimActionCode is not present.
   */
  @Test
  public void mapEobCommonGroupInpSNFWhenNoFiClmActnCdExpectNoFiClmActnCdExtension() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_clm_actn_cd";

    TransformerUtilsV2.addCommonEobInformationInpatientSNF(
        eob,
        ' ',
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty());

    assertNotNull(eob.getExtension());
    assertTrue(eob.getExtension().isEmpty());
    Extension fiClmActnCdExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNull(fiClmActnCdExtension);
  }

  /**
   * Ensures the Fi_Clm_Proc_Dt is correctly mapped to an eob as an extension when the input
   * fiscalIntermediaryClaimProcessDate is present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenFiClmProcDtExistsExpectExtensionOnEob() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    LocalDate fiClmProcDt = LocalDate.of(2014, 02, 07);
    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt";

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.empty(),
        Optional.empty(),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.of(fiClmProcDt),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.empty(),
        Profile.C4BB);

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());

    Extension ex =
        TransformerTestUtilsV2.findExtensionByUrl(
            "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt", eob.getExtension());

    Extension compare =
        new Extension(
            "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt",
            new DateType("2014-02-07"));

    assertTrue(compare.equalsDeep(ex));
  }

  /**
   * Ensures the Fi_Clm_Proc_Dt is not mapped to an eob as an extension when the input
   * fiscalIntermediaryClaimProcessDate is not present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenNoFiClmProcDtExpectFiClmProcDtExtension() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt";

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.empty(),
        Optional.empty(),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.empty(),
        Profile.C4BB);

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiClmProcDtExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNull(fiClmProcDtExtension);
  }

  /**
   * Ensures the Fi_Clm_Proc_Dt is not mapped to an eob as an extension when the input
   * fiscalIntermediaryClaimProcessDate is not present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenClaimQueryCodeExists() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // TODO: Is this really the expectedDiscriminator? Should this be used, i.e. asserted?
    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_clm_proc_dt";

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.empty(),
        Optional.empty(),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        C4BBInstutionalClaimSubtypes.Inpatient,
        Optional.of('3'),
        Profile.C4BB);

    assertNotNull(eob.getBillablePeriod());
    assertFalse(eob.getBillablePeriod().isEmpty());
  }

  /** Verifies that createCoding can take a Character type value and create a Coding from it. */
  @Test
  public void createCodingWhenValueIsCharacterExpectCodingWithValue() {

    Character codingValue = 'a';
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    Coding coding =
        TransformerUtilsV2.createCoding(eob, CcwCodebookVariable.BENE_HOSPC_PRD_CNT, codingValue);

    assertEquals(codingValue.toString(), coding.getCode());
  }

  /** Verifies that createCoding can take a String type value and create a Coding from it. */
  @Test
  public void createCodingWhenValueIsStringExpectCodingWithValue() {

    String codingValue = "abc";
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    Coding coding =
        TransformerUtilsV2.createCoding(eob, CcwCodebookVariable.BENE_HOSPC_PRD_CNT, codingValue);

    assertEquals(codingValue, coding.getCode());
  }

  /**
   * Verifies that createCoding throws an exception when an unexpected typed coding is passed to it.
   */
  @Test
  public void createCodingWhenValueIsUnexpectedTypeExpectException() {

    BigInteger codingValue = BigInteger.ONE;
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    assertThrows(
        BadCodeMonkeyException.class,
        () -> {
          TransformerUtilsV2.createCoding(eob, CcwCodebookVariable.BENE_HOSPC_PRD_CNT, codingValue);
        });
  }

  /**
   * Tests createTotalAdjudicationAmountSlice when the input amount Optional is empty, expect an
   * empty Optional returned.
   */
  @Test
  public void createTotalAdjudicationAmountSliceWhenAmountEmptyExpectEmptyOptionalReturned() {
    Optional<BigDecimal> inputValue = Optional.empty();
    C4BBAdjudication inputStatus = C4BBAdjudication.DISCOUNT;
    Optional<ExplanationOfBenefit.TotalComponent> totalOptional =
        TransformerUtilsV2.createTotalAdjudicationAmountSlice(inputStatus, inputValue);

    assertTrue(totalOptional.isEmpty());
  }

  /**
   * Tests createTotalAdjudicationAmountSlice when the input amount Optional is not empty, expect a
   * TotalComponent is returned with the expected total values.
   */
  @Test
  public void
      createTotalAdjudicationAmountSliceWhenNonEmptyAmountExpectFilledOutOptionalReturned() {
    Optional<BigDecimal> inputValue = Optional.of(new BigDecimal("64.22"));
    C4BBAdjudication inputStatus = C4BBAdjudication.COINSURANCE;
    Optional<ExplanationOfBenefit.TotalComponent> totalOptional =
        TransformerUtilsV2.createTotalAdjudicationAmountSlice(inputStatus, inputValue);

    assertFalse(totalOptional.isEmpty());
    ExplanationOfBenefit.TotalComponent total = totalOptional.get();
    assertEquals(inputValue.get(), total.getAmount().getValue());
    assertNotNull(total.getCategory());
    assertEquals(inputStatus.toCode(), total.getCategory().getCoding().get(0).getCode());
    assertEquals(inputStatus.getDisplay(), total.getCategory().getCoding().get(0).getDisplay());
    assertEquals(inputStatus.getSystem(), total.getCategory().getCoding().get(0).getSystem());
  }

  /**
   * Tests createTotalAdjudicationStatusAmountSlice when the input amount Optional is empty, expect
   * an empty Optional returned.
   */
  @Test
  public void createTotalAdjudicationStatusAmountSliceWhenAmountEmptyExpectEmptyOptionalReturned() {
    Optional<BigDecimal> inputValue = Optional.empty();
    C4BBAdjudicationStatus inputStatus = C4BBAdjudicationStatus.OTHER;
    Optional<ExplanationOfBenefit.TotalComponent> totalOptional =
        TransformerUtilsV2.createTotalAdjudicationStatusAmountSlice(inputStatus, inputValue);

    assertTrue(totalOptional.isEmpty());
  }

  /**
   * Tests createTotalAdjudicationStatusAmountSlice when the input amount Optional is not empty,
   * expect a TotalComponent is returned with the expected total values and the category data is
   * set.
   */
  @Test
  public void
      createTotalAdjudicationStatusAmountSliceWhenNonEmptyAmountExpectFilledOutOptionalReturned() {
    Optional<BigDecimal> inputValue = Optional.of(new BigDecimal("23.56"));
    C4BBAdjudicationStatus inputStatus = C4BBAdjudicationStatus.OTHER;
    Optional<ExplanationOfBenefit.TotalComponent> totalOptional =
        TransformerUtilsV2.createTotalAdjudicationStatusAmountSlice(inputStatus, inputValue);

    assertFalse(totalOptional.isEmpty());
    ExplanationOfBenefit.TotalComponent total = totalOptional.get();
    assertEquals(inputValue.get(), total.getAmount().getValue());
    assertNotNull(total.getCategory());
    assertEquals(inputStatus.toCode(), total.getCategory().getCoding().get(0).getCode());
    assertEquals(inputStatus.getDisplay(), total.getCategory().getCoding().get(0).getDisplay());
    assertEquals(inputStatus.getSystem(), total.getCategory().getCoding().get(0).getSystem());
  }

  /** Tests should have a care team entry with a npi org associated with it. */
  @Test
  public void addCareTeamMemberWithNpiOrgShouldCreateCareTeamEntry() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent();
    eob.addItem(item);

    C4BBPractitionerIdentifierType type = C4BBPractitionerIdentifierType.NPI;
    C4BBClaimProfessionalAndNonClinicianCareTeamRole role =
        C4BBClaimProfessionalAndNonClinicianCareTeamRole.PRIMARY;
    String id = "123";
    Optional<String> npiOrgDisplay = Optional.of(RDATestUtils.FAKE_NPI_ORG_NAME);

    CareTeamComponent careTeamEntry =
        TransformerUtilsV2.addCareTeamMemberWithNpiOrg(eob, item, type, role, id, npiOrgDisplay);
    assertEquals("primary", careTeamEntry.getRole().getCoding().get(0).getCode());
    assertEquals(RDATestUtils.FAKE_NPI_ORG_NAME, careTeamEntry.getProvider().getDisplay());
    assertEquals(id, careTeamEntry.getProvider().getIdentifier().getValue());
    assertEquals(
        "npi", careTeamEntry.getProvider().getIdentifier().getType().getCoding().get(0).getCode());
    assertEquals(
        "National Provider Identifier",
        careTeamEntry.getProvider().getIdentifier().getType().getCoding().get(0).getDisplay());
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

    TransformerUtilsV2.addCareTeamQualification(careTeam, eob, codebookVariable, value);

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

    TransformerUtilsV2.addCareTeamExtension(codebookVariable, value, careTeam, eob);

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

    TransformerUtilsV2.addCareTeamExtension(codebookVariable, expectedValue, careTeam, eob);

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

    TransformerUtilsV2.addCareTeamExtension(codebookVariable, value, careTeam, eob);

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

    TransformerUtilsV2.addCareTeamExtension(codebookVariable, value, careTeam, eob);

    assertFalse(careTeam.hasExtension(expectedUrl));
  }

  /** Verifies that {@link TransformerUtilsV2#createBundle} sets bundle size of 2 correctly. */
  @Test
  public void createBundleWithoutPagingWithASizeOf2() throws IOException {

    NPIOrgLookup npiOrgLookup = RDATestUtils.createTestNpiOrgLookup();
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

    FhirContext fhirContext = FhirContext.forR4();
    ClaimTransformerInterfaceV2 claimTransformerInterface =
        new HHAClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    ExplanationOfBenefit genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(hhaClaim, securityTags));
    TransformerUtilsV2.enrichEob(
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
        new HospiceClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(hospiceClaim, securityTags));
    TransformerUtilsV2.enrichEob(
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
    claimTransformerInterface =
        new DMEClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(dmeClaim, securityTags));
    TransformerUtilsV2.enrichEob(
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
        new InpatientClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(inpatientClaim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    parser = fhirContext.newJsonParser();
    json = parser.encodeResourceToString(genEob);
    eobs.add(parser.parseResource(ExplanationOfBenefit.class, json));

    Bundle bundle = TransformerUtilsV2.createBundle(paging, eobs, Instant.now());
    assertEquals(4, bundle.getTotal());
    assertEquals(2, Integer.parseInt(BfdMDC.get("resources_returned_count")));
  }

  /**
   * Verifies that {@link TransformerUtilsV2#createBundle} returns an empty bundle when no eob items
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

    Bundle bundle = TransformerUtilsV2.createBundle(paging, eobs, Instant.now());
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
            () -> TransformerUtilsV2.createBundle(paging, eobs, Instant.now()));
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
            () -> TransformerUtilsV2.createBundle(paging, eobs, Instant.now()));
    assertEquals(
        "Value for startIndex (3) must be less than than result size (3)",
        expectedException.getMessage());
  }

  /** Verifies that {@link TransformerUtilsV2#createBundle} sets bundle size correctly. */
  @Test
  public void createBundleWithoutPagingWithZeroEobs() throws IOException {

    RequestDetails requestDetails = mock(RequestDetails.class);
    OffsetLinkBuilder paging = new OffsetLinkBuilder(requestDetails, "/ExplanationOfBenefit?");

    List<IBaseResource> eobs = new ArrayList<IBaseResource>();

    Bundle bundle = TransformerUtilsV2.createBundle(paging, eobs, Instant.now());
    assertEquals(0, bundle.getTotal());
    assertEquals(0, Integer.parseInt(BfdMDC.get("resources_returned_count")));
  }

  /**
   * Verifies that {@link TransformerUtilsV2#createBundle} sets bundle with paging size correctly.
   */
  @Test
  public void createBundleWithoutPaging() throws IOException {

    RequestDetails requestDetails = mock(RequestDetails.class);
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

    FhirContext fhirContext = FhirContext.forR4();
    ClaimTransformerInterfaceV2 claimTransformerInterface =
        new HHAClaimTransformerV2(new MetricRegistry(), securityTagManager, false);
    ExplanationOfBenefit genEob =
        claimTransformerInterface.transform(
            new ClaimWithSecurityTags<>(hhaClaim, securityTags));
    TransformerUtilsV2.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());
    IParser parser = fhirContext.newJsonParser();
    String json = parser.encodeResourceToString(genEob);
    List<IBaseResource> eobs = new ArrayList<IBaseResource>();
    eobs.add(parser.parseResource(ExplanationOfBenefit.class, json));

    Bundle bundle = TransformerUtilsV2.createBundle(paging, eobs, Instant.now());
    assertEquals(1, bundle.getTotal());
    assertEquals(1, Integer.parseInt(BfdMDC.get("resources_returned_count")));
  }

  /**
   * Verifies that {@link TransformerUtilsV2#findOrCreateContainedOrganization} sets a new
   * organization and eob correctly.
   */
  @Test
  public void createContainedOrganizationSetsOrganizationAndEobCorrectly() throws IOException {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    String expectedId = "#provider-org";

    Organization actualOrganization =
        TransformerUtilsV2.findOrCreateContainedOrganization(eob, expectedId, Profile.C4BB);

    assertEquals(expectedId, actualOrganization.getId());
    assertTrue(
        actualOrganization.getMeta().getProfile().stream()
            .anyMatch(v -> v.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL)));

    Optional<Resource> resource =
        eob.getContained().stream().filter(r -> r.getId().equals(expectedId)).findFirst();

    assertTrue(resource.isPresent());
    Organization actualEobContainedOrganizationResource = (Organization) resource.get();
    assertTrue(
        actualEobContainedOrganizationResource.getMeta().getProfile().stream()
            .anyMatch(p -> p.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL)));
  }

  /**
   * Verifies that {@link TransformerUtilsV2#findOrCreateContainedOrganization} finds the
   * organization and eob doesn't have duplicate entries for the contained resource.
   */
  @Test
  public void findContainedOrganizationSetsOrganizationAndEobCorrectly() throws IOException {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    String expectedId = "#provider-org";

    Organization actualOrganization =
        TransformerUtilsV2.findOrCreateContainedOrganization(eob, expectedId, Profile.C4BB);

    assertEquals(expectedId, actualOrganization.getId());
    assertTrue(
        actualOrganization.getMeta().getProfile().stream()
            .anyMatch(v -> v.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL)));

    List<Resource> resource =
        eob.getContained().stream()
            .filter(r -> r.getId().equals(expectedId))
            .collect(Collectors.toList());
    assertEquals(1, resource.size());

    // Call findOrCreateContainedOrganization and make sure it finds the organization that
    // was created above and doesn't have duplicate entries for eob.
    actualOrganization =
        TransformerUtilsV2.findOrCreateContainedOrganization(eob, expectedId, Profile.C4BB);

    resource =
        eob.getContained().stream()
            .filter(r -> r.getId().equals(expectedId))
            .collect(Collectors.toList());

    assertEquals(1, resource.size());

    Organization actualEobContainedOrganizationResource = (Organization) resource.get(0);
    assertTrue(
        actualEobContainedOrganizationResource.getMeta().getProfile().stream()
            .anyMatch(p -> p.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL)));
  }

  /**
   * Verifies that {@link TransformerUtilsV2#addProviderSlice} sets organization and eob correctly.
   */
  @Test
  public void addProviderSliceSetsOrganizationAndEobCorrectly() throws IOException {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    String expectedNpiOrgName = "expectedNpi";
    String expectedValue = "1";
    String expectedId = "provider-org";
    Instant instant = Instant.now();

    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.NPI,
        Optional.of(expectedValue),
        Optional.of(expectedNpiOrgName),
        Optional.of(instant),
        Profile.C4BB);

    Optional<Resource> resource =
        eob.getContained().stream().filter(r -> r.getId().equals(expectedId)).findFirst();

    assertTrue(resource.isPresent());
    Organization actualEobContainedOrganizationResource = (Organization) resource.get();
    assertTrue(actualEobContainedOrganizationResource.getActive());
    assertEquals(expectedNpiOrgName, actualEobContainedOrganizationResource.getName());
    Identifier identifier =
        actualEobContainedOrganizationResource.getIdentifier().stream()
            .filter(i -> i.getSystem().equals(TransformerConstants.CODING_NPI_US))
            .findFirst()
            .get();
    assertEquals(expectedValue, identifier.getValue());
    assertTrue(
        actualEobContainedOrganizationResource.getMeta().getProfile().stream()
            .filter(p -> p.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL))
            .findFirst()
            .isPresent());
  }

  /**
   * Verifies that {@link TransformerUtilsV2#addProviderSlice} sets organization and eob correctly.
   */
  @Test
  public void addProviderSliceSetsOrganizationAndEobCorrectlyWithDefaultNpiValueOfUnknown()
      throws IOException {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    Instant instant = Instant.now();
    String expectedNpiOrgName = "UNKNOWN";
    String expectedValue = "1";
    String expectedId = "provider-org";

    TransformerUtilsV2.addProviderSlice(
        eob,
        C4BBOrganizationIdentifierType.NPI,
        Optional.of(expectedValue),
        Optional.empty(),
        Optional.of(instant),
        Profile.C4BB);

    List<Resource> c = eob.getContained();
    Optional<Resource> resource =
        eob.getContained().stream().filter(r -> r.getId().equals(expectedId)).findFirst();

    assertTrue(resource.isPresent());
    Organization actualEobContainedOrganizationResource = (Organization) resource.get();
    assertEquals("provider-org", actualEobContainedOrganizationResource.getId());
    assertEquals(true, actualEobContainedOrganizationResource.getActive());
    assertEquals(expectedNpiOrgName, actualEobContainedOrganizationResource.getName());
    Identifier identifier =
        actualEobContainedOrganizationResource.getIdentifier().stream()
            .filter(i -> i.getSystem().equals(TransformerConstants.CODING_NPI_US))
            .findFirst()
            .get();
    assertEquals(expectedValue, identifier.getValue());
    assertTrue(
        actualEobContainedOrganizationResource.getMeta().getProfile().stream()
            .filter(p -> p.getValue().equals(ProfileConstants.C4BB_ORGANIZATION_URL))
            .findFirst()
            .isPresent());
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
    EnumSet<ClaimType> availSet = TransformerUtilsV2.fetchClaimsAvailability(allClaimSet, testVal);

    assertTrue(availSet.contains(ClaimType.HHA));
    assertTrue(availSet.contains(ClaimType.SNF));
    assertTrue(availSet.contains(ClaimType.DME));
    assertFalse(availSet.contains(ClaimType.INPATIENT));

    // check efficacy of EnumSet filter vs. bit mask of data.
    EnumSet<ClaimType> someClaimSet = EnumSet.noneOf(ClaimType.class);
    someClaimSet.add(ClaimType.CARRIER);
    someClaimSet.add(ClaimType.PDE);

    availSet = TransformerUtilsV2.fetchClaimsAvailability(someClaimSet, testVal);
    assertFalse(availSet.contains(ClaimType.HHA));
    assertFalse(availSet.contains(ClaimType.SNF));
    assertFalse(availSet.contains(ClaimType.DME));
    assertFalse(availSet.contains(ClaimType.CARRIER));
    // adjust data bit mask and try again
    testVal = testVal | QueryUtils.V_CARRIER_HAS_DATA;
    availSet = TransformerUtilsV2.fetchClaimsAvailability(someClaimSet, testVal);
    assertTrue(availSet.contains(ClaimType.CARRIER));
  }
}
