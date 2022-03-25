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
          new McsTransformer(arguments.getMbiMap())
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
    McsClaim expectedClaim = TestData.createDefaultClaimBuilder().build();
    McsClaimChange expectedClaimChange =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
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
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

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
    McsClaimChange expectedClaimChange =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
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
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  private static Arguments recurringClaimCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("LINE_NUM", "2");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    McsClaimChange recurringClaim =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(TestData.createDefaultClaimBuilder().build())
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
            .build();
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
                    .build())
            .build();
    McsClaimChange expectedClaimChange =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
            .build();

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

  private static Arguments recurringClaimInvalidLineNumberCase() {
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("LINE_NUM", "3");
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    McsClaimChange recurringClaim =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(TestData.createDefaultClaimBuilder().build())
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
            .build();
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
                    .build())
            .build();
    McsClaimChange expectedClaimChange =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
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
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, Optional.empty(), expectedSampledMbis),
        expectedException);
  }

  private static Arguments newNonFirstClaimCase() {
    final String NEW_CLAIM_ICN = "icn87654321";
    Map<String, BeneficiaryData> mbiMap = TestData.createDefaultMbiMap();
    Map<String, String> dataMap = new HashMap<>(TestData.createDefaultDataMap());
    dataMap.put("CARR_CLM_CNTL_NUM", NEW_CLAIM_ICN);
    Parser.Data<String> data = TestData.createDataParser(dataMap);
    McsClaimChange previouslyProcessedClaim =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(TestData.createDefaultClaimBuilder().build())
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
            .build();
    WrappedMessage wrappedMessage = new WrappedMessage();
    wrappedMessage.setLineNumber(1);
    wrappedMessage.setMessage(previouslyProcessedClaim);
    WrappedCounter wrappedCounter = new WrappedCounter(2);
    DataSampler<String> dataSampler = TestData.createDefaultDataSampler();
    dataSampler.add(TestData.MCS_SAMPLE_ID, TestData.MBI);

    // Expected values
    McsClaim expectedResponseClaim = TestData.createDefaultClaimBuilder().build();
    McsClaimChange expectedResponseClaimChange =
        McsClaimChange.newBuilder()
            .setSeq(1)
            .setClaim(expectedResponseClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(TestData.CARR_CLM_CNTL_NUM)
            .build();

    Optional<MessageOrBuilder> expectedResponse = Optional.of(expectedResponseClaimChange);

    McsClaim.Builder defaultClaim = TestData.createDefaultClaimBuilder();
    McsClaim expectedWrappedClaim =
        defaultClaim
            .setIdrClmHdIcn(NEW_CLAIM_ICN)
            .setMcsDiagnosisCodes(
                0,
                defaultClaim
                    .getMcsDiagnosisCodes(0)
                    .toBuilder()
                    .setIdrClmHdIcn(NEW_CLAIM_ICN)
                    .build())
            .build();
    McsClaimChange expectedWrappedClaimChange =
        McsClaimChange.newBuilder()
            .setSeq(2)
            .setClaim(expectedWrappedClaim)
            .setChangeType(ChangeType.CHANGE_TYPE_UPDATE)
            .setIcn(NEW_CLAIM_ICN)
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
            mbiMap, wrappedMessage, wrappedCounter, data, dataSampler, TestData.MCS_SAMPLE_ID),
        new ExpectedValues(
            expectedWrappedMessage, expectedWrappedCounter, expectedResponse, expectedSampledMbis),
        expectedException);
  }

  private static class TestData {
    private static final String LINE_NUM = "1";
    private static final String BENE_ID = "beneid";
    private static final String BENE_FIRST_NAME = "F";
    private static final String BENE_LAST_NAME = "Lastna";
    private static final String BENE_MID_INIT = "M";
    private static final String BENE_DOB = "2020-01-01";
    private static final int BENE_GENDER = 1;
    private static final String CARR_CLM_CNTL_NUM = "icn12345678";
    private static final String CLM_FROM_DT = "01-Jan-2001";
    private static final String CLM_THRU_DT = "03-Mar-2001";
    private static final String NCH_CARR_CLM_SBMTD_CHRG_AMT = "832.12";
    private static final String ORG_NPI_NUM = "8888888888";
    private static final String CLM_ID = "-999999999";
    private static final String ICD_DGNS_CD1 = "JJJJ";
    private static final String ICD_DGNS_VRSN_CD1 = "0";
    private static final String LINE_ICD_DGNS_CD = "12";
    private static final String LINE_ICD_DGNS_VRSN_CD = "0";
    private static final String HCPCS_CD = "123";
    private static final String HCPCS_1ST_MDFR_CD = "abc";
    private static final String HCPCS_2ND_MDFR_CD = "cba";
    private static final String LINE_1ST_EXPNS_DT = "20-Feb-2008";
    private static final String LINE_LAST_EXPNS_DT = "30-Jun-2008";
    private static final String MBI = "mbimbimbimbi";

    private static final String HARDCODED_BILL_PROV_EIN = "XX-XXXXXXX";
    private static final String HARDCODED_BILL_PROV_SPEC = "01";
    private static final String HARDCODED_BILL_PROV_TYPE = "20";
    private static final String HARDCODED_RECEIVED_DATE_CYMD = "1970-01-01";
    private static final String HARDCODED_CONTROL_ID = "00000";
    private static final String HARDCODED_STATUS_DATE = "1970-01-01";

    private static final int FISS_SAMPLE_ID = 0;
    private static final int MCS_SAMPLE_ID = 1;

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
          .setIdrBeneSexEnumValue(BENE_GENDER - 1)
          .setIdrBillProvNpi(ORG_NPI_NUM)
          .setIdrTotBilledAmt(NCH_CARR_CLM_SBMTD_CHRG_AMT)
          .addMcsDiagnosisCodes(
              McsDiagnosisCode.newBuilder()
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
                  .build())
          .setIdrHdrFromDos(CLM_FROM_DT)
          .setIdrHdrToDos(CLM_THRU_DT);
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
              String.valueOf(BENE_GENDER)));
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
