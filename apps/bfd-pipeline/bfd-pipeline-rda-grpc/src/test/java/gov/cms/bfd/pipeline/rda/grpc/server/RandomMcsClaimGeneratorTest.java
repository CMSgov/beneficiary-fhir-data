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
            + "  \"idrStatusCodeEnum\": \"STATUS_CODE_ACTIVE_SEPARATE_HISTORY\",\n"
            + "  \"idrStatusDate\": \"2021-05-05\",\n"
            + "  \"idrBillProvNpi\": \"cs2\",\n"
            + "  \"idrBillProvNum\": \"4601656\",\n"
            + "  \"idrBillProvEin\": \"9\",\n"
            + "  \"idrBillProvType\": \"n\",\n"
            + "  \"idrBillProvSpec\": \"jd\",\n"
            + "  \"idrBillProvPriceSpec\": \"w\",\n"
            + "  \"idrBillProvCounty\": \"c\",\n"
            + "  \"idrBillProvLoc\": \"qc\",\n"
            + "  \"idrTotAllowed\": \"12749.95\",\n"
            + "  \"idrCoinsurance\": \"0.69\",\n"
            + "  \"idrDeductible\": \"322.93\",\n"
            + "  \"idrBillProvStatusCdEnum\": \"BILLING_PROVIDER_STATUS_CODE_PARTICIPATING\",\n"
            + "  \"idrTotBilledAmt\": \"0.01\",\n"
            + "  \"idrClaimReceiptDate\": \"2021-03-06\",\n"
            + "  \"idrClaimMbi\": \"5qbm7h0\",\n"
            + "  \"mcsDiagnosisCodes\": [{\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDiagCode\": \"b9b6\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDiagCode\": \"v3\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagCode\": \"f24\",\n"
            + "    \"idrDiagIcdTypeEnumUnrecognized\": \"k\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDiagCode\": \"xp3k0b\"\n"
            + "  }, {\n"
            + "    \"idrClmHdIcn\": \"84555\",\n"
            + "    \"idrDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDiagCode\": \"s2vgfx\"\n"
            + "  }],\n"
            + "  \"mcsDetails\": [{\n"
            + "    \"idrDtlFromDate\": \"2021-02-03\",\n"
            + "    \"idrDtlToDate\": \"2021-04-24\",\n"
            + "    \"idrProcCode\": \"6f\",\n"
            + "    \"idrModOne\": \"zx\",\n"
            + "    \"idrModTwo\": \"2\",\n"
            + "    \"idrModThree\": \"6s\",\n"
            + "    \"idrModFour\": \"g4\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD9\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"x9q\",\n"
            + "    \"idrKPosLnameOrg\": \"hkjtsdcjmrtggtbqzrdqrmkbvfxhnvxzgbvnvjhrcsjprvjgnkkxdtrjgj\",\n"
            + "    \"idrKPosFname\": \"bvcdd\",\n"
            + "    \"idrKPosMname\": \"mr\",\n"
            + "    \"idrKPosAddr1\": \"qhvqh3c96zdpt8521vrb9sdtkrmm3q85tsrxk06k06\",\n"
            + "    \"idrKPosAddr21st\": \"rsz5gm0xt8m1fhkgs7130q\",\n"
            + "    \"idrKPosAddr22nd\": \"5p\",\n"
            + "    \"idrKPosCity\": \"ghqkzchskzft\",\n"
            + "    \"idrKPosState\": \"nk\",\n"
            + "    \"idrKPosZip\": \"77477992440000\",\n"
            + "    \"idrDtlStatusUnrecognized\": \"b\"\n"
            + "  }, {\n"
            + "    \"idrDtlStatusEnum\": \"DETAIL_STATUS_SENT\",\n"
            + "    \"idrDtlFromDate\": \"2021-01-19\",\n"
            + "    \"idrDtlToDate\": \"2021-04-13\",\n"
            + "    \"idrProcCode\": \"107gz\",\n"
            + "    \"idrModOne\": \"n\",\n"
            + "    \"idrModTwo\": \"x\",\n"
            + "    \"idrModThree\": \"v\",\n"
            + "    \"idrModFour\": \"vw\",\n"
            + "    \"idrDtlDiagIcdTypeEnum\": \"DIAGNOSIS_ICD_TYPE_ICD10\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"69\",\n"
            + "    \"idrKPosLnameOrg\": \"tkmhgzvpxtnmscmxcxpcmvcwdbbggrqspvhfsrsftwkxvrqnwggjpdvdqcgj\",\n"
            + "    \"idrKPosFname\": \"nhvvbpqckqk\",\n"
            + "    \"idrKPosMname\": \"gqmsz\",\n"
            + "    \"idrKPosAddr1\": \"k56gg184r764zdhn70st6z50nj073j4zjbmbt6439p19kn4mf56kkvf\",\n"
            + "    \"idrKPosAddr21st\": \"t24x\",\n"
            + "    \"idrKPosAddr22nd\": \"0mpd4ttn0tp9pfvkkh1\",\n"
            + "    \"idrKPosCity\": \"rfszzmqggvvsskfwbxgkjcndfz\",\n"
            + "    \"idrKPosState\": \"j\",\n"
            + "    \"idrKPosZip\": \"603472001\"\n"
            + "  }, {\n"
            + "    \"idrDtlFromDate\": \"2021-01-26\",\n"
            + "    \"idrDtlToDate\": \"2021-05-19\",\n"
            + "    \"idrProcCode\": \"6ft\",\n"
            + "    \"idrModOne\": \"6\",\n"
            + "    \"idrModTwo\": \"3w\",\n"
            + "    \"idrModThree\": \"99\",\n"
            + "    \"idrModFour\": \"fm\",\n"
            + "    \"idrDtlPrimaryDiagCode\": \"4\",\n"
            + "    \"idrKPosLnameOrg\": \"bjjdrsgvhgdjqqqtzssmznctfdpkqbzzdvgzfmb\",\n"
            + "    \"idrKPosFname\": \"jgccv\",\n"
            + "    \"idrKPosMname\": \"wphbnjzzmrsffktghppzpnq\",\n"
            + "    \"idrKPosAddr1\": \"0g3xwgqvsbsmg1639wtf9874v7n\",\n"
            + "    \"idrKPosAddr21st\": \"dfftt5hq678b79220qkrpj787q5gc\",\n"
            + "    \"idrKPosAddr22nd\": \"w030hphj5rzf\",\n"
            + "    \"idrKPosCity\": \"qpmkxcqmtdnvxrxjwxzdm\",\n"
            + "    \"idrKPosState\": \"g\",\n"
            + "    \"idrKPosZip\": \"43774863768\",\n"
            + "    \"idrDtlDiagIcdTypeUnrecognized\": \"b\",\n"
            + "    \"idrDtlStatusUnrecognized\": \"x\"\n"
            + "  }],\n"
            + "  \"idrBillProvGroupIndUnrecognized\": \"s\",\n"
            + "  \"idrHdrFromDos\": \"2021-01-19\",\n"
            + "  \"idrHdrToDos\": \"2021-05-19\"\n"
            + "}",
        json);
  }
}
