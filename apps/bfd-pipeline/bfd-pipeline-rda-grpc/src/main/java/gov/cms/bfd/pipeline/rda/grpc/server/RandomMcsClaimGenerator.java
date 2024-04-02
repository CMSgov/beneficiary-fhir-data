package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import gov.cms.mpsm.rda.v1.mcs.McsAdjustment;
import gov.cms.mpsm.rda.v1.mcs.McsAudit;
import gov.cms.mpsm.rda.v1.mcs.McsAuditIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsBeneficiarySex;
import gov.cms.mpsm.rda.v1.mcs.McsBillingProviderIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsBillingProviderStatusCode;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaimAssignmentCode;
import gov.cms.mpsm.rda.v1.mcs.McsClaimLevelIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsClaimType;
import gov.cms.mpsm.rda.v1.mcs.McsCutbackAuditDisposition;
import gov.cms.mpsm.rda.v1.mcs.McsCutbackAuditIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDetailStatus;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisIcdType;
import gov.cms.mpsm.rda.v1.mcs.McsLocation;
import gov.cms.mpsm.rda.v1.mcs.McsLocationActivityCode;
import gov.cms.mpsm.rda.v1.mcs.McsSplitReasonCode;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import gov.cms.mpsm.rda.v1.mcs.McsTwoDigitPlanOfService;
import gov.cms.mpsm.rda.v1.mcs.McsTypeOfService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Objects of this class create populated {@link McsClaim} objects using random data. The purpose is
 * simply to rapidly produce objects for pipeline testing to try out different scenarios for
 * transformation. The purpose is NOT to produce realistic/valid data. The random number seed is
 * settable in the constructor to allow for predictable unit tests. Every optional field has a 50%
 * chance of being present in each claim. Arrays have randomly assigned variable length (including
 * zero).
 */
public class RandomMcsClaimGenerator extends AbstractRandomClaimGenerator<McsClaim> {
  /** The max adjustments to generate. */
  private static final int MAX_ADJUSTMENTS = 10;

  /** The max audits to generate. */
  private static final int MAX_AUDITS = 20;

  /** The max details to generate. */
  private static final int MAX_DETAILS = 4;

  /** The max diagnosis codes to generate. */
  private static final int MAX_DIAG_CODES = 7;

  /** The max locations to generate. */
  private static final int MAX_LOCATIONS = 8;

  /** A list of the enums for the MCS claim types. */
  private static final List<McsClaimType> McsClaimTypeEnums = enumValues(McsClaimType.values());

  /** A list of the enums for the MCS beneficiary sexes. */
  private static final List<McsBeneficiarySex> McsBeneficiarySexEnums =
      enumValues(McsBeneficiarySex.values());

  /** A list of the enums for the MCS status codes. */
  private static final List<McsStatusCode> McsStatusCodeEnums;

  /** A list of the enums for the MCS billing provider indicators. */
  private static final List<McsBillingProviderIndicator> McsBillingProviderIndicatorEnums =
      enumValues(McsBillingProviderIndicator.values());

  /** A list of the enums for the MCS billing provider status codes. */
  private static final List<McsBillingProviderStatusCode> McsBillingProviderStatusCodeEnums =
      enumValues(McsBillingProviderStatusCode.values());

  /** A list of the enums for the MCS diagnosis icd types. */
  private static final List<McsDiagnosisIcdType> McsDiagnosisIcdTypeEnums =
      enumValues(McsDiagnosisIcdType.values());

  /** A list of the enums for the MCS detail statuses. */
  private static final List<McsDetailStatus> McsDetailStatusEnums =
      enumValues(McsDetailStatus.values());

  /** A list of the enums for the MCS claim assignment codes. */
  private static final List<McsClaimAssignmentCode> McsClaimAssignmentCodeEnums =
      enumValues(McsClaimAssignmentCode.values());

  /** A list of the enums for the MCS claim level indicators. */
  private static final List<McsClaimLevelIndicator> McsClaimLevelIndicatorEnums =
      enumValues(McsClaimLevelIndicator.values());

  /** A list of the enums for the MCS audit indicators. */
  private static final List<McsAuditIndicator> McsAuditIndicatorEnums =
      enumValues(McsAuditIndicator.values());

  /** A list of the enums for the MCS split reason codes. */
  private static final List<McsSplitReasonCode> McsSplitReasonCodeEnums =
      enumValues(McsSplitReasonCode.values());

  /** A list of the enums for the MCS types of service. */
  private static final List<McsTypeOfService> McsTypeOfServiceEnums =
      enumValues(McsTypeOfService.values());

  /** A list of the enums for the MCS two digit plan of services. */
  private static final List<McsTwoDigitPlanOfService> McsTwoDigitPlanOfServiceEnums =
      enumValues(McsTwoDigitPlanOfService.values());

  /** A list of the enums for the MCS cutback audit indicators. */
  private static final List<McsCutbackAuditIndicator> McsCutbackAuditIndicatorEnums =
      enumValues(McsCutbackAuditIndicator.values());

  /** A list of the enums for the MCS cutback audit dispositions. */
  private static final List<McsCutbackAuditDisposition> McsCutbackAuditDispositionEnums =
      enumValues(McsCutbackAuditDisposition.values());

  /** A list of the enums for the MCS location activity codes. */
  private static final List<McsLocationActivityCode> McsLocationActivityCodeEnums =
      enumValues(McsLocationActivityCode.values());

  /** Max length of a FISS claim id. */
  private static final int McsClaimIdLength = 15;

  /** Max length of a MBI. */
  private static final int MbiLength = 11;

  /** Field length used when forcing a transformation error in a claim. */
  @VisibleForTesting static final int ForcedErrorFieldLength = 50;

  static {
    // MCS status codes are special since the STATUS_CODE_NOT_USED is considered invalid by the
    // transformer. This block preserves the natural ordering of the enum values while removing the
    // bad value.
    Set<McsStatusCode> statusCodes = new LinkedHashSet<>(Arrays.asList(McsStatusCode.values()));
    statusCodes.remove(McsStatusCode.STATUS_CODE_NOT_USED);
    McsStatusCodeEnums = enumValues(new ArrayList<>(statusCodes).toArray(new McsStatusCode[0]));
  }

  /**
   * Creates an instance with the specified settings.
   *
   * @param config configuration settings
   */
  public RandomMcsClaimGenerator(RandomClaimGeneratorConfig config) {
    super(config);
  }

  @Override
  public McsClaim createRandomClaim() {
    final int detailCount = 1 + randomInt(MAX_DETAILS);
    McsClaim.Builder claim = McsClaim.newBuilder();

    always(
        "mcs",
        () -> {
          always(
              "idrClmHdIcn",
              () -> claim.setIdrClmHdIcn(randomDigit(McsClaimIdLength, McsClaimIdLength)));
          final String idrClmHdIcn = claim.getIdrClmHdIcn();

          addRandomFieldValues(claim, detailCount);
          addAdjustments(claim);
          addAudits(claim);
          addDiagnosisCodes(claim, idrClmHdIcn);
          addDetails(claim, detailCount);
          addLocations(claim);
          adjustServiceDatesFromDetails(claim);
          adjustFieldsBasedOnConfigOverrides(claim);
        });
    return claim.build();
  }

  /**
   * Adds random values to the basic fields of the given claim object.
   *
   * @param claim The claim object to add random base field values to
   * @param detailCount the detail count
   */
  private void addRandomFieldValues(McsClaim.Builder claim, int detailCount) {
    always("idrContrId", () -> claim.setIdrContrId(randomDigit(1, 5)));
    optional("idrHic", () -> claim.setIdrHic(randomDigit(1, 12)));
    oneOf(
        "idrClaimType",
        () -> claim.setIdrClaimTypeEnum(randomEnum(McsClaimTypeEnums)),
        () -> claim.setIdrClaimTypeUnrecognized(randomLetter(1, 1)));
    always("idrDtlCnt", () -> claim.setIdrDtlCnt(detailCount));
    optional("idrBeneLast16", () -> claim.setIdrBeneLast16(randomLetter(1, 6)));
    optional("idrBeneFirstInit", () -> claim.setIdrBeneFirstInit(randomLetter(1, 1)));
    optional("idrBeneMidInit", () -> claim.setIdrBeneMidInit(randomLetter(1, 1)));
    oneOf(
        "idrBeneSex",
        () -> claim.setIdrBeneSexEnum(randomEnum(McsBeneficiarySexEnums)),
        () -> claim.setIdrBeneSexUnrecognized(randomLetter(1, 1)));
    claim.setIdrStatusCodeEnum(randomEnum(McsStatusCodeEnums));
    optional("idrStatusDate", () -> claim.setIdrStatusDate(randomDate()));
    optional("idrBillProvNpi", () -> claim.setIdrBillProvNpi(randomAlphaNumeric(1, 10)));
    optional("idrBillProvNum", () -> claim.setIdrBillProvNum(randomDigit(1, 10)));
    optional("idrBillProvEin", () -> claim.setIdrBillProvEin(randomAlphaNumeric(1, 10)));
    optional("idrBillProvType", () -> claim.setIdrBillProvType(randomLetter(1, 2)));
    optional("idrBillProvSpec", () -> claim.setIdrBillProvSpec(randomAlphaNumeric(1, 2)));
    oneOf(
        "idrBillProvGroupInd",
        () -> claim.setIdrBillProvGroupIndEnum(randomEnum(McsBillingProviderIndicatorEnums)),
        () -> claim.setIdrBillProvGroupIndUnrecognized(randomLetter(1, 1)));
    optional("idrBillProvPriceSpec", () -> claim.setIdrBillProvPriceSpec(randomAlphaNumeric(1, 2)));
    optional("idrBillProvCounty", () -> claim.setIdrBillProvCounty(randomAlphaNumeric(1, 2)));
    optional("idrBillProvLoc", () -> claim.setIdrBillProvLoc(randomAlphaNumeric(1, 2)));
    optional("idrTotAllowed", () -> claim.setIdrTotAllowed(randomAmount()));
    optional("idrCoinsurance", () -> claim.setIdrCoinsurance(randomAmount()));
    optional("idrDeductible", () -> claim.setIdrDeductible(randomAmount()));
    oneOf(
        "idrBillProvStatusCd",
        () -> claim.setIdrBillProvStatusCdEnum(randomEnum(McsBillingProviderStatusCodeEnums)),
        () -> claim.setIdrBillProvStatusCdUnrecognized(randomLetter(1, 1)));
    optional("idrTotBilledAmt", () -> claim.setIdrTotBilledAmt(randomAmount()));
    optional("idrClaimReceiptDate", () -> claim.setIdrClaimReceiptDate(randomDate()));
    optional("idrClaimMbi", () -> claim.setIdrClaimMbi(randomAlphaNumeric(MbiLength, MbiLength)));
    // IdrHdrFromDos will be set later
    // IdrHdrToDos will be set later
    oneOf(
        "idrAssignment",
        () -> claim.setIdrAssignmentEnum(randomEnum(McsClaimAssignmentCodeEnums)),
        () -> claim.setIdrAssignmentUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        "idrClmLevelInd",
        () -> claim.setIdrClmLevelIndEnum(randomEnum(McsClaimLevelIndicatorEnums)),
        () -> claim.setIdrClmLevelIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional("idrHdrAudit", (() -> claim.setIdrHdrAudit(randomInt(32767))));
    oneOf(
        "idrHdrAuditInd",
        () -> claim.setIdrHdrAuditIndEnum(randomEnum(McsAuditIndicatorEnums)),
        () -> claim.setIdrHdrAuditIndUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        "idrUSplitReason",
        () -> claim.setIdrUSplitReasonEnum(randomEnum(McsSplitReasonCodeEnums)),
        () -> claim.setIdrUSplitReasonUnrecognized(randomAlphaNumeric(1, 1)));
    optional(
        "idrJReferringProvNpi", () -> claim.setIdrJReferringProvNpi(randomAlphaNumeric(1, 10)));
    optional("idrJFacProvNpi", () -> claim.setIdrJFacProvNpi(randomAlphaNumeric(1, 10)));
    optional("idrUDemoProvNpi", () -> claim.setIdrUDemoProvNpi(randomAlphaNumeric(1, 10)));
    optional("idrUSuperNpi", () -> claim.setIdrUSuperNpi(randomAlphaNumeric(1, 10)));
    optional("idrUFcadjBilNpi", () -> claim.setIdrUFcadjBilNpi(randomAlphaNumeric(1, 10)));
    optional(
        "idrAmbPickupAddresLine1",
        () -> claim.setIdrAmbPickupAddresLine1(randomAlphaNumeric(1, 25)));
    optional(
        "idrAmbPickupAddresLine2",
        () -> claim.setIdrAmbPickupAddresLine2(randomAlphaNumeric(1, 20)));
    optional("idrAmbPickupCity", () -> claim.setIdrAmbPickupCity(randomAlphaNumeric(1, 20)));
    optional("idrAmbPickupState", () -> claim.setIdrAmbPickupState(randomAlphaNumeric(1, 2)));
    optional("idrAmbPickupZipcode", () -> claim.setIdrAmbPickupZipcode(randomAlphaNumeric(1, 9)));
    optional("idrAmbDropoffName", () -> claim.setIdrAmbDropoffName(randomAlphaNumeric(1, 24)));
    optional(
        "idrAmbDropoffAddrLine1", () -> claim.setIdrAmbDropoffAddrLine1(randomAlphaNumeric(1, 25)));
    optional(
        "idrAmbDropoffAddrLine2", () -> claim.setIdrAmbDropoffAddrLine2(randomAlphaNumeric(1, 20)));
    optional("idrAmbDropoffCity", () -> claim.setIdrAmbDropoffCity(randomAlphaNumeric(1, 20)));
    optional("idrAmbDropoffState", () -> claim.setIdrAmbDropoffState(randomAlphaNumeric(1, 2)));
    optional("idrAmbDropoffZipcode", () -> claim.setIdrAmbDropoffZipcode(randomAlphaNumeric(1, 9)));
  }

  /**
   * Adds randomly generated adjustment objects to the claim.
   *
   * @param claim The claim object instance to add random adjustment objects to
   */
  private void addAdjustments(McsClaim.Builder claim) {
    always(
        "adjustment",
        () -> {
          final int count = randomInt(MAX_ADJUSTMENTS);

          for (int i = 1; i <= count; ++i) {
            final McsAdjustment.Builder adjustment = McsAdjustment.newBuilder();

            always(
                String.format("[%d]", i),
                () -> {
                  optional("idrAdjDate", () -> adjustment.setIdrAdjDate(randomDate()));
                  optional("idrXrefIcn", () -> adjustment.setIdrXrefIcn(randomAlphaNumeric(1, 15)));
                  optional(
                      "idrAdjClerk", () -> adjustment.setIdrAdjClerk(randomAlphaNumeric(1, 4)));
                  optional("idrInitCcn", () -> adjustment.setIdrInitCcn(randomAlphaNumeric(1, 15)));
                  optional("idrAdjChkWrtDt", () -> adjustment.setIdrAdjChkWrtDt(randomDate()));
                  optional("idrAdjBEombAmt", () -> adjustment.setIdrAdjBEombAmt(randomAmount()));
                  optional("idrAdjPEombAmt", () -> adjustment.setIdrAdjPEombAmt(randomAmount()));
                });

            adjustment.setRdaPosition(i);
            claim.addMcsAdjustments(adjustment.build());
          }
        });
  }

  /**
   * Adds randomly generated audit objects to the claim.
   *
   * @param claim The claim object instance to add random audit objects to
   */
  private void addAudits(McsClaim.Builder claim) {
    always(
        "audit",
        () -> {
          final int count = randomInt(MAX_AUDITS);

          for (int i = 1; i <= count; ++i) {
            final McsAudit.Builder audit = McsAudit.newBuilder();

            always(
                String.format("[%d]", i),
                () -> {
                  optional("idrJAuditNum", () -> audit.setIdrJAuditNum(randomInt(32767)));
                  oneOf(
                      "idrJAuditInd",
                      () -> audit.setIdrJAuditIndEnum(randomEnum(McsCutbackAuditIndicatorEnums)),
                      () -> audit.setIdrJAuditIndUnrecognized(randomAlphaNumeric(1, 1)));
                  oneOf(
                      "idrJAuditDisp",
                      () -> audit.setIdrJAuditDispEnum(randomEnum(McsCutbackAuditDispositionEnums)),
                      () -> audit.setIdrJAuditDispUnrecognized(randomAlphaNumeric(1, 1)));
                });

            audit.setRdaPosition(i);
            claim.addMcsAudits(audit.build());
          }
        });
  }

  /**
   * Adds a random diagnosis code to the given claim object.
   *
   * @param claim The claim object to add the random diagnosis code to
   * @param idrClmHdIcn The ICN of the current claim being created
   */
  private void addDiagnosisCodes(McsClaim.Builder claim, String idrClmHdIcn) {
    always(
        "diagnosisCode",
        () -> {
          final int count = randomInt(MAX_DIAG_CODES);

          for (int i = 1; i <= count; ++i) {
            final McsDiagnosisCode.Builder code = McsDiagnosisCode.newBuilder();
            code.setIdrClmHdIcn(idrClmHdIcn);

            always(
                String.format("[%d]", i),
                () -> {
                  oneOf(
                      "idrDiagIcdType",
                      () -> code.setIdrDiagIcdTypeEnum(randomEnum(McsDiagnosisIcdTypeEnums)),
                      () -> code.setIdrDiagIcdTypeUnrecognized(randomLetter(1, 1)));
                  always("idrDiagCode", () -> code.setIdrDiagCode(randomAlphaNumeric(1, 7)));
                });

            code.setRdaPosition(i);
            claim.addMcsDiagnosisCodes(code.build());
          }
        });
  }

  /**
   * Adds a random details to the given claim object.
   *
   * @param claim The claim object to add the random details to
   * @param detailCount The number of details to add
   */
  private void addDetails(McsClaim.Builder claim, int detailCount) {
    always(
        "detail",
        () -> {
          for (int i = 1; i <= detailCount; ++i) {
            final McsDetail.Builder detail = McsDetail.newBuilder();
            detail.setIdrDtlNumber(i);

            always(
                String.format("[%d]", i),
                () -> {
                  oneOf(
                      "idrDtlStatus",
                      () -> detail.setIdrDtlStatusEnum(randomEnum(McsDetailStatusEnums)),
                      () -> detail.setIdrDtlStatusUnrecognized(randomLetter(1, 1)));
                  optional(
                      "idrDtlDates",
                      () -> {
                        final List<String> dates = Arrays.asList(randomDate(), randomDate());
                        dates.sort(Comparator.naturalOrder());
                        detail.setIdrDtlFromDate(dates.get(0));
                        detail.setIdrDtlToDate(dates.get(1));
                      });
                  optional("idrProcCode", () -> detail.setIdrProcCode(randomAlphaNumeric(1, 5)));
                  optional("idrModOne", () -> detail.setIdrModOne(randomAlphaNumeric(1, 2)));
                  optional("idrModTwo", () -> detail.setIdrModTwo(randomAlphaNumeric(1, 2)));
                  optional("idrModThree", () -> detail.setIdrModThree(randomAlphaNumeric(1, 2)));
                  optional("idrModFour", () -> detail.setIdrModFour(randomAlphaNumeric(1, 2)));
                  oneOf(
                      "idrDtlDiagIcdType",
                      () -> detail.setIdrDtlDiagIcdTypeEnum(randomEnum(McsDiagnosisIcdTypeEnums)),
                      () -> detail.setIdrDtlDiagIcdTypeUnrecognized(randomLetter(1, 1)));
                  optional(
                      "idrDtlPrimaryDiagCode",
                      () -> detail.setIdrDtlPrimaryDiagCode(randomAlphaNumeric(1, 7)));
                  optional(
                      "idrKPosLnameOrg", (() -> detail.setIdrKPosLnameOrg(randomLetter(1, 60))));
                  optional("idrKPosFname", (() -> detail.setIdrKPosFname(randomLetter(1, 35))));
                  optional("idrKPosMname", (() -> detail.setIdrKPosMname(randomLetter(1, 25))));
                  optional(
                      "idrKPosAddr1", (() -> detail.setIdrKPosAddr1(randomAlphaNumeric(1, 55))));
                  optional(
                      "idrKPosAddr21St",
                      (() -> detail.setIdrKPosAddr21St(randomAlphaNumeric(1, 30))));
                  optional(
                      "idrKPosAddr22Nd",
                      (() -> detail.setIdrKPosAddr22Nd(randomAlphaNumeric(1, 25))));
                  optional("idrKPosCity", (() -> detail.setIdrKPosCity(randomLetter(1, 30))));
                  optional("idrKPosState", (() -> detail.setIdrKPosState(randomLetter(1, 2))));
                  optional("idrKPosZip", (() -> detail.setIdrKPosZip(randomDigit(1, 15))));
                  oneOf(
                      "idrTos",
                      () -> detail.setIdrTosEnum(randomEnum(McsTypeOfServiceEnums)),
                      () -> detail.setIdrTosUnrecognized(randomAlphaNumeric(1, 1)));
                  oneOf(
                      "idrTwoDigitPos",
                      () -> detail.setIdrTwoDigitPosEnum(randomEnum(McsTwoDigitPlanOfServiceEnums)),
                      () -> detail.setIdrTwoDigitPosUnrecognized(randomAlphaNumeric(1, 2)));
                  optional(
                      "idrDtlRendType", (() -> detail.setIdrDtlRendType(randomAlphaNumeric(1, 2))));
                  optional(
                      "idrDtlRendSpec", (() -> detail.setIdrDtlRendSpec(randomAlphaNumeric(1, 2))));
                  optional(
                      "idrDtlRendNpi", (() -> detail.setIdrDtlRendNpi(randomAlphaNumeric(1, 10))));
                  optional(
                      "idrDtlRendProv",
                      (() -> detail.setIdrDtlRendProv(randomAlphaNumeric(1, 10))));
                  optional(
                      "idrKDtlFacProvNpi",
                      (() -> detail.setIdrKDtlFacProvNpi(randomAlphaNumeric(1, 10))));
                  optional(
                      "idrDtlAmbPickupAddres1",
                      (() -> detail.setIdrDtlAmbPickupAddres1(randomAlphaNumeric(1, 25))));
                  optional(
                      "idrDtlAmbPickupAddres2",
                      (() -> detail.setIdrDtlAmbPickupAddres2(randomAlphaNumeric(1, 20))));
                  optional(
                      "idrDtlAmbPickupCity",
                      (() -> detail.setIdrDtlAmbPickupCity(randomAlphaNumeric(1, 20))));
                  optional(
                      "idrDtlAmbPickupState",
                      (() -> detail.setIdrDtlAmbPickupState(randomAlphaNumeric(1, 2))));
                  optional(
                      "idrDtlAmbPickupZipcode",
                      (() -> detail.setIdrDtlAmbPickupZipcode(randomAlphaNumeric(1, 9))));
                  optional(
                      "idrDtlAmbDropoffName",
                      (() -> detail.setIdrDtlAmbDropoffName(randomAlphaNumeric(1, 24))));
                  optional(
                      "idrDtlAmbDropoffAddrL1",
                      (() -> detail.setIdrDtlAmbDropoffAddrL1(randomAlphaNumeric(1, 25))));
                  optional(
                      "idrDtlAmbDropoffAddrL2",
                      (() -> detail.setIdrDtlAmbDropoffAddrL2(randomAlphaNumeric(1, 20))));
                  optional(
                      "idrDtlAmbDropoffCity",
                      (() -> detail.setIdrDtlAmbDropoffCity(randomAlphaNumeric(1, 20))));
                  optional(
                      "idrDtlAmbDropoffState",
                      (() -> detail.setIdrDtlAmbDropoffState(randomAlphaNumeric(1, 2))));
                  optional(
                      "idrDtlAmbDropoffZipcode",
                      (() -> detail.setIdrDtlAmbDropoffZipcode(randomAlphaNumeric(1, 9))));
                  optional("idrDtlNdc", (() -> detail.setIdrDtlNdc(randomAlphaNumeric(1, 48))));
                  optional(
                      "idrDtlNdcUnitCount",
                      (() -> detail.setIdrDtlNdcUnitCount(randomAlphaNumeric(1, 15))));
                });

            claim.addMcsDetails(detail.build());
          }
        });
  }

  /**
   * Adds randomly generated location objects to the claim.
   *
   * @param claim The claim object instance to add random location objects to
   */
  private void addLocations(McsClaim.Builder claim) {
    always(
        "location",
        () -> {
          final int count = randomInt(MAX_LOCATIONS);

          for (int i = 1; i <= count; ++i) {
            final McsLocation.Builder location = McsLocation.newBuilder();

            always(
                String.format("[%d]", i),
                () -> {
                  optional(
                      "idrLocClerk", (() -> location.setIdrLocClerk(randomAlphaNumeric(1, 4))));
                  optional("idrLocCode", (() -> location.setIdrLocCode(randomAlphaNumeric(1, 3))));
                  optional("idrLocDate", (() -> location.setIdrLocDate(randomDate())));
                  oneOf(
                      "idrLocActvCode",
                      () ->
                          location.setIdrLocActvCodeEnum(randomEnum(McsLocationActivityCodeEnums)),
                      () -> location.setIdrLocActvCodeUnrecognized(randomAlphaNumeric(1, 1)));
                });

            location.setRdaPosition(i);
            claim.addMcsLocations(location.build());
          }
        });
  }

  /**
   * Ensures the "from" date and "to" date are set using the min/max dates in all the details (if
   * any). If either is missing from the details neither of the dates are set in the claim.
   *
   * @param claim the claim to adjust
   */
  private void adjustServiceDatesFromDetails(McsClaim.Builder claim) {
    final Optional<String> minDate =
        claim.getMcsDetailsList().stream()
            .map(detail -> Strings.nullToEmpty(detail.getIdrDtlFromDate()))
            .filter(date -> date.length() > 0)
            .min(Comparator.naturalOrder());
    final Optional<String> maxDate =
        claim.getMcsDetailsList().stream()
            .map(detail -> Strings.nullToEmpty(detail.getIdrDtlToDate()))
            .filter(date -> date.length() > 0)
            .max(Comparator.naturalOrder());
    if (minDate.isPresent() && maxDate.isPresent()) {
      claim.setIdrHdrFromDos(minDate.get());
      claim.setIdrHdrToDos(maxDate.get());
    }
  }

  /**
   * Replace field values if necessary based on the overrides specified in the {@link
   * RandomClaimGeneratorConfig}.
   *
   * @param claim claim to be updated
   */
  private void adjustFieldsBasedOnConfigOverrides(McsClaim.Builder claim) {
    if (getMaxUniqueClaimIds() > 0) {
      claim.setIdrClmHdIcn(randomDigitStringInRange(McsClaimIdLength, getMaxUniqueClaimIds()));
    }
    if (getMaxUniqueMbis() > 0 && !claim.getIdrClaimMbi().isEmpty()) {
      claim.setIdrClaimMbi(randomDigitStringInRange(MbiLength, getMaxUniqueMbis()));
    }
    if (shouldInsertErrorIntoCurrentClaim()) {
      // update a random field with a string that is too long
      oneOf(
          "random-error",
          () -> claim.setIdrContrId(randomDigit(ForcedErrorFieldLength, ForcedErrorFieldLength)),
          () -> claim.setIdrHic(randomDigit(ForcedErrorFieldLength, ForcedErrorFieldLength)));
    }
  }
}
