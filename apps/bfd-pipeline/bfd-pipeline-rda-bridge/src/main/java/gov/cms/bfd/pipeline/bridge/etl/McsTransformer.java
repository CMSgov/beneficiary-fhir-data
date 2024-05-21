package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Mcs;
import gov.cms.bfd.pipeline.bridge.util.DataSampler;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.bfd.pipeline.bridge.util.WrappedMessage;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RecordSource;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaimType;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisIcdType;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;

/** Transforms data into RDA MCS claim change objects. */
@RequiredArgsConstructor
public class McsTransformer extends AbstractTransformer {

  /** Maps the mbi number to its beneficiary data. */
  private final Map<String, BeneficiaryData> mbiMap;

  /** Hardcoded DETAIL NDC that can't be extracted from RIF. */
  public static final String DEFAULT_HARDCODED_DTL_NDC = "00777310502";

  /** Hardcoded DETAIL NDC UNIT COUNT that can't be extracted from RIF. */
  public static final String DEFAULT_HARDCODED_NDC_UNIT_COUNT = "20";

  /** {@inheritDoc} */
  @Override
  public Optional<MessageOrBuilder> transform(
      WrappedMessage message,
      WrappedCounter sequenceNumber,
      Parser.Data<String> data,
      DataSampler<String> mbiSampler,
      int sampleId,
      String fileName) {
    McsClaimChange claimToReturn;

    int lineNumber = getLineNumber(data, Mcs.LINE_NUM);

    if (message.getMessage() instanceof McsClaimChange storedClaim) {
      // There is an existing claim from a previous run
      if (message.getLineNumber() == (lineNumber - 1)) {
        // If it's the next sequential line number, add to previous claim
        message.setMessage(addToExistingClaim(lineNumber, storedClaim, data));
        message.setLineNumber(lineNumber);
        claimToReturn = null;
      } else if (lineNumber == 1) {
        // If the line number is 1, it's a new claim, return the old, store the new
        claimToReturn = storedClaim;
        message.setLineNumber(1);
        message.setMessage(
            transformNewClaim(sequenceNumber, lineNumber, data, mbiSampler, sampleId));
      } else {
        // If it's not the next claim or a new one starting at 1, then something is wrong.
        throw new IllegalStateException(
            String.format(
                "Invalid row sequence, previous line number: %d, current line number: %d",
                message.getLineNumber(), lineNumber));
      }
    } else {
      // This is the first run, no claims have been created yet
      if (lineNumber != 1) {
        throw new IllegalStateException(
            String.format(
                "Invalid row sequence, expected: 1, current line number: %d", lineNumber));
      }

      message.setLineNumber(lineNumber);
      message.setMessage(transformNewClaim(sequenceNumber, lineNumber, data, mbiSampler, sampleId));
      claimToReturn = null;
    }

    return Optional.ofNullable(claimToReturn);
  }

  /**
   * Adds additional line items to an existing claim.
   *
   * @param lineNumber The lineNumber for the claim
   * @param mcsClaimChange The claim to add line items to
   * @param data The data to grab new line items from
   * @return The newly constructed claim with additional line items added
   */
  @VisibleForTesting
  McsClaimChange addToExistingClaim(
      int lineNumber, McsClaimChange mcsClaimChange, Parser.Data<String> data) {
    McsClaim.Builder claimBuilder = mcsClaimChange.getClaim().toBuilder();

    claimBuilder.addMcsDetails(buildDetails(lineNumber, data));

    return mcsClaimChange.toBuilder().setClaim(claimBuilder.build()).build();
  }

  /**
   * Creates a new claim from the given {@link Parser.Data}.
   *
   * @param sequenceNumber The sequence number of the current claim
   * @param lineNumber The line number of the current claim
   * @param data The {@link Parser.Data} to pull claim data for building the claim
   * @param mbiSampler The samples for the MBIs
   * @param sampleId The samples of IDs
   * @return A new claim built from parsing the given {@link Parser.Data}
   */
  McsClaimChange transformNewClaim(
      WrappedCounter sequenceNumber,
      int lineNumber,
      Parser.Data<String> data,
      DataSampler<String> mbiSampler,
      int sampleId) {
    String beneId = data.get(Mcs.BENE_ID).orElse("");
    String icn = ifNull(data.get(Mcs.CARR_CLM_CNTL_NUM).orElse(null), () -> convertIcn(data));

    mbiSampler.add(sampleId, mbiMap.get(beneId).getMbi());

    McsClaim.Builder claimBuilder =
        McsClaim.newBuilder()
            .setIdrClmHdIcn(icn)
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
    data.getFromType(Mcs.CLM_FROM_DT, Parser.Data.Type.DATE)
        .ifPresent(claimBuilder::setIdrHdrFromDos);
    data.getFromType(Mcs.CLM_THRU_DT, Parser.Data.Type.DATE)
        .ifPresent(claimBuilder::setIdrHdrToDos);
    data.get(Mcs.NCH_CARR_CLM_SBMTD_CHRG_AMT).ifPresent(claimBuilder::setIdrTotBilledAmt);
    data.get(Mcs.ORG_NPI_NUM).ifPresent(claimBuilder::setIdrBillProvNpi);

    claimBuilder.addMcsDetails(buildDetails(lineNumber, data));

    addDiagnosisCodes(claimBuilder, data, claimBuilder.getIdrClmHdIcn());

    return McsClaimChange.newBuilder()
        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
        .setSeq(sequenceNumber.inc())
        .setClaim(claimBuilder)
        .setIcn(icn)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setSource(
            RecordSource.newBuilder()
                .setPhase("P1")
                .setPhaseSeqNum(0)
                .setExtractDate(LocalDate.now().minusDays(2).toString())
                .setTransmissionTimestamp(Instant.now().minus(1, ChronoUnit.DAYS).toString())
                .build())
        .build();
  }

  /**
   * Fallback method for creating a claim identifier from the CLM_ID field.
   *
   * @param data The data to pull from for claim data
   * @return The generated claim identifier
   */
  @VisibleForTesting
  String convertIcn(Parser.Data<String> data) {
    String claimId =
        data.get(Mcs.CLM_ID)
            .orElseThrow(() -> new IllegalStateException("Claim did not contain a Claim ID"));
    return "-" + DigestUtils.sha256Hex(claimId).substring(0, 14);
  }

  /**
   * Adds diagnosis codes to the given claim, parsed from the given {@link Parser.Data}.
   *
   * @param claimBuilder The claim to add diagnosis codes to
   * @param data The {@link Parser.Data} to pull diagnosis codes from
   * @param icn The icn that is being set for the diagnosis codes
   */
  @VisibleForTesting
  void addDiagnosisCodes(McsClaim.Builder claimBuilder, Parser.Data<String> data, String icn) {
    for (int i = 0; i < Mcs.MAX_DIAGNOSIS_CODES; ++i) {
      // We can't use the loop index directly because value must be final in the lambda expression
      final int INDEX = i;

      data.get(Mcs.ICD_DGNS_CD.get(INDEX))
          .ifPresent(
              diagnosisCode ->
                  claimBuilder.addMcsDiagnosisCodes(
                      McsDiagnosisCode.newBuilder()
                          .setRdaPosition(claimBuilder.getMcsDiagnosisCodesCount() + 1)
                          .setIdrClmHdIcn(icn)
                          .setIdrDiagCode(diagnosisCode)
                          .setIdrDiagIcdTypeEnum(
                              data.get(Mcs.ICD_DGNS_VRSN_CD.get(INDEX))
                                  .map(this::mapVersionCode)
                                  .orElse(McsDiagnosisIcdType.UNRECOGNIZED))
                          .build()));
    }
  }

  /**
   * Maps the MCS raw string value to a {@link McsDiagnosisIcdType}.
   *
   * @param code The raw MCS string value
   * @return The converted {@link McsDiagnosisIcdType}
   */
  private McsDiagnosisIcdType mapVersionCode(String code) {
    return switch (code) {
      case "0" -> McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10;
      case "9" -> McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD9;
      default -> throw new IllegalStateException("Invalid diagnosis code type: '" + code + "'");
    };
  }

  /**
   * Builds a list of details (line items), parsed from the given {@link Parser.Data}.
   *
   * @param lineNumber The line number of the claim
   * @param data The {@link Parser.Data} to pull procedure codes from
   * @return The list of build {@link McsDetail}s
   */
  private McsDetail buildDetails(int lineNumber, Parser.Data<String> data) {
    McsDetail.Builder detailBuilder = McsDetail.newBuilder();

    data.get(Mcs.LINE_ICD_DGNS_CD).ifPresent(detailBuilder::setIdrDtlPrimaryDiagCode);
    data.get(Mcs.LINE_ICD_DGNS_VRSN_CD)
        .map(this::mapVersionCode)
        .ifPresent(detailBuilder::setIdrDtlDiagIcdTypeEnum);
    data.get(Mcs.HCPCS_CD).ifPresent(detailBuilder::setIdrProcCode);
    data.get(Mcs.HCPCS_1ST_MDFR_CD).ifPresent(detailBuilder::setIdrModOne);
    data.get(Mcs.HCPCS_2ND_MDFR_CD).ifPresent(detailBuilder::setIdrModTwo);
    data.getFromType(Mcs.LINE_1ST_EXPNS_DT, Parser.Data.Type.DATE)
        .ifPresent(detailBuilder::setIdrDtlFromDate);
    data.getFromType(Mcs.LINE_LAST_EXPNS_DT, Parser.Data.Type.DATE)
        .ifPresent(detailBuilder::setIdrDtlToDate);
    detailBuilder.setIdrDtlNdc(DEFAULT_HARDCODED_DTL_NDC);
    detailBuilder.setIdrDtlNdcUnitCount(DEFAULT_HARDCODED_NDC_UNIT_COUNT);
    detailBuilder.setIdrDtlNumber(lineNumber);

    return detailBuilder.build();
  }
}
