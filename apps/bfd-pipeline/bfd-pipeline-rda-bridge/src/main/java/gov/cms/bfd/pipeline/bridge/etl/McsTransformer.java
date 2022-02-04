package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Mcs;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.bfd.pipeline.bridge.util.WrappedMessage;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaimType;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

/** Transforms data into RDA MCS claim change objects. */
@RequiredArgsConstructor
public class McsTransformer extends AbstractTransformer {

  private static final int MAX_DIAGNOSIS_CODES = 12;

  private final Map<String, BeneficiaryData> mbiMap;

  /**
   * Transforms the given {@link Parser.Data} into RDA {@link McsClaimChange} data.
   *
   * @param data The parsed {@link Parser.Data} to transform into RDA {@link McsClaimChange} data.
   * @return The RDA {@link McsClaimChange} object generated from the given data.
   */
  @Override
  public Optional<MessageOrBuilder> transform(
      WrappedCounter sequenceNumber, Parser.Data<String> data, WrappedMessage message) {
    McsClaimChange claimToReturn;

    int lineNumber = getLineNumber(data, Mcs.LINE_NUM);

    if (message.getMessage() instanceof McsClaimChange) {
      // There is an existing claim from a previous run
      McsClaimChange storedClaim = (McsClaimChange) message.getMessage();

      if (message.getLineNumber() == (lineNumber - 1)) {
        // If it's the next sequential line number, add to previous claim
        message.setMessage(addToExistingClaim(storedClaim, data));
        message.setLineNumber(lineNumber);
        claimToReturn = null;
      } else if (lineNumber == 1) {
        // If the line number is 1, it's a new claim, return the old, store the new
        claimToReturn = storedClaim;
        message.setLineNumber(1);
        message.setMessage(transformNewClaim(sequenceNumber, data));
      } else {
        // If it's not the next claim or a new one starting at 1, then something is wrong.
        throw new IllegalStateException(
            String.format(
                "Invalid row sequence (entry %d), previous line number: %d, current line number: %d",
                data.getEntryNumber(), message.getLineNumber(), lineNumber));
      }
    } else {
      // This must be the first run, store a new claim
      message.setLineNumber(1);
      message.setMessage(transformNewClaim(sequenceNumber, data));
      claimToReturn = null;
    }

    return Optional.ofNullable(claimToReturn);
  }

  @VisibleForTesting
  McsClaimChange addToExistingClaim(McsClaimChange mcsClaimChange, Parser.Data<String> data) {
    McsClaim.Builder claimBuilder = mcsClaimChange.getClaim().toBuilder();

    // Not currently implemented for MCS claims, just store the same claim.

    return mcsClaimChange.toBuilder().setClaim(claimBuilder.build()).build();
  }

  McsClaimChange transformNewClaim(WrappedCounter sequenceNumber, Parser.Data<String> data) {
    String beneId = data.get(Mcs.BENE_ID).orElse("");

    McsClaim.Builder claimBuilder =
        McsClaim.newBuilder()
            .setIdrClmHdIcn(
                ifNull(data.get(Mcs.CARR_CLM_CNTRL_NUM).orElse(null), () -> convertIcn(data)))
            .setIdrClaimMbi(mbiMap.get(beneId).getMbi())
            // Not generated
            .setIdrBillProvEin("XX-XXXXXXX")
            .setIdrBillProvSpec("01")
            .setIdrBillProvType("20")
            .setIdrClaimReceiptDate("1970-01-01")
            .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL) // "3"
            .setIdrContrId("00000")
            .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A)
            .setIdrStatusDate("1970-01-01");

    consumeIfNotNull(
        mbiMap.get(beneId).getFirstName(),
        value -> claimBuilder.setIdrBeneFirstInit(value.substring(0, 1)));
    consumeIfNotNull(
        mbiMap.get(beneId).getLastName(),
        value -> claimBuilder.setIdrBeneLast16(String.format("%.6s", value)));
    consumeIfNotNull(
        mbiMap.get(beneId).getMidName(),
        value -> claimBuilder.setIdrBeneMidInit(value.substring(0, 1)));
    consumeIfNotNull(
        mbiMap.get(beneId).getGender(),
        value -> {
          // RIF mappings are  0 - unknown, 1 - male, 2 - female
          // RDA mappings are -1 - unrecognized, 0 - male, 1 - female
          int enumValue = Integer.parseInt(value);
          if (enumValue != 0) { // Skip RIF "unknown" values since they don't map to MCS RDA
            --enumValue; // Transform to RDA values
            enumValue = (enumValue == 0 || enumValue == 1) ? enumValue : -1;
            claimBuilder.setIdrBeneSexEnumValue(enumValue);
          }
        });
    data.getFromType(Mcs.CLM_FRM_DT, Parser.Data.Type.DATE)
        .ifPresent(claimBuilder::setIdrHdrFromDos);
    data.getFromType(Mcs.CLM_THRU_DT, Parser.Data.Type.DATE)
        .ifPresent(claimBuilder::setIdrHdrToDos);
    data.get(Mcs.NCH_CARR_CLM_SBMTD_CHRG_AMT).ifPresent(claimBuilder::setIdrTotBilledAmt);
    data.get(Mcs.ORG_NPI_NUM).ifPresent(claimBuilder::setIdrBillProvNpi);

    McsDetail.Builder detailBuilder = McsDetail.newBuilder();

    data.get(Mcs.LINE_ICD_DGNS_CD).ifPresent(detailBuilder::setIdrDtlPrimaryDiagCode);
    data.get(Mcs.HCPCS_CD).ifPresent(detailBuilder::setIdrProcCode);
    data.get(Mcs.HCPCS_1_MDFR_CD).ifPresent(detailBuilder::setIdrModOne);
    data.get(Mcs.HCPCS_2_MDFR_CD).ifPresent(detailBuilder::setIdrModTwo);
    data.getFromType(Mcs.LINE_1ST_EXPNS_DT, Parser.Data.Type.DATE)
        .ifPresent(detailBuilder::setIdrDtlFromDate);
    data.getFromType(Mcs.LINE_LAST_EXPNS_DT, Parser.Data.Type.DATE)
        .ifPresent(detailBuilder::setIdrDtlToDate);

    claimBuilder.addMcsDetails(detailBuilder.build());

    addDiagnosisCodes(claimBuilder, data, claimBuilder.getIdrClmHdIcn());

    return McsClaimChange.newBuilder()
        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
        .setSeq(sequenceNumber.inc())
        .setClaim(claimBuilder)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .build();
  }

  @VisibleForTesting
  String convertIcn(Parser.Data<String> data) {
    String claimId =
        data.get(Mcs.CLM_ID)
            .orElseThrow(() -> new IllegalStateException("Claim did not contain a Claim ID"));
    return "-" + DigestUtils.sha256Hex(claimId).substring(0, 14);
  }

  @VisibleForTesting
  void addDiagnosisCodes(McsClaim.Builder claimBuilder, Parser.Data<String> data, String icn) {
    for (int i = 1; i <= MAX_DIAGNOSIS_CODES; ++i) {
      final int INDEX = i;

      data.get(Mcs.ICD_DGNS_CD + i)
          .ifPresent(
              diagnosisCode ->
                  claimBuilder.addMcsDiagnosisCodes(
                      McsDiagnosisCode.newBuilder()
                          .setIdrClmHdIcn(icn)
                          .setIdrDiagCode(diagnosisCode)
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
  }
}
