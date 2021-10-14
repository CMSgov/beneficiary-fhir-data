package gov.cms.bfd.pipeline.bridge.etl;

import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Fiss;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;

/** Transforms data into RDA FISS claim change objects. */
@RequiredArgsConstructor
public class FissTransformer extends AbstractTransformer {

  private static final AtomicLong dcnCounter = new AtomicLong();

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
    String npi = data.get(Fiss.ORG_NPI_NUM).orElse("");

    FissClaim.Builder claimBuilder =
        FissClaim.newBuilder()
            // Prefixed DCN numbers with '*' to designate synthetic data
            .setDcn(String.format("*%022d", dcnCounter.getAndIncrement()))
            .setMbi(mbiMap.get(beneId).getMbi())
            .setHicNo(mbiMap.get(beneId).getHicNo())
            .setCurrLoc1Unrecognized("?") // Not generated
            .setCurrLoc2Unrecognized("?") // Not generated
            .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_ROUTING) // Not generated
            .setNpiNumber(npi)
            .setTotalChargeAmount(data.get(Fiss.CLM_TOT_CHRG_AMT).orElse(""))
            .setPrincipleDiag(data.get(Fiss.PRNCPAL_DGNS_CD).orElse(""));

    data.get(Fiss.ADMTG_DGNS_CD).ifPresent(claimBuilder::setAdmDiagCode);

    claimBuilder.setMedaProvId("");

    for (int i = 1; i <= MAX_PROC_CODES; ++i) {
      final int INDEX = i;

      // HHA and Hospice do not include procedure codes
      data.get(Fiss.ICD_PRCDR_CD + i)
          .ifPresent(
              value ->
                  claimBuilder.addFissProcCodes(
                      FissProcedureCode.newBuilder()
                          .setProcCd(data.get(Fiss.ICD_PRCDR_CD + INDEX).orElse(""))
                          .setProcDt(convertRifDate(data.get(Fiss.PRCDR_DT + INDEX).orElse("")))
                          .build()));
    }

    return FissClaimChange.newBuilder()
        .setClaim(claimBuilder.build())
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .build();
  }

  public static void setStartingDCN(long start) {
    dcnCounter.set(start);
  }
}
