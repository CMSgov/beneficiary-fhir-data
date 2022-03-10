package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.model.rda.PreAdjMcsAdjustment;
import gov.cms.bfd.model.rda.PreAdjMcsAudit;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjMcsLocation;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.mpsm.rda.v1.McsClaimChange;
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
import java.time.Instant;
import java.util.List;
import lombok.Getter;

public class McsClaimTransformer {
  private final EnumStringExtractor<McsClaim, McsClaimType> PreAdjMcsClaim_idrClaimType_Extractor;

  private final EnumStringExtractor<McsClaim, McsBeneficiarySex>
      PreAdjMcsClaim_idrBeneSex_Extractor;

  private final EnumStringExtractor<McsClaim, McsStatusCode> PreAdjMcsClaim_idrStatusCode_Extractor;

  private final EnumStringExtractor<McsClaim, McsBillingProviderIndicator>
      PreAdjMcsClaim_idrBillProvGroupInd_Extractor;

  private final EnumStringExtractor<McsClaim, McsBillingProviderStatusCode>
      PreAdjMcsClaim_idrBillProvStatusCd_Extractor;

  private final EnumStringExtractor<McsClaim, McsClaimAssignmentCode>
      PreAdjMcsClaim_idrAssignment_Extractor;

  private final EnumStringExtractor<McsClaim, McsClaimLevelIndicator>
      PreAdjMcsClaim_idrClmLevelInd_Extractor;

  private final EnumStringExtractor<McsClaim, McsAuditIndicator>
      PreAdjMcsClaim_idrHdrAuditInd_Extractor;

  private final EnumStringExtractor<McsClaim, McsSplitReasonCode>
      PreAdjMcsClaim_idrUSplitReason_Extractor;

  private final EnumStringExtractor<McsDetail, McsDetailStatus>
      PreAdjMcsDetail_idrDtlStatus_Extractor;

  private final EnumStringExtractor<McsDetail, McsDiagnosisIcdType>
      PreAdjMcsDetail_idrDtlDiagIcdType_Extractor;

  private final EnumStringExtractor<McsDetail, McsTypeOfService> PreAdjMcsDetail_idrTos_Extractor;

  private final EnumStringExtractor<McsDetail, McsTwoDigitPlanOfService>
      PreAdjMcsDetail_idrTwoDigitPos_Extractor;

  private final EnumStringExtractor<McsDiagnosisCode, McsDiagnosisIcdType>
      PreAdjMcsDiagnosisCode_idrDiagIcdType_Extractor;

  private final EnumStringExtractor<McsAudit, McsCutbackAuditIndicator>
      PreAdjMcsAudit_idrJAuditInd_Extractor;

  private final EnumStringExtractor<McsAudit, McsCutbackAuditDisposition>
      PreAdjMcsAudit_idrJAuditDisp_Extractor;

  private final EnumStringExtractor<McsLocation, McsLocationActivityCode>
      PreAdjMcsLocation_idrLocActvCode_Extractor;

  private final Clock clock;
  @Getter private final MbiCache mbiCache;

  public McsClaimTransformer(Clock clock, MbiCache mbiCache) {
    this.clock = clock;
    this.mbiCache = mbiCache;
    PreAdjMcsClaim_idrClaimType_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrClaimTypeEnum,
            McsClaim::getIdrClaimTypeEnum,
            McsClaim::hasIdrClaimTypeUnrecognized,
            McsClaim::getIdrClaimTypeUnrecognized,
            McsClaimType.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsClaim_idrBeneSex_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrBeneSexEnum,
            McsClaim::getIdrBeneSexEnum,
            McsClaim::hasIdrBeneSexUnrecognized,
            McsClaim::getIdrBeneSexUnrecognized,
            McsBeneficiarySex.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsClaim_idrStatusCode_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrStatusCodeEnum,
            McsClaim::getIdrStatusCodeEnum,
            McsClaim::hasIdrStatusCodeUnrecognized,
            McsClaim::getIdrStatusCodeUnrecognized,
            McsStatusCode.UNRECOGNIZED,
            ImmutableSet.of(McsStatusCode.STATUS_CODE_NOT_USED),
            ImmutableSet.of(EnumStringExtractor.Options.RejectUnrecognized));
    PreAdjMcsClaim_idrBillProvGroupInd_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrBillProvGroupIndEnum,
            McsClaim::getIdrBillProvGroupIndEnum,
            McsClaim::hasIdrBillProvGroupIndUnrecognized,
            McsClaim::getIdrBillProvGroupIndUnrecognized,
            McsBillingProviderIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsClaim_idrBillProvStatusCd_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrBillProvStatusCdEnum,
            McsClaim::getIdrBillProvStatusCdEnum,
            McsClaim::hasIdrBillProvStatusCdUnrecognized,
            McsClaim::getIdrBillProvStatusCdUnrecognized,
            McsBillingProviderStatusCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsClaim_idrAssignment_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrAssignmentEnum,
            McsClaim::getIdrAssignmentEnum,
            McsClaim::hasIdrAssignmentUnrecognized,
            McsClaim::getIdrAssignmentUnrecognized,
            McsClaimAssignmentCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsClaim_idrClmLevelInd_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrClmLevelIndEnum,
            McsClaim::getIdrClmLevelIndEnum,
            McsClaim::hasIdrClmLevelIndUnrecognized,
            McsClaim::getIdrClmLevelIndUnrecognized,
            McsClaimLevelIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsClaim_idrHdrAuditInd_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrHdrAuditIndEnum,
            McsClaim::getIdrHdrAuditIndEnum,
            McsClaim::hasIdrHdrAuditIndUnrecognized,
            McsClaim::getIdrHdrAuditIndUnrecognized,
            McsAuditIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsClaim_idrUSplitReason_Extractor =
        new EnumStringExtractor<>(
            McsClaim::hasIdrUSplitReasonEnum,
            McsClaim::getIdrUSplitReasonEnum,
            McsClaim::hasIdrUSplitReasonUnrecognized,
            McsClaim::getIdrUSplitReasonUnrecognized,
            McsSplitReasonCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsDetail_idrDtlStatus_Extractor =
        new EnumStringExtractor<>(
            McsDetail::hasIdrDtlStatusEnum,
            McsDetail::getIdrDtlStatusEnum,
            McsDetail::hasIdrDtlStatusUnrecognized,
            McsDetail::getIdrDtlStatusUnrecognized,
            McsDetailStatus.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsDetail_idrDtlDiagIcdType_Extractor =
        new EnumStringExtractor<>(
            McsDetail::hasIdrDtlDiagIcdTypeEnum,
            McsDetail::getIdrDtlDiagIcdTypeEnum,
            McsDetail::hasIdrDtlDiagIcdTypeUnrecognized,
            McsDetail::getIdrDtlDiagIcdTypeUnrecognized,
            McsDiagnosisIcdType.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsDetail_idrTos_Extractor =
        new EnumStringExtractor<>(
            McsDetail::hasIdrTosEnum,
            McsDetail::getIdrTosEnum,
            McsDetail::hasIdrTosUnrecognized,
            McsDetail::getIdrTosUnrecognized,
            McsTypeOfService.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsDetail_idrTwoDigitPos_Extractor =
        new EnumStringExtractor<>(
            McsDetail::hasIdrTwoDigitPosEnum,
            McsDetail::getIdrTwoDigitPosEnum,
            McsDetail::hasIdrTwoDigitPosUnrecognized,
            McsDetail::getIdrTwoDigitPosUnrecognized,
            McsTwoDigitPlanOfService.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsDiagnosisCode_idrDiagIcdType_Extractor =
        new EnumStringExtractor<>(
            McsDiagnosisCode::hasIdrDiagIcdTypeEnum,
            McsDiagnosisCode::getIdrDiagIcdTypeEnum,
            McsDiagnosisCode::hasIdrDiagIcdTypeUnrecognized,
            McsDiagnosisCode::getIdrDiagIcdTypeUnrecognized,
            McsDiagnosisIcdType.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsAudit_idrJAuditInd_Extractor =
        new EnumStringExtractor<>(
            McsAudit::hasIdrJAuditIndEnum,
            McsAudit::getIdrJAuditIndEnum,
            McsAudit::hasIdrJAuditIndUnrecognized,
            McsAudit::getIdrJAuditIndUnrecognized,
            McsCutbackAuditIndicator.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsAudit_idrJAuditDisp_Extractor =
        new EnumStringExtractor<>(
            McsAudit::hasIdrJAuditDispEnum,
            McsAudit::getIdrJAuditDispEnum,
            McsAudit::hasIdrJAuditDispUnrecognized,
            McsAudit::getIdrJAuditDispUnrecognized,
            McsCutbackAuditDisposition.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
    PreAdjMcsLocation_idrLocActvCode_Extractor =
        new EnumStringExtractor<>(
            McsLocation::hasIdrLocActvCodeEnum,
            McsLocation::getIdrLocActvCodeEnum,
            McsLocation::hasIdrLocActvCodeUnrecognized,
            McsLocation::getIdrLocActvCodeUnrecognized,
            McsLocationActivityCode.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
  }

  /**
   * Hook to allow the McsClaimRdaSink to install an alternative MbiCache implementation that
   * supports caching MBI values in a database table.
   *
   * @param mbiCache alternative MbiCache to use for obtaining Mbi instances
   * @return a new transformer with the same clock but alternative MbiCache
   */
  public McsClaimTransformer withMbiCache(MbiCache mbiCache) {
    return new McsClaimTransformer(clock, mbiCache);
  }

  public RdaChange<PreAdjMcsClaim> transformClaim(McsClaimChange change) {
    McsClaim from = change.getClaim();

    final DataTransformer transformer = new DataTransformer();
    final PreAdjMcsClaim to = transformMessage(from, transformer, clock.instant());
    to.setSequenceNumber(change.getSeq());

    final List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: clmHdIcn=%s errors=%s",
              errors.size(), from.getIdrClmHdIcn(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }
    return new RdaChange<>(
        change.getSeq(),
        RdaApiUtils.mapApiChangeType(change.getChangeType()),
        to,
        transformer.instant(change.getTimestamp()));
  }

  private PreAdjMcsClaim transformMessage(McsClaim from, DataTransformer transformer, Instant now) {
    final PreAdjMcsClaim to = transformMessageImpl(from, transformer, now, "");
    transformMessageArrays(from, to, transformer, now, "");
    return to;
  }

  private PreAdjMcsClaim transformMessageImpl(
      McsClaim from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjMcsClaim to = new PreAdjMcsClaim();
    transformer.copyString(
        namePrefix + PreAdjMcsClaim.Fields.idrClmHdIcn,
        false,
        1,
        15,
        from.getIdrClmHdIcn(),
        to::setIdrClmHdIcn);
    transformer.copyString(
        namePrefix + PreAdjMcsClaim.Fields.idrContrId,
        false,
        1,
        5,
        from.getIdrContrId(),
        to::setIdrContrId);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrHic,
        1,
        12,
        from::hasIdrHic,
        from::getIdrHic,
        to::setIdrHic);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrClaimType,
        false,
        1,
        PreAdjMcsClaim_idrClaimType_Extractor.getEnumString(from),
        to::setIdrClaimType);
    transformer.copyOptionalInt(from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBeneLast_1_6,
        1,
        6,
        from::hasIdrBeneLast16,
        from::getIdrBeneLast16,
        to::setIdrBeneLast_1_6);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBeneFirstInit,
        1,
        1,
        from::hasIdrBeneFirstInit,
        from::getIdrBeneFirstInit,
        to::setIdrBeneFirstInit);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBeneMidInit,
        1,
        1,
        from::hasIdrBeneMidInit,
        from::getIdrBeneMidInit,
        to::setIdrBeneMidInit);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrBeneSex,
        true,
        1,
        PreAdjMcsClaim_idrBeneSex_Extractor.getEnumString(from),
        to::setIdrBeneSex);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrStatusCode,
        true,
        1,
        PreAdjMcsClaim_idrStatusCode_Extractor.getEnumString(from),
        to::setIdrStatusCode);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsClaim.Fields.idrStatusDate,
        from::hasIdrStatusDate,
        from::getIdrStatusDate,
        to::setIdrStatusDate);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvNpi,
        1,
        10,
        from::hasIdrBillProvNpi,
        from::getIdrBillProvNpi,
        to::setIdrBillProvNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvNum,
        1,
        10,
        from::hasIdrBillProvNum,
        from::getIdrBillProvNum,
        to::setIdrBillProvNum);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvEin,
        1,
        10,
        from::hasIdrBillProvEin,
        from::getIdrBillProvEin,
        to::setIdrBillProvEin);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvType,
        1,
        2,
        from::hasIdrBillProvType,
        from::getIdrBillProvType,
        to::setIdrBillProvType);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvSpec,
        1,
        2,
        from::hasIdrBillProvSpec,
        from::getIdrBillProvSpec,
        to::setIdrBillProvSpec);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvGroupInd,
        true,
        1,
        PreAdjMcsClaim_idrBillProvGroupInd_Extractor.getEnumString(from),
        to::setIdrBillProvGroupInd);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvPriceSpec,
        1,
        2,
        from::hasIdrBillProvPriceSpec,
        from::getIdrBillProvPriceSpec,
        to::setIdrBillProvPriceSpec);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvCounty,
        1,
        2,
        from::hasIdrBillProvCounty,
        from::getIdrBillProvCounty,
        to::setIdrBillProvCounty);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvLoc,
        1,
        2,
        from::hasIdrBillProvLoc,
        from::getIdrBillProvLoc,
        to::setIdrBillProvLoc);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjMcsClaim.Fields.idrTotAllowed,
        from::hasIdrTotAllowed,
        from::getIdrTotAllowed,
        to::setIdrTotAllowed);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjMcsClaim.Fields.idrCoinsurance,
        from::hasIdrCoinsurance,
        from::getIdrCoinsurance,
        to::setIdrCoinsurance);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjMcsClaim.Fields.idrDeductible,
        from::hasIdrDeductible,
        from::getIdrDeductible,
        to::setIdrDeductible);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrBillProvStatusCd,
        true,
        1,
        PreAdjMcsClaim_idrBillProvStatusCd_Extractor.getEnumString(from),
        to::setIdrBillProvStatusCd);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjMcsClaim.Fields.idrTotBilledAmt,
        from::hasIdrTotBilledAmt,
        from::getIdrTotBilledAmt,
        to::setIdrTotBilledAmt);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsClaim.Fields.idrClaimReceiptDate,
        from::hasIdrClaimReceiptDate,
        from::getIdrClaimReceiptDate,
        to::setIdrClaimReceiptDate);
    if (from.hasIdrClaimMbi()) {
      final var mbi = from.getIdrClaimMbi();
      if (transformer.validateString(
          namePrefix + PreAdjMcsClaim.Fields.idrClaimMbi, false, 1, 11, mbi)) {
        to.setMbiRecord(mbiCache.lookupMbi(mbi));
      }
    }
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsClaim.Fields.idrHdrFromDateOfSvc,
        from::hasIdrHdrFromDos,
        from::getIdrHdrFromDos,
        to::setIdrHdrFromDateOfSvc);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsClaim.Fields.idrHdrToDateOfSvc,
        from::hasIdrHdrToDos,
        from::getIdrHdrToDos,
        to::setIdrHdrToDateOfSvc);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrAssignment,
        true,
        1,
        PreAdjMcsClaim_idrAssignment_Extractor.getEnumString(from),
        to::setIdrAssignment);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrClmLevelInd,
        true,
        1,
        PreAdjMcsClaim_idrClmLevelInd_Extractor.getEnumString(from),
        to::setIdrClmLevelInd);
    transformer.copyOptionalInt(from::hasIdrHdrAudit, from::getIdrHdrAudit, to::setIdrHdrAudit);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrHdrAuditInd,
        true,
        1,
        PreAdjMcsClaim_idrHdrAuditInd_Extractor.getEnumString(from),
        to::setIdrHdrAuditInd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrUSplitReason,
        true,
        1,
        PreAdjMcsClaim_idrUSplitReason_Extractor.getEnumString(from),
        to::setIdrUSplitReason);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrJReferringProvNpi,
        1,
        10,
        from::hasIdrJReferringProvNpi,
        from::getIdrJReferringProvNpi,
        to::setIdrJReferringProvNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrJFacProvNpi,
        1,
        10,
        from::hasIdrJFacProvNpi,
        from::getIdrJFacProvNpi,
        to::setIdrJFacProvNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrUDemoProvNpi,
        1,
        10,
        from::hasIdrUDemoProvNpi,
        from::getIdrUDemoProvNpi,
        to::setIdrUDemoProvNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrUSuperNpi,
        1,
        10,
        from::hasIdrUSuperNpi,
        from::getIdrUSuperNpi,
        to::setIdrUSuperNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrUFcadjBilNpi,
        1,
        10,
        from::hasIdrUFcadjBilNpi,
        from::getIdrUFcadjBilNpi,
        to::setIdrUFcadjBilNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbPickupAddresLine1,
        1,
        25,
        from::hasIdrAmbPickupAddresLine1,
        from::getIdrAmbPickupAddresLine1,
        to::setIdrAmbPickupAddresLine1);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbPickupAddresLine2,
        1,
        20,
        from::hasIdrAmbPickupAddresLine2,
        from::getIdrAmbPickupAddresLine2,
        to::setIdrAmbPickupAddresLine2);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbPickupCity,
        1,
        20,
        from::hasIdrAmbPickupCity,
        from::getIdrAmbPickupCity,
        to::setIdrAmbPickupCity);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbPickupState,
        1,
        2,
        from::hasIdrAmbPickupState,
        from::getIdrAmbPickupState,
        to::setIdrAmbPickupState);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbPickupZipcode,
        1,
        9,
        from::hasIdrAmbPickupZipcode,
        from::getIdrAmbPickupZipcode,
        to::setIdrAmbPickupZipcode);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbDropoffName,
        1,
        24,
        from::hasIdrAmbDropoffName,
        from::getIdrAmbDropoffName,
        to::setIdrAmbDropoffName);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbDropoffAddrLine1,
        1,
        25,
        from::hasIdrAmbDropoffAddrLine1,
        from::getIdrAmbDropoffAddrLine1,
        to::setIdrAmbDropoffAddrLine1);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbDropoffAddrLine2,
        1,
        20,
        from::hasIdrAmbDropoffAddrLine2,
        from::getIdrAmbDropoffAddrLine2,
        to::setIdrAmbDropoffAddrLine2);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbDropoffCity,
        1,
        20,
        from::hasIdrAmbDropoffCity,
        from::getIdrAmbDropoffCity,
        to::setIdrAmbDropoffCity);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbDropoffState,
        1,
        2,
        from::hasIdrAmbDropoffState,
        from::getIdrAmbDropoffState,
        to::setIdrAmbDropoffState);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrAmbDropoffZipcode,
        1,
        9,
        from::hasIdrAmbDropoffZipcode,
        from::getIdrAmbDropoffZipcode,
        to::setIdrAmbDropoffZipcode);
    to.setLastUpdated(now);
    return to;
  }

  private void transformMessageArrays(
      McsClaim from,
      PreAdjMcsClaim to,
      DataTransformer transformer,
      Instant now,
      String namePrefix) {
    for (short index = 0; index < from.getMcsDetailsCount(); ++index) {
      final String itemNamePrefix = namePrefix + "detail" + "-" + index + "-";
      final McsDetail itemFrom = from.getMcsDetails(index);
      final PreAdjMcsDetail itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setIdrClmHdIcn(from.getIdrClmHdIcn());
      itemTo.setPriority(index);
      to.getDetails().add(itemTo);
    }
    for (short index = 0; index < from.getMcsDiagnosisCodesCount(); ++index) {
      final String itemNamePrefix = namePrefix + "diagCode" + "-" + index + "-";
      final McsDiagnosisCode itemFrom = from.getMcsDiagnosisCodes(index);
      final PreAdjMcsDiagnosisCode itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setIdrClmHdIcn(from.getIdrClmHdIcn());
      itemTo.setPriority(index);
      to.getDiagCodes().add(itemTo);
    }
    for (short index = 0; index < from.getMcsAdjustmentsCount(); ++index) {
      final String itemNamePrefix = namePrefix + "adjustment" + "-" + index + "-";
      final McsAdjustment itemFrom = from.getMcsAdjustments(index);
      final PreAdjMcsAdjustment itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setIdrClmHdIcn(from.getIdrClmHdIcn());
      itemTo.setPriority(index);
      to.getAdjustments().add(itemTo);
    }
    for (short index = 0; index < from.getMcsAuditsCount(); ++index) {
      final String itemNamePrefix = namePrefix + "audit" + "-" + index + "-";
      final McsAudit itemFrom = from.getMcsAudits(index);
      final PreAdjMcsAudit itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setIdrClmHdIcn(from.getIdrClmHdIcn());
      itemTo.setPriority(index);
      to.getAudits().add(itemTo);
    }
    for (short index = 0; index < from.getMcsLocationsCount(); ++index) {
      final String itemNamePrefix = namePrefix + "location" + "-" + index + "-";
      final McsLocation itemFrom = from.getMcsLocations(index);
      final PreAdjMcsLocation itemTo =
          transformMessageImpl(itemFrom, transformer, now, itemNamePrefix);
      itemTo.setIdrClmHdIcn(from.getIdrClmHdIcn());
      itemTo.setPriority(index);
      to.getLocations().add(itemTo);
    }
  }

  private PreAdjMcsDetail transformMessageImpl(
      McsDetail from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjMcsDetail to = new PreAdjMcsDetail();
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlStatus,
        true,
        1,
        PreAdjMcsDetail_idrDtlStatus_Extractor.getEnumString(from),
        to::setIdrDtlStatus);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlFromDate,
        from::hasIdrDtlFromDate,
        from::getIdrDtlFromDate,
        to::setIdrDtlFromDate);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlToDate,
        from::hasIdrDtlToDate,
        from::getIdrDtlToDate,
        to::setIdrDtlToDate);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrProcCode,
        1,
        5,
        from::hasIdrProcCode,
        from::getIdrProcCode,
        to::setIdrProcCode);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrModOne,
        1,
        2,
        from::hasIdrModOne,
        from::getIdrModOne,
        to::setIdrModOne);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrModTwo,
        1,
        2,
        from::hasIdrModTwo,
        from::getIdrModTwo,
        to::setIdrModTwo);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrModThree,
        1,
        2,
        from::hasIdrModThree,
        from::getIdrModThree,
        to::setIdrModThree);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrModFour,
        1,
        2,
        from::hasIdrModFour,
        from::getIdrModFour,
        to::setIdrModFour);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlDiagIcdType,
        true,
        1,
        PreAdjMcsDetail_idrDtlDiagIcdType_Extractor.getEnumString(from),
        to::setIdrDtlDiagIcdType);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlPrimaryDiagCode,
        1,
        7,
        from::hasIdrDtlPrimaryDiagCode,
        from::getIdrDtlPrimaryDiagCode,
        to::setIdrDtlPrimaryDiagCode);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosLnameOrg,
        1,
        60,
        from::hasIdrKPosLnameOrg,
        from::getIdrKPosLnameOrg,
        to::setIdrKPosLnameOrg);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosFname,
        1,
        35,
        from::hasIdrKPosFname,
        from::getIdrKPosFname,
        to::setIdrKPosFname);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosMname,
        1,
        25,
        from::hasIdrKPosMname,
        from::getIdrKPosMname,
        to::setIdrKPosMname);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosAddr1,
        1,
        55,
        from::hasIdrKPosAddr1,
        from::getIdrKPosAddr1,
        to::setIdrKPosAddr1);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosAddr2_1st,
        1,
        30,
        from::hasIdrKPosAddr21St,
        from::getIdrKPosAddr21St,
        to::setIdrKPosAddr2_1st);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosAddr2_2nd,
        1,
        25,
        from::hasIdrKPosAddr22Nd,
        from::getIdrKPosAddr22Nd,
        to::setIdrKPosAddr2_2nd);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosCity,
        1,
        30,
        from::hasIdrKPosCity,
        from::getIdrKPosCity,
        to::setIdrKPosCity);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosState,
        1,
        2,
        from::hasIdrKPosState,
        from::getIdrKPosState,
        to::setIdrKPosState);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKPosZip,
        1,
        15,
        from::hasIdrKPosZip,
        from::getIdrKPosZip,
        to::setIdrKPosZip);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsDetail.Fields.idrTos,
        true,
        1,
        PreAdjMcsDetail_idrTos_Extractor.getEnumString(from),
        to::setIdrTos);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsDetail.Fields.idrTwoDigitPos,
        true,
        2,
        PreAdjMcsDetail_idrTwoDigitPos_Extractor.getEnumString(from),
        to::setIdrTwoDigitPos);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlRendType,
        1,
        2,
        from::hasIdrDtlRendType,
        from::getIdrDtlRendType,
        to::setIdrDtlRendType);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlRendSpec,
        1,
        2,
        from::hasIdrDtlRendSpec,
        from::getIdrDtlRendSpec,
        to::setIdrDtlRendSpec);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlRendNpi,
        1,
        10,
        from::hasIdrDtlRendNpi,
        from::getIdrDtlRendNpi,
        to::setIdrDtlRendNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlRendProv,
        1,
        10,
        from::hasIdrDtlRendProv,
        from::getIdrDtlRendProv,
        to::setIdrDtlRendProv);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrKDtlFacProvNpi,
        1,
        10,
        from::hasIdrKDtlFacProvNpi,
        from::getIdrKDtlFacProvNpi,
        to::setIdrKDtlFacProvNpi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbPickupAddres1,
        1,
        25,
        from::hasIdrDtlAmbPickupAddres1,
        from::getIdrDtlAmbPickupAddres1,
        to::setIdrDtlAmbPickupAddres1);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbPickupAddres2,
        1,
        20,
        from::hasIdrDtlAmbPickupAddres2,
        from::getIdrDtlAmbPickupAddres2,
        to::setIdrDtlAmbPickupAddres2);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbPickupCity,
        1,
        20,
        from::hasIdrDtlAmbPickupCity,
        from::getIdrDtlAmbPickupCity,
        to::setIdrDtlAmbPickupCity);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbPickupState,
        1,
        2,
        from::hasIdrDtlAmbPickupState,
        from::getIdrDtlAmbPickupState,
        to::setIdrDtlAmbPickupState);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbPickupZipcode,
        1,
        9,
        from::hasIdrDtlAmbPickupZipcode,
        from::getIdrDtlAmbPickupZipcode,
        to::setIdrDtlAmbPickupZipcode);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbDropoffName,
        1,
        24,
        from::hasIdrDtlAmbDropoffName,
        from::getIdrDtlAmbDropoffName,
        to::setIdrDtlAmbDropoffName);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbDropoffAddrL1,
        1,
        25,
        from::hasIdrDtlAmbDropoffAddrL1,
        from::getIdrDtlAmbDropoffAddrL1,
        to::setIdrDtlAmbDropoffAddrL1);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbDropoffAddrL2,
        1,
        20,
        from::hasIdrDtlAmbDropoffAddrL2,
        from::getIdrDtlAmbDropoffAddrL2,
        to::setIdrDtlAmbDropoffAddrL2);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbDropoffCity,
        1,
        20,
        from::hasIdrDtlAmbDropoffCity,
        from::getIdrDtlAmbDropoffCity,
        to::setIdrDtlAmbDropoffCity);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbDropoffState,
        1,
        2,
        from::hasIdrDtlAmbDropoffState,
        from::getIdrDtlAmbDropoffState,
        to::setIdrDtlAmbDropoffState);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlAmbDropoffZipcode,
        1,
        9,
        from::hasIdrDtlAmbDropoffZipcode,
        from::getIdrDtlAmbDropoffZipcode,
        to::setIdrDtlAmbDropoffZipcode);
    to.setLastUpdated(now);
    return to;
  }

  private PreAdjMcsDiagnosisCode transformMessageImpl(
      McsDiagnosisCode from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjMcsDiagnosisCode to = new PreAdjMcsDiagnosisCode();
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsDiagnosisCode.Fields.idrDiagIcdType,
        true,
        1,
        PreAdjMcsDiagnosisCode_idrDiagIcdType_Extractor.getEnumString(from),
        to::setIdrDiagIcdType);
    transformer.copyString(
        namePrefix + PreAdjMcsDiagnosisCode.Fields.idrDiagCode,
        false,
        1,
        7,
        from.getIdrDiagCode(),
        to::setIdrDiagCode);
    to.setLastUpdated(now);
    return to;
  }

  private PreAdjMcsAdjustment transformMessageImpl(
      McsAdjustment from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjMcsAdjustment to = new PreAdjMcsAdjustment();
    to.setLastUpdated(now);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsAdjustment.Fields.idrAdjDate,
        from::hasIdrAdjDate,
        from::getIdrAdjDate,
        to::setIdrAdjDate);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsAdjustment.Fields.idrXrefIcn,
        1,
        15,
        from::hasIdrXrefIcn,
        from::getIdrXrefIcn,
        to::setIdrXrefIcn);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsAdjustment.Fields.idrAdjClerk,
        1,
        4,
        from::hasIdrAdjClerk,
        from::getIdrAdjClerk,
        to::setIdrAdjClerk);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsAdjustment.Fields.idrInitCcn,
        1,
        15,
        from::hasIdrInitCcn,
        from::getIdrInitCcn,
        to::setIdrInitCcn);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsAdjustment.Fields.idrAdjChkWrtDt,
        from::hasIdrAdjChkWrtDt,
        from::getIdrAdjChkWrtDt,
        to::setIdrAdjChkWrtDt);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjMcsAdjustment.Fields.idrAdjBEombAmt,
        from::hasIdrAdjBEombAmt,
        from::getIdrAdjBEombAmt,
        to::setIdrAdjBEombAmt);
    transformer.copyOptionalAmount(
        namePrefix + PreAdjMcsAdjustment.Fields.idrAdjPEombAmt,
        from::hasIdrAdjPEombAmt,
        from::getIdrAdjPEombAmt,
        to::setIdrAdjPEombAmt);
    return to;
  }

  private PreAdjMcsAudit transformMessageImpl(
      McsAudit from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjMcsAudit to = new PreAdjMcsAudit();
    to.setLastUpdated(now);
    transformer.copyOptionalInt(from::hasIdrJAuditNum, from::getIdrJAuditNum, to::setIdrJAuditNum);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsAudit.Fields.idrJAuditInd,
        true,
        1,
        PreAdjMcsAudit_idrJAuditInd_Extractor.getEnumString(from),
        to::setIdrJAuditInd);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsAudit.Fields.idrJAuditDisp,
        true,
        1,
        PreAdjMcsAudit_idrJAuditDisp_Extractor.getEnumString(from),
        to::setIdrJAuditDisp);
    return to;
  }

  private PreAdjMcsLocation transformMessageImpl(
      McsLocation from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjMcsLocation to = new PreAdjMcsLocation();
    to.setLastUpdated(now);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsLocation.Fields.idrLocClerk,
        1,
        4,
        from::hasIdrLocClerk,
        from::getIdrLocClerk,
        to::setIdrLocClerk);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsLocation.Fields.idrLocCode,
        1,
        3,
        from::hasIdrLocCode,
        from::getIdrLocCode,
        to::setIdrLocCode);
    transformer.copyOptionalDate(
        namePrefix + PreAdjMcsLocation.Fields.idrLocDate,
        from::hasIdrLocDate,
        from::getIdrLocDate,
        to::setIdrLocDate);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsLocation.Fields.idrLocActvCode,
        true,
        1,
        PreAdjMcsLocation_idrLocActvCode_Extractor.getEnumString(from),
        to::setIdrLocActvCode);
    return to;
  }
}
