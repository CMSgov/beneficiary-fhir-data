package gov.cms.bfd.pipeline.rda.grpc.server;

import com.google.common.base.Strings;
import gov.cms.mpsm.rda.v1.mcs.McsBeneficiarySex;
import gov.cms.mpsm.rda.v1.mcs.McsBillingProviderIndicator;
import gov.cms.mpsm.rda.v1.mcs.McsBillingProviderStatusCode;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaimType;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDetailStatus;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisIcdType;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import java.time.Clock;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class RandomMcsClaimGenerator extends AbstractRandomClaimGenerator {
  private static final int MAX_DETAILS = 4;
  private static final int MAX_DIAG_CODES = 7;
  private static final List<McsClaimType> CLAIM_TYPES = enumValues(McsClaimType.values());
  private static final List<McsBeneficiarySex> BENE_SEXES = enumValues(McsBeneficiarySex.values());
  private static final List<McsStatusCode> STATUS_CODES = enumValues(McsStatusCode.values());
  private static final List<McsBillingProviderIndicator> PROV_INDICATORS =
      enumValues(McsBillingProviderIndicator.values());
  private static final List<McsBillingProviderStatusCode> PROV_STATUS_CODES =
      enumValues(McsBillingProviderStatusCode.values());
  private static final List<McsDiagnosisIcdType> DIAG_ICD_TYPES =
      enumValues(McsDiagnosisIcdType.values());
  private static final List<McsDetailStatus> DETAIL_STATUSES = enumValues(McsDetailStatus.values());

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
    final String idrHdIcn = randomDigit(1, 12);
    McsClaim.Builder claim = McsClaim.newBuilder();
    addRandomFieldValues(claim, idrHdIcn, detailCount);
    addDiagnosisCodes(claim, idrHdIcn);
    addDetails(claim, detailCount);
    adjustServiceDatesFromDetails(claim);
    return claim.build();
  }

  private void addRandomFieldValues(McsClaim.Builder claim, String idrHdIcn, int detailCount) {
    claim.setIdrClmHdIcn(randomAlphaNumeric(5, 8));
    claim.setIdrContrId(randomDigit(1, 5));
    optional(() -> claim.setIdrHic(idrHdIcn));
    either(
        () -> claim.setIdrClaimTypeEnum(randomEnum(CLAIM_TYPES)),
        () -> claim.setIdrClaimTypeUnrecognized(randomLetter(1, 1)));
    claim.setIdrDtlCnt(detailCount);
    optional(() -> claim.setIdrBeneLast16(randomLetter(1, 6)));
    optional(() -> claim.setIdrBeneFirstInit(randomLetter(1, 1)));
    optional(() -> claim.setIdrBeneMidInit(randomLetter(1, 1)));
    either(
        () -> claim.setIdrBeneSexEnum(randomEnum(BENE_SEXES)),
        () -> claim.setIdrBeneSexUnrecognized(randomLetter(1, 1)));
    either(
        () -> claim.setIdrStatusCodeEnum(randomEnum(STATUS_CODES)),
        () -> claim.setIdrStatusCodeUnrecognized(randomLetter(1, 1)));
    optional(() -> claim.setIdrStatusDate(randomDate()));
    optional(() -> claim.setIdrBillProvNpi(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrBillProvNum(randomDigit(1, 10)));
    optional(() -> claim.setIdrBillProvEin(randomAlphaNumeric(1, 10)));
    optional(() -> claim.setIdrBillProvType(randomLetter(1, 2)));
    optional(() -> claim.setIdrBillProvSpec(randomAlphaNumeric(1, 2)));
    either(
        () -> claim.setIdrBillProvGroupIndEnum(randomEnum(PROV_INDICATORS)),
        () -> claim.setIdrBillProvGroupIndUnrecognized(randomLetter(1, 1)));
    optional(() -> claim.setIdrBillProvPriceSpec(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrBillProvCounty(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrBillProvLoc(randomAlphaNumeric(1, 2)));
    optional(() -> claim.setIdrTotAllowed(randomAmount()));
    optional(() -> claim.setIdrCoinsurance(randomAmount()));
    optional(() -> claim.setIdrDeductible(randomAmount()));
    either(
        () -> claim.setIdrBillProvStatusCdEnum(randomEnum(PROV_STATUS_CODES)),
        () -> claim.setIdrBillProvStatusCdUnrecognized(randomLetter(1, 1)));
    optional(() -> claim.setIdrTotBilledAmt(randomAmount()));
    optional(() -> claim.setIdrClaimReceiptDate(randomDate()));
    optional(() -> claim.setIdrClaimMbi(randomAlphaNumeric(1, 13)));
    // IdrHdrFromDos will be set later
    // IdrHdrToDos will be set later
  }

  private void addDiagnosisCodes(McsClaim.Builder claim, String idrHdIcn) {
    final int count = randomInt(MAX_DIAG_CODES);
    for (int i = 1; i <= count; ++i) {
      final McsDiagnosisCode.Builder code = McsDiagnosisCode.newBuilder();
      code.setIdrClmHdIcn(idrHdIcn);
      either(
          () -> code.setIdrDiagIcdTypeEnum(randomEnum(DIAG_ICD_TYPES)),
          () -> code.setIdrDiagIcdTypeEnumUnrecognized(randomLetter(1, 1)));
      optional(() -> code.setIdrDiagCode(randomAlphaNumeric(1, 7)));
      claim.addMcsDiagnosisCodes(code.build());
    }
  }

  private void addDetails(McsClaim.Builder claim, int detailCount) {
    for (int i = 1; i <= detailCount; ++i) {
      final McsDetail.Builder detail = McsDetail.newBuilder();
      either(
          () -> detail.setIdrDtlStatusEnum(randomEnum(DETAIL_STATUSES)),
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
      either(
          () -> detail.setIdrDtlDiagIcdTypeEnum(randomEnum(DIAG_ICD_TYPES)),
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
      claim.addMcsDetails(detail.build());
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
