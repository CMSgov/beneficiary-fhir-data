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
            + "  \"idrDtlCnt\": 3,\n"
            + "  \"idrBeneLast16\": \"trd\",\n"
            + "  \"idrBeneFirstInit\": \"d\",\n"
            + "  \"idrBeneMidInit\": \"h\",\n"
            + "  \"idrStatusCodeEnum\": \"STATUS_CODE_APPROVED_AND_PAID_SEPARATE_HISTORY\",\n"
            + "  \"idrStatusDate\": \"2021-04-30\",\n"
            + "  \"idrBillProvNpi\": \"s2xqbz\",\n"
            + "  \"idrBillProvNum\": \"6\",\n"
            + "  \"idrBillProvEin\": \"0h948\",\n"
            + "  \"idrBillProvType\": \"pg\",\n"
            + "  \"idrBillProvSpec\": \"rz\",\n"
            + "  \"idrBillProvPriceSpec\": \"8q\",\n"
            + "  \"idrBillProvCounty\": \"p9\",\n"
            + "  \"idrBillProvLoc\": \"7q\",\n"
            + "  \"idrTotAllowed\": \"7956.07\",\n"
            + "  \"idrCoinsurance\": \"3.32\",\n"
            + "  \"idrDeductible\": \"93.96\",\n"
            + "  \"idrTotBilledAmt\": \"87514.22\",\n"
            + "  \"idrClaimReceiptDate\": \"2021-03-07\",\n"
            + "  \"idrClaimMbi\": \"gdsb9b66k2v\",\n"
            + "  \"mcsDiagnosisCodes\": [{\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDiagCode\": \"wf2\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagCode\": \"xp3k0b\",\n"
            + "    \"idrDiagIcdTypeEnumUnrecognized\": \"t\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagCode\": \"2vgf\",\n"
            + "    \"idrDiagIcdTypeEnumUnrecognized\": \"b\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDiagCode\": \"jn3q6f\"\n"
            + "  }],\n"
            + "  \"mcsDetails\": [{\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_REJECTED\",\n"
            + "    \"idrDtlFromDate\": \"2021-03-11\",\n"
            + "    \"idrDtlToDate\": \"2021-05-07\",\n"
            + "    \"idrProcCode\": \"66s0\",\n"
            + "    \"idrModOne\": \"4r\",\n"
            + "    \"idrModTwo\": \"q\",\n"
            + "    \"idrModThree\": \"9\",\n"
            + "    \"idrModFour\": \"p2\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"rh\",\n"
            + "    \"idrKPosLnameOrg\": \"mrtggtbqzrdqrmkbvfxhnvxzgbvnvjhrcsjprv\",\n"
            + "    \"idrKPosFname\": \"gnkkxdtrjgjpbvcddmmrx\",\n"
            + "    \"idrKPosMname\": \"j\",\n"
            + "    \"idrKPosAddr1\": \"qh3c96zdpt8521vr\",\n"
            + "    \"idrKPosAddr21st\": \"9sdtkrmm3q85tsrxk06k\",\n"
            + "    \"idrKPosAddr22nd\": \"6xrsz5gm0xt8m1fhkg\",\n"
            + "    \"idrKPosCity\": \"bbtckhsxgghq\",\n"
            + "    \"idrKPosState\": \"z\",\n"
            + "    \"idrKPosZip\": \"215465\",\n"
            + "    \"idrDtlDiagIcdTypeUnrecognized\": \"t\"\n"
            + "  }, {\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_DELETED\",\n"
            + "    \"idrDtlFromDate\": \"2021-02-03\",\n"
            + "    \"idrDtlToDate\": \"2021-04-29\",\n"
            + "    \"idrProcCode\": \"06\",\n"
            + "    \"idrModOne\": \"g\",\n"
            + "    \"idrModTwo\": \"fg\",\n"
            + "    \"idrModThree\": \"4\",\n"
            + "    \"idrModFour\": \"4\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"h188107\",\n"
            + "    \"idrKPosLnameOrg\": \"m\",\n"
            + "    \"idrKPosFname\": \"w\",\n"
            + "    \"idrKPosMname\": \"bchhjgrktwqrtkmhgzvpxtnm\",\n"
            + "    \"idrKPosAddr1\": \"2ns1p4\",\n"
            + "    \"idrKPosAddr21st\": \"9hc5ft3rk49cd\",\n"
            + "    \"idrKPosAddr22nd\": \"ftsdx4kt02v1zxw89k5b31z\",\n"
            + "    \"idrKPosCity\": \"gjsnhvvbpqckqkmgqmszqckdhtxm\",\n"
            + "    \"idrKPosState\": \"c\",\n"
            + "    \"idrKPosZip\": \"489908477\"\n"
            + "  }, {\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_PENDING\",\n"
            + "    \"idrDtlFromDate\": \"2021-03-23\",\n"
            + "    \"idrDtlToDate\": \"2021-06-27\",\n"
            + "    \"idrProcCode\": \"nj0\",\n"
            + "    \"idrModOne\": \"3j\",\n"
            + "    \"idrModTwo\": \"z\",\n"
            + "    \"idrModThree\": \"b\",\n"
            + "    \"idrModFour\": \"bt\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"9p19kn4\",\n"
            + "    \"idrKPosLnameOrg\": \"fzsjfvsxfvrrthpmqgzchktgcnhgr\",\n"
            + "    \"idrKPosFname\": \"djsrfs\",\n"
            + "    \"idrKPosMname\": \"zmqggvvsskfwbxgkjc\",\n"
            + "    \"idrKPosAddr1\": \"n6mkjqnxr52b2rkwhd2f16ft56t3wn999fm623744mvzmq\",\n"
            + "    \"idrKPosAddr21st\": \"8r0xxv5gxnn4nnnq706bbxz2s\",\n"
            + "    \"idrKPosAddr22nd\": \"m14px5zg7g85c\",\n"
            + "    \"idrKPosCity\": \"wphbnjzzmrsff\",\n"
            + "    \"idrKPosState\": \"tg\",\n"
            + "    \"idrKPosZip\": \"934003261159817\"\n"
            + "  }],\n"
            + "  \"idrClaimTypeUnrecognized\": \"n\",\n"
            + "  \"idrBeneSexUnrecognized\": \"b\",\n"
            + "  \"idrBillProvGroupIndUnrecognized\": \"t\",\n"
            + "  \"idrBillProvStatusCdUnrecognized\": \"r\",\n"
            + "  \"idrHdrFromDos\": \"2021-02-03\",\n"
            + "  \"idrHdrToDos\": \"2021-06-27\"\n"
            + "}",
        json);
  }
}
