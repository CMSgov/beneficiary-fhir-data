package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Fiss;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;

/** Transforms data into RDA FISS claim change objects. */
@RequiredArgsConstructor
public class FissTransformer extends AbstractTransformer {

  private static final int MAX_PROC_CODES = 25;
  private static final int MAX_DIAG_CODES = 25;

  private final Map<String, BeneficiaryData> mbiMap;

  /**
   * Transforms the given {@link Parser.Data} into RDA {@link FissClaimChange} data.
   *
   * @param data The parsed {@link Parser.Data} to transform into RDA {@link FissClaimChange} data.
   * @return The RDA {@link FissClaimChange} object generated from the given data.
   */
  @Override
  public MessageOrBuilder transform(WrappedCounter sequenceNumber, Parser.Data<String> data) {
    String beneId = data.get(Fiss.BENE_ID).orElse("");

    // Some break claims into multiple lines (rows).  Synthea isn't doing this, but just
    // to protect against it if it does happen, we'll ignore any row with CLM_LINE_NUM > 1
    if (isFirstLineNum(data)) {
      FissClaim.Builder claimBuilder =
          FissClaim.newBuilder()
              .setDcn(
                  ifNull(data.get(Fiss.FI_DOC_CLM_CNTRL_NUM).orElse(null), () -> convertDcn(data)))
              .setMbi(mbiMap.get(beneId).getMbi())
              .setHicNo(mbiMap.get(beneId).getHicNo())
              // Not generated
              .setCurrLoc1Unrecognized("?")
              .setCurrLoc2Unrecognized("?")
              .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_ROUTING)
              .setCurrTranDtCymd("1970-01-01")
              .setFedTaxNb("XX-XXXXXXX")
              .setRecdDtCymd("1970-01-01");

      // Build beneZ payer object
      FissBeneZPayer.Builder payerBuilder = FissBeneZPayer.newBuilder();
      consumeIfNotNull(
          mbiMap.get(beneId).getFirstName(),
          value -> payerBuilder.setBeneFirstName(String.format("%.10s", value)));
      consumeIfNotNull(
          mbiMap.get(beneId).getLastName(),
          value -> payerBuilder.setBeneLastName(String.format("%.15s", value)));
      consumeIfNotNull(
          mbiMap.get(beneId).getMidName(), s -> payerBuilder.setBeneMidInit(s.substring(0, 1)));
      consumeIf(
          data.get(mbiMap.get(beneId).getGender()).orElse(null),
          NumberUtils::isDigits,
          value -> {
            // RIF mappings are  0 - unknown, 1 - male, 2 - female
            // RDA mappings are -1 - unrecognized, 0 - female, 1 - male, 2 - unknown
            int enumValue = Integer.parseInt(value);
            enumValue = (enumValue >= 0 && enumValue <= 2) ? 2 - enumValue : -1;
            payerBuilder.setBeneSexEnumValue(enumValue);
          });
      consumeIfNotNull(mbiMap.get(beneId).getDob(), payerBuilder::setBeneDob);

      claimBuilder.addFissPayers(
          FissPayer.newBuilder().setBeneZPayer(payerBuilder.build()).build());

      data.get(Fiss.ADMTG_DGNS_CD).ifPresent(claimBuilder::setAdmDiagCode);
      consumeIf(
          data.get(Fiss.CLM_FREQ_CD).orElse(null),
          NumberUtils::isDigits,
          value -> claimBuilder.setFreqCdEnumValue(Integer.parseInt(value)));
      data.getFromType(Fiss.CLM_FRM_DT, Parser.Data.Type.DATE)
          .ifPresent(claimBuilder::setStmtCovFromCymd);
      consumeIf(
          data.get(Fiss.CLM_SRVC_CLSFCTN_TYPE_CD).orElse(null),
          NumberUtils::isDigits,
          value -> claimBuilder.setServTypeCdEnumValue(Integer.parseInt(value)));
      data.getFromType(Fiss.CLM_THRU_DT, Parser.Data.Type.DATE)
          .ifPresent(claimBuilder::setStmtCovToCymd);
      data.get(Fiss.CLM_TOT_CHRG_AMT).ifPresent(claimBuilder::setTotalChargeAmount);
      consumeIf(
          data.get(Fiss.CLM_FAC_TYPE_CD).orElse(null),
          NumberUtils::isDigits,
          value -> claimBuilder.setLobCdEnumValue(Integer.parseInt(value)));
      data.get(Fiss.ORG_NPI_NUM).ifPresent(claimBuilder::setNpiNumber);
      data.get(Fiss.PRNCPAL_DGNS_CD).ifPresent(claimBuilder::setPrincipleDiag);
      data.get(Fiss.PRVDR_NUM)
          .ifPresent(value -> claimBuilder.setMedaProv6(String.format("%.6s", value)));

      addDiagCodes(claimBuilder, data);
      addProcCodes(claimBuilder, data);

      return FissClaimChange.newBuilder()
          .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
          .setSeq(sequenceNumber.inc())
          .setClaim(claimBuilder.build())
          .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
          .build();
    }

    // We're skipping any entry with CLM_LIN_NUM > 1
    return null;
  }

  @VisibleForTesting
  boolean isFirstLineNum(Parser.Data<String> data) {
    Optional<String> lineNum = data.get(Fiss.CLM_LINE_NUM);

    return lineNum.isEmpty() || lineNum.get().equals("1");
  }

  @VisibleForTesting
  String convertDcn(Parser.Data<String> data) {
    String claimId =
        data.get(Fiss.CLM_ID)
            .orElseThrow(() -> new IllegalStateException("Claim did not contain a Claim ID"));
    return "-" + DigestUtils.sha256Hex(claimId).substring(0, 22);
  }

  @VisibleForTesting
  void addDiagCodes(FissClaim.Builder claimBuilder, Parser.Data<String> data) {
    for (int i = 1; i <= MAX_DIAG_CODES; ++i) {
      final int INDEX = i;

      // HHA and Hospice do not include procedure codes
      data.get(Fiss.ICD_DGNS_CD + i)
          .ifPresent(
              value -> {
                FissDiagnosisCode.Builder diagBuilder =
                    FissDiagnosisCode.newBuilder().setDiagCd2(value);

                consumeIf(
                    data.get(Fiss.CLM_POA_IND_SW + INDEX).orElse(null),
                    NumberUtils::isDigits,
                    poa -> diagBuilder.setDiagPoaIndEnumValue(Integer.parseInt(poa)));

                claimBuilder.addFissDiagCodes(diagBuilder.build());
              });
    }
  }

  @VisibleForTesting
  void addProcCodes(FissClaim.Builder claimBuilder, Parser.Data<String> data) {
    for (int i = 1; i <= MAX_PROC_CODES; ++i) {
      final int INDEX = i;

      // HHA and Hospice do not include procedure codes
      data.get(Fiss.ICD_PRCDR_CD + i)
          .ifPresent(
              value ->
                  claimBuilder.addFissProcCodes(
                      FissProcedureCode.newBuilder()
                          .setProcCd(data.get(Fiss.ICD_PRCDR_CD + INDEX).orElse(""))
                          .setProcDt(
                              data.getFromType(Fiss.PRCDR_DT + INDEX, Parser.Data.Type.DATE)
                                  .orElse(""))
                          .build()));
    }
  }
}
