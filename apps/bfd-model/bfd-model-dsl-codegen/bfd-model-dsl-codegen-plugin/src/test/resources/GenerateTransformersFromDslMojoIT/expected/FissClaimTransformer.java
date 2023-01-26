package gov.cms.test;

import gov.cms.model.dsl.codegen.library.DataTransformer;
import gov.cms.model.dsl.codegen.library.EnumStringExtractor;
import gov.cms.model.dsl.codegen.library.ExternalTransformation;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassification;
import gov.cms.mpsm.rda.v1.fiss.FissBillClassificationForClinics;
import gov.cms.mpsm.rda.v1.fiss.FissBillFacilityType;
import gov.cms.mpsm.rda.v1.fiss.FissBillFrequency;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissPatientRelationshipCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissSourceOfAdmission;
import java.lang.String;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public class FissClaimTransformer {
  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissClaimStatus> FissClaim_currStatus_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissBillFacilityType> FissClaim_lobCd_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissBillClassification> FissClaim_servTypeCd_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissBillClassificationForClinics> FissClaim_servTypeCdForClinics_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissBillFrequency> FissClaim_freqCd_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissSourceOfAdmission> FissClaim_admSource_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissPayer, FissPayersCode> FissPayer_insuredPayer_payersId_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissPayer, FissBeneficiarySex> FissPayer_insuredPayer_insuredSex_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissPayer, FissPatientRelationshipCode> FissPayer_insuredPayer_insuredRelX12_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissPayer, FissPatientRelationshipCode> FissPayer_beneZPayer_beneRel_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissPayer, FissBeneficiarySex> FissPayer_beneZPayer_insuredSex_Extractor;

  private final EnumStringExtractor<gov.cms.mpsm.rda.v1.fiss.FissPayer, FissPatientRelationshipCode> FissPayer_beneZPayer_insuredRelX12_Extractor;

  private final Function<String, String> idHasher;

  private final ExternalTransformation<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissClaim> applyMbiCache;

  public FissClaimTransformer(Function<String, String> idHasher,
      EnumStringExtractor.Factory enumExtractorFactory,
      ExternalTransformation<gov.cms.mpsm.rda.v1.fiss.FissClaim, FissClaim> applyMbiCache) {
    this.idHasher = idHasher;
    FissClaim_currStatus_Extractor = enumExtractorFactory.createEnumStringExtractor(gov.cms.mpsm.rda.v1.fiss.FissClaim::hasCurrStatusEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::getCurrStatusEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::hasCurrStatusUnrecognized,gov.cms.mpsm.rda.v1.fiss.FissClaim::getCurrStatusUnrecognized,FissClaimStatus.UNRECOGNIZED,Set.of(),Set.of(EnumStringExtractor.Options.RejectUnrecognized));
    FissClaim_lobCd_Extractor = enumExtractorFactory.createEnumStringExtractor(gov.cms.mpsm.rda.v1.fiss.FissClaim::hasLobCdEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::getLobCdEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::hasLobCdUnrecognized,gov.cms.mpsm.rda.v1.fiss.FissClaim::getLobCdUnrecognized,FissBillFacilityType.UNRECOGNIZED,Set.of(),Set.of());
    FissClaim_servTypeCd_Extractor = enumExtractorFactory.createEnumStringExtractor(gov.cms.mpsm.rda.v1.fiss.FissClaim::hasServTypeCdEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::getServTypeCdEnum,ignored -> false,ignored -> null,FissBillClassification.UNRECOGNIZED,Set.of(),Set.of());
    FissClaim_servTypeCdForClinics_Extractor = enumExtractorFactory.createEnumStringExtractor(gov.cms.mpsm.rda.v1.fiss.FissClaim::hasServTypeCdForClinicsEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::getServTypeCdForClinicsEnum,ignored -> false,ignored -> null,FissBillClassificationForClinics.UNRECOGNIZED,Set.of(),Set.of());
    FissClaim_freqCd_Extractor = enumExtractorFactory.createEnumStringExtractor(gov.cms.mpsm.rda.v1.fiss.FissClaim::hasFreqCdEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::getFreqCdEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::hasFreqCdUnrecognized,gov.cms.mpsm.rda.v1.fiss.FissClaim::getFreqCdUnrecognized,FissBillFrequency.UNRECOGNIZED,Set.of(),Set.of());
    FissClaim_admSource_Extractor = enumExtractorFactory.createEnumStringExtractor(gov.cms.mpsm.rda.v1.fiss.FissClaim::hasAdmSourceEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::getAdmSourceEnum,gov.cms.mpsm.rda.v1.fiss.FissClaim::hasAdmSourceUnrecognized,gov.cms.mpsm.rda.v1.fiss.FissClaim::getAdmSourceUnrecognized,FissSourceOfAdmission.UNRECOGNIZED,Set.of(),Set.of());
    this.applyMbiCache = applyMbiCache;
    FissPayer_insuredPayer_payersId_Extractor = enumExtractorFactory.createEnumStringExtractor(message -> message.hasInsuredPayer() && message.getInsuredPayer().hasPayersIdEnum(),message -> message.getInsuredPayer().getPayersIdEnum(),message -> message.hasInsuredPayer() && message.getInsuredPayer().hasPayersIdUnrecognized(),message -> message.getInsuredPayer().getPayersIdUnrecognized(),FissPayersCode.UNRECOGNIZED,Set.of(),Set.of());
    FissPayer_insuredPayer_insuredSex_Extractor = enumExtractorFactory.createEnumStringExtractor(message -> message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredSexEnum(),message -> message.getInsuredPayer().getInsuredSexEnum(),message -> message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredSexUnrecognized(),message -> message.getInsuredPayer().getInsuredSexUnrecognized(),FissBeneficiarySex.UNRECOGNIZED,Set.of(),Set.of());
    FissPayer_insuredPayer_insuredRelX12_Extractor = enumExtractorFactory.createEnumStringExtractor(message -> message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredRelX12Enum(),message -> message.getInsuredPayer().getInsuredRelX12Enum(),message -> message.hasInsuredPayer() && message.getInsuredPayer().hasInsuredRelX12Unrecognized(),message -> message.getInsuredPayer().getInsuredRelX12Unrecognized(),FissPatientRelationshipCode.UNRECOGNIZED,Set.of(),Set.of());
    FissPayer_beneZPayer_beneRel_Extractor = enumExtractorFactory.createEnumStringExtractor(message -> message.hasBeneZPayer() && message.getBeneZPayer().hasBeneRelEnum(),message -> message.getBeneZPayer().getBeneRelEnum(),message -> message.hasBeneZPayer() && message.getBeneZPayer().hasBeneRelUnrecognized(),message -> message.getBeneZPayer().getBeneRelUnrecognized(),FissPatientRelationshipCode.UNRECOGNIZED,Set.of(),Set.of());
    FissPayer_beneZPayer_insuredSex_Extractor = enumExtractorFactory.createEnumStringExtractor(message -> message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredSexEnum(),message -> message.getBeneZPayer().getInsuredSexEnum(),message -> message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredSexUnrecognized(),message -> message.getBeneZPayer().getInsuredSexUnrecognized(),FissBeneficiarySex.UNRECOGNIZED,Set.of(),Set.of());
    FissPayer_beneZPayer_insuredRelX12_Extractor = enumExtractorFactory.createEnumStringExtractor(message -> message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredRelX12Enum(),message -> message.getBeneZPayer().getInsuredRelX12Enum(),message -> message.hasBeneZPayer() && message.getBeneZPayer().hasInsuredRelX12Unrecognized(),message -> message.getBeneZPayer().getInsuredRelX12Unrecognized(),FissPatientRelationshipCode.UNRECOGNIZED,Set.of(),Set.of());
  }

  public FissClaim transformMessage(gov.cms.mpsm.rda.v1.fiss.FissClaim from) {
    final DataTransformer transformer = new DataTransformer();;
    final FissClaim to = transformMessage(from, transformer, Instant.now());
    if (transformer.getErrors().size() > 0) {
      throw new DataTransformer.TransformationException("data transformation failed", transformer.getErrors());
    }
    return to;
  }

  public FissClaim transformMessage(gov.cms.mpsm.rda.v1.fiss.FissClaim from,
      DataTransformer transformer, Instant now) {
    final FissClaim to = transformMessageToFissClaim(from,transformer,now,"");
    transformMessageArraysToFissClaim(from,to,transformer,now,"");
    return to;
  }

  public FissClaim transformMessageToFissClaim(gov.cms.mpsm.rda.v1.fiss.FissClaim from,
      DataTransformer transformer, Instant now, String namePrefix) {
    final FissClaim to = new FissClaim();
    transformer.copyString(namePrefix + FissClaim.Fields.dcn, false, 1, 23, from.getDcn(), to::setDcn);
    transformer.copyLong(from.getSeq(), to::setSequenceNumber);
    transformer.copyEnumAsCharacter(namePrefix + FissClaim.Fields.currStatus, FissClaim_currStatus_Extractor.getEnumString(from), to::setCurrStatus);
    transformer.copyOptionalString(namePrefix + FissClaim.Fields.provStateCd, 1, 2, from::hasProvStateCd, from::getProvStateCd, to::setProvStateCd);
    transformer.copyOptionalString(namePrefix + FissClaim.Fields.medaProvId, 1, 13, from::hasMedaProvId, from::getMedaProvId, to::setMedaProvId);
    transformer.copyOptionalString(namePrefix + FissClaim.Fields.medaProv_6, 1, 6, from::hasMedaProv6, from::getMedaProv6, to::setMedaProv_6);
    transformer.copyOptionalAmount(namePrefix + FissClaim.Fields.totalChargeAmount, from::hasTotalChargeAmount, from::getTotalChargeAmount, to::setTotalChargeAmount);
    transformer.copyOptionalDate(namePrefix + FissClaim.Fields.receivedDate, from::hasRecdDtCymd, from::getRecdDtCymd, to::setReceivedDate);
    transformer.copyOptionalString(namePrefix + FissClaim.Fields.pracLocAddr1, 1, 2147483647, from::hasPracLocAddr1, from::getPracLocAddr1, to::setPracLocAddr1);
    transformer.copyEnumAsString(namePrefix + FissClaim.Fields.lobCd, true, 0, 1, FissClaim_lobCd_Extractor.getEnumString(from), to::setLobCd);
    if (from.hasServTypeCdEnum()) {
      to.setServTypeCdMapping(FissClaim.ServTypeCdMapping.Normal);
    }
    transformer.copyEnumAsString(namePrefix + FissClaim.Fields.servTypeCd, true, 0, 1, FissClaim_servTypeCd_Extractor.getEnumString(from), to::setServTypeCd);
    transformer.copyEnumAsString(namePrefix + FissClaim.Fields.servTypeCd, true, 0, 1, FissClaim_servTypeCdForClinics_Extractor.getEnumString(from), to::setServTypeCd);
    transformer.copyEnumAsString(namePrefix + FissClaim.Fields.paidDt, true, 0, 0, FissClaim_freqCd_Extractor.getEnumString(from), to::setPaidDt);
    transformer.copyEnumAsString(namePrefix + FissClaim.Fields.admSource, true, 0, 1, FissClaim_admSource_Extractor.getEnumString(from), to::setAdmSource);
    to.setLastUpdated(now);
    applyMbiCache.transformField(transformer, namePrefix, from, to);
    return to;
  }

  public void transformMessageArraysToFissClaim(gov.cms.mpsm.rda.v1.fiss.FissClaim from,
      FissClaim to, DataTransformer transformer, Instant now, String namePrefix) {
    for (short index = 0; index < from.getFissProcCodesCount(); ++index) {
      final String itemNamePrefix = namePrefix + "procCodes" + "-" + index + "-";
      final FissProcedureCode itemFrom = from.getFissProcCodes(index);
      final FissProcCode itemTo = transformMessageToFissProcCode(itemFrom,transformer,now,itemNamePrefix);
      itemTo.setDcn(from.getDcn());
      itemTo.setPriority(index);
      to.getProcCodes().add(itemTo);
    }
    for (short index = 0; index < from.getFissPayersCount(); ++index) {
      final String itemNamePrefix = namePrefix + "payers" + "-" + index + "-";
      final gov.cms.mpsm.rda.v1.fiss.FissPayer itemFrom = from.getFissPayers(index);
      final FissPayer itemTo = transformMessageToFissPayer(itemFrom,transformer,now,itemNamePrefix);
      itemTo.setPriority(index);
      itemTo.setParentClaim(to);
      to.getPayers().add(itemTo);
    }
  }

  public FissProcCode transformMessageToFissProcCode(FissProcedureCode from,
      DataTransformer transformer, Instant now, String namePrefix) {
    final FissProcCode to = new FissProcCode();
    transformer.copyString(namePrefix + FissProcCode.Fields.procCode, false, 1, 10, from.getProcCd(), to::setProcCode);
    transformer.copyOptionalString(namePrefix + FissProcCode.Fields.procFlag, 1, 4, from::hasProcFlag, from::getProcFlag, value -> to.setProcFlag(Optional.ofNullable(value)));
    transformer.copyOptionalDate(namePrefix + FissProcCode.Fields.procDate, from::hasProcDt, from::getProcDt, value -> to.setProcDate(Optional.ofNullable(value)));
    to.setLastUpdated(Optional.ofNullable(now));
    return to;
  }

  public FissPayer transformMessageToFissPayer(gov.cms.mpsm.rda.v1.fiss.FissPayer from,
      DataTransformer transformer, Instant now, String namePrefix) {
    final FissPayer to = new FissPayer();
    if (from.hasBeneZPayer()) {
      to.setPayerType(FissPayer.PayerType.BeneZ);
    }
    if (from.hasInsuredPayer()) {
      to.setPayerType(FissPayer.PayerType.Insured);
    }
    transformer.copyEnumAsString(namePrefix + FissPayer.Fields.payersId, true, 0, 1, FissPayer_insuredPayer_payersId_Extractor.getEnumString(from), to::setPayersId);
    transformer.copyOptionalAmount(namePrefix + FissPayer.Fields.estAmtDue, () -> from.hasInsuredPayer() && from.getInsuredPayer().hasEstAmtDue(), () -> from.getInsuredPayer().getEstAmtDue(), to::setEstAmtDue);
    transformer.copyOptionalString(namePrefix + FissPayer.Fields.insuredName, 1, 25, () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredName(), () -> from.getInsuredPayer().getInsuredName(), to::setInsuredName);
    transformer.copyEnumAsString(namePrefix + FissPayer.Fields.insuredSex, true, 0, 1, FissPayer_insuredPayer_insuredSex_Extractor.getEnumString(from), to::setInsuredSex);
    transformer.copyEnumAsString(namePrefix + FissPayer.Fields.insuredRelX12, true, 0, 2, FissPayer_insuredPayer_insuredRelX12_Extractor.getEnumString(from), to::setInsuredRelX12);
    transformer.copyOptionalDate(namePrefix + FissPayer.Fields.insuredDob, () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredDob(), () -> from.getInsuredPayer().getInsuredDob(), to::setInsuredDob);
    transformer.copyOptionalString(namePrefix + FissPayer.Fields.insuredDobText, 1, 9, () -> from.hasInsuredPayer() && from.getInsuredPayer().hasInsuredDobText(), () -> from.getInsuredPayer().getInsuredDobText(), to::setInsuredDobText);
    transformer.copyOptionalAmount(namePrefix + FissPayer.Fields.estAmtDue, () -> from.hasBeneZPayer() && from.getBeneZPayer().hasEstAmtDue(), () -> from.getBeneZPayer().getEstAmtDue(), to::setEstAmtDue);
    transformer.copyEnumAsString(namePrefix + FissPayer.Fields.beneRel, true, 0, 2, FissPayer_beneZPayer_beneRel_Extractor.getEnumString(from), to::setBeneRel);
    transformer.copyEnumAsString(namePrefix + FissPayer.Fields.insuredSex, true, 0, 1, FissPayer_beneZPayer_insuredSex_Extractor.getEnumString(from), to::setInsuredSex);
    transformer.copyEnumAsString(namePrefix + FissPayer.Fields.insuredRelX12, true, 0, 2, FissPayer_beneZPayer_insuredRelX12_Extractor.getEnumString(from), to::setInsuredRelX12);
    transformer.copyOptionalString(namePrefix + FissPayer.Fields.hashMe, 1, 64, from::hasMbi, ()-> idHasher.apply(from.getMbi()), to::setHashMe);
    to.setLastUpdated(now);
    return to;
  }
}
