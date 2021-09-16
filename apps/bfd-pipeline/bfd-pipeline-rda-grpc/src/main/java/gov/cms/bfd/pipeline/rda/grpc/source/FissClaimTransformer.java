package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.collect.ImmutableSet;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PreAdjFissPayer;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissAssignmentOfBenefitsIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassification;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForClinics;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForSpecialFacilities;
import gov.cms.mpsm.rda.v1.fiss.FissBillFacilityType;
import gov.cms.mpsm.rda.v1.fiss.FissBillFrequency;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissCurrentLocation2;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisPresentOnAdmissionIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissInsuredPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPatientRelationshipCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcessingType;
import gov.cms.mpsm.rda.v1.fiss.FissReleaseOfInformation;
import java.time.Clock;
import java.util.List;

/**
 * Transforms a gRPC FissClaim object into a Hibernate PreAdjFissClaim object. Note that the gRPC
 * data objects are not proper java beans since their optional field getters should only be called
 * if their corresponding &quot;has&quot; methods return true. Optional fields are ignored when not
 * present. All other fields are validated and copied into a new PreAdjFissClaim object. A
 * lastUpdated time stamp is set using a Clock (for easier testing) and the MBI is hashed using an
 * IdHasher.
 */
public class FissClaimTransformer {
  private static final EnumStringExtractor<FissClaim, FissClaimStatus> currStatus =
      new EnumStringExtractor<>(
          FissClaim::hasCurrStatusEnum,
          FissClaim::getCurrStatusEnum,
          FissClaim::hasCurrStatusUnrecognized,
          FissClaim::getCurrStatusUnrecognized,
          FissClaimStatus.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of(EnumStringExtractor.Options.RejectUnrecognized));
  private static final EnumStringExtractor<FissClaim, FissProcessingType> currLoc1 =
      new EnumStringExtractor<>(
          FissClaim::hasCurrLoc1Enum,
          FissClaim::getCurrLoc1Enum,
          FissClaim::hasCurrLoc1Unrecognized,
          FissClaim::getCurrLoc1Unrecognized,
          FissProcessingType.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<FissClaim, FissCurrentLocation2> currLoc2 =
      new EnumStringExtractor<>(
          FissClaim::hasCurrLoc2Enum,
          FissClaim::getCurrLoc2Enum,
          FissClaim::hasCurrLoc2Unrecognized,
          FissClaim::getCurrLoc2Unrecognized,
          FissCurrentLocation2.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<FissClaim, FissBillFacilityType> lobCd =
      new EnumStringExtractor<>(
          FissClaim::hasLobCdEnum,
          FissClaim::getLobCdEnum,
          FissClaim::hasLobCdUnrecognized,
          FissClaim::getLobCdUnrecognized,
          FissBillFacilityType.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<FissClaim, FissBillClassification> servTypeCd =
      new EnumStringExtractor<>(
          FissClaim::hasServTypeCdEnum,
          FissClaim::getServTypeCdEnum,
          ignored -> false,
          ignored -> null,
          FissBillClassification.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<FissClaim, FissBillClassificationForClinics>
      servTypeCdForClinics =
          new EnumStringExtractor<>(
              FissClaim::hasServTypeCdForClinicsEnum,
              FissClaim::getServTypeCdForClinicsEnum,
              ignored -> false,
              ignored -> null,
              FissBillClassificationForClinics.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissClaim, FissBillClassificationForSpecialFacilities>
      servTypeCdForSpecialFacilities =
          new EnumStringExtractor<>(
              FissClaim::hasServTypeCdForSpecialFacilitiesEnum,
              FissClaim::getServTypeCdForSpecialFacilitiesEnum,
              ignored -> false,
              ignored -> null,
              FissBillClassificationForSpecialFacilities.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissClaim, FissBillFrequency> freqCd =
      new EnumStringExtractor<>(
          FissClaim::hasFreqCdEnum,
          FissClaim::getFreqCdEnum,
          FissClaim::hasFreqCdUnrecognized,
          FissClaim::getFreqCdUnrecognized,
          FissBillFrequency.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());

  private static final EnumStringExtractor<
          FissDiagnosisCode, FissDiagnosisPresentOnAdmissionIndicator>
      diagPoaInd =
          new EnumStringExtractor<>(
              FissDiagnosisCode::hasDiagPoaIndEnum,
              FissDiagnosisCode::getDiagPoaIndEnum,
              FissDiagnosisCode::hasDiagPoaIndUnrecognized,
              FissDiagnosisCode::getDiagPoaIndUnrecognized,
              FissDiagnosisPresentOnAdmissionIndicator.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());

  private static final EnumStringExtractor<FissInsuredPayer, FissPayersCode> insuredPayerPayersId =
      new EnumStringExtractor<>(
          FissInsuredPayer::hasPayersIdEnum,
          FissInsuredPayer::getPayersIdEnum,
          FissInsuredPayer::hasPayersIdUnrecognized,
          FissInsuredPayer::getPayersIdUnrecognized,
          FissPayersCode.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<FissInsuredPayer, FissReleaseOfInformation>
      insuredPayerRelInd =
          new EnumStringExtractor<>(
              FissInsuredPayer::hasRelIndEnum,
              FissInsuredPayer::getRelIndEnum,
              FissInsuredPayer::hasRelIndUnrecognized,
              FissInsuredPayer::getRelIndUnrecognized,
              FissReleaseOfInformation.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissInsuredPayer, FissAssignmentOfBenefitsIndicator>
      insuredPayerAssignInd =
          new EnumStringExtractor<>(
              FissInsuredPayer::hasAssignIndEnum,
              FissInsuredPayer::getAssignIndEnum,
              FissInsuredPayer::hasAssignIndUnrecognized,
              FissInsuredPayer::getAssignIndUnrecognized,
              FissAssignmentOfBenefitsIndicator.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissInsuredPayer, FissPatientRelationshipCode>
      insuredPayerInsuredRel =
          new EnumStringExtractor<>(
              FissInsuredPayer::hasInsuredRelEnum,
              FissInsuredPayer::getInsuredRelEnum,
              FissInsuredPayer::hasInsuredRelUnrecognized,
              FissInsuredPayer::getInsuredRelUnrecognized,
              FissPatientRelationshipCode.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissInsuredPayer, FissBeneficiarySex>
      insuredPayerInsuredSex =
          new EnumStringExtractor<>(
              FissInsuredPayer::hasInsuredSexEnum,
              FissInsuredPayer::getInsuredSexEnum,
              FissInsuredPayer::hasInsuredSexUnrecognized,
              FissInsuredPayer::getInsuredSexUnrecognized,
              FissBeneficiarySex.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissInsuredPayer, FissPatientRelationshipCode>
      insuredPayerInsuredRelX12 =
          new EnumStringExtractor<>(
              FissInsuredPayer::hasInsuredRelX12Enum,
              FissInsuredPayer::getInsuredRelX12Enum,
              FissInsuredPayer::hasInsuredRelX12Unrecognized,
              FissInsuredPayer::getInsuredRelX12Unrecognized,
              FissPatientRelationshipCode.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());

  private static final EnumStringExtractor<FissBeneZPayer, FissPayersCode> benezPayerPayersId =
      new EnumStringExtractor<>(
          FissBeneZPayer::hasPayersIdEnum,
          FissBeneZPayer::getPayersIdEnum,
          FissBeneZPayer::hasPayersIdUnrecognized,
          FissBeneZPayer::getPayersIdUnrecognized,
          FissPayersCode.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<FissBeneZPayer, FissReleaseOfInformation>
      benezPayerRelInd =
          new EnumStringExtractor<>(
              FissBeneZPayer::hasRelIndEnum,
              FissBeneZPayer::getRelIndEnum,
              FissBeneZPayer::hasRelIndUnrecognized,
              FissBeneZPayer::getRelIndUnrecognized,
              FissReleaseOfInformation.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissBeneZPayer, FissAssignmentOfBenefitsIndicator>
      benezPayerAssignInd =
          new EnumStringExtractor<>(
              FissBeneZPayer::hasAssignIndEnum,
              FissBeneZPayer::getAssignIndEnum,
              FissBeneZPayer::hasAssignIndUnrecognized,
              FissBeneZPayer::getAssignIndUnrecognized,
              FissAssignmentOfBenefitsIndicator.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissBeneZPayer, FissPatientRelationshipCode>
      benezPayerBeneRel =
          new EnumStringExtractor<>(
              FissBeneZPayer::hasBeneRelEnum,
              FissBeneZPayer::getBeneRelEnum,
              FissBeneZPayer::hasBeneRelUnrecognized,
              FissBeneZPayer::getBeneRelUnrecognized,
              FissPatientRelationshipCode.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissBeneZPayer, FissBeneficiarySex> benezPayerBeneSex =
      new EnumStringExtractor<>(
          FissBeneZPayer::hasBeneSexEnum,
          FissBeneZPayer::getBeneSexEnum,
          FissBeneZPayer::hasBeneSexUnrecognized,
          FissBeneZPayer::getBeneSexUnrecognized,
          FissBeneficiarySex.UNRECOGNIZED,
          ImmutableSet.of(),
          ImmutableSet.of());
  private static final EnumStringExtractor<FissBeneZPayer, FissBeneficiarySex>
      benezPayerInsuredSex =
          new EnumStringExtractor<>(
              FissBeneZPayer::hasInsuredSexEnum,
              FissBeneZPayer::getInsuredSexEnum,
              FissBeneZPayer::hasInsuredSexUnrecognized,
              FissBeneZPayer::getInsuredSexUnrecognized,
              FissBeneficiarySex.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());
  private static final EnumStringExtractor<FissBeneZPayer, FissPatientRelationshipCode>
      benezPayerInsuredRelX12 =
          new EnumStringExtractor<>(
              FissBeneZPayer::hasInsuredRelX12Enum,
              FissBeneZPayer::getInsuredRelX12Enum,
              FissBeneZPayer::hasInsuredRelX12Unrecognized,
              FissBeneZPayer::getInsuredRelX12Unrecognized,
              FissPatientRelationshipCode.UNRECOGNIZED,
              ImmutableSet.of(),
              ImmutableSet.of());

  private final Clock clock;
  private final IdHasher idHasher;

  public FissClaimTransformer(Clock clock, IdHasher idHasher) {
    this.clock = clock;
    this.idHasher = idHasher;
  }

  public RdaChange<PreAdjFissClaim> transformClaim(FissClaimChange change) {
    FissClaim from = change.getClaim();
    final DataTransformer transformer = new DataTransformer();
    final PreAdjFissClaim to = transformClaim(from, transformer);
    to.setSequenceNumber(change.getSeq());

    transformProcCodes(from, transformer, to);
    transformDiagCodes(from, transformer, to);
    transformPayers(from, transformer, to);
    List<DataTransformer.ErrorMessage> errors = transformer.getErrors();
    if (errors.size() > 0) {
      String message =
          String.format(
              "failed with %d errors: dcn=%s errors=%s", errors.size(), from.getDcn(), errors);
      throw new DataTransformer.TransformationException(message, errors);
    }
    return new RdaChange<>(
        change.getSeq(),
        RdaApiUtils.mapApiChangeType(change.getChangeType()),
        to,
        transformer.instant(change.getTimestamp()));
  }

  private PreAdjFissClaim transformClaim(FissClaim from, DataTransformer transformer) {
    final PreAdjFissClaim to = new PreAdjFissClaim();
    transformer
        .copyString(PreAdjFissClaim.Fields.dcn, false, 1, 23, from.getDcn(), to::setDcn)
        .copyString(PreAdjFissClaim.Fields.hicNo, false, 1, 12, from.getHicNo(), to::setHicNo)
        .copyEnumAsCharacter(
            PreAdjFissClaim.Fields.currStatus, currStatus.getEnumString(from), to::setCurrStatus)
        .copyEnumAsCharacter(
            PreAdjFissClaim.Fields.currLoc1, currLoc1.getEnumString(from), to::setCurrLoc1)
        .copyEnumAsString(
            PreAdjFissClaim.Fields.currLoc2,
            false,
            1,
            5,
            currLoc2.getEnumString(from),
            to::setCurrLoc2)
        .copyOptionalString(
            PreAdjFissClaim.Fields.medaProvId,
            1,
            13,
            from::hasMedaProvId,
            from::getMedaProvId,
            to::setMedaProvId)
        .copyOptionalString(
            PreAdjFissClaim.Fields.medaProv_6,
            1,
            6,
            from::hasMedaProv6,
            from::getMedaProv6,
            to::setMedaProv_6)
        .copyOptionalAmount(
            PreAdjFissClaim.Fields.totalChargeAmount,
            from::hasTotalChargeAmount,
            from::getTotalChargeAmount,
            to::setTotalChargeAmount)
        .copyOptionalDate(
            PreAdjFissClaim.Fields.receivedDate,
            from::hasRecdDtCymd,
            from::getRecdDtCymd,
            to::setReceivedDate)
        .copyOptionalDate(
            PreAdjFissClaim.Fields.currTranDate,
            from::hasCurrTranDtCymd,
            from::getCurrTranDtCymd,
            to::setCurrTranDate)
        .copyOptionalString(
            PreAdjFissClaim.Fields.admitDiagCode,
            1,
            7,
            from::hasAdmDiagCode,
            from::getAdmDiagCode,
            to::setAdmitDiagCode)
        .copyOptionalString(
            PreAdjFissClaim.Fields.principleDiag,
            1,
            7,
            from::hasPrincipleDiag,
            from::getPrincipleDiag,
            to::setPrincipleDiag)
        .copyOptionalString(
            PreAdjFissClaim.Fields.npiNumber,
            1,
            10,
            from::hasNpiNumber,
            from::getNpiNumber,
            to::setNpiNumber)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocAddr1,
            1,
            Integer.MAX_VALUE,
            from::hasPracLocAddr1,
            from::getPracLocAddr1,
            to::setPracLocAddr1)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocAddr2,
            1,
            Integer.MAX_VALUE,
            from::hasPracLocAddr2,
            from::getPracLocAddr2,
            to::setPracLocAddr2)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocCity,
            1,
            Integer.MAX_VALUE,
            from::hasPracLocCity,
            from::getPracLocCity,
            to::setPracLocCity)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocState,
            1,
            2,
            from::hasPracLocState,
            from::getPracLocState,
            to::setPracLocState)
        .copyOptionalString(
            PreAdjFissClaim.Fields.pracLocZip,
            1,
            15,
            from::hasPracLocZip,
            from::getPracLocZip,
            to::setPracLocZip);
    if (from.hasMbi()) {
      final String mbi = from.getMbi();
      transformer
          .copyString(PreAdjFissClaim.Fields.mbi, true, 1, 13, mbi, to::setMbi)
          .copyString(
              PreAdjFissClaim.Fields.mbiHash,
              true,
              64,
              64,
              idHasher.computeIdentifierHash(mbi),
              to::setMbiHash);
    }
    transformer
        .copyOptionalString(
            PreAdjFissClaim.Fields.fedTaxNumber,
            1,
            10,
            from::hasFedTaxNb,
            from::getFedTaxNb,
            to::setFedTaxNumber)
        .copyOptionalDate(
            PreAdjFissClaim.Fields.stmtCovFromDate,
            from::hasStmtCovFromCymd,
            from::getStmtCovFromCymd,
            to::setStmtCovFromDate)
        .copyOptionalDate(
            PreAdjFissClaim.Fields.stmtCovToDate,
            from::hasStmtCovToCymd,
            from::getStmtCovToCymd,
            to::setStmtCovToDate)
        .copyEnumAsString(
            PreAdjFissClaim.Fields.lobCd, true, 1, 1, lobCd.getEnumString(from), to::setLobCd)
        .copyEnumAsString(
            PreAdjFissClaim.Fields.servTypeCd,
            true,
            1,
            1,
            servTypeCd.getEnumString(from),
            value -> {
              to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Normal);
              to.setServTypeCd(value);
            })
        .copyEnumAsString(
            PreAdjFissClaim.Fields.servTypeCd,
            true,
            1,
            1,
            servTypeCdForClinics.getEnumString(from),
            value -> {
              to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Clinic);
              to.setServTypeCd(value);
            })
        .copyEnumAsString(
            PreAdjFissClaim.Fields.servTypeCd,
            true,
            1,
            1,
            servTypeCdForSpecialFacilities.getEnumString(from),
            value -> {
              to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.SpecialFacility);
              to.setServTypeCd(value);
            })
        .copyOptionalString(
            PreAdjFissClaim.Fields.servTypeCd,
            1,
            1,
            from::hasServTypCdUnrecognized,
            from::getServTypCdUnrecognized,
            value -> {
              to.setServTypeCdMapping(PreAdjFissClaim.ServTypeCdMapping.Unrecognized);
              to.setServTypeCd(value);
            })
        .copyEnumAsString(
            PreAdjFissClaim.Fields.freqCd, true, 1, 1, freqCd.getEnumString(from), to::setFreqCd)
        .copyOptionalString(
            PreAdjFissClaim.Fields.billTypCd,
            1,
            3,
            from::hasBillTypCd,
            from::getBillTypCd,
            to::setBillTypCd);
    to.setLastUpdated(clock.instant());
    return to;
  }

  private void transformProcCodes(FissClaim from, DataTransformer transformer, PreAdjFissClaim to) {
    short priority = 0;
    for (FissProcedureCode fromCode : from.getFissProcCodesList()) {
      String fieldPrefix = "procCode-" + priority + "-";
      PreAdjFissProcCode toCode =
          transformProcCode(transformer, to, priority, fromCode, fieldPrefix);
      to.getProcCodes().add(toCode);
      priority += 1;
    }
  }

  private void transformDiagCodes(FissClaim from, DataTransformer transformer, PreAdjFissClaim to) {
    short priority = 0;
    for (FissDiagnosisCode fromCode : from.getFissDiagCodesList()) {
      String fieldPrefix = "diagCode-" + priority + "-";
      PreAdjFissDiagnosisCode toCode =
          transformDiagCode(transformer, to, priority, fromCode, fieldPrefix);
      to.getDiagCodes().add(toCode);
      priority += 1;
    }
  }

  private void transformPayers(FissClaim from, DataTransformer transformer, PreAdjFissClaim to) {
    short priority = 0;
    for (FissPayer fromPayer : from.getFissPayersList()) {
      String fieldPrefix = "payer-" + priority + "-";
      PreAdjFissPayer toPayer =
          fromPayer.hasInsuredPayer()
              ? transformInsuredPayer(
                  transformer, to, priority, fromPayer.getInsuredPayer(), fieldPrefix)
              : transformBeneZPayer(
                  transformer, to, priority, fromPayer.getBeneZPayer(), fieldPrefix);
      to.getPayers().add(toPayer);
      priority += 1;
    }
  }

  private PreAdjFissProcCode transformProcCode(
      DataTransformer transformer,
      PreAdjFissClaim to,
      short priority,
      FissProcedureCode fromCode,
      String fieldPrefix) {
    PreAdjFissProcCode toCode = new PreAdjFissProcCode();
    toCode.setDcn(to.getDcn());
    toCode.setPriority(priority);
    transformer
        .copyString(
            fieldPrefix + PreAdjFissProcCode.Fields.procCode,
            false,
            1,
            10,
            fromCode.getProcCd(),
            toCode::setProcCode)
        .copyOptionalString(
            fieldPrefix + PreAdjFissProcCode.Fields.procFlag,
            1,
            4,
            fromCode::hasProcFlag,
            fromCode::getProcFlag,
            toCode::setProcFlag)
        .copyOptionalDate(
            fieldPrefix + PreAdjFissProcCode.Fields.procDate,
            fromCode::hasProcDt,
            fromCode::getProcDt,
            toCode::setProcDate);
    toCode.setLastUpdated(to.getLastUpdated());
    return toCode;
  }

  private PreAdjFissDiagnosisCode transformDiagCode(
      DataTransformer transformer,
      PreAdjFissClaim to,
      short priority,
      FissDiagnosisCode fromCode,
      String fieldPrefix) {
    PreAdjFissDiagnosisCode toCode = new PreAdjFissDiagnosisCode();
    toCode.setDcn(to.getDcn());
    toCode.setPriority(priority);
    transformer
        .copyString(
            fieldPrefix + PreAdjFissDiagnosisCode.Fields.diagCd2,
            false,
            1,
            7,
            fromCode.getDiagCd2(),
            toCode::setDiagCd2)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissDiagnosisCode.Fields.diagPoaInd,
            false,
            1,
            1,
            diagPoaInd.getEnumString(fromCode),
            toCode::setDiagPoaInd)
        .copyOptionalString(
            fieldPrefix + PreAdjFissDiagnosisCode.Fields.bitFlags,
            1,
            4,
            fromCode::hasBitFlags,
            fromCode::getBitFlags,
            toCode::setBitFlags);
    toCode.setLastUpdated(to.getLastUpdated());
    return toCode;
  }

  private PreAdjFissPayer transformInsuredPayer(
      DataTransformer transformer,
      PreAdjFissClaim to,
      short priority,
      FissInsuredPayer fromPayer,
      String fieldPrefix) {
    PreAdjFissPayer toPayer = new PreAdjFissPayer();
    toPayer.setDcn(to.getDcn());
    toPayer.setPriority(priority);
    toPayer.setPayerType(PreAdjFissPayer.PayerType.Insured);
    transformer
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.payersId,
            true,
            1,
            1,
            insuredPayerPayersId.getEnumString(fromPayer),
            toPayer::setPayersId)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.payersName,
            1,
            32,
            fromPayer::hasPayersName,
            fromPayer::getPayersName,
            toPayer::setPayersName)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.relInd,
            true,
            1,
            1,
            insuredPayerRelInd.getEnumString(fromPayer),
            toPayer::setRelInd)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.assignInd,
            true,
            1,
            1,
            insuredPayerAssignInd.getEnumString(fromPayer),
            toPayer::setAssignInd)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.providerNumber,
            1,
            13,
            fromPayer::hasProviderNumber,
            fromPayer::getProviderNumber,
            toPayer::setProviderNumber)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.adjDcnIcn,
            1,
            23,
            fromPayer::hasAdjDcnIcn,
            fromPayer::getAdjDcnIcn,
            toPayer::setAdjDcnIcn)
        .copyOptionalAmount(
            PreAdjFissPayer.Fields.priorPmt,
            fromPayer::hasPriorPmt,
            fromPayer::getPriorPmt,
            toPayer::setPriorPmt)
        .copyOptionalAmount(
            PreAdjFissPayer.Fields.estAmtDue,
            fromPayer::hasEstAmtDue,
            fromPayer::getEstAmtDue,
            toPayer::setEstAmtDue)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredRel,
            true,
            1,
            2,
            insuredPayerInsuredRel.getEnumString(fromPayer),
            toPayer::setInsuredRel)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredName,
            1,
            25,
            fromPayer::hasInsuredName,
            fromPayer::getInsuredName,
            toPayer::setInsuredName)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredSsnHic,
            1,
            19,
            fromPayer::hasInsuredSsnHic,
            fromPayer::getInsuredSsnHic,
            toPayer::setInsuredSsnHic)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredGroupName,
            1,
            17,
            fromPayer::hasInsuredGroupName,
            fromPayer::getInsuredGroupName,
            toPayer::setInsuredGroupName)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredGroupNbr,
            1,
            20,
            fromPayer::hasInsuredGroupNbr,
            fromPayer::getInsuredGroupNbr,
            toPayer::setInsuredGroupNbr)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.treatAuthCd,
            1,
            18,
            fromPayer::hasTreatAuthCd,
            fromPayer::getTreatAuthCd,
            toPayer::setTreatAuthCd)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredSex,
            true,
            1,
            1,
            insuredPayerInsuredSex.getEnumString(fromPayer),
            toPayer::setInsuredSex)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredRelX12,
            true,
            1,
            2,
            insuredPayerInsuredRelX12.getEnumString(fromPayer),
            toPayer::setInsuredRelX12)
        .copyOptionalDate(
            fieldPrefix + PreAdjFissPayer.Fields.insuredDob,
            fromPayer::hasInsuredDob,
            fromPayer::getInsuredDob,
            toPayer::setInsuredDob)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredDobText,
            1,
            9,
            fromPayer::hasInsuredDobText,
            fromPayer::getInsuredDobText,
            toPayer::setInsuredDobText);
    toPayer.setLastUpdated(to.getLastUpdated());
    return toPayer;
  }

  private PreAdjFissPayer transformBeneZPayer(
      DataTransformer transformer,
      PreAdjFissClaim to,
      short priority,
      FissBeneZPayer fromPayer,
      String fieldPrefix) {
    PreAdjFissPayer toPayer = new PreAdjFissPayer();
    toPayer.setDcn(to.getDcn());
    toPayer.setPriority(priority);
    toPayer.setPayerType(PreAdjFissPayer.PayerType.BeneZ);
    transformer
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.payersId,
            true,
            1,
            1,
            benezPayerPayersId.getEnumString(fromPayer),
            toPayer::setPayersId)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.payersName,
            1,
            32,
            fromPayer::hasPayersName,
            fromPayer::getPayersName,
            toPayer::setPayersName)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.relInd,
            true,
            1,
            1,
            benezPayerRelInd.getEnumString(fromPayer),
            toPayer::setRelInd)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.assignInd,
            true,
            1,
            1,
            benezPayerAssignInd.getEnumString(fromPayer),
            toPayer::setAssignInd)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.providerNumber,
            1,
            13,
            fromPayer::hasProviderNumber,
            fromPayer::getProviderNumber,
            toPayer::setProviderNumber)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.adjDcnIcn,
            1,
            23,
            fromPayer::hasAdjDcnIcn,
            fromPayer::getAdjDcnIcn,
            toPayer::setAdjDcnIcn)
        .copyOptionalAmount(
            PreAdjFissPayer.Fields.priorPmt,
            fromPayer::hasPriorPmt,
            fromPayer::getPriorPmt,
            toPayer::setPriorPmt)
        .copyOptionalAmount(
            PreAdjFissPayer.Fields.estAmtDue,
            fromPayer::hasEstAmtDue,
            fromPayer::getEstAmtDue,
            toPayer::setEstAmtDue)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.beneRel,
            true,
            1,
            2,
            benezPayerBeneRel.getEnumString(fromPayer),
            toPayer::setBeneRel)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.beneLastName,
            1,
            15,
            fromPayer::hasBeneLastName,
            fromPayer::getBeneLastName,
            toPayer::setBeneLastName)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.beneFirstName,
            1,
            10,
            fromPayer::hasBeneFirstName,
            fromPayer::getBeneFirstName,
            toPayer::setBeneFirstName)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.beneMidInit,
            1,
            1,
            fromPayer::hasBeneMidInit,
            fromPayer::getBeneMidInit,
            toPayer::setBeneMidInit)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.beneSsnHic,
            1,
            19,
            fromPayer::hasBeneSsnHic,
            fromPayer::getBeneSsnHic,
            toPayer::setBeneSsnHic)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredGroupName,
            1,
            17,
            fromPayer::hasInsuredGroupName,
            fromPayer::getInsuredGroupName,
            toPayer::setInsuredGroupName)
        .copyOptionalDate(
            fieldPrefix + PreAdjFissPayer.Fields.beneDob,
            fromPayer::hasBeneDob,
            fromPayer::getBeneDob,
            toPayer::setBeneDob)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.beneSex,
            true,
            1,
            1,
            benezPayerBeneSex.getEnumString(fromPayer),
            toPayer::setBeneSex)
        .copyOptionalString(
            fieldPrefix + PreAdjFissPayer.Fields.treatAuthCd,
            1,
            18,
            fromPayer::hasTreatAuthCd,
            fromPayer::getTreatAuthCd,
            toPayer::setTreatAuthCd)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredSex,
            true,
            1,
            1,
            benezPayerInsuredSex.getEnumString(fromPayer),
            toPayer::setInsuredSex)
        .copyEnumAsString(
            fieldPrefix + PreAdjFissPayer.Fields.insuredRelX12,
            true,
            1,
            2,
            benezPayerInsuredRelX12.getEnumString(fromPayer),
            toPayer::setInsuredRelX12);
    toPayer.setLastUpdated(to.getLastUpdated());
    return toPayer;
  }
}
