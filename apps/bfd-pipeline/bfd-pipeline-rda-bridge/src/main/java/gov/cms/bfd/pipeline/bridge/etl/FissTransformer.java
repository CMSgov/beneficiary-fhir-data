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
import gov.cms.mpsm.rda.v1.RecordSource;
import gov.cms.mpsm.rda.v1.fiss.FissAdmTypeCode;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissClaimTypeIndicator;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissNdcQtyQual;
import gov.cms.mpsm.rda.v1.fiss.FissNonBillRevCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissPayersCode;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
import gov.cms.mpsm.rda.v1.fiss.FissRevenueLine;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.math.NumberUtils;

/** Transforms data into RDA FISS claim change objects. */
@RequiredArgsConstructor
public class FissTransformer extends AbstractTransformer {

  /** Hardcoded date values that can't be extracted from RIF */
  public static final String DEFAULT_HARDCODED_DATE = "1970-01-01";

  /** Holds the map of beneficiary data from beneficiary_history, keyed by the bene_id. */
  private final Map<String, BeneficiaryData> mbiMap;

  /** Constant value used within the code. */
  private static final String MEDICARE = "MEDICARE";

  /** Hardcoded NDC quantity qualifier unit that can't be extracted from RIF. */
  public static final FissNdcQtyQual DEFAULT_HARDCODED_NDC_QTY_QUAL =
      FissNdcQtyQual.NDC_QTY_QUAL_ML;

  /** Hardcoded NDC quantity that can't be extracted from RIF. */
  public static final String DEFAULT_HARDCODED_NDC_QTY = "1.5";

  /** Hardcoded NDC that can't be extracted from RIF. */
  public static final String DEFAULT_HARDCODED_NDC = "00777310502";

  /** {@inheritDoc} */
  @Override
  public Optional<MessageOrBuilder> transform(
      WrappedMessage message,
      WrappedCounter sequenceNumber,
      Parser.Data<String> data,
      DataSampler<String> mbiSampler,
      int sampleId,
      String fileName) {
    FissClaimChange claimToReturn;

    int lineNumber = getLineNumber(data, Fiss.CLM_LINE_NUM);

    if (message.getMessage() instanceof FissClaimChange storedClaim) {
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
            transformNewClaim(sequenceNumber, lineNumber, data, mbiSampler, sampleId, fileName));
      } else {
        // If it's not the next claim or a new one starting at 1, then something is wrong.
        throw new IllegalStateException(
            String.format(
                "Invalid row sequence, previous line number: %d, current line number: %d",
                message.getLineNumber(), lineNumber));
      }
    } else {
      // This would be the first run of a claim, create a new claim and store it
      if (lineNumber != 1) {
        throw new IllegalStateException(
            String.format(
                "Invalid row sequence, expected: 1, current line number: %d", lineNumber));
      }

      message.setLineNumber(lineNumber);
      message.setMessage(
          transformNewClaim(sequenceNumber, lineNumber, data, mbiSampler, sampleId, fileName));
      claimToReturn = null;
    }

    return Optional.ofNullable(claimToReturn);
  }

  /**
   * Adds additional line items to an existing claim.
   *
   * <p>Not currently implemented for FISS claims.
   *
   * @param lineNumber The lineNumber for the claim
   * @param fissClaimChange The claim to add line items to
   * @param data The data to grab new line items from
   * @return The newly constructed claim with additional line items added
   */
  @VisibleForTesting
  FissClaimChange addToExistingClaim(
      int lineNumber, FissClaimChange fissClaimChange, Parser.Data<String> data) {
    FissClaim.Builder claimBuilder = fissClaimChange.getClaim().toBuilder();

    claimBuilder.addFissRevenueLines(buildRevenueLines(lineNumber, data));

    return fissClaimChange.toBuilder().setClaim(claimBuilder.build()).build();
  }

  /**
   * Creates a new claim from the given {@link Parser.Data}.
   *
   * @param sequenceNumber The sequence number of the current claim
   * @param lineNumber The lineNumber for the claim
   * @param data The {@link Parser.Data} to pull claim data for building the claim
   * @param mbiSampler The {@link DataSampler} of the MBIs
   * @param sampleId The sample of ids
   * @param fileName The name of the file the data was extracted from
   * @return A new claim built from parsing the given {@link Parser.Data}
   */
  @VisibleForTesting
  FissClaimChange transformNewClaim(
      WrappedCounter sequenceNumber,
      int lineNumber,
      Parser.Data<String> data,
      DataSampler<String> mbiSampler,
      int sampleId,
      String fileName) {
    String beneId = data.get(Fiss.BENE_ID).orElse("");

    mbiSampler.add(sampleId, mbiMap.get(beneId).getMbi());

    final String claimId = getClaimId(data);
    final String dcn =
        ifNull(data.get(Fiss.FI_DOC_CLM_CNTL_NUM).orElse(null), () -> convertDcn(claimId));

    String admTypCd = data.get(Fiss.ADM_TYP_CD).orElse("");

    FissClaim.Builder claimBuilder =
        FissClaim.newBuilder()
            .setRdaClaimKey(claimId)
            .setDcn(dcn)
            .setMbi(mbiMap.get(beneId).getMbi())
            .setHicNo(mbiMap.get(beneId).getHicNo())
            .setClmTypIndEnum(getClaimTypeEnum(fileName))
            // Not generated
            .setCurrLoc1Unrecognized("?")
            .setCurrLoc2Unrecognized("?")
            .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_ROUTING)
            .setCurrTranDtCymd(DEFAULT_HARDCODED_DATE)
            .setFedTaxNb("XX-XXXXXXX")
            .setIntermediaryNb("?")
            .setRecdDtCymd(DEFAULT_HARDCODED_DATE);

    // Build beneZ payer object
    FissBeneZPayer.Builder payerBuilder = FissBeneZPayer.newBuilder().setRdaPosition(1);
    consumeIfNotNull(
        mbiMap.get(beneId).getFirstName(),
        value -> payerBuilder.setBeneFirstName(String.format("%.10s", value)));
    consumeIfNotNull(
        mbiMap.get(beneId).getLastName(),
        value -> payerBuilder.setBeneLastName(String.format("%.15s", value)));
    consumeIfNotNull(
        mbiMap.get(beneId).getMidName(), s -> payerBuilder.setBeneMidInit(s.substring(0, 1)));
    consumeIf(
        mbiMap.get(beneId).getGender(),
        NumberUtils::isDigits,
        value -> {
          // RIF mappings are  0 - unknown, 1 - male, 2 - female
          // RDA mappings are -1 - unrecognized, 0 - female, 1 - male, 2 - unknown
          int enumValue = Integer.parseInt(value);
          enumValue = (enumValue >= 0 && enumValue <= 2) ? 2 - enumValue : -1;
          payerBuilder.setBeneSexEnumValue(enumValue);
        });
    consumeIfNotNull(mbiMap.get(beneId).getDob(), payerBuilder::setBeneDob);

    payerBuilder.setPayersIdEnum(FissPayersCode.PAYERS_CODE_MEDICARE); // 'Z'
    payerBuilder.setPayersName(MEDICARE);

    claimBuilder.addFissPayers(FissPayer.newBuilder().setBeneZPayer(payerBuilder.build()).build());

    data.get(Fiss.CLM_DRG_CD).ifPresent(claimBuilder::setDrgCd);
    data.get(Fiss.ADMTG_DGNS_CD).ifPresent(claimBuilder::setAdmDiagCode);
    consumeIf(
        data.get(Fiss.CLM_FREQ_CD).orElse(null),
        NumberUtils::isDigits,
        value -> claimBuilder.setFreqCdEnumValue(Integer.parseInt(value)));
    data.getFromType(Fiss.CLM_FROM_DT, Parser.Data.Type.DATE)
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
    data.get(Fiss.ADM_TYP_CD)
        .ifPresent(value -> claimBuilder.setAdmTypCdEnum(getAdmTypeCodeEnum(value)));

    addDiagCodes(claimBuilder, data);
    addProcCodes(claimBuilder, data);

    claimBuilder.addFissRevenueLines(buildRevenueLines(lineNumber, data));

    FissClaim claim = claimBuilder.build();
    return FissClaimChange.newBuilder()
        .setTimestamp(Timestamp.newBuilder().setSeconds(Instant.now().getEpochSecond()).build())
        .setSeq(sequenceNumber.inc())
        .setClaim(claim)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setDcn(dcn)
        .setRdaClaimKey(claim.getRdaClaimKey())
        .setIntermediaryNb(claim.getIntermediaryNb())
        .setSource(
            RecordSource.newBuilder()
                // Hardcoding values for test data, this data is only used in production for
                // analysis
                .setPhase("P1")
                .setPhaseSeqNum(0)
                .setExtractDate(LocalDate.now().minusDays(2).toString())
                .setTransmissionTimestamp(Instant.now().minus(1, ChronoUnit.DAYS).toString())
                .build())
        .build();
  }

  /**
   * Determines the {@link FissClaimTypeIndicator} to use based on the provided fileName of the file
   * the data was extracted from.
   *
   * @param fileName The file name of the file the data was extracted from.
   * @return The corresponding {@link FissClaimTypeIndicator} for the extracted file
   */
  private FissClaimTypeIndicator getClaimTypeEnum(String fileName) {
    return switch (fileName.trim()) {
      case "inpatient" -> FissClaimTypeIndicator.CLAIM_TYPE_INPATIENT;
      case "outpatient" -> FissClaimTypeIndicator.CLAIM_TYPE_OUTPATIENT;
      case "home", "hha" ->
      // Home and HHA are synonymous in this context
      FissClaimTypeIndicator.CLAIM_TYPE_HOME_HEALTH;
      case "hospice" -> FissClaimTypeIndicator.CLAIM_TYPE_HOSPICE;
      case "snf" -> FissClaimTypeIndicator.CLAIM_TYPE_SNF;
      default -> FissClaimTypeIndicator.UNRECOGNIZED;
    };
  }

  /**
   * Determines the {@link gov.cms.mpsm.rda.v1.fiss.FissAdmTypeCode} from the claim inpatient
   * admission type code.
   *
   * @param clmIpAdmsnTypeCd The Claim Inpatient Admission Type Code
   * @return The corresponding {@link FissAdmTypeCode}
   */
  private FissAdmTypeCode getAdmTypeCodeEnum(String clmIpAdmsnTypeCd) {
    return switch (clmIpAdmsnTypeCd) {
      case "0" -> FissAdmTypeCode.ADM_TYPE_0;
      case "1" -> FissAdmTypeCode.ADM_TYPE_EMERGENCY;
      case "2" -> FissAdmTypeCode.ADM_TYPE_URGENT;
      case "3" -> FissAdmTypeCode.ADM_TYPE_ELECTIVE;
      case "4" -> FissAdmTypeCode.ADM_TYPE_NEWBORN;
      case "5" -> FissAdmTypeCode.ADM_TYPE_TRAUMA_CENTER;
      case "9" -> FissAdmTypeCode.ADM_TYPE_9;
      default -> FissAdmTypeCode.UNRECOGNIZED;
    };
  }

  /**
   * Fallback method for creating a claim identifier from the CLM_ID field. Uses a hash to generate
   * a string of the appropriate length.
   *
   * @param claimId The value returned by {@link #getClaimId}
   * @return The generated claim identifier
   */
  @VisibleForTesting
  String convertDcn(String claimId) {
    return "-" + DigestUtils.sha256Hex(claimId).substring(0, 22);
  }

  /**
   * Get the value for use as a {@link FissClaim#getRdaClaimKey} value. Requires that the record
   * contain a CLM_ID field value.
   *
   * @param data The data to pull from for claim data
   * @return The generated claim identifier
   */
  private static String getClaimId(Parser.Data<String> data) {
    return data.get(Fiss.CLM_ID)
        .orElseThrow(() -> new IllegalStateException("Claim did not contain a Claim ID"));
  }

  /**
   * Adds diagnosis codes to the given claim, parsed from the given {@link Parser.Data}.
   *
   * @param claimBuilder The claim to add diagnosis codes to
   * @param data The {@link Parser.Data} to pull diagnosis codes from
   */
  @VisibleForTesting
  void addDiagCodes(FissClaim.Builder claimBuilder, Parser.Data<String> data) {
    for (int i = 0; i < Fiss.MAX_DIAG_CODES; ++i) {
      // We can't use the loop index directly because value must be final in the lambda expression
      final int INDEX = i;

      // HHA and Hospice do not include procedure codes
      data.get(Fiss.ICD_DGNS_CD.get(INDEX))
          .ifPresent(
              value -> {
                FissDiagnosisCode.Builder diagBuilder =
                    FissDiagnosisCode.newBuilder()
                        .setRdaPosition(claimBuilder.getFissDiagCodesCount() + 1)
                        .setDiagCd2(value);

                consumeIf(
                    data.get(Fiss.CLM_POA_IND_SW.get(INDEX)).orElse(null),
                    NumberUtils::isDigits,
                    poa -> diagBuilder.setDiagPoaIndEnumValue(Integer.parseInt(poa)));

                claimBuilder.addFissDiagCodes(diagBuilder.build());
              });
    }
  }

  /**
   * Adds procedure codes to the given claim, parsed from the given {@link Parser.Data}.
   *
   * @param claimBuilder The claim to add procedure codes to
   * @param data The {@link Parser.Data} to pull procedure codes from
   */
  @VisibleForTesting
  void addProcCodes(FissClaim.Builder claimBuilder, Parser.Data<String> data) {
    for (int i = 0; i < Fiss.MAX_PROC_CODES; ++i) {
      // We can't use the loop index directly because value must be final in the lambda expression
      final int INDEX = i;

      // HHA and Hospice do not include procedure codes
      data.get(Fiss.ICD_PRCDR_CD.get(INDEX))
          .ifPresent(
              value ->
                  claimBuilder.addFissProcCodes(
                      FissProcedureCode.newBuilder()
                          .setRdaPosition(claimBuilder.getFissProcCodesCount() + 1)
                          .setProcCd(value)
                          .setProcDt(
                              data.getFromType(Fiss.PRCDR_DT.get(INDEX), Parser.Data.Type.DATE)
                                  .orElse(""))
                          .build()));
    }
  }

  /**
   * Maps RIF data to a {@link FissRevenueLine} object.
   *
   * @param lineNumber The line number of the claim
   * @param data The {@link Parser.Data} to pull procedure codes from
   * @return The created {@link FissRevenueLine} objects.
   */
  FissRevenueLine buildRevenueLines(int lineNumber, Parser.Data<String> data) {
    FissRevenueLine.Builder builder =
        FissRevenueLine.newBuilder()
            .setRdaPosition(lineNumber)
            .setHcpcInd("A")
            .setApcHcpcsApc("00000")
            .setNonBillRevCodeEnum(FissNonBillRevCode.NON_BILL_ESRD)
            .setServDtCymd(DEFAULT_HARDCODED_DATE)
            .setServDtCymdText(DEFAULT_HARDCODED_DATE)
            .setNdcQtyQualEnum(DEFAULT_HARDCODED_NDC_QTY_QUAL)
            .setNdcQty(DEFAULT_HARDCODED_NDC_QTY)
            .setNdc(DEFAULT_HARDCODED_NDC);

    data.get(Fiss.REV_CNTR).ifPresent(builder::setRevCd);
    consumeIf(
        data.get(Fiss.LINE_SRVC_CNT).orElse(null),
        NumberUtils::isDigits,
        value -> builder.setRevUnitsBilled(Integer.parseInt(value)));
    consumeIf(
        data.get(Fiss.REV_CNTR_UNIT_CNT).orElse(null),
        NumberUtils::isDigits,
        value -> builder.setRevServUnitCnt(Integer.parseInt(value)));
    data.get(Fiss.HCPCS_1_MDFR_CD).ifPresent(builder::setHcpcModifier);
    data.get(Fiss.HCPCS_2_MDFR_CD).ifPresent(builder::setHcpcModifier2);
    data.get(Fiss.HCPCS_3_MDFR_CD).ifPresent(builder::setHcpcModifier3);
    data.get(Fiss.HCPCS_4_MDFR_CD).ifPresent(builder::setHcpcModifier4);
    data.get(Fiss.HCPCS_CD).ifPresent(builder::setHcpcCd);

    return builder.build();
  }
}
