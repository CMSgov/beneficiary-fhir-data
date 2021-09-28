package gov.cms.bfd.pipeline.bridge.etl;

import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Mcs;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import java.util.Map;
import java.util.Random;
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
    String npi = String.valueOf(new Random().nextInt(Integer.MAX_VALUE - 999999999) + 999999999);

    McsClaim.Builder claimBuilder =
        McsClaim.newBuilder()
            // Prefixed ICN numbers with '*' to designate synthetic data
            .setIdrClmHdIcn(String.format("*%014d", icnCounter.getAndIncrement()))
            .setIdrStatusCodeEnumValue(new Random().nextInt(29))
            .setIdrBillProvNpi(npi)
            .setIdrTotBilledAmt(data.get(Mcs.NCH_CARR_CLM_SBMTD_CHRG_AMT).orElse(""))
            .setIdrClaimMbi(mbiMap.get(beneId).getMbi());

    if (!mpnLookupMap.containsKey(npi)) {
      mpnLookupMap.putIfAbsent(npi, String.format("%06d", mpnCounter.getAndIncrement()));
    }

    claimBuilder.setIdrBillProvNum(mpnLookupMap.get(npi));

    claimBuilder.addMcsDetails(
        McsDetail.newBuilder()
            .setIdrProcCode(data.get(Mcs.HCPCS_CD).orElse(""))
            .setIdrDtlToDate(data.get(Mcs.CLM_THRU_DT).orElse(""))
            .build());

    for (int i = 1; i <= MAX_DIAGNOSIS_CODES; ++i) {
      final int INDEX = i;

      data.get(Mcs.ICD_DGNS_CD + i)
          .ifPresent(
              value ->
                  claimBuilder.addMcsDiagnosisCodes(
                      McsDiagnosisCode.newBuilder()
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
}
