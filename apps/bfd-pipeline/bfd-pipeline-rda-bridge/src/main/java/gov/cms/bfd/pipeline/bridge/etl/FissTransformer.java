package gov.cms.bfd.pipeline.bridge.etl;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Timestamp;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.model.Fiss;
import gov.cms.bfd.pipeline.bridge.util.DataSampler;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.bfd.pipeline.bridge.util.WrappedMessage;
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
  public Optional<MessageOrBuilder> transform(
      WrappedMessage message,
      WrappedCounter sequenceNumber,
      Parser.Data<String> data,
      DataSampler<String> mbiSampler,
      int sampleId) {
    FissClaimChange claimToReturn;

    int lineNumber = getLineNumber(data, Fiss.CLM_LINE_NUM);

    if (message.getMessage() instanceof FissClaimChange) {
      // There is an existing claim from a previous run
      FissClaimChange storedClaim = (FissClaimChange) message.getMessage();

      if (message.getLineNumber() == (lineNumber - 1)) {
        // If it's the next sequential line number, add to previous claim
        message.setMessage(addToExistingClaim(storedClaim, data));
        message.setLineNumber(lineNumber);
        claimToReturn = null;
      } else if (lineNumber == 1) {
        // If the line number is 1, it's a new claim, return the old, store the new
        claimToReturn = storedClaim;
        message.setLineNumber(1);
        message.setMessage(transformNewClaim(sequenceNumber, data, mbiSampler, sampleId));
      } else {
        // If it's not the next claim or a new one starting at 1, then something is wrong.
        throw new IllegalStateException(
            String.format(
                "Invalid row sequence, previous line number: %d, current line number: %d",
                message.getLineNumber(), lineNumber));
      }
    } else {
      // This must be the first run, store a new claim
      message.setLineNumber(1);
      message.setMessage(transformNewClaim(sequenceNumber, data, mbiSampler, sampleId));
      claimToReturn = null;
    }

    return Optional.ofNullable(claimToReturn);
  }

  /**
   * Adds additional line items to an existing claim.
   *
   * <p>Not currently implemented for FISS claims.
   *
   * @param fissClaimChange The claim to add line items to.
   * @param data The data to grab new line items from.
   * @return The newly constructed claim with additional line items added.
   */
  @VisibleForTesting
  FissClaimChange addToExistingClaim(FissClaimChange fissClaimChange, Parser.Data<String> data) {
    FissClaim.Builder claimBuilder = fissClaimChange.getClaim().toBuilder();

    // Not currently implemented for FISS claims, just store the same claim

    return fissClaimChange.toBuilder().setClaim(claimBuilder.build()).build();
  }

  /**
   * Creates a new claim from the given {@link Parser.Data}.
   *
   * @param sequenceNumber The sequence number of the current claim.
   * @param data The {@link Parser.Data} to pull claim data for building the claim.
   * @return A new claim built from parsing the given {@link Parser.Data}.
   */
  @VisibleForTesting
  FissClaimChange transformNewClaim(
      WrappedCounter sequenceNumber,
      Parser.Data<String> data,
      DataSampler<String> mbiSampler,
      int sampleId) {
    String beneId = data.get(Fiss.BENE_ID).orElse("");

    mbiSampler.add(sampleId, mbiMap.get(beneId).getMbi());

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

    claimBuilder.addFissPayers(FissPayer.newBuilder().setBeneZPayer(payerBuilder.build()).build());

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

  /**
   * Fallback method for creating a claim identifier from the CLM_ID field.
   *
   * @param data The data to pull from for claim data.
   * @return The generated claim identifier.
   */
  @VisibleForTesting
  String convertDcn(Parser.Data<String> data) {
    String claimId =
        data.get(Fiss.CLM_ID)
            .orElseThrow(() -> new IllegalStateException("Claim did not contain a Claim ID"));
    return "-" + DigestUtils.sha256Hex(claimId).substring(0, 22);
  }

  /**
   * Adds diagnosis codes to the given claim, parsed from the given {@link Parser.Data}.
   *
   * @param claimBuilder The claim to add diagnosis codes to.
   * @param data The {@link Parser.Data} to pull diagnosis codes from.
   */
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

  /**
   * Adds procedure codes to the given claim, parsed from the given {@link Parser.Data}.
   *
   * @param claimBuilder The claim to add procedure codes to.
   * @param data The {@link Parser.Data} to pull procedure codes from.
   */
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
