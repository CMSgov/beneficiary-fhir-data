package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Fiss;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

/** Transforms data into RDA FISS claim change objects. */
@RequiredArgsConstructor
public class FissTransformer implements AbstractTransformer {

  private static final int MAX_PROC_CODES = 25;

  private final Map<String, BeneficiaryData> mbiMap;

  /**
   * Transforms the given {@link Parser.Data} into RDA {@link FissClaimChange} data.
   *
   * @param data The parsed {@link Parser.Data} to transform into RDA {@link FissClaimChange} data.
   * @return The RDA {@link FissClaimChange} object generated from the given data.
   */
  @Override
  public MessageOrBuilder transform(Parser.Data<String> data) {
    String beneId = data.get(Fiss.BENE_ID).orElse("");

    // Some break claims into multiple lines (rows).  Synthea isn't doing this, but just
    // to protect against it if it does happen, we'll ignore any row with CLM_LINE_NUM > 1
    if (isFirstLineNum(data)) {
      FissClaim.Builder claimBuilder =
          FissClaim.newBuilder()
              // Prefixed DCN numbers with '*' to designate synthetic data
              .setDcn(convertDcn(data))
              .setMbi(mbiMap.get(beneId).getMbi())
              .setHicNo(mbiMap.get(beneId).getHicNo())
              .setCurrLoc1Unrecognized("?") // Not generated
              .setCurrLoc2Unrecognized("?") // Not generated
              .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_ROUTING) // Not generated
              .setNpiNumber(data.get(Fiss.ORG_NPI_NUM).orElse(""))
              .setTotalChargeAmount(data.get(Fiss.CLM_TOT_CHRG_AMT).orElse(""));

      data.get(Fiss.PRNCPAL_DGNS_CD).ifPresent(claimBuilder::setPrincipleDiag);
      data.get(Fiss.ADMTG_DGNS_CD).ifPresent(claimBuilder::setAdmDiagCode);

      claimBuilder.setMedaProvId("0000000000000");

      addProcCodes(claimBuilder, data);

      return FissClaimChange.newBuilder()
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

    return !lineNum.isPresent() || lineNum.get().equals("1");
  }

  @VisibleForTesting
  String convertDcn(Parser.Data<String> data) {
    String claimId =
        data.get(Fiss.CLM_ID)
            .orElseThrow(() -> new IllegalStateException("Claim did not contain a Claim ID"));
    return "-" + DigestUtils.sha256Hex(claimId).substring(0, 22);
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
