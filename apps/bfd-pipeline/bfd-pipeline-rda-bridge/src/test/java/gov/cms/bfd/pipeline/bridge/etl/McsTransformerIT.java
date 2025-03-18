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
import gov.cms.mpsm.rda.v1.McsClaimChange;
import gov.cms.mpsm.rda.v1.RecordSource;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsClaimType;
import gov.cms.mpsm.rda.v1.mcs.McsDetail;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisIcdType;
import gov.cms.mpsm.rda.v1.mcs.McsStatusCode;
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

/** Test to check the functionality of the {@link McsTransformer} class. */
public class McsTransformerIT {

  /** Sets the data for the McsTransformer arguments. */
  @Data
  private static class TransformerArguments {
    /** Map of the mbis from the beneficiary data. */
    private final Map<String, BeneficiaryData> mbiMap;

    /**
     * Helper class for carrying claims between processing iterations so additional line items can
     * be added to the claim if any are found.
     */
    private final WrappedMessage wrappedMessage;

    /** Wrapped long value used for keeping a counter between method scopes. */
    private final WrappedCounter wrappedCounter;

    /** Abstract classes for predefined methods. */
    private final Parser.Data<String> data;

    /**
     * Used to create a sample of data from various sources, enforcing sampling proportions per
     * source.
     */
    private final DataSampler<String> mbiSampler;

    /** The sample id for the data. */
    private final int sampleId;
  }

  /** This static class is for the expected values of the test. */
  @Data
  private static class ExpectedValues {
    /**
     * Helper class for carrying claims between processing iterations so additional line items can
     * be added to the claim if any are found.
     */
    private final WrappedMessage wrappedMessage;

    /** Wrapped long value used for keeping a counter between method scopes. */
    private final WrappedCounter wrappedCounter;

    /** Used to for the expected response. */
    private final Optional<MessageOrBuilder> response;

    /** Used to store a set of expected sample mbis. */
    private final Set<String> sampledMbis;
  }

  /**
   * Produces expected claims and their data samples.
   *
   * @return {@link Stream} of arguments
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
   * Test helper function to produced expected claims and their data samples.
   *
   * @param testName the name of the test
   * @param arguments the arguments for the test
   * @param expectedValues the expected values for the test
   * @param expectedException the expected exception
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
          new McsTransformer(arguments.getMbiMap())
              .transform(
                  arguments.getWrappedMessage(),
                  arguments.getWrappedCounter(),
                  arguments.getData(),
                  arguments.getMbiSampler(),
                  arguments.getSampleId(),
                  "doesNotMatter");

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
   * New first claim tests. This function provides that the first claim is processed and the
   * expected values are set and the same as the Test Data class.
   *
   * @return {@link Arguments}
   */
  private static Arguments newFirstClaimTestCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Parser.Data<String> data = TestData.createDataParser(TestData.createDefaultDataMap());
    WrappedMessage wrappedMessage = new WrappedMessage();
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();

    // Expected values
    McsClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    McsClaimChange expectedClaimChange = createMcsClaimChange(expectedClaim);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(1);
    expectedWrappedMessage.setMessage(expectedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(2);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException = null;

    return Arguments.arguments(
        "New first claim, Line 1, should process",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * New first claim invalid line number tests.
   *
   * @return {@link Arguments}
   */
  private static Arguments newFirstClaimInvalidLineNumberCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("LINE_NUM", "2");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    WrappedMessage wrappedMessage = new WrappedMessage();
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();

    // Expected values
    McsClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    McsClaimChange expectedClaimChange = createMcsClaimChange(expectedClaim);

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
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * Recurring claims function.
   *
   * @return {@link Arguments}
   */
  private static Arguments recurringClaimCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("LINE_NUM", "2");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    McsClaimChange recurringClaim =
        createMcsClaimChange(TestData.createDefaultClaimBuilder().build());
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(recurringClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.MCS_SAMPLE_ID, TestData.MBI);

    // Expected values
    McsClaim expectedClaim =
        TestData.createDefaultClaimBuilder()
            .addMcsDetails(
                McsDetail.newBuilder()
                    .setIdrDtlFromDate(TestData.LINE_1ST_EXPNS_DT)
                    .setIdrDtlToDate(TestData.LINE_LAST_EXPNS_DT)
                    .setIdrProcCode(TestData.HCPCS_CD)
                    .setIdrModOne(TestData.HCPCS_1ST_MDFR_CD)
                    .setIdrModTwo(TestData.HCPCS_2ND_MDFR_CD)
                    .setIdrDtlPrimaryDiagCode(TestData.LINE_ICD_DGNS_CD)
                    .setIdrDtlDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10)
                    .setIdrDtlNumber(2)
                    .setIdrDtlNdc("00777310502")
                    .setIdrDtlNdcUnitCount("20")
                    .build())
            .build();
    McsClaimChange expectedClaimChange = createMcsClaimChange(expectedClaim);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(2);
    expectedWrappedMessage.setMessage(expectedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(1);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException = null;

    return Arguments.arguments(
        "Recurring claim, should return appended claim",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * Recurring claim invalid line number test case.
   *
   * @return {@link Arguments}
   */
  private static Arguments recurringClaimInvalidLineNumberCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("LINE_NUM", "3");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    McsClaimChange recurringClaim =
        createMcsClaimChange(TestData.createDefaultClaimBuilder().build());
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(recurringClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.MCS_SAMPLE_ID, TestData.MBI);

    // Expected values
    McsClaim expectedClaim =
        TestData.createDefaultClaimBuilder()
            .addMcsDetails(
                McsDetail.newBuilder()
                    .setIdrDtlFromDate(TestData.LINE_1ST_EXPNS_DT)
                    .setIdrDtlToDate(TestData.LINE_LAST_EXPNS_DT)
                    .setIdrProcCode(TestData.HCPCS_CD)
                    .setIdrModOne(TestData.HCPCS_1ST_MDFR_CD)
                    .setIdrModTwo(TestData.HCPCS_2ND_MDFR_CD)
                    .setIdrDtlPrimaryDiagCode(TestData.LINE_ICD_DGNS_CD)
                    .setIdrDtlDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10)
                    .setIdrDtlNdc("00777310502")
                    .setIdrDtlNdcUnitCount("20")
                    .build())
            .build();
    McsClaimChange expectedClaimChange = createMcsClaimChange(expectedClaim);

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
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  /**
   * New non first claim case.
   *
   * @return {@link Arguments}
   */
  private static Arguments newNonFirstClaimCase() {
    final String NEW_CLAIM_ICN = "icn87654321";
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("CARR_CLM_CNTL_NUM", NEW_CLAIM_ICN);
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    McsClaimChange previouslyProcessedClaim =
        createMcsClaimChange(TestData.createDefaultClaimBuilder().build());
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(previouslyProcessedClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(2);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.MCS_SAMPLE_ID, TestData.MBI);

    // Expected values
    McsClaim expectedResponseClaim = TestData.createDefaultClaimBuilder().build();
    McsClaimChange expectedResponseClaimChange = createMcsClaimChange(expectedResponseClaim);

    Optional<MessageOrBuilder> expectedResponse = Optional.of(expectedResponseClaimChange);

    McsClaim.Builder defaultClaim = TestData.createDefaultClaimBuilder();
    McsClaim expectedWrappedClaim =
        defaultClaim
            .setIdrClmHdIcn(NEW_CLAIM_ICN)
            .setMcsDiagnosisCodes(
                0,
                defaultClaim.getMcsDiagnosisCodes(0).toBuilder()
                    .setIdrClmHdIcn(NEW_CLAIM_ICN)
                    .build())
            .build();
    McsClaimChange expectedWrappedClaimChange =
        createMcsClaimChange(expectedWrappedClaim, NEW_CLAIM_ICN, 2);

    WrappedMessage expectedWrappedMessage = new WrappedMessage();
    expectedWrappedMessage.setLineNumber(1);
    expectedWrappedMessage.setMessage(expectedWrappedClaimChange);

    WrappedCounter expectedWrappedCounter = new WrappedCounter(3);

    Set<String> expectedSampledMbis = Set.of(TestData.MBI);

    Exception expectedException = null;

    return Arguments.arguments(
        "new non-first claim, should process and return",
        new TransformerArguments(
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, expectedResponse, expectedSampledMbis),
        expectedException);
  }

  /**
   * Helper method to create {@link McsClaimChange} objects.
   *
   * @param claim The {@link McsClaim} to wrap in the created {@link McsClaimChange} object
   * @return The created {@link McsClaimChange} object
   */
  private static McsClaimChange createMcsClaimChange(McsClaim claim) {
    return createMcsClaimChange(claim, TestData.CARR_CLM_CNTL_NUM, 1);
  }

  /**
   * Helper method to create {@link McsClaimChange} objects.
   *
   * @param claim The {@link McsClaim} to wrap in the created {@link McsClaimChange} object
   * @param icn The ICN to use
   * @param sequenceNumber THe sequence number to use
   * @return The created {@link McsClaimChange} object
   */
  private static McsClaimChange createMcsClaimChange(
      McsClaim claim, String icn, int sequenceNumber) {
    return McsClaimChange.newBuilder()
        .setSeq(sequenceNumber)
        .setClaim(claim)
        .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
        .setIcn(icn)
        .setSource(
            RecordSource.newBuilder()
                .setPhase("P1")
                .setPhaseSeqNum(0)
                .setExtractDate("1970-01-01")
                .setTransmissionTimestamp("1970-01-01T00:00:00.000000Z")
                .build())
        .build();
  }

  /** Test Data class set. */
  private static class TestData {
    /** Line Number. */
    private static final String LINE_NUM = "1";

    /** Beneficiary Id. */
    private static final String BENE_ID = "beneid";

    /** Beneficiary First Name. */
    private static final String BENE_FIRST_NAME = "F";

    /** Beneficiary Last Name. */
    private static final String BENE_LAST_NAME = "Lastna";

    /** Beneficiary Middle Initial. */
    private static final String BENE_MID_INIT = "M";

    /** Beneficiary DOB. */
    private static final String BENE_DOB = "2020-01-01";

    /** Beneficiary sex. */
    private static final int BENE_SEX = 1;

    /** Carrier Claim Control Number.. */
    private static final String CARR_CLM_CNTL_NUM = "icn12345678";

    /** Claim From Date. */
    private static final String CLM_FROM_DT = "01-Jan-2001";

    /** Claim Thru Date. */
    private static final String CLM_THRU_DT = "03-Mar-2001";

    /** NCH Carrier Claim Submitted Charge Amount. */
    private static final String NCH_CARR_CLM_SBMTD_CHRG_AMT = "832.12";

    /** Original NPI Number. */
    private static final String ORG_NPI_NUM = "8888888888";

    /** Claim Id. */
    private static final String CLM_ID = "-999999999";

    /** Diagnosis Code. */
    private static final String ICD_DGNS_CD1 = "JJJJ";

    /** Diagnosis Version Code. */
    private static final String ICD_DGNS_VRSN_CD1 = "0";

    /** Line Diagnosis Code. */
    private static final String LINE_ICD_DGNS_CD = "12";

    /** Line Diagnosis Version Code. */
    private static final String LINE_ICD_DGNS_VRSN_CD = "0";

    /** HCPCS Code. */
    private static final String HCPCS_CD = "123";

    /** HCPCS first code. */
    private static final String HCPCS_1ST_MDFR_CD = "abc";

    /** HCPCS second code. */
    private static final String HCPCS_2ND_MDFR_CD = "cba";

    /** Line Expns Date. */
    private static final String LINE_1ST_EXPNS_DT = "20-Feb-2008";

    /** Line Last Expns Date. */
    private static final String LINE_LAST_EXPNS_DT = "30-Jun-2008";

    /** MBI number. */
    private static final String MBI = "mbimbimbimbi";

    /** Hardcoded Bill EIN. */
    private static final String HARDCODED_BILL_PROV_EIN = "XX-XXXXXXX";

    /** Hardcoded Bill Spec. */
    private static final String HARDCODED_BILL_PROV_SPEC = "01";

    /** Hardcoded Bill Type. */
    private static final String HARDCODED_BILL_PROV_TYPE = "20";

    /** Hardcoded Bill Received Date. */
    private static final String HARDCODED_RECEIVED_DATE_CYMD = "1970-01-01";

    /** Hardcoded Control ID. */
    private static final String HARDCODED_CONTROL_ID = "00000";

    /** Hardcoded Status Date. */
    private static final String HARDCODED_STATUS_DATE = "1970-01-01";

    /** Fiss Sample ID. */
    private static final int FISS_SAMPLE_ID = 0;

    /** Mcs Sample ID. */
    private static final int MCS_SAMPLE_ID = 1;

    /** Hardcoded DTL_NDC. */
    private static final String DTL_NDC = "00777310502";

    /** Hardcoded NDC-UNIT-COUNT. */
    private static final String HARDCODED_NDC_UNIT_COUNT = "20";

    /**
     * Returns the default claim builder.
     *
     * @return {@link McsClaim#Builder}
     */
    public static McsClaim.Builder createDefaultClaimBuilder() {
      return McsClaim.newBuilder()
          .setIdrClmHdIcn(CARR_CLM_CNTL_NUM)
          .setIdrClaimMbi(MBI)
          .setIdrBillProvEin(HARDCODED_BILL_PROV_EIN)
          .setIdrBillProvSpec(HARDCODED_BILL_PROV_SPEC)
          .setIdrBillProvType(HARDCODED_BILL_PROV_TYPE)
          .setIdrClaimReceiptDate(HARDCODED_RECEIVED_DATE_CYMD)
          .setIdrClaimTypeEnum(McsClaimType.CLAIM_TYPE_MEDICAL) // "3"
          .setIdrContrId(HARDCODED_CONTROL_ID)
          .setIdrStatusCodeEnum(McsStatusCode.STATUS_CODE_ACTIVE_A)
          .setIdrStatusDate(HARDCODED_STATUS_DATE)
          .setIdrClmHdIcn(CARR_CLM_CNTL_NUM)
          .setIdrBeneFirstInit(BENE_FIRST_NAME)
          .setIdrBeneMidInit(BENE_MID_INIT)
          .setIdrBeneLast16(BENE_LAST_NAME)
          .setIdrBeneSexEnumValue(BENE_SEX - 1)
          .setIdrBillProvNpi(ORG_NPI_NUM)
          .setIdrTotBilledAmt(NCH_CARR_CLM_SBMTD_CHRG_AMT)
          .addMcsDiagnosisCodes(
              McsDiagnosisCode.newBuilder()
                  .setRdaPosition(1)
                  .setIdrClmHdIcn(CARR_CLM_CNTL_NUM)
                  .setIdrDiagCode(ICD_DGNS_CD1)
                  .setIdrDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10)
                  .build())
          .addMcsDetails(
              McsDetail.newBuilder()
                  .setIdrDtlFromDate(LINE_1ST_EXPNS_DT)
                  .setIdrDtlToDate(LINE_LAST_EXPNS_DT)
                  .setIdrProcCode(HCPCS_CD)
                  .setIdrModOne(HCPCS_1ST_MDFR_CD)
                  .setIdrModTwo(HCPCS_2ND_MDFR_CD)
                  .setIdrDtlPrimaryDiagCode(LINE_ICD_DGNS_CD)
                  .setIdrDtlDiagIcdTypeEnum(McsDiagnosisIcdType.DIAGNOSIS_ICD_TYPE_ICD10)
                  .setIdrDtlNumber(1)
                  .setIdrDtlNdc(DTL_NDC)
                  .setIdrDtlNdcUnitCount(HARDCODED_NDC_UNIT_COUNT)
                  .build())
          .setIdrHdrFromDos(CLM_FROM_DT)
          .setIdrHdrToDos(CLM_THRU_DT);
    }

    /**
     * Static function creates a data parser.
     *
     * @param parserData the data to be parsed
     * @return {@link Parser}
     */
    public static Parser.Data<String> createDataParser(Map<String, String> parserData) {
      return new Parser.Data<>() {
        private final Map<String, String> dataMap = parserData;

        /** {@inheritDoc} */
        @Override
        public long getEntryNumber() {
          return 1;
        }

        /**
         * Gets the {@link fieldName} from the dataMap.
         *
         * @param fieldName of the data map
         * @return {@link Optional}
         */
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
     * Creates a map for the default data.
     *
     * @return {@link Map}
     */
    public static Map<String, String> createDefaultDataMap() {
      return Map.ofEntries(
          Map.entry("LINE_NUM", LINE_NUM),
          Map.entry("BENE_ID", BENE_ID),
          Map.entry("CARR_CLM_CNTL_NUM", CARR_CLM_CNTL_NUM),
          Map.entry("CLM_FROM_DT", CLM_FROM_DT),
          Map.entry("CLM_THRU_DT", CLM_THRU_DT),
          Map.entry("NCH_CARR_CLM_SBMTD_CHRG_AMT", NCH_CARR_CLM_SBMTD_CHRG_AMT),
          Map.entry("ORG_NPI_NUM", ORG_NPI_NUM),
          Map.entry("CLM_ID", CLM_ID),
          Map.entry("ICD_DGNS_CD1", ICD_DGNS_CD1),
          Map.entry("ICD_DGNS_VRSN_CD1", ICD_DGNS_VRSN_CD1),
          Map.entry("LINE_ICD_DGNS_CD", LINE_ICD_DGNS_CD),
          Map.entry("LINE_ICD_DGNS_VRSN_CD", LINE_ICD_DGNS_VRSN_CD),
          Map.entry("HCPCS_CD", HCPCS_CD),
          Map.entry("HCPCS_1ST_MDFR_CD", HCPCS_1ST_MDFR_CD),
          Map.entry("HCPCS_2ND_MDFR_CD", HCPCS_2ND_MDFR_CD),
          Map.entry("LINE_1ST_EXPNS_DT", LINE_1ST_EXPNS_DT),
          Map.entry("LINE_LAST_EXPNS_DT", LINE_LAST_EXPNS_DT));
    }

    /**
     * Creates a map for the default mbis.
     *
     * @return {@link Map}
     */
    public static Map<String, BeneficiaryData> createDefaultMbiMap() {
      return Map.of(
          BENE_ID,
          new BeneficiaryData(
              BENE_ID,
              MBI,
              "",
              BENE_FIRST_NAME,
              BENE_LAST_NAME,
              BENE_MID_INIT,
              BENE_DOB,
              String.valueOf(BENE_SEX)));
    }

    /**
     * Created the default for the dataSampler class.
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
