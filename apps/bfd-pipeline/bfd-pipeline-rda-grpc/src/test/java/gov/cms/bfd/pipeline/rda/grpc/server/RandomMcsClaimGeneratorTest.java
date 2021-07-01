package gov.cms.bfd.pipeline.rda.grpc.server;

import static junit.framework.TestCase.assertEquals;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.mcs.McsClaim;
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
    assertEquals(
        "{\n"
            + "  \"idrClmHdIcn\": \"v4ghjf6\",\n"
            + "  \"idrContrId\": \"33707\",\n"
            + "  \"idrHic\": \"84555\",\n"
            + "  \"idrClaimTypeEnum\": \"CLAIM_TYPE_ADJUSTMENT\",\n"
            + "  \"idrDtlCnt\": 3,\n"
            + "  \"idrBeneLast16\": \"gcbwpp\",\n"
            + "  \"idrBeneFirstInit\": \"m\",\n"
            + "  \"idrBeneMidInit\": \"v\",\n"
            + "  \"idrStatusCodeEnum\": \"STATUS_CODE_APPROVED_AWAITING_CWF_RESPONSE_SEPARATE_HISTORY\",\n"
            + "  \"idrStatusDate\": \"2021-02-17\",\n"
            + "  \"idrBillProvNpi\": \"h948kj\",\n"
            + "  \"idrBillProvNum\": \"183476769\",\n"
            + "  \"idrBillProvEin\": \"p957qtc8f2\",\n"
            + "  \"idrBillProvType\": \"t\",\n"
            + "  \"idrBillProvSpec\": \"2s\",\n"
            + "  \"idrBillProvPriceSpec\": \"6\",\n"
            + "  \"idrBillProvCounty\": \"x\",\n"
            + "  \"idrBillProvLoc\": \"v\",\n"
            + "  \"idrTotAllowed\": \"60.60\",\n"
            + "  \"idrCoinsurance\": \"7.14\",\n"
            + "  \"idrDeductible\": \"2.15\",\n"
            + "  \"idrTotBilledAmt\": \"94488.24\",\n"
            + "  \"idrClaimReceiptDate\": \"2021-05-05\",\n"
            + "  \"idrClaimMbi\": \"f2\",\n"
            + "  \"mcsDiagnosisCodes\": [{\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagCode\": \"p3k\",\n"
            + "    \"idrDiagIcdTypeEnumUnrecognized\": \"n\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDiagCode\": \"h5s2v\"\n"
            + "  }],\n"
            + "  \"mcsDetails\": [{\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_FINAL\",\n"
            + "    \"idrDtlFromDate\": \"2021-01-18\",\n"
            + "    \"idrDtlToDate\": \"2021-05-11\",\n"
            + "    \"idrProcCode\": \"jn\",\n"
            + "    \"idrModOne\": \"q\",\n"
            + "    \"idrModTwo\": \"f\",\n"
            + "    \"idrModThree\": \"zx\",\n"
            + "    \"idrModFour\": \"2\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"0g4rqqx\",\n"
            + "    \"idrKPosLnameOrg\": \"qp24pb3rhw165ttc3cdnq42s3tkjz1\",\n"
            + "    \"idrKPosFname\": \"gc8hrc5p47khhhn0\",\n"
            + "    \"idrKPosMname\": \"66frjsg26038pgrm3s4\",\n"
            + "    \"idrKPosAddr1\": \"b7qhvqh3c96zdpt8521vrb9sdtkrmm3q85tsrxk06k\",\n"
            + "    \"idrKPosAddr21st\": \"6xr\",\n"
            + "    \"idrKPosAddr22nd\": \"z5gm0xt8m1f\",\n"
            + "    \"idrKPosCity\": \"kgs7130q55p5fq7s8bcsxfrcghqkg0\",\n"
            + "    \"idrKPosState\": \"x\",\n"
            + "    \"idrKPosZip\": \"992440000384\"\n"
            + "  }, {\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_REJECTED\",\n"
            + "    \"idrDtlFromDate\": \"2021-02-25\",\n"
            + "    \"idrDtlToDate\": \"2021-04-03\",\n"
            + "    \"idrProcCode\": \"gzz\",\n"
            + "    \"idrModOne\": \"n\",\n"
            + "    \"idrModTwo\": \"9\",\n"
            + "    \"idrModThree\": \"5\",\n"
            + "    \"idrModFour\": \"ws\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"9\",\n"
            + "    \"idrKPosLnameOrg\": \"fchwwgghm8qjf2ns1p429hc5ft3rk49cdhftsdx4kt02v1zxw89k5b31zq84\",\n"
            + "    \"idrKPosFname\": \"pnkvxbx79m5\",\n"
            + "    \"idrKPosMname\": \"mr7sj\",\n"
            + "    \"idrKPosAddr1\": \"k56gg184r764zdhn70st6z50nj073j4zjbmbt6439p19kn4mf56kkvf\",\n"
            + "    \"idrKPosAddr21st\": \"t24x\",\n"
            + "    \"idrKPosAddr22nd\": \"0mpd4ttn0tp9pfvkkh1\",\n"
            + "    \"idrKPosCity\": \"msg1mvmvt12dc24p9p4tq8vn6m\",\n"
            + "    \"idrKPosState\": \"j\",\n"
            + "    \"idrKPosZip\": \"603472001\"\n"
            + "  }, {\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_DELETED\",\n"
            + "    \"idrDtlFromDate\": \"2021-01-26\",\n"
            + "    \"idrDtlToDate\": \"2021-03-04\",\n"
            + "    \"idrProcCode\": \"16ft\",\n"
            + "    \"idrModOne\": \"6\",\n"
            + "    \"idrModTwo\": \"3w\",\n"
            + "    \"idrModThree\": \"99\",\n"
            + "    \"idrModFour\": \"fm\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"744mvz\",\n"
            + "    \"idrKPosLnameOrg\": \"qx8r0xxv5gxnn4nnnq706bbxz2s2m1\",\n"
            + "    \"idrKPosFname\": \"px5zg7g85c7bh1xhtdkp1xkrq1\",\n"
            + "    \"idrKPosMname\": \"wb6bj92w0g3xwgqvsbsmg16\",\n"
            + "    \"idrKPosAddr1\": \"9wtf9874v7njdfftt5hq678b792\",\n"
            + "    \"idrKPosAddr21st\": \"0qkrpj787q5gc2\",\n"
            + "    \"idrKPosAddr22nd\": \"030hphj5rzf479sb7zmkp5\",\n"
            + "    \"idrKPosCity\": \"h25t42b8smzn0nknb5k8sj\",\n"
            + "    \"idrKPosState\": \"q\",\n"
            + "    \"idrKPosZip\": \"15450353\"\n"
            + "  }],\n"
            + "  \"idrBeneSexUnrecognized\": \"j\",\n"
            + "  \"idrBillProvGroupIndUnrecognized\": \"r\",\n"
            + "  \"idrBillProvStatusCdUnrecognized\": \"t\",\n"
            + "  \"idrHdrFromDos\": \"2021-01-18\",\n"
            + "  \"idrHdrToDos\": \"2021-05-11\"\n"
            + "}",
        json);
  }
}
