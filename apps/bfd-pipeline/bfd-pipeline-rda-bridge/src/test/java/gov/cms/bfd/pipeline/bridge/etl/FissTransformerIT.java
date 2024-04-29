package gov.cms.bfd.pipeline.bridge.etl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.Sets;
import com.google.protobuf.MessageOrBuilder;
import gov.cms.bfd.pipeline.bridge.model.BeneficiaryData;
import gov.cms.bfd.pipeline.bridge.util.DataSampler;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import gov.cms.bfd.pipeline.bridge.util.WrappedMessage;
import gov.cms.mpsm.rda.v1.ChangeType;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.RecordSource;
import gov.cms.mpsm.rda.v1.fiss.FissAdmTypeCode;
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import lombok.Data;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import utils.TestUtils;

/** Test to check the functionality of the {@link FissTransformer} class. */
public class FissTransformerIT {

  /** Transformer Arguments data class. */
  @Data
  private static class TransformerArguments {
    /** Map of mbis. */
    private final Map<String, BeneficiaryData> mbiMap;

    /**
     * Helper class for carrying claims between processing iterations so additional line items can
     * be added to the claim if any are found.
     */
    private final WrappedMessage wrappedMessage;

    /** Wrapped long value used for keeping a counter between method scopes. */
    private final WrappedCounter wrappedCounter;

    /**
     * The {@link Parser} creates {@link Data} objects that are then used to retrieve the data in a
     * common manner.
     */
    private final Parser.Data<String> data;

    /**
     * Used to create a sample of data from various sources, enforcing sampling proportions per
     * source.
     */
    private final DataSampler<String> mbiSampler;

    /** Sample Id. */
    private final int sampleId;
  }

  /** Expected values class. */
  @Data
  private static class ExpectedValues {
    /** Expected wrapped message. */
    private final WrappedMessage wrappedMessage;

    /** Expected wrapped counter. */
    private final WrappedCounter wrappedCounter;

    /** Expected response. */
    private final Optional<MessageOrBuilder> response;

    /** Expected sample mbis. */
    private final Set<String> sampledMbis;
  }

  /**
   * Happy path testing to produce expected claims and data samples.
   *
   * @return the expected data samples
   */
  private static Stream<Arguments> shouldProduceExpectedClaimsAndDataSamples() {
    return Stream.of(
        newFirstClaimTestCase(),
        newFirstClaimInvalidLineNumberCase(),
        recurringClaimCase(),
        recurringClaimInvalidLineNumberCase(),
        newNonFirstClaimCase());
  }

  /**
   * Happy path test to ensure for various scenarios, claim responses are generated as expected and
   * certain pieces of data are set correctly when passed through the {@link FissTransformer}.
   *
   * @param testName for the test name
   * @param arguments for the arguments of the test
   * @param expectedValues for the expected values of the test
   * @param expectedException if there is a expected exception
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  void shouldProduceExpectedClaimsAndDataSamples(
      String testName,
      TransformerArguments arguments,
      ExpectedValues expectedValues,
      Exception expectedException) {
    try {
      final Set<String> jsonCompareIgnorePaths =
          Set.of("/timestamp", "/source/transmissionTimestamp", "/source/extractDate");

      Optional<MessageOrBuilder> actualResponse =
          new FissTransformer(arguments.getMbiMap())
              .transform(
                  arguments.getWrappedMessage(),
                  arguments.getWrappedCounter(),
                  arguments.getData(),
                  arguments.getMbiSampler(),
                  arguments.getSampleId(),
                  "inpatient");

      if (expectedException != null) {
        fail("Expected exception to be thrown, but none thrown");
      }

      if (expectedValues.getResponse().isEmpty()) {
        assertTrue(actualResponse.isEmpty(), "Expected empty response, but got one instead");
      } else if (actualResponse.isEmpty()) {
        fail("Expected a response, but got an empty one");
      } else {
        TestUtils.assertMessagesEqual(
            expectedValues.getResponse().get(), actualResponse.get(), jsonCompareIgnorePaths);
      }

      Set<String> actualMbis = Sets.newHashSet(arguments.getMbiSampler());
      assertEquals(expectedValues.getSampledMbis(), actualMbis);

      assertEquals(expectedValues.getWrappedCounter().get(), arguments.getWrappedCounter().get());

      if (expectedValues.getWrappedMessage().getMessage() == null) {
        assertNull(arguments.getWrappedMessage().getMessage());
      } else if (arguments.getWrappedMessage().getMessage() == null) {
        fail("Wrapped message did not contain a message, but it was expected to.");
      } else {
        assertEquals(
            expectedValues.getWrappedMessage().getLineNumber(),
            arguments.getWrappedMessage().getLineNumber());
        TestUtils.assertMessagesEqual(
            expectedValues.getWrappedMessage().getMessage(),
            arguments.getWrappedMessage().getMessage(),
            jsonCompareIgnorePaths);
      }
    } catch (Exception actualException) {
      if (expectedException != null) {
        assertEquals(expectedException.getClass(), actualException.getClass());
        assertEquals(expectedException.getMessage(), actualException.getMessage());
      } else {
        throw actualException;
      }
    }
  }

  /**
   * This function sets the {@link Arguments} with the expected data for the default/first claim for
   * the FissClaim, the default data map, and the default MBI to compare against the actual results.
   *
   * @return {@link Arguments} the expected claim lines and their associated numbers
   */
  private static Arguments newFirstClaimTestCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Parser.Data<String> data = TestData.createDataParser(TestData.createDefaultDataMap());
    WrappedMessage wrappedMessage = new WrappedMessage();
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();

    // Expected values
    FissClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedClaimChange = createFissClaimChange(expectedClaim);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(1);
    expectedWrappedMessage.setMessage(expectedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(2);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException = null;

    return Arguments.arguments(
        "New first claim, Line 1, should process",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.FISS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * This function returns {@link Arguments} for expected results for a test case where an invalid
   * line number will throw an expected IllegalStateException.
   *
   * @return {@link Arguments} the expected claim lines and their associated numbers
   */
  private static Arguments newFirstClaimInvalidLineNumberCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("CLM_LINE_NUM", "2");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    WrappedMessage wrappedMessage = new WrappedMessage();
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();

    // Expected values
    FissClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedClaimChange = createFissClaimChange(expectedClaim);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(1);
    expectedWrappedMessage.setMessage(expectedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(2);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException =
        new IllegalStateException("Invalid row sequence, expected: 1, current line number: 2");

    return Arguments.arguments(
        "New claim, unexpected line number, should throw",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.FISS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * Returns the {@link Arguments} where the data is set for the second claim to use the same data
   * as the first claim to test recurring claims.
   *
   * @return {@link Arguments} the expected claim lines and their associated numbers
   */
  private static Arguments recurringClaimCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("CLM_LINE_NUM", "2");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    FissClaimChange recurringClaim =
        createFissClaimChange(TestData.createDefaultClaimBuilder().build());
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(recurringClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.FISS_SAMPLE_ID, TestData.MBI);

    // Expected values
    FissClaim expectedClaim =
        TestData.createDefaultClaimBuilder()
            .addFissRevenueLines(
                FissRevenueLine.newBuilder()
                    .setRdaPosition(2)
                    .setHcpcInd("A")
                    .setApcHcpcsApc("00000")
                    .setNonBillRevCodeEnum(FissNonBillRevCode.NON_BILL_ESRD)
                    .setServDtCymd("1970-01-01")
                    .setServDtCymdText("1970-01-01")
                    .setNdc("00777310502")
                    .setNdcQty("20")
                    .setNdcQtyQualEnum(FissNdcQtyQual.NDC_QTY_QUAL_ME)
                    .build())
            .build();
    FissClaimChange expectedClaimChange = createFissClaimChange(expectedClaim);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(2);
    expectedWrappedMessage.setMessage(expectedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(1);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException = null;

    return Arguments.arguments(
        "Recurring claim, should return same claim",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.FISS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * Sets {@link Arguments} to return the same claim in Line Number 1 and Line Number 3 to test
   * recurring claims with different line numbers.
   *
   * @return {@link Arguments} the expected claim lines and their associated numbers
   */
  private static Arguments recurringClaimInvalidLineNumberCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("CLM_LINE_NUM", "3");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    FissClaimChange recurringClaim =
        createFissClaimChange(TestData.createDefaultClaimBuilder().build());
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(recurringClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.FISS_SAMPLE_ID, TestData.MBI);

    // Expected values
    FissClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedClaimChange = createFissClaimChange(expectedClaim);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(2);
    expectedWrappedMessage.setMessage(expectedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(1);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException =
        new IllegalStateException(
            "Invalid row sequence, previous line number: 1, current line number: 3");

    return Arguments.arguments(
        "Recurring claim, unexpected line number, should throw",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.FISS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * Sets {@link Arguments} the claim to a new claim that has been previously processed.
   *
   * @return {@link Arguments} the expected claim lines and their associated numbers
   */
  private static Arguments newNonFirstClaimCase() {
    final String NEW_CLAIM_DCN = "dcn87654321";
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("FI_DOC_CLM_CNTL_NUM", NEW_CLAIM_DCN);
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    FissClaimChange previouslyProcessedClaim =
        createFissClaimChange(TestData.createDefaultClaimBuilder().build());
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(previouslyProcessedClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(2);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.FISS_SAMPLE_ID, TestData.MBI);

    // Expected values
    FissClaim expectedResponseClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedResponseClaimChange = createFissClaimChange(expectedResponseClaim);

    Optional<MessageOrBuilder> expectedResponse = Optional.of(expectedResponseClaimChange);

    FissClaim expectedWrappedClaim =
        TestData.createDefaultClaimBuilder()
            .setDcn(NEW_CLAIM_DCN)
            .setRdaClaimKey(TestData.CLM_ID)
            .build();
    FissClaimChange expectedWrappedClaimChange =
        createFissClaimChange(expectedWrappedClaim, NEW_CLAIM_DCN, 2);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(1);
    expectedWrappedMessage.setMessage(expectedWrappedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(3);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException = null;

    return Arguments.arguments(
        "new non-first claim, should process and return",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.FISS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, expectedResponse, expectedSampledMbis),
        expectedException);
  }

  /**
   * Helper method to generate {@link FissClaimChange} objects.
   *
   * @param claim The {@link FissClaim} to wrap in the generated {@link FissClaimChange} object
   * @return The created wrapping {@link FissClaimChange} object
   */
  private static FissClaimChange createFissClaimChange(FissClaim claim) {
    return createFissClaimChange(claim, TestData.FI_DOC_CLM_CNTL_NUM, 1);
  }

  /**
   * Helper method to generate {@link FissClaimChange} objects.
   *
   * @param claim The {@link FissClaim} to wrap in the generated {@link FissClaimChange} object
   * @param dcn The dcn to use
   * @param sequenceNumber The sequence number to use
   * @return The created wrapping {@link FissClaimChange} object
   */
  private static FissClaimChange createFissClaimChange(
      FissClaim claim, String dcn, int sequenceNumber) {
    return FissClaimChange.newBuilder()
        .setSeq(sequenceNumber)
        .setClaim(claim)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setDcn(dcn)
        .setRdaClaimKey(TestData.CLM_ID)
        .setIntermediaryNb(claim.getIntermediaryNb())
        .setSource(
            RecordSource.newBuilder()
                .setPhase("P1")
                .setPhaseSeqNum(0)
                .setExtractDate("1970-01-01")
                .setTransmissionTimestamp("1970-01-01T00:00:00.000000Z")
                .build())
        .build();
  }

  /** TestData return class. */
  private static class TestData {
    /** Beneficiary Id. */
    private static final String BENE_ID = "beneid";

    /** Beneficiary First Name. */
    private static final String BENE_FIRST_NAME = "Firstname";

    /** Beneficiary Last Name. */
    private static final String BENE_LAST_NAME = "Lastname";

    /** Beneficiary Middle Initial. */
    private static final String BENE_MID_INIT = "M";

    /** Beneficiary Date of Birth. */
    private static final String BENE_DOB = "2020-01-01";

    /** Beneficiary Gender. */
    private static final String BENE_GENDER = "1";

    /** Fi Document Claim Control Number. */
    private static final String FI_DOC_CLM_CNTL_NUM = "dcn12345678";

    /** HICN Number. */
    private static final String HIC_NO = "hicno123";

    /** MBI Number. */
    private static final String MBI = "mbimbimbimbi";

    /** Admitting Diagnosis Code. */
    private static final String ADMTG_DGNS_CD = "admitcd";

    /** Claim Frequency Code. */
    private static final String CLM_FREQ_CD = "freqCode";

    /** Claim From Date. */
    private static final String CLM_FROM_DT = "01-Jan-2001";

    /** Claim Type Code. */
    private static final int CLM_SRVC_CLSFCTN_TYPE_CD = 1;

    /** Claim Thru Date. */
    private static final String CLM_THRU_DT = "03-Mar-2001";

    /** Claim Tot Charge Amount. */
    private static final String CLM_TOT_CHRG_AMT = "3218.33";

    /** Claim Fac Type Code. */
    private static final int CLM_FAC_TYPE_CD = 8;

    /** Orginial NPI Number. */
    private static final String ORG_NPI_NUM = "8888888888";

    /** Principal Diagnosis Code. */
    private static final String PRNCPAL_DGNS_CD = "princode";

    /** Provider Number. */
    private static final String PRVDR_NUM = "222222";

    /** Claim ID. */
    private static final String CLM_ID = "-999999999";

    /** ICD Diagnosis Code. */
    private static final String ICD_DGNS_CD1 = "JJJJ";

    /** Claim POA. */
    private static final int CLM_POA_IND_SW1 = 1;

    /** Icd Procedure code. */
    private static final String ICD_PRCDR_CD1 = "pc1";

    /** Procedure Date. */
    private static final String PRCDR_DT1 = "10-Jan-2011";

    /** Claim Line Number. */
    private static final String CLM_LINE_NUM = "1";

    /** Hardcoded IntermediaryNb. */
    private static final String HARDCODED_INTERMEDIARY_NB = "?";

    /** Hardcoded Location1. */
    private static final String HARDCODED_LOC1 = "?";

    /** Hardcoded Location2. */
    private static final String HARDCODED_LOC2 = "?";

    /** Hardcoded Transaction Date. */
    private static final String HARDCODED_TRAN_DATE_CYMD = "1970-01-01";

    /** Hardcoded Federal Tax Number. */
    private static final String HARDCODED_FED_TAX_NUMBER = "XX-XXXXXXX";

    /** Hardcoded Received Date. */
    private static final String HARDCODED_RECEIVED_DATE_CYMD = "1970-01-01";

    /** Hardcoded Claim Inpatient Admission Type Code. */
    private static final String HARDCODED_CLM_IP_ADMSN_TYPE_CD = "2";

    /** Hardcoded NDC. */
    private static final String NDC = "00777310502";

    /** Hardcoded NDC-QTY. */
    public static final String NDC_QTY = "20";

    /** Hardcoded NDC-QTY-QUAL. */
    public static final FissNdcQtyQual NDC_QTY_QUAL = FissNdcQtyQual.NDC_QTY_QUAL_ME;

    /** Fiss Sample ID. */
    private static final int FISS_SAMPLE_ID = 0;

    /** Mcs Sample ID. */
    private static final int MCS_SAMPLE_ID = 1;

    /**
     * Function creates a default claim builder.
     *
     * @return {@link FissClaim}
     */
    public static FissClaim.Builder createDefaultClaimBuilder() {
      return FissClaim.newBuilder()
          .setRdaClaimKey(CLM_ID)
          .setDcn(FI_DOC_CLM_CNTL_NUM)
          .setHicNo(HIC_NO)
          .setMbi(MBI)
          .setClmTypIndEnum(FissClaimTypeIndicator.CLAIM_TYPE_INPATIENT)
          .setCurrLoc1Unrecognized(HARDCODED_LOC1)
          .setCurrLoc2Unrecognized(HARDCODED_LOC2)
          .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_ROUTING)
          .setCurrTranDtCymd(HARDCODED_TRAN_DATE_CYMD)
          .setFedTaxNb(HARDCODED_FED_TAX_NUMBER)
          .setIntermediaryNb(HARDCODED_INTERMEDIARY_NB)
          .setRecdDtCymd(HARDCODED_RECEIVED_DATE_CYMD)
          .setAdmTypCdEnum(FissAdmTypeCode.ADM_TYPE_URGENT)
          .addFissPayers(
              FissPayer.newBuilder()
                  .setBeneZPayer(
                      FissBeneZPayer.newBuilder()
                          .setPayersIdEnum(FissPayersCode.PAYERS_CODE_MEDICARE)
                          .setRdaPosition(1)
                          .setPayersName("MEDICARE")
                          .setBeneFirstName("Firstname")
                          .setBeneMidInit("M")
                          .setBeneLastName("Lastname")
                          .setBeneDob("2020-01-01")
                          .setBeneSexEnum(FissBeneficiarySex.BENEFICIARY_SEX_MALE)
                          .build())
                  .build())
          .setTotalChargeAmount(CLM_TOT_CHRG_AMT)
          .setAdmDiagCode(ADMTG_DGNS_CD)
          .setPrincipleDiag(PRNCPAL_DGNS_CD)
          .setNpiNumber(ORG_NPI_NUM)
          .addFissProcCodes(
              FissProcedureCode.newBuilder()
                  .setRdaPosition(1)
                  .setProcCd(ICD_PRCDR_CD1)
                  .setProcDt(PRCDR_DT1)
                  .build())
          .addFissDiagCodes(
              FissDiagnosisCode.newBuilder()
                  .setRdaPosition(1)
                  .setDiagCd2(ICD_DGNS_CD1)
                  .setDiagPoaIndEnumValue(CLM_POA_IND_SW1)
                  .build())
          .setStmtCovFromCymd(CLM_FROM_DT)
          .setStmtCovToCymd(CLM_THRU_DT)
          .setMedaProv6(PRVDR_NUM)
          .setLobCdEnumValue(CLM_FAC_TYPE_CD)
          .setServTypeCdEnumValue(CLM_SRVC_CLSFCTN_TYPE_CD)
          .addFissRevenueLines(
              FissRevenueLine.newBuilder()
                  .setRdaPosition(1)
                  .setHcpcInd("A")
                  .setApcHcpcsApc("00000")
                  .setNonBillRevCodeEnum(FissNonBillRevCode.NON_BILL_ESRD)
                  .setServDtCymd("1970-01-01")
                  .setServDtCymdText("1970-01-01")
                  .setNdc(NDC)
                  .setNdcQty(NDC_QTY)
                  .setNdcQtyQualEnum(NDC_QTY_QUAL)
                  .build());
    }

    /**
     * Creates a default data parser.
     *
     * @param parserData the data that needs to be parsed
     * @return {@link Parser} data parser
     */
    public static Parser.Data<String> createDataParser(Map<String, String> parserData) {
      return new Parser.Data<>() {
        private final Map<String, String> dataMap = parserData;

        /** {@inheritDoc} */
        @Override
        public long getEntryNumber() {
          return 1;
        }

        /** {@inheritDoc} */
        @Override
        public Optional<String> get(String fieldName) {
          Optional<String> result;

          if (dataMap.containsKey(fieldName)) {
            result = Optional.of(dataMap.get(fieldName));
          } else {
            result = Optional.empty();
          }

          return result;
        }
      };
    }

    /**
     * Creates a default data map.
     *
     * @return {@link Map}
     */
    public static Map<String, String> createDefaultDataMap() {
      return Map.ofEntries(
          Map.entry("BENE_ID", BENE_ID),
          Map.entry("FI_DOC_CLM_CNTL_NUM", FI_DOC_CLM_CNTL_NUM),
          Map.entry("ADMTG_DGNS_CD", ADMTG_DGNS_CD),
          Map.entry("CLM_FREQ_CD", CLM_FREQ_CD),
          Map.entry("CLM_FROM_DT", CLM_FROM_DT),
          Map.entry("CLM_SRVC_CLSFCTN_TYPE_CD", String.valueOf(CLM_SRVC_CLSFCTN_TYPE_CD)),
          Map.entry("CLM_THRU_DT", CLM_THRU_DT),
          Map.entry("CLM_TOT_CHRG_AMT", CLM_TOT_CHRG_AMT),
          Map.entry("CLM_FAC_TYPE_CD", String.valueOf(CLM_FAC_TYPE_CD)),
          Map.entry("ORG_NPI_NUM", ORG_NPI_NUM),
          Map.entry("PRNCPAL_DGNS_CD", PRNCPAL_DGNS_CD),
          Map.entry("PRVDR_NUM", PRVDR_NUM),
          Map.entry("CLM_ID", CLM_ID),
          Map.entry("ICD_DGNS_CD1", ICD_DGNS_CD1),
          Map.entry("CLM_POA_IND_SW1", String.valueOf(CLM_POA_IND_SW1)),
          Map.entry("ICD_PRCDR_CD1", ICD_PRCDR_CD1),
          Map.entry("PRCDR_DT1", PRCDR_DT1),
          Map.entry("CLM_LINE_NUM", CLM_LINE_NUM),
          Map.entry("CLM_IP_ADMSN_TYPE_CD", HARDCODED_CLM_IP_ADMSN_TYPE_CD));
    }

    /**
     * Creates a default mbi map.
     *
     * @return {@link Map}
     */
    public static Map<String, BeneficiaryData> createDefaultMbiMap() {
      return Map.of(
          BENE_ID,
          new BeneficiaryData(
              BENE_ID,
              MBI,
              HIC_NO,
              BENE_FIRST_NAME,
              BENE_LAST_NAME,
              BENE_MID_INIT,
              BENE_DOB,
              BENE_GENDER));
    }

    /**
     * Creates a default data sampler.
     *
     * @return {@link DataSampler}
     */
    public static DataSampler<String> createDefaultDataSampler() {
      return new DataSampler.Builder<String>()
          .registerSampleSet(FISS_SAMPLE_ID, 0.5F)
          .registerSampleSet(MCS_SAMPLE_ID, 0.5F)
          .maxValues(5)
          .build();
    }
  }
}
