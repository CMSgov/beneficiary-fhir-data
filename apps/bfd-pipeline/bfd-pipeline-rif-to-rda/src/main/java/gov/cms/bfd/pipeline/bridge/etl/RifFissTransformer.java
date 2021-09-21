package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.model.Inpatient;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RifFissTransformer implements ETLJob.Transformer<String[], MessageOrBuilder> {

  private static final AtomicLong dcnCounter = new AtomicLong();
  private static final AtomicInteger mpnCounter = new AtomicInteger();

  private static final Map<String, String> mpnLookupMap = new ConcurrentHashMap<>();

  private final Map<String, Integer> headerIndexMap;
  private final Map<String, String> mbiMap;

  public RifFissTransformer(Map<String, Integer> headerIndexMap, Map<String, String> mbiMap) {
    this.headerIndexMap = headerIndexMap;
    this.mbiMap = mbiMap;
  }

  @Override
  public MessageOrBuilder transform(String[] rowData) {
    TransformHelper helper = new TransformHelper(headerIndexMap, rowData);

    String beneId = helper.get(Inpatient.BENE_ID);
    String npi = helper.get(Inpatient.ORG_NPI_NUM);

    FissClaim.Builder claimBuilder =
        FissClaim.newBuilder()
            .setDcn(String.format("%010d", dcnCounter.getAndIncrement()))
            .setMbi(mbiMap.get(beneId))
            .setCurrLoc1EnumValue(0)
            .setCurrLoc2EnumValue(0)
            .setCurrStatusEnumValue(new Random().nextInt(9))
            .setNpiNumber(npi)
            .setTotalChargeAmount(helper.get(Inpatient.CLM_TOT_CHRG_AMT))
            .setPrincipleDiag(helper.get(Inpatient.PRNCPAL_DGNS_CD))
            .setAdmDiagCode(helper.get(Inpatient.ADMTG_DGNS_CD));

    if (!mpnLookupMap.containsKey(npi)) {
      mpnLookupMap.putIfAbsent(npi, String.format("%06d", mpnCounter.getAndIncrement()));
    }

    claimBuilder.setMedaProvId(mpnLookupMap.get(npi));

    int i = 1;

    while (i <= 25 && !helper.get(Inpatient.ICD_PRCDR_CD + i).isEmpty()) {
      claimBuilder.addFissProcCodes(
          FissProcedureCode.newBuilder()
              .setProcCd(helper.get(Inpatient.ICD_PRCDR_CD + i))
              .setProcDt(helper.get(Inpatient.PRCDR_DT + i))
              .build());

      ++i;
    }

    return FissClaimChange.newBuilder()
        .setClaim(claimBuilder.build())
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .build();
  }

  @Data
  @VisibleForTesting
  static class TransformHelper {

    private final Map<String, Integer> headerIndexMap;
    private final String[] rawData;

    public String get(String rifIdentifier) {
      if (headerIndexMap.containsKey(rifIdentifier)) {
        return rawData[headerIndexMap.get(rifIdentifier)];
      }

      return "NOT_FOUND";
    }
  }
}
