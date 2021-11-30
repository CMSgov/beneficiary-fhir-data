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
  private static final EnumStringExtractor<McsClaim, McsClaimType> idrClaimType =
      new EnumStringExtractor<>(
          McsClaim::hasIdrClaimTypeEnum,
          McsClaim::getIdrClaimTypeEnum,
          McsClaim::hasIdrClaimTypeUnrecognized,
          McsClaim::getIdrClaimTypeUnrecognized,
          McsClaimType.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<McsClaim, McsBeneficiarySex> idrBeneSex =
      new EnumStringExtractor<>(
          McsClaim::hasIdrBeneSexEnum,
          McsClaim::getIdrBeneSexEnum,
          McsClaim::hasIdrBeneSexUnrecognized,
          McsClaim::getIdrBeneSexUnrecognized,
          McsBeneficiarySex.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<McsClaim, McsStatusCode> idrStatusCode =
      new EnumStringExtractor<>(
          McsClaim::hasIdrStatusCodeEnum,
          McsClaim::getIdrStatusCodeEnum,
          McsClaim::hasIdrStatusCodeUnrecognized,
          McsClaim::getIdrStatusCodeUnrecognized,
          McsStatusCode.UNRECOGNIZED,
          ImmutableSet.of(McsStatusCode.STATUS_CODE_NOT_USED),
          ImmutableSet.of(EnumStringExtractor.Options.RejectUnrecognized));
  private static final EnumStringExtractor<McsClaim, McsBillingProviderIndicator>
      idrBillProvGroupInd =
          new EnumStringExtractor<>(
              McsClaim::hasIdrBillProvGroupIndEnum,
              McsClaim::getIdrBillProvGroupIndEnum,
              McsClaim::hasIdrBillProvGroupIndUnrecognized,
              McsClaim::getIdrBillProvGroupIndUnrecognized,
              McsBillingProviderIndicator.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<McsClaim, McsBillingProviderStatusCode>
      idrBillProvStatusCd =
          new EnumStringExtractor<>(
              McsClaim::hasIdrBillProvStatusCdEnum,
              McsClaim::getIdrBillProvStatusCdEnum,
              McsClaim::hasIdrBillProvStatusCdUnrecognized,
              McsClaim::getIdrBillProvStatusCdUnrecognized,
              McsBillingProviderStatusCode.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<McsDiagnosisCode, McsDiagnosisIcdType> idrDiagIcdType =
      new EnumStringExtractor<>(
          McsDiagnosisCode::hasIdrDiagIcdTypeEnum,
          McsDiagnosisCode::getIdrDiagIcdTypeEnum,
          McsDiagnosisCode::hasIdrDiagIcdTypeUnrecognized,
          McsDiagnosisCode::getIdrDiagIcdTypeUnrecognized,
          McsDiagnosisIcdType.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<McsDetail, McsDetailStatus> idrDtlStatus =
      new EnumStringExtractor<>(
          McsDetail::hasIdrDtlStatusEnum,
          McsDetail::getIdrDtlStatusEnum,
          McsDetail::hasIdrDtlStatusUnrecognized,
          McsDetail::getIdrDtlStatusUnrecognized,
          McsDetailStatus.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<McsDetail, McsDiagnosisIcdType> idrDtlDiagIcdType =
      new EnumStringExtractor<>(
          McsDetail::hasIdrDtlDiagIcdTypeEnum,
          McsDetail::getIdrDtlDiagIcdTypeEnum,
          McsDetail::hasIdrDtlDiagIcdTypeUnrecognized,
          McsDetail::getIdrDtlDiagIcdTypeUnrecognized,
          McsDiagnosisIcdType.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());

  private final Clock clock;
  private final IdHasher idHasher;

  public McsClaimTransformer(Clock clock, IdHasher idHasher) {
    this.clock = clock;
    this.idHasher = idHasher;
  }

  public RdaChange<PreAdjMcsClaim> transformClaim(McsClaimChange change) {
    McsClaim from = change.getClaim();

    final DataTransformer transformer = new DataTransformer();
    final PreAdjMcsClaim to = transformClaim(from, transformer);
    to.setSequenceNumber(change.getSeq());

    List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
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

  private PreAdjMcsClaim transformClaim(McsClaim from, DataTransformer transformer) {
    final Instant now = clock.instant();
    final PreAdjMcsClaim to = new PreAdjMcsClaim();
    transformer
        .copyString(
            PreAdjMcsClaim.Fields.idrClmHdIcn,
            false,
            1,
            15,
            from.getIdrClmHdIcn(),
            to::setIdrClmHdIcn)
        .copyString(
            PreAdjMcsClaim.Fields.idrContrId, false, 1, 5, from.getIdrContrId(), to::setIdrContrId)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrHic, 1, 12, from::hasIdrHic, from::getIdrHic, to::setIdrHic)
        .copyEnumAsString(
            PreAdjMcsClaim.Fields.idrClaimType,
            false,
            1,
            1,
            idrClaimType.getEnumString(from),
            to::setIdrClaimType)
        .copyOptionalInt(from::hasIdrDtlCnt, from::getIdrDtlCnt, to::setIdrDtlCnt)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBeneLast_1_6,
            1,
            6,
            from::hasIdrBeneLast16,
            from::getIdrBeneLast16,
            to::setIdrBeneLast_1_6)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBeneFirstInit,
            1,
            1,
            from::hasIdrBeneFirstInit,
            from::getIdrBeneFirstInit,
            to::setIdrBeneFirstInit)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBeneMidInit,
            1,
            1,
            from::hasIdrBeneMidInit,
            from::getIdrBeneMidInit,
            to::setIdrBeneMidInit)
        .copyEnumAsString(
            PreAdjMcsClaim.Fields.idrBeneSex,
            true,
            1,
            1,
            idrBeneSex.getEnumString(from),
            to::setIdrBeneSex)
        .copyEnumAsString(
            PreAdjMcsClaim.Fields.idrStatusCode,
            true,
            1,
            1,
            idrStatusCode.getEnumString(from),
            to::setIdrStatusCode)
        .copyOptionalDate(
            PreAdjMcsClaim.Fields.idrStatusDate,
            from::hasIdrStatusDate,
            from::getIdrStatusDate,
            to::setIdrStatusDate)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvNpi,
            1,
            10,
            from::hasIdrBillProvNpi,
            from::getIdrBillProvNpi,
            to::setIdrBillProvNpi)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvNum,
            1,
            10,
            from::hasIdrBillProvNum,
            from::getIdrBillProvNum,
            to::setIdrBillProvNum)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvEin,
            1,
            10,
            from::hasIdrBillProvEin,
            from::getIdrBillProvEin,
            to::setIdrBillProvEin)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvType,
            1,
            2,
            from::hasIdrBillProvType,
            from::getIdrBillProvType,
            to::setIdrBillProvType)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvSpec,
            1,
            2,
            from::hasIdrBillProvSpec,
            from::getIdrBillProvSpec,
            to::setIdrBillProvSpec)
        .copyEnumAsString(
            PreAdjMcsClaim.Fields.idrBillProvGroupInd,
            true,
            1,
            1,
            idrBillProvGroupInd.getEnumString(from),
            to::setIdrBillProvGroupInd)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvPriceSpec,
            1,
            2,
            from::hasIdrBillProvPriceSpec,
            from::getIdrBillProvPriceSpec,
            to::setIdrBillProvPriceSpec)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvCounty,
            1,
            2,
            from::hasIdrBillProvCounty,
            from::getIdrBillProvCounty,
            to::setIdrBillProvCounty)
        .copyOptionalString(
            PreAdjMcsClaim.Fields.idrBillProvLoc,
            1,
            2,
            from::hasIdrBillProvLoc,
            from::getIdrBillProvLoc,
            to::setIdrBillProvLoc)
        .copyOptionalAmount(
            PreAdjMcsClaim.Fields.idrTotAllowed,
            from::hasIdrTotAllowed,
            from::getIdrTotAllowed,
            to::setIdrTotAllowed)
        .copyOptionalAmount(
            PreAdjMcsClaim.Fields.idrCoinsurance,
            from::hasIdrCoinsurance,
            from::getIdrCoinsurance,
            to::setIdrCoinsurance)
        .copyOptionalAmount(
            PreAdjMcsClaim.Fields.idrDeductible,
            from::hasIdrDeductible,
            from::getIdrDeductible,
            to::setIdrDeductible)
        .copyEnumAsString(
            PreAdjMcsClaim.Fields.idrBillProvStatusCd,
            true,
            1,
            1,
            idrBillProvStatusCd.getEnumString(from),
            to::setIdrBillProvStatusCd)
        .copyOptionalAmount(
            PreAdjMcsClaim.Fields.idrTotBilledAmt,
            from::hasIdrTotBilledAmt,
            from::getIdrTotBilledAmt,
            to::setIdrTotBilledAmt)
        .copyOptionalDate(
            PreAdjMcsClaim.Fields.idrClaimReceiptDate,
            from::hasIdrClaimReceiptDate,
            from::getIdrClaimReceiptDate,
            to::setIdrClaimReceiptDate);
    if (from.hasIdrClaimMbi()) {
      final String mbi = from.getIdrClaimMbi();
      transformer
          .copyString(PreAdjMcsClaim.Fields.idrClaimMbi, true, 1, 13, mbi, to::setIdrClaimMbi)
          .copyString(
              PreAdjMcsClaim.Fields.idrClaimMbiHash,
              true,
              64,
              64,
              idHasher.computeIdentifierHash(mbi),
              to::setIdrClaimMbiHash);
    }
    transformer
        .copyOptionalDate(
            PreAdjMcsClaim.Fields.idrHdrFromDateOfSvc,
            from::hasIdrHdrFromDos,
            from::getIdrHdrFromDos,
            to::setIdrHdrFromDateOfSvc)
        .copyOptionalDate(
            PreAdjMcsClaim.Fields.idrHdrToDateOfSvc,
            from::hasIdrHdrToDos,
            from::getIdrHdrToDos,
            to::setIdrHdrToDateOfSvc);

    to.setLastUpdated(now);

    transformDiagnosisCodes(transformer, now, from, to);
    transformDetails(transformer, now, from, to);

    return to;
  }

  private void transformDiagnosisCodes(
      DataTransformer transformer, Instant now, McsClaim from, PreAdjMcsClaim to) {
    short priority = 0;
    for (McsDiagnosisCode fromDiagnosisCode : from.getMcsDiagnosisCodesList()) {
      String fieldPrefix = "diagCode-" + priority + "-";
      PreAdjMcsDiagnosisCode toDiagnosisCode =
          transformDiagnosisCode(
              transformer, now, fromDiagnosisCode, from.getIdrClmHdIcn(), priority, fieldPrefix);
      to.getDiagCodes().add(toDiagnosisCode);
      priority += 1;
    }
  }

  private PreAdjMcsDiagnosisCode transformDiagnosisCode(
      DataTransformer transformer,
      Instant now,
      McsDiagnosisCode from,
      String idrClmHdIcn,
      short priority,
      String fieldPrefix) {
    final PreAdjMcsDiagnosisCode to = new PreAdjMcsDiagnosisCode();
    transformer
        .copyStringWithExpectedValue(
            fieldPrefix + PreAdjMcsDiagnosisCode.Fields.idrClmHdIcn,
            false,
            1,
            15,
            idrClmHdIcn,
            from.getIdrClmHdIcn(),
            to::setIdrClmHdIcn)
        .copyEnumAsString(
            fieldPrefix + PreAdjMcsDiagnosisCode.Fields.idrDiagIcdType,
            true,
            1,
            1,
            idrDiagIcdType.getEnumString(from),
            to::setIdrDiagIcdType)
        .copyString(
            fieldPrefix + PreAdjMcsDiagnosisCode.Fields.idrDiagCode,
            false,
            1,
            7,
            from.getIdrDiagCode(),
            to::setIdrDiagCode);
    to.setPriority(priority);
    to.setLastUpdated(now);
    return to;
  }

  private void transformDetails(
      DataTransformer transformer, Instant now, McsClaim from, PreAdjMcsClaim to) {
    short priority = 0;
    for (McsDetail fromDetail : from.getMcsDetailsList()) {
      String fieldPrefix = "detail-" + priority + "-";
      PreAdjMcsDetail toDetail =
          transformDetail(
              transformer, now, from.getIdrClmHdIcn(), fromDetail, priority, fieldPrefix);
      to.getDetails().add(toDetail);
      priority += 1;
    }
  }

  private PreAdjMcsDetail transformDetail(
      DataTransformer transformer,
      Instant now,
      String idrClmHdIcn,
      McsDetail from,
      short priority,
      String fieldPrefix) {
    final PreAdjMcsDetail to = new PreAdjMcsDetail();
    transformer
        .copyEnumAsString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrDtlStatus,
            true,
            1,
            1,
            idrDtlStatus.getEnumString(from),
            to::setIdrDtlStatus)
        .copyOptionalDate(
            fieldPrefix + PreAdjMcsDetail.Fields.idrDtlFromDate,
            from::hasIdrDtlFromDate,
            from::getIdrDtlFromDate,
            to::setIdrDtlFromDate)
        .copyOptionalDate(
            fieldPrefix + PreAdjMcsDetail.Fields.idrDtlToDate,
            from::hasIdrDtlToDate,
            from::getIdrDtlToDate,
            to::setIdrDtlToDate)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrProcCode,
            1,
            5,
            from::hasIdrProcCode,
            from::getIdrProcCode,
            to::setIdrProcCode)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrModOne,
            1,
            2,
            from::hasIdrModOne,
            from::getIdrModOne,
            to::setIdrModOne)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrModTwo,
            1,
            2,
            from::hasIdrModTwo,
            from::getIdrModTwo,
            to::setIdrModTwo)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrModThree,
            1,
            2,
            from::hasIdrModThree,
            from::getIdrModThree,
            to::setIdrModThree)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrModFour,
            1,
            2,
            from::hasIdrModFour,
            from::getIdrModFour,
            to::setIdrModFour)
        .copyEnumAsString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrDtlDiagIcdType,
            true,
            1,
            1,
            idrDtlDiagIcdType.getEnumString(from),
            to::setIdrDtlDiagIcdType)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrDtlPrimaryDiagCode,
            1,
            7,
            from::hasIdrDtlPrimaryDiagCode,
            from::getIdrDtlPrimaryDiagCode,
            to::setIdrDtlPrimaryDiagCode)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosLnameOrg,
            1,
            60,
            from::hasIdrKPosLnameOrg,
            from::getIdrKPosLnameOrg,
            to::setIdrKPosLnameOrg)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosFname,
            1,
            35,
            from::hasIdrKPosFname,
            from::getIdrKPosFname,
            to::setIdrKPosFname)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosMname,
            1,
            25,
            from::hasIdrKPosMname,
            from::getIdrKPosMname,
            to::setIdrKPosMname)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosAddr1,
            1,
            55,
            from::hasIdrKPosAddr1,
            from::getIdrKPosAddr1,
            to::setIdrKPosAddr1)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosAddr2_1st,
            1,
            30,
            from::hasIdrKPosAddr21St,
            from::getIdrKPosAddr21St,
            to::setIdrKPosAddr2_1st)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosAddr2_2nd,
            1,
            25,
            from::hasIdrKPosAddr22Nd,
            from::getIdrKPosAddr22Nd,
            to::setIdrKPosAddr2_2nd)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosCity,
            1,
            30,
            from::hasIdrKPosCity,
            from::getIdrKPosCity,
            to::setIdrKPosCity)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosState,
            1,
            2,
            from::hasIdrKPosState,
            from::getIdrKPosState,
            to::setIdrKPosState)
        .copyOptionalString(
            fieldPrefix + PreAdjMcsDetail.Fields.idrKPosZip,
            1,
            15,
            from::hasIdrKPosZip,
            from::getIdrKPosZip,
            to::setIdrKPosZip);

    to.setIdrClmHdIcn(idrClmHdIcn);
    to.setPriority(priority);
    to.setLastUpdated(now);
    return to;
  }
}
