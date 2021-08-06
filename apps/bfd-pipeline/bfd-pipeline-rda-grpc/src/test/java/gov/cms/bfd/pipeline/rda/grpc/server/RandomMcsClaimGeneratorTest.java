package gov.cms.bfd.pipeline.rda.grpc.server;

import static junit.framework.TestCase.assertEquals;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
import gov.cms.mpsm.rda.v1.mcs.McsDiagnosisCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

public class RandomMcsClaimGeneratorTest {
  @Test
  public void randomClaim() throws InvalidProtocolBufferException {
    final Clock july1 = Clock.fixed(Instant.ofEpochMilli(1625172944844L), ZoneOffset.UTC);
    final RandomMcsClaimGenerator generator = new RandomMcsClaimGenerator(1, true, july1);
    final McsClaim claim = generator.randomClaim();
    final String json = JsonFormat.printer().print(claim);
    assertEquals(claim.getIdrDtlCnt(), claim.getMcsDetailsList().size());
    for (McsDiagnosisCode diagnosisCode : claim.getMcsDiagnosisCodesList()) {
      assertEquals(claim.getIdrClmHdIcn(), diagnosisCode.getIdrClmHdIcn());
    }
    assertEquals(
        "{\n"
            + "  \"idrClmHdIcn\": \"84555\",\n"
            + "  \"idrContrId\": \"99\",\n"
            + "  \"idrHic\": \"48\",\n"
            + "  \"idrClaimTypeEnum\": \"CLAIM_TYPE_MEDICAL\",\n"
            + "  \"idrDtlCnt\": 3,\n"
            + "  \"idrBeneLast16\": \"mtrdh\",\n"
            + "  \"idrBeneFirstInit\": \"t\",\n"
            + "  \"idrBeneMidInit\": \"g\",\n"
            + "  \"idrBeneSexEnum\": \"BENEFICIARY_SEX_FEMALE\",\n"
            + "  \"idrStatusDate\": \"2021-06-06\",\n"
            + "  \"idrBillProvNpi\": \"2xqbz\",\n"
            + "  \"idrBillProvNum\": \"6\",\n"
            + "  \"idrBillProvEin\": \"0h948\",\n"
            + "  \"idrBillProvType\": \"pg\",\n"
            + "  \"idrBillProvSpec\": \"rz\",\n"
            + "  \"idrBillProvGroupIndEnum\": \"BILLING_PROVIDER_INDICATOR_GROUP\",\n"
            + "  \"idrBillProvPriceSpec\": \"c\",\n"
            + "  \"idrBillProvCounty\": \"qc\",\n"
            + "  \"idrBillProvLoc\": \"9\",\n"
            + "  \"idrTotAllowed\": \"7.97\",\n"
            + "  \"idrCoinsurance\": \"6907.93\",\n"
            + "  \"idrDeductible\": \"228.32\",\n"
            + "  \"idrTotBilledAmt\": \"11875.62\",\n"
            + "  \"idrClaimReceiptDate\": \"2021-06-30\",\n"
            + "  \"idrClaimMbi\": \"8gdsb9\",\n"
            + "  \"mcsDiagnosisCodes\": [{\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDiagCode\": \"2v3fs2\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDiagCode\": \"4p5mxp\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagCode\": \"th5\",\n"
            + "    \"idrDiagIcdTypeEnumUnrecognized\": \"n\"\n"
            + "  }],\n"
            + "  \"mcsDetails\": [{\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_PENDING\",\n"
            + "    \"idrDtlFromDate\": \"2021-06-06\",\n"
            + "    \"idrDtlToDate\": \"2021-06-16\",\n"
            + "    \"idrProcCode\": \"x1\",\n"
            + "    \"idrModOne\": \"jn\",\n"
            + "    \"idrModTwo\": \"q\",\n"
            + "    \"idrModThree\": \"f\",\n"
            + "    \"idrModFour\": \"zx\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"6s0g\",\n"
            + "    \"idrKPosLnameOrg\": \"kdghzschkjtsdcjm\",\n"
            + "    \"idrKPosFname\": \"tggtbqzrdqrmkbvfxhnvxzg\",\n"
            + "    \"idrKPosMname\": \"vnvjhrc\",\n"
            + "    \"idrKPosAddr1\": \"hn0466frjsg26\",\n"
            + "    \"idrKPosAddr21st\": \"38pgr\",\n"
            + "    \"idrKPosAddr22nd\": \"3s4wb7qhvqh3c96z\",\n"
            + "    \"idrKPosCity\": \"bxvhsmrzhhxdxfwjfnjqpqshqgkwwz\",\n"
            + "    \"idrKPosState\": \"h\",\n"
            + "    \"idrKPosZip\": \"16430749294607\"\n"
            + "  }, {\n"
            + "    \"idrDtlFromDate\": \"2021-06-03\",\n"
            + "    \"idrDtlToDate\": \"2021-06-27\",\n"
            + "    \"idrProcCode\": \"q5\",\n"
            + "    \"idrModOne\": \"p5\",\n"
            + "    \"idrModTwo\": \"q\",\n"
            + "    \"idrModThree\": \"s8\",\n"
            + "    \"idrModFour\": \"c\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"cg\",\n"
            + "    \"idrKPosLnameOrg\": \"ktffjxrqjhdfhxjsndmcckffzmcwtbchhjgrktwqrtkmhgzvpxtnms\",\n"
            + "    \"idrKPosFname\": \"mxcxpcmvcwd\",\n"
            + "    \"idrKPosMname\": \"bggrqspvhfsrsftwkxvrqn\",\n"
            + "    \"idrKPosAddr1\": \"89k5b31zq84dpnkvxbx79m54mr7sj6k56gg184r\",\n"
            + "    \"idrKPosAddr21st\": \"64zdhn70st6z50nj073j4zjb\",\n"
            + "    \"idrKPosAddr22nd\": \"bt6439p19kn4mf56kkvfgt24x\",\n"
            + "    \"idrKPosCity\": \"hpmqgzchktgcnhgrfdjsrfszzmqgg\",\n"
            + "    \"idrKPosState\": \"v\",\n"
            + "    \"idrKPosZip\": \"17100\",\n"
            + "    \"idrDtlDiagIcdTypeUnrecognized\": \"z\",\n"
            + "    \"idrDtlStatusUnrecognized\": \"b\"\n"
            + "  }, {\n"
            + "    \"idrDtlFromDate\": \"2021-03-14\",\n"
            + "    \"idrDtlToDate\": \"2021-06-22\",\n"
            + "    \"idrProcCode\": \"n\",\n"
            + "    \"idrModOne\": \"mk\",\n"
            + "    \"idrModTwo\": \"q\",\n"
            + "    \"idrModThree\": \"xr\",\n"
            + "    \"idrModFour\": \"2b\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"hd2f16\",\n"
            + "    \"idrKPosLnameOrg\": \"qcmbgjzxbnbkfhbwrgbjjdrsgvhgd\",\n"
            + "    \"idrKPosFname\": \"qqqtzssmznctfdpkqbzzdvgzfmbnjgcc\",\n"
            + "    \"idrKPosMname\": \"rwphbn\",\n"
            + "    \"idrKPosAddr1\": \"dkp1xkrq1nwb6bj92w0g3xwgqvsbsmg16\",\n"
            + "    \"idrKPosAddr21st\": \"9wtf9874v7njdfftt5hq678b792\",\n"
            + "    \"idrKPosAddr22nd\": \"0qkr\",\n"
            + "    \"idrKPosCity\": \"hxbkzmdcpzthrdhpcrwdjgqpmkxcqm\",\n"
            + "    \"idrKPosState\": \"dn\",\n"
            + "    \"idrKPosZip\": \"91905912403643\",\n"
            + "    \"idrDtlDiagIcdTypeUnrecognized\": \"z\",\n"
            + "    \"idrDtlStatusUnrecognized\": \"k\"\n"
            + "  }],\n"
            + "  \"idrStatusCodeUnrecognized\": \"p\",\n"
            + "  \"idrBillProvStatusCdUnrecognized\": \"p\",\n"
            + "  \"idrHdrFromDos\": \"2021-03-14\",\n"
            + "  \"idrHdrToDos\": \"2021-06-27\"\n"
            + "}",
        json);
  }
}
