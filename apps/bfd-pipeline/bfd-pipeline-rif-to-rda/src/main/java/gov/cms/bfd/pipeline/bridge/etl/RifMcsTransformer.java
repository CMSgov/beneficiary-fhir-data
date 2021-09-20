package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.pipeline.bridge.model.Carrier;
import gov.cms.mpsm.rda.v1.ClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;

public class RifMcsTransformer implements ETLJob.Transformer<String[], ClaimChange> {

  private static final AtomicLong icnCounter = new AtomicLong();
  private static final AtomicInteger mpnCounter = new AtomicInteger();

  private static final Map<String, String> mpnLookupMap = new ConcurrentHashMap<>();

  private final Map<String, Integer> headerIndexMap;
  private final Map<String, String> mbiMap;

  public RifMcsTransformer(Map<String, Integer> headerIndexMap, Map<String, String> mbiMap) {
    this.headerIndexMap = new ConcurrentHashMap<>(headerIndexMap);
    this.mbiMap = mbiMap;
  }

  @Override
  public ClaimChange transform(String[] rowData) {
    TransformHelper helper = new TransformHelper(headerIndexMap, rowData);

    String beneId = helper.get(Carrier.BENE_ID);
    String npi = String.valueOf(new Random().nextInt(Integer.MAX_VALUE - 999999999) + 999999999);

    McsClaim.Builder claimBuilder =
        McsClaim.newBuilder()
            .setIdrClmHdIcn(String.format("%015d", icnCounter.getAndIncrement()))
            .setIdrStatusCodeEnumValue(new Random().nextInt(29))
            .setIdrBillProvNpi(npi)
            .setIdrTotBilledAmt(helper.get(Carrier.NCH_CARR_CLM_SBMTD_CHRG_AMT))
            .setIdrClaimMbi(mbiMap.get(beneId));

    if (!mpnLookupMap.containsKey(npi)) {
      mpnLookupMap.putIfAbsent(npi, String.format("%06d", mpnCounter.getAndIncrement()));
    }

    claimBuilder.setIdrBillProvNum(mpnLookupMap.get(npi));

    claimBuilder.addMcsDetails(
        McsDetail.newBuilder()
            .setIdrProcCode(helper.get(Carrier.HCPCS_CD))
            .setIdrDtlToDate(helper.get(Carrier.CLM_THRU_DT))
            .build());

    int i = 1;

    while (i <= 12 && !helper.get(Carrier.ICD_DGNS_CD + i).isEmpty()) {
      claimBuilder.addMcsDiagnosisCodes(
          McsDiagnosisCode.newBuilder()
              .setIdrDiagCode(helper.get(Carrier.ICD_DGNS_CD + i))
              .setIdrDiagIcdTypeEnumValue(helper.getInt(Carrier.ICD_DGNS_VRSN_CD + i).orElse(-1))
              .build());

      ++i;
    }

    return ClaimChange.newBuilder()
        .setMcsClaim(claimBuilder)
        .setChangeType(ClaimChange.ChangeType.CHANGE_TYPE_UPDATE)
        .build();
  }

  @Data
  @VisibleForTesting
  static class TransformHelper {

    private final Map<String, Integer> headerIndexMap;
    private final String[] rawData;

    public String get(String rifIdentifier) {
      return rawData[headerIndexMap.get(rifIdentifier)];
    }

    public Optional<Integer> getInt(String rifIdentifier) {
      try {
        return Optional.of(Integer.parseInt(rawData[headerIndexMap.get(rifIdentifier)]));
      } catch (NumberFormatException e) {
        return Optional.empty();
      }
    }
  }
}
