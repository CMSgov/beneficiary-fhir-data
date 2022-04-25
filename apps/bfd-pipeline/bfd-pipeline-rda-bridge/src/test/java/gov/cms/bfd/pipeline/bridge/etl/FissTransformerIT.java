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
import gov.cms.mpsm.rda.v1.fiss.FissBeneZPayer;
import gov.cms.mpsm.rda.v1.fiss.FissBeneficiarySex;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import gov.cms.mpsm.rda.v1.fiss.FissClaimStatus;
import gov.cms.mpsm.rda.v1.fiss.FissDiagnosisCode;
import gov.cms.mpsm.rda.v1.fiss.FissPayer;
import gov.cms.mpsm.rda.v1.fiss.FissProcedureCode;
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

  @Data
  private static class TransformerArguments {
    private final Map<String, BeneficiaryData> mbiMap;
    private final WrappedMessage wrappedMessage;
    private final WrappedCounter wrappedCounter;
    private final Parser.Data<String> data;
    private final DataSampler<String> mbiSampler;
    private final int sampleId;
  }

  @Data
  private static class ExpectedValues {
    private final WrappedMessage wrappedMessage;
    private final WrappedCounter wrappedCounter;
    private final Optional<MessageOrBuilder> response;
    private final Set<String> sampledMbis;
  }

  private static Stream<Arguments> shouldProduceExpectedClaimsAndDataSamples() {
    return Stream.of(
        newFirstClaimTestCase(),
        newFirstClaimInvalidLineNumberCase(),
        recurringClaimCase(),
        recurringClaimInvalidLineNumberCase(),
        newNonFirstClaimCase());
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  void shouldProduceExpectedClaimsAndDataSamples(
      String testName,
      TransformerArguments arguments,
      ExpectedValues expectedValues,
      Exception expectedException) {
    try {
      final Set<String> jsonCompareIgnorePaths = Set.of("/timestamp");

      Optional<MessageOrBuilder> actualResponse =
          new FissTransformer(arguments.getMbiMap())
              .transform(
                  arguments.getWrappedMessage(),
                  arguments.getWrappedCounter(),
                  arguments.getData(),
                  arguments.getMbiSampler(),
                  arguments.getSampleId());

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

  private static Arguments newFirstClaimTestCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Parser.Data<String> data = TestData.createDataParser(TestData.createDefaultDataMap());
    WrappedMessage wrappedMessage = new WrappedMessage();
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();

    // Expected values
    FissClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedClaimChange =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();

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
    FissClaimChange expectedClaimChange =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();

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

  private static Arguments recurringClaimCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("CLM_LINE_NUM", "2");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    FissClaimChange recurringClaim =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(TestData.createDefaultClaimBuilder().build())
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(recurringClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.FISS_SAMPLE_ID, TestData.MBI);

    // Expected values
    FissClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedClaimChange =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();

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

  private static Arguments recurringClaimInvalidLineNumberCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("CLM_LINE_NUM", "3");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    FissClaimChange recurringClaim =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(TestData.createDefaultClaimBuilder().build())
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(recurringClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(1);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.FISS_SAMPLE_ID, TestData.MBI);

    // Expected values
    FissClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedClaimChange =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();

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

  private static Arguments newNonFirstClaimCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("FI_DOC_CLM_CNTL_NUM", "dcn87654321");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    FissClaimChange previouslyProcessedClaim =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(TestData.createDefaultClaimBuilder().build())
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(previouslyProcessedClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(2);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.FISS_SAMPLE_ID, TestData.MBI);

    // Expected values
    FissClaim expectedResponseClaim = TestData.createDefaultClaimBuilder().build();
    FissClaimChange expectedResponseClaimChange =
        FissClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedResponseClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn(TestData.FI_DOC_CLM_CNTL_NUM)
            .build();

    Optional<MessageOrBuilder> expectedResponse = Optional.of(expectedResponseClaimChange);

    FissClaim expectedWrappedClaim =
        TestData.createDefaultClaimBuilder().setDcn("dcn87654321").build();
    FissClaimChange expectedWrappedClaimChange =
        FissClaimChange.newBuilder()
            .setSeq(2)
            .setClaim(expectedWrappedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setDcn("dcn87654321")
            .build();

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

  private static class TestData {
    private static final String BENE_ID = "beneid";
    private static final String BENE_FIRST_NAME = "Firstname";
    private static final String BENE_LAST_NAME = "Lastname";
    private static final String BENE_MID_INIT = "M";
    private static final String BENE_DOB = "2020-01-01";
    private static final String BENE_GENDER = "1";
    private static final String FI_DOC_CLM_CNTL_NUM = "dcn12345678";
    private static final String HIC_NO = "hicno123";
    private static final String MBI = "mbimbimbimbi";
    private static final String ADMTG_DGNS_CD = "admitcd";
    private static final String CLM_FREQ_CD = "freqCode";
    private static final String CLM_FROM_DT = "01-Jan-2001";
    private static final int CLM_SRVC_CLSFCTN_TYPE_CD = 1;
    private static final String CLM_THRU_DT = "03-Mar-2001";
    private static final String CLM_TOT_CHRG_AMT = "3218.33";
    private static final int CLM_FAC_TYPE_CD = 8;
    private static final String ORG_NPI_NUM = "8888888888";
    private static final String PRNCPAL_DGNS_CD = "princode";
    private static final String PRVDR_NUM = "222222";
    private static final String CLM_ID = "-999999999";
    private static final String ICD_DGNS_CD1 = "JJJJ";
    private static final int CLM_POA_IND_SW1 = 1;
    private static final String ICD_PRCDR_CD1 = "pc1";
    private static final String PRCDR_DT1 = "10-Jan-2011";
    private static final String CLM_LINE_NUM = "1";

    private static final String HARDCODED_LOC1 = "?";
    private static final String HARDCODED_LOC2 = "?";
    private static final String HARDCODED_TRAN_DATE_CYMD = "1970-01-01";
    private static final String HARDCODED_FED_TAX_NUMBER = "XX-XXXXXXX";
    private static final String HARDCODED_RECEIVED_DATE_CYMD = "1970-01-01";

    private static final int FISS_SAMPLE_ID = 0;
    private static final int MCS_SAMPLE_ID = 1;

    public static FissClaim.Builder createDefaultClaimBuilder() {
      return FissClaim.newBuilder()
          .setDcn(FI_DOC_CLM_CNTL_NUM)
          .setHicNo(HIC_NO)
          .setMbi(MBI)
          .setCurrLoc1Unrecognized(HARDCODED_LOC1)
          .setCurrLoc2Unrecognized(HARDCODED_LOC2)
          .setCurrStatusEnum(FissClaimStatus.CLAIM_STATUS_ROUTING)
          .setCurrTranDtCymd(HARDCODED_TRAN_DATE_CYMD)
          .setFedTaxNb(HARDCODED_FED_TAX_NUMBER)
          .setRecdDtCymd(HARDCODED_RECEIVED_DATE_CYMD)
          .addFissPayers(
              FissPayer.newBuilder()
                  .setBeneZPayer(
                      FissBeneZPayer.newBuilder()
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
              FissProcedureCode.newBuilder().setProcCd(ICD_PRCDR_CD1).setProcDt(PRCDR_DT1).build())
          .addFissDiagCodes(
              FissDiagnosisCode.newBuilder()
                  .setDiagCd2(ICD_DGNS_CD1)
                  .setDiagPoaIndEnumValue(CLM_POA_IND_SW1)
                  .build())
          .setStmtCovFromCymd(CLM_FROM_DT)
          .setStmtCovToCymd(CLM_THRU_DT)
          .setMedaProv6(PRVDR_NUM)
          .setLobCdEnumValue(CLM_FAC_TYPE_CD)
          .setServTypeCdEnumValue(CLM_SRVC_CLSFCTN_TYPE_CD);
    }

    public static Parser.Data<String> createDataParser(Map<String, String> parserData) {
      return new Parser.Data<>() {
        private final Map<String, String> dataMap = parserData;

        @Override
        public long getEntryNumber() {
          return 1;
        }

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
          Map.entry("CLM_LINE_NUM", CLM_LINE_NUM));
    }

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

    public static DataSampler<String> createDefaultDataSampler() {
      return new DataSampler.Builder<String>()
          .registerSampleSet(FISS_SAMPLE_ID, 0.5F)
          .registerSampleSet(MCS_SAMPLE_ID, 0.5F)
          .maxValues(5)
          .build();
    }
  }
}
