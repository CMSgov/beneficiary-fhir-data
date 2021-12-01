package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDetail;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.McsClaimChange;
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
import java.time.Instant;
import java.util.List;

public class McsClaimTransformer {
  private final EnumStringExtractor<McsClaim, McsClaimType> PreAdjMcsClaim_idrClaimType_Extractor;

  private final EnumStringExtractor<McsClaim, McsBeneficiarySex>
      PreAdjMcsClaim_idrBeneSex_Extractor;

  private final EnumStringExtractor<McsClaim, McsStatusCode> PreAdjMcsClaim_idrStatusCode_Extractor;

  private final EnumStringExtractor<McsClaim, McsBillingProviderIndicator>
      PreAdjMcsClaim_idrBillProvGroupInd_Extractor;

  private final EnumStringExtractor<McsClaim, McsBillingProviderStatusCode>
      PreAdjMcsClaim_idrBillProvStatusCd_Extractor;

  private final EnumStringExtractor<McsDetail, McsDetailStatus>
      PreAdjMcsDetail_idrDtlStatus_Extractor;

  private final EnumStringExtractor<McsDetail, McsDiagnosisIcdType>
      PreAdjMcsDetail_idrDtlDiagIcdType_Extractor;

  private final EnumStringExtractor<McsDiagnosisCode, McsDiagnosisIcdType>
      PreAdjMcsDiagnosisCode_idrDiagIcdType_Extractor;

  private final Clock clock;
  private final IdHasher idHasher;

  public McsClaimTransformer(Clock clock, IdHasher idHasher) {
    this.clock = clock;
    this.idHasher = idHasher;
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
    PreAdjMcsDiagnosisCode_idrDiagIcdType_Extractor =
        new EnumStringExtractor<>(
            McsDiagnosisCode::hasIdrDiagIcdTypeEnum,
            McsDiagnosisCode::getIdrDiagIcdTypeEnum,
            McsDiagnosisCode::hasIdrDiagIcdTypeUnrecognized,
            McsDiagnosisCode::getIdrDiagIcdTypeUnrecognized,
            McsDiagnosisIcdType.UNRECOGNIZED,
            ImmutableSet.of(),
            ImmutableSet.of());
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
        1,
        PreAdjMcsClaim_idrBeneSex_Extractor.getEnumString(from),
        to::setIdrBeneSex);
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsClaim.Fields.idrStatusCode,
        true,
        1,
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
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrClaimMbi,
        1,
        13,
        from::hasIdrClaimMbi,
        from::getIdrClaimMbi,
        to::setIdrClaimMbi);
    transformer.copyOptionalString(
        namePrefix + PreAdjMcsClaim.Fields.idrClaimMbiHash,
        1,
        64,
        from::hasIdrClaimMbi,
        () -> idHasher.computeIdentifierHash(from.getIdrClaimMbi()),
        to::setIdrClaimMbiHash);
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
  }

  private PreAdjMcsDetail transformMessageImpl(
      McsDetail from, DataTransformer transformer, Instant now, String namePrefix) {
    final PreAdjMcsDetail to = new PreAdjMcsDetail();
    transformer.copyEnumAsString(
        namePrefix + PreAdjMcsDetail.Fields.idrDtlStatus,
        true,
        1,
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
}
