package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Mcs;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;

/** Transforms data into MCS FISS claim change objects. */
@RequiredArgsConstructor
public class McsTransformer extends AbstractTransformer {

  private static final AtomicLong icnCounter = new AtomicLong();

  private static final Map<String, String> mpnLookupMap = new ConcurrentHashMap<>();
  private static final int MAX_DIAGNOSIS_CODES = 12;

  private final Map<String, BeneficiaryData> mbiMap;

  /**
   * Transforms the given {@link Parser.Data} into RDA {@link McsClaimChange} data.
   *
   * @param data The parsed {@link Parser.Data} to transform into RDA {@link McsClaimChange} data.
   * @return The RDA {@link McsClaimChange} object generated from the given data.
   */
  @Override
  public MessageOrBuilder transform(Parser.Data<String> data) {
    String beneId = data.get(Mcs.BENE_ID).orElse("");
    String icn = String.format("*%014d", icnCounter.getAndIncrement());

    // Carrier claims break claims into multiple lines (rows).  Synthea isn't doing this, but just
    // to protect against it
    // if it does happen, we'll ignore any row with LINE_NUM > 1
    if (isFirstLineNum(data)) {
      McsClaim.Builder claimBuilder =
          McsClaim.newBuilder()
              // Prefixed ICN numbers with '*' to designate synthetic data
              .setIdrClmHdIcn(icn)
              .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A) // Not generated
              .setIdrBillProvNpi("") // Not generated
              .setIdrTotBilledAmt(data.get(Mcs.NCH_CARR_CLM_SBMTD_CHRG_AMT).orElse(""))
              .setIdrClaimMbi(mbiMap.get(beneId).getMbi())
              .setIdrContrId("") // Not generated
              .setIdrClaimTypeUnrecognized("?") // Not generated
              .setIdrBillProvNum(""); // Not generated

      claimBuilder.addMcsDetails(
          McsDetail.newBuilder()
              .setIdrProcCode(data.get(Mcs.HCPCS_CD).orElse("00000"))
              .setIdrDtlToDate(convertRifDate(data.get(Mcs.CLM_THRU_DT).orElse("")))
              .build());

      for (int i = 1; i <= MAX_DIAGNOSIS_CODES; ++i) {
        final int INDEX = i;

        data.get(Mcs.ICD_DGNS_CD + i)
            .ifPresent(
                value ->
                    claimBuilder.addMcsDiagnosisCodes(
                        McsDiagnosisCode.newBuilder()
                            .setIdrClmHdIcn(icn)
                            .setIdrDiagCode(data.get(Mcs.ICD_DGNS_CD + INDEX).orElse(""))
                            .setIdrDiagIcdTypeEnumValue(
                                data.get(Mcs.ICD_DGNS_VRSN_CD + INDEX)
                                    .map(
                                        dxVersionCode -> {
                                          try {
                                            return Integer.parseInt(dxVersionCode);
                                          } catch (NumberFormatException e) {
                                            return -1;
                                          }
                                        })
                                    .orElse(-1))
                            .build()));
      }

      return McsClaimChange.newBuilder()
          .setClaim(claimBuilder)
          .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
          .build();
    }

    // We're skipping any entry with LIN_NUM > 1
    return null;
  }

  public static void setStartingICN(long start) {
    icnCounter.set(start);
  }

  @VisibleForTesting
  boolean isFirstLineNum(Parser.Data<String> data) {
    Optional<String> lineNum = data.get("LINE_NUM");

    return !lineNum.isPresent() || lineNum.get().equals("1");
  }
}
