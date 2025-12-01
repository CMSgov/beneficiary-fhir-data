package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.SearchStyleEnum;
import ca.uhn.fhir.rest.gclient.DateClientParam;
import ca.uhn.fhir.rest.gclient.IQuery;
import ca.uhn.fhir.rest.gclient.TokenClientParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import gov.cms.bfd.server.ng.claim.model.ClaimSubtype;
import gov.cms.bfd.server.ng.eob.EobResourceProvider;
import gov.cms.bfd.server.ng.testUtil.ThreadSafeAppender;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.IdrConstants;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

class EobSearchIT extends IntegrationTestBase {
  @Autowired private EobResourceProvider eobResourceProvider;
  @Autowired private EntityManager entityManager;
  @Mock HttpServletRequest request;

  private IQuery<Bundle> searchBundle() {
    return getFhirClient()
        .search()
        .forResource(ExplanationOfBenefit.class)
        .returnBundle(Bundle.class);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchById(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID)
                    .exactly()
                    .identifier("1071939711295"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(1, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  @Test
  void eobSearchQueryCount() {
    var events = ThreadSafeAppender.startRecord();
    var bundle =
        eobResourceProvider.searchByPatient(
            new ReferenceParam("178083966"), null, null, null, null, null, null, request);
    assertFalse(bundle.getEntry().isEmpty());
    assertEquals(3, queryCount(events));
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByIdEmpty(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(new TokenClientParam(ExplanationOfBenefit.SP_RES_ID).exactly().identifier("999"))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByPatient(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(5, eobBundle.getEntry().size());
    expectFhir().scenario(searchStyle.name()).toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByPatientHistorical(SearchStyleEnum searchStyle) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(HISTORICAL_MERGED_BENE_SK))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void eobSearchByPatientLimitOffset(int offset) {
    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .count(1)
            .offset(offset)
            .execute();
    assertEquals(1, eobBundle.getEntry().size());
    expectFhir().scenario("offset" + offset).toMatchSnapshot(eobBundle);
  }

  @Test
  void eobSearchByDate() {
    var lastUpdated =
        entityManager
            .createQuery(
                "SELECT c.meta.updatedTimestamp FROM Claim c WHERE c.beneficiary.xrefSk = :beneSk",
                ZonedDateTime.class)
            .setParameter("beneSk", BENE_ID_ALL_PARTS_WITH_XREF)
            .getResultList()
            .getFirst();

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .afterOrEquals()
                    .day(DateUtil.toDate(lastUpdated)))
            .execute();
    assertEquals(5, eobBundle.getEntry().size());

    eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(
                new DateClientParam(Constants.PARAM_LASTUPDATED)
                    .before()
                    .day(DateUtil.toDate(lastUpdated.minusDays(1))))
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
  }

  @Test
  void eobSearchByServiceDate() {
    var claimId = "1071939711295";
    var serviceDate =
        (LocalDate)
            entityManager
                .createQuery(
                    "SELECT billablePeriod.claimThroughDate FROM Claim c WHERE c.claimUniqueId = :id",
                    Optional.class)
                .setParameter("id", claimId)
                .getResultList()
                .getFirst()
                .get();

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_RES_ID).exactly().identifier(claimId))
            .and(
                new DateClientParam("service-date")
                    .afterOrEquals()
                    .day(DateUtil.toDate(serviceDate)))
            .execute();
    assertEquals(1, eobBundle.getEntry().size());

    eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT).exactly().identifier(claimId))
            .and(
                new DateClientParam("service-date")
                    .before()
                    .day(DateUtil.toDate(serviceDate.minusDays(1))))
            .execute();
    assertEquals(0, eobBundle.getEntry().size());
  }

  @Test
  void eobSearchByPatientInvalidResourceType() {
    var searchWithidentifier =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier("Blah/" + BENE_ID_ALL_PARTS_WITH_XREF));
    assertThrows(InvalidRequestException.class, searchWithidentifier::execute);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByTag(SearchStyleEnum searchStyle) {
    String validTag = IdrConstants.ADJUDICATION_STATUS_FINAL;

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam(Constants.PARAM_TAG).exactly().identifier(validTag))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        3, eobBundle.getEntry().size(), "Should find EOBs with the specified adjudication status");

    expectFhir().scenario(searchStyle.name() + "_WithTag_" + validTag).toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchBySystemUrlTag(SearchStyleEnum searchStyle) {
    String tagSystem = SystemUrls.SYS_ADJUDICATION_STATUS;
    String tagCode = "Adjudicated";

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(
                new TokenClientParam(Constants.PARAM_TAG)
                    .exactly()
                    .systemAndCode(tagSystem, tagCode))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        3, eobBundle.getEntry().size(), "Should find EOBs with the specified adjudication status");

    expectFhir()
        .scenario(searchStyle.name() + "_WithSystemTag_Adjudicated")
        .toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByTagEmpty(SearchStyleEnum searchStyle) {
    String validTagWithNoMatches = IdrConstants.ADJUDICATION_STATUS_PARTIAL;

    var eobBundle =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(
                new TokenClientParam(Constants.PARAM_TAG)
                    .exactly()
                    .identifier(validTagWithNoMatches))
            .usingStyle(searchStyle)
            .execute();

    expectFhir().scenario(searchStyle.name() + "_WithTag_EmptyResult").toMatchSnapshot(eobBundle);
  }

  @ParameterizedTest
  @EnumSource(SearchStyleEnum.class)
  void eobSearchByType(SearchStyleEnum searchStyle) {
    var outpatientType = ClaimSubtype.OUTPATIENT.getCode();

    var eobBundleOutpatient =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam("type").exactly().identifier(outpatientType))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(
        3,
        eobBundleOutpatient.getEntry().size(),
        "Should find EOBs with the outpatient claim type");

    expectFhir()
        .scenario(searchStyle.name() + "_WithClaimType_" + outpatientType)
        .toMatchSnapshot(eobBundleOutpatient);

    var hhaType = ClaimSubtype.HHA.getCode();

    var eobBundleMultipleTypes =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam("type").exactly().identifier(hhaType))
            .and(new TokenClientParam("type").exactly().identifier(outpatientType))
            .usingStyle(searchStyle)
            .execute();
    assertEquals(
        4,
        eobBundleMultipleTypes.getEntry().size(),
        "Should find EOBs with both HHA and Outpatient claim types");

    expectFhir()
        .scenario(searchStyle.name() + "_WithMultipleClaimTypes_" + hhaType + "," + outpatientType)
        .toMatchSnapshot(eobBundleMultipleTypes);

    var wildcardType = "*";
    var eobBundleWildcard =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam("type").exactly().identifier(wildcardType))
            .usingStyle(searchStyle)
            .execute();

    assertEquals(5, eobBundleWildcard.getEntry().size(), "Should find ALL EOBs for '*' type");
    expectFhir().scenario(searchStyle.name() + "_WithWildcard").toMatchSnapshot(eobBundleWildcard);

    String[] zeroResultClaimTypes = {
      ClaimSubtype.DME.getCode(),
      ClaimSubtype.SNF.getCode(),
      ClaimSubtype.PDE.getCode(),
      ClaimSubtype.HOSPICE.getCode(),
      ClaimSubtype.INPATIENT.getCode()
    };

    for (var claimType : zeroResultClaimTypes) {
      var eobBundleZero =
          searchBundle()
              .where(
                  new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                      .exactly()
                      .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
              .and(new TokenClientParam("type").exactly().identifier(claimType))
              .usingStyle(searchStyle)
              .execute();

      assertEquals(
          0,
          eobBundleZero.getEntry().size(),
          "Should find 0 EOBs for " + claimType + " claim type for this patient");
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "invalidStatus",
        "in-progress",
        "test",
        "https://bluebutton.cms.gov/fhir/CodeSystem/test"
      })
  void eobSearchByInvalidTagBadRequest(String invalidTag) {
    var searchWithIdentifier =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(BENE_ID_ALL_PARTS_WITH_XREF))
            .and(new TokenClientParam(Constants.PARAM_TAG).exactly().identifier(invalidTag));
    assertThrows(
        InvalidRequestException.class,
        searchWithIdentifier::execute,
        "Should throw InvalidRequestException for unsupported _tag value: " + invalidTag);
  }

  @Test
  void returnsCorrectClaimsWhenFilteringWithMergedBenes() {
    // Ensure claim IDs are de-duplicated correctly when using a limit
    // This could have problems with merged benes since they can produce duplicate claim IDs if not
    // filtered properly

    // There should be more than one bene_sk/xref_sk pair here to ensure this test has the
    // correct preconditions.
    var beneCount =
        (Integer)
            entityManager
                .createNativeQuery(
                    "SELECT COUNT(*) FROM idr.beneficiary WHERE bene_xref_efctv_sk_computed ="
                        + " :beneSk AND bene_sk = :beneSk",
                    Integer.class)
                .setParameter("beneSk", Long.parseLong(CURRENT_MERGED_BENE_SK))
                .getResultList()
                .getFirst();
    assertTrue(beneCount > 1);

    var searchWithIdentifier =
        searchBundle()
            .where(
                new TokenClientParam(ExplanationOfBenefit.SP_PATIENT)
                    .exactly()
                    .identifier(CURRENT_MERGED_BENE_SK))
            .count(2);
    var bundle = searchWithIdentifier.execute();
    var results =
        bundle.getEntry().stream().map(e -> e.getResource().getId()).collect(Collectors.toSet());
    assertEquals(2, results.size());
  }
}
