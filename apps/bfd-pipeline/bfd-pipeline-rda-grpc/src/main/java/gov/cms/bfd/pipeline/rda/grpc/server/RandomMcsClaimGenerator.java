package gov.cms.bfd.pipeline.rda.grpc.server;

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
import java.time.Clock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RandomMcsClaimGenerator extends AbstractRandomClaimGenerator {
  private static final int MAX_ADJUSTMENTS = 10;
  private static final int MAX_AUDITS = 20;
  private static final int MAX_DETAILS = 4;
  private static final int MAX_DIAG_CODES = 7;
  private static final int MAX_LOCATIONS = 8;
  private static final List<McsClaimType> McsClaimTypeEnums = enumValues(McsClaimType.values());
  private static final List<McsBeneficiarySex> McsBeneficiarySexEnums =
      enumValues(McsBeneficiarySex.values());
  private static final List<McsStatusCode> McsStatusCodeEnums;
  private static final List<McsBillingProviderIndicator> McsBillingProviderIndicatorEnums =
      enumValues(McsBillingProviderIndicator.values());
  private static final List<McsBillingProviderStatusCode> McsBillingProviderStatusCodeEnums =
      enumValues(McsBillingProviderStatusCode.values());
  private static final List<McsDiagnosisIcdType> McsDiagnosisIcdTypeEnums =
      enumValues(McsDiagnosisIcdType.values());
  private static final List<McsDetailStatus> McsDetailStatusEnums =
      enumValues(McsDetailStatus.values());
  private static final List<McsClaimAssignmentCode> McsClaimAssignmentCodeEnums =
      enumValues(McsClaimAssignmentCode.values());
  private static final List<McsClaimLevelIndicator> McsClaimLevelIndicatorEnums =
      enumValues(McsClaimLevelIndicator.values());
  private static final List<McsAuditIndicator> McsAuditIndicatorEnums =
      enumValues(McsAuditIndicator.values());
  private static final List<McsSplitReasonCode> McsSplitReasonCodeEnums =
      enumValues(McsSplitReasonCode.values());
  private static final List<McsTypeOfService> McsTypeOfServiceEnums =
      enumValues(McsTypeOfService.values());
  private static final List<McsTwoDigitPlanOfService> McsTwoDigitPlanOfServiceEnums =
      enumValues(McsTwoDigitPlanOfService.values());
  private static final List<McsCutbackAuditIndicator> McsCutbackAuditIndicatorEnums =
      enumValues(McsCutbackAuditIndicator.values());
  private static final List<McsCutbackAuditDisposition> McsCutbackAuditDispositionEnums =
      enumValues(McsCutbackAuditDisposition.values());
  private static final List<McsLocationActivityCode> McsLocationActivityCodeEnums =
      enumValues(McsLocationActivityCode.values());

  static {
    // MCS status codes are special since the STATUS_CODE_NOT_USED is considered invalid by the
    // transformer. This block preserves the natural ordering of the enum values while removing the
    // bad value.
    Set<McsStatusCode> statusCodes = new LinkedHashSet<>(Arrays.asList(McsStatusCode.values()));
    statusCodes.remove(McsStatusCode.STATUS_CODE_NOT_USED);
    McsStatusCodeEnums = enumValues(new ArrayList<>(statusCodes).toArray(new McsStatusCode[0]));
  }

  /**
   * Creates an instance with the specified seed.
   *
   * @param seed seed for the PRNG
   */
  public RandomMcsClaimGenerator(long seed) {
    super(seed, false, Clock.systemUTC());
  }

  /**
   * Creates an instance for use in unit tests. Setting optionalTrue to true causes all optional
   * fields to be added to the claim. This is useful in some tests.
   *
   * @param seed seed for the PRNG
   * @param optionalTrue true if all optional fields should be populated
   */
  public RandomMcsClaimGenerator(long seed, boolean optionalTrue, Clock clock) {
    super(seed, optionalTrue, clock);
  }

  public McsClaim randomClaim() {
    final int detailCount = 1 + randomInt(MAX_DETAILS);
    final String idrClmHdIcn = randomDigit(5, 8);
    McsClaim.Builder claim = McsClaim.newBuilder();
    addRandomFieldValues(claim, idrClmHdIcn, detailCount);
    addAdjustments(claim);
    addAudits(claim);
    addDiagnosisCodes(claim, idrClmHdIcn);
    addDetails(claim, detailCount);
    addLocations(claim);
    adjustServiceDatesFromDetails(claim);
    return claim.build();
  }

  private void addRandomFieldValues(McsClaim.Builder claim, String idrClmHdIcn, int detailCount) {
    claim.setIdrClmHdIcn(idrClmHdIcn);
    claim.setIdrContrId(randomDigit(1, 5));
    optional(() -> claim.setIdrHic(randomDigit(1, 12)));
    oneOf(
        () -> claim.setIdrClaimTypeEnum(randomEnum(McsClaimTypeEnums)),
        () -> claim.setIdrClaimTypeUnrecognized(randomLetter(1, 1)));
    claim.setIdrDtlCnt(detailCount);
    optional(() -> claim.setIdrBeneLast16(randomLetter(1, 6)));
    optional(() -> claim.setIdrBeneFirstInit(randomLetter(1, 1)));
    optional(() -> claim.setIdrBeneMidInit(randomLetter(1, 1)));
    oneOf(
        () -> claim.setIdrBeneSexEnum(randomEnum(McsBeneficiarySexEnums)),
        () -> claim.setIdrBeneSexUnrecognized(randomLetter(1, 1)));
    claim.setIdrStatusCodeEnum(randomEnum(McsStatusCodeEnums));
    optional(() -> claim.setIdrStatusDate(randomDate()));
    optional(() -> claim.setIdrBillProvNpi(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrBillProvNum(randomDigit(1, 10)));
    optional(() -> claim.setIdrBillProvEin(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrBillProvType(randomLetter(1, 2)));
    optional(() -> claim.setIdrBillProvSpec(randomAlphaNumeric(1, 2)));
    oneOf(
        () -> claim.setIdrBillProvGroupIndEnum(randomEnum(McsBillingProviderIndicatorEnums)),
        () -> claim.setIdrBillProvGroupIndUnrecognized(randomLetter(1, 1)));
    optional(() -> claim.setIdrBillProvPriceSpec(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrBillProvCounty(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrBillProvLoc(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrTotAllowed(randomAmount()));
    optional(() -> claim.setIdrCoinsurance(randomAmount()));
    optional(() -> claim.setIdrDeductible(randomAmount()));
    oneOf(
        () -> claim.setIdrBillProvStatusCdEnum(randomEnum(McsBillingProviderStatusCodeEnums)),
        () -> claim.setIdrBillProvStatusCdUnrecognized(randomLetter(1, 1)));
    optional(() -> claim.setIdrTotBilledAmt(randomAmount()));
    optional(() -> claim.setIdrClaimReceiptDate(randomDate()));
    optional(() -> claim.setIdrClaimMbi(randomAlphaNumeric(11, 11)));
    // IdrHdrFromDos will be set later
    // IdrHdrToDos will be set later
    oneOf(
        () -> claim.setIdrAssignmentEnum(randomEnum(McsClaimAssignmentCodeEnums)),
        () -> claim.setIdrAssignmentUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setIdrClmLevelIndEnum(randomEnum(McsClaimLevelIndicatorEnums)),
        () -> claim.setIdrClmLevelIndUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setIdrHdrAudit(randomInt(32767)));
    oneOf(
        () -> claim.setIdrHdrAuditIndEnum(randomEnum(McsAuditIndicatorEnums)),
        () -> claim.setIdrHdrAuditIndUnrecognized(randomAlphaNumeric(1, 1)));
    oneOf(
        () -> claim.setIdrUSplitReasonEnum(randomEnum(McsSplitReasonCodeEnums)),
        () -> claim.setIdrUSplitReasonUnrecognized(randomAlphaNumeric(1, 1)));
    optional(() -> claim.setIdrJReferringProvNpi(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrJFacProvNpi(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrUDemoProvNpi(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrUSuperNpi(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrUFcadjBilNpi(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrAmbPickupAddresLine1(randomAlphaNumeric(1, 25)));
    optional(() -> claim.setIdrAmbPickupAddresLine2(randomAlphaNumeric(1, 20)));
    optional(() -> claim.setIdrAmbPickupCity(randomAlphaNumeric(1, 20)));
    optional(() -> claim.setIdrAmbPickupState(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrAmbPickupZipcode(randomAlphaNumeric(1, 9)));
    optional(() -> claim.setIdrAmbDropoffName(randomAlphaNumeric(1, 24)));
    optional(() -> claim.setIdrAmbDropoffAddrLine1(randomAlphaNumeric(1, 25)));
    optional(() -> claim.setIdrAmbDropoffAddrLine2(randomAlphaNumeric(1, 20)));
    optional(() -> claim.setIdrAmbDropoffCity(randomAlphaNumeric(1, 20)));
    optional(() -> claim.setIdrAmbDropoffState(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrAmbDropoffZipcode(randomAlphaNumeric(1, 9)));
  }

  private void addAdjustments(McsClaim.Builder claim) {
    final int count = randomInt(MAX_ADJUSTMENTS);
    for (int i = 1; i <= count; ++i) {
      final McsAdjustment.Builder adjustment = McsAdjustment.newBuilder();
      optional(() -> adjustment.setIdrAdjDate(randomDate()));
      optional(() -> adjustment.setIdrXrefIcn(randomAlphaNumeric(1, 15)));
      optional(() -> adjustment.setIdrAdjClerk(randomAlphaNumeric(1, 4)));
      optional(() -> adjustment.setIdrInitCcn(randomAlphaNumeric(1, 15)));
      optional(() -> adjustment.setIdrAdjChkWrtDt(randomDate()));
      optional(() -> adjustment.setIdrAdjBEombAmt(randomAmount()));
      optional(() -> adjustment.setIdrAdjPEombAmt(randomAmount()));
      claim.addMcsAdjustments(adjustment.build());
    }
  }

  private void addAudits(McsClaim.Builder claim) {
    final int count = randomInt(MAX_AUDITS);
    for (int i = 1; i <= count; ++i) {
      final McsAudit.Builder audit = McsAudit.newBuilder();
      optional(() -> audit.setIdrJAuditNum(randomInt(32767)));
      oneOf(
          () -> audit.setIdrJAuditIndEnum(randomEnum(McsCutbackAuditIndicatorEnums)),
          () -> audit.setIdrJAuditIndUnrecognized(randomAlphaNumeric(1, 1)));
      oneOf(
          () -> audit.setIdrJAuditDispEnum(randomEnum(McsCutbackAuditDispositionEnums)),
          () -> audit.setIdrJAuditDispUnrecognized(randomAlphaNumeric(1, 1)));
      claim.addMcsAudits(audit.build());
    }
  }

  private void addDiagnosisCodes(McsClaim.Builder claim, String idrClmHdIcn) {
    final int count = randomInt(MAX_DIAG_CODES);
    for (int i = 1; i <= count; ++i) {
      final McsDiagnosisCode.Builder code = McsDiagnosisCode.newBuilder();
      code.setIdrClmHdIcn(idrClmHdIcn);
      oneOf(
          () -> code.setIdrDiagIcdTypeEnum(randomEnum(McsDiagnosisIcdTypeEnums)),
          () -> code.setIdrDiagIcdTypeUnrecognized(randomLetter(1, 1)));
      code.setIdrDiagCode(randomAlphaNumeric(1, 7));
      claim.addMcsDiagnosisCodes(code.build());
    }
  }

  private void addDetails(McsClaim.Builder claim, int detailCount) {
    for (int i = 1; i <= detailCount; ++i) {
      final McsDetail.Builder detail = McsDetail.newBuilder();
      oneOf(
          () -> detail.setIdrDtlStatusEnum(randomEnum(McsDetailStatusEnums)),
          () -> detail.setIdrDtlStatusUnrecognized(randomLetter(1, 1)));
      optional(
          () -> {
            final List<String> dates = Arrays.asList(randomDate(), randomDate());
            dates.sort(Comparator.naturalOrder());
            detail.setIdrDtlFromDate(dates.get(0));
            detail.setIdrDtlToDate(dates.get(1));
          });
      optional(() -> detail.setIdrProcCode(randomAlphaNumeric(1, 5)));
      optional(() -> detail.setIdrModOne(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrModTwo(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrModThree(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrModFour(randomAlphaNumeric(1, 2)));
      oneOf(
          () -> detail.setIdrDtlDiagIcdTypeEnum(randomEnum(McsDiagnosisIcdTypeEnums)),
          () -> detail.setIdrDtlDiagIcdTypeUnrecognized(randomLetter(1, 1)));
      optional(() -> detail.setIdrDtlPrimaryDiagCode(randomAlphaNumeric(1, 7)));
      optional(() -> detail.setIdrKPosLnameOrg(randomLetter(1, 60)));
      optional(() -> detail.setIdrKPosFname(randomLetter(1, 35)));
      optional(() -> detail.setIdrKPosMname(randomLetter(1, 25)));
      optional(() -> detail.setIdrKPosAddr1(randomAlphaNumeric(1, 55)));
      optional(() -> detail.setIdrKPosAddr21St(randomAlphaNumeric(1, 30)));
      optional(() -> detail.setIdrKPosAddr22Nd(randomAlphaNumeric(1, 25)));
      optional(() -> detail.setIdrKPosCity(randomLetter(1, 30)));
      optional(() -> detail.setIdrKPosState(randomLetter(1, 2)));
      optional(() -> detail.setIdrKPosZip(randomDigit(1, 15)));
      oneOf(
          () -> detail.setIdrTosEnum(randomEnum(McsTypeOfServiceEnums)),
          () -> detail.setIdrTosUnrecognized(randomAlphaNumeric(1, 1)));
      oneOf(
          () -> detail.setIdrTwoDigitPosEnum(randomEnum(McsTwoDigitPlanOfServiceEnums)),
          () -> detail.setIdrTwoDigitPosUnrecognized(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrDtlRendType(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrDtlRendSpec(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrDtlRendNpi(randomAlphaNumeric(1, 10)));
      optional(() -> detail.setIdrDtlRendProv(randomAlphaNumeric(1, 10)));
      optional(() -> detail.setIdrKDtlFacProvNpi(randomAlphaNumeric(1, 10)));
      optional(() -> detail.setIdrDtlAmbPickupAddres1(randomAlphaNumeric(1, 25)));
      optional(() -> detail.setIdrDtlAmbPickupAddres2(randomAlphaNumeric(1, 20)));
      optional(() -> detail.setIdrDtlAmbPickupCity(randomAlphaNumeric(1, 20)));
      optional(() -> detail.setIdrDtlAmbPickupState(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrDtlAmbPickupZipcode(randomAlphaNumeric(1, 9)));
      optional(() -> detail.setIdrDtlAmbDropoffName(randomAlphaNumeric(1, 24)));
      optional(() -> detail.setIdrDtlAmbDropoffAddrL1(randomAlphaNumeric(1, 25)));
      optional(() -> detail.setIdrDtlAmbDropoffAddrL2(randomAlphaNumeric(1, 20)));
      optional(() -> detail.setIdrDtlAmbDropoffCity(randomAlphaNumeric(1, 20)));
      optional(() -> detail.setIdrDtlAmbDropoffState(randomAlphaNumeric(1, 2)));
      optional(() -> detail.setIdrDtlAmbDropoffZipcode(randomAlphaNumeric(1, 9)));
      claim.addMcsDetails(detail.build());
    }
  }

  private void addLocations(McsClaim.Builder claim) {
    final int count = randomInt(MAX_LOCATIONS);
    for (int i = 1; i <= count; ++i) {
      final McsLocation.Builder location = McsLocation.newBuilder();
      optional(() -> location.setIdrLocClerk(randomAlphaNumeric(1, 4)));
      optional(() -> location.setIdrLocCode(randomAlphaNumeric(1, 3)));
      optional(() -> location.setIdrLocDate(randomDate()));
      oneOf(
          () -> location.setIdrLocActvCodeEnum(randomEnum(McsLocationActivityCodeEnums)),
          () -> location.setIdrLocActvCodeUnrecognized(randomAlphaNumeric(1, 1)));
      claim.addMcsLocations(location.build());
    }
  }

  /**
   * Ensures the from date and to date are set using the min/max dates in all of the details (if
   * any). If either is missing from the details neither of the dates are set in the claim.
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
}
