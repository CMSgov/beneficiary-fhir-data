package gov.cms.bfd.pipeline.rda.grpc.server;

import static junit.framework.TestCase.assertEquals;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.Test;

public class RandomFissClaimGeneratorTest {
  @Test
  public void randomClaim() throws InvalidProtocolBufferException {
    final Clock july1 = Clock.fixed(Instant.ofEpochMilli(1625172944844L), ZoneOffset.UTC);
    final RandomFissClaimGenerator generator = new RandomFissClaimGenerator(1, true, july1);
    final FissClaim claim = generator.randomClaim();
    final String json = JsonFormat.printer().print(claim);
    assertEquals(
        "{\n"
            + "  \"dcn\": \"9845557\",\n"
            + "  \"hicNo\": \"904843533707\",\n"
            + "  \"currLoc1Enum\": \"PROCESSING_TYPE_OFFLINE\",\n"
            + "  \"totalChargeAmount\": \"1.34\",\n"
            + "  \"recdDtCymd\": \"2021-03-07\",\n"
            + "  \"currTranDtCymd\": \"2021-04-17\",\n"
            + "  \"admDiagCode\": \"qwt\",\n"
            + "  \"principleDiag\": \"skz\",\n"
            + "  \"npiNumber\": \"5127497956\",\n"
            + "  \"mbi\": \"z2sssv462x9vx\",\n"
            + "  \"fedTaxNb\": \"6006018751\",\n"
            + "  \"fissProcCodes\": [{\n"
            + "    \"procCd\": \"skz\",\n"
            + "    \"procFlag\": \"tknk\",\n"
            + "    \"procDt\": \"2021-02-03\"\n"
            + "  }, {\n"
            + "    \"procCd\": \"f\",\n"
            + "    \"procFlag\": \"x\",\n"
            + "    \"procDt\": \"2021-03-07\"\n"
            + "  }, {\n"
            + "    \"procCd\": \"j\",\n"
            + "    \"procFlag\": \"dfhx\",\n"
            + "    \"procDt\": \"2021-01-23\"\n"
            + "  }, {\n"
            + "    \"procCd\": \"ndmcc\",\n"
            + "    \"procFlag\": \"ffzm\",\n"
            + "    \"procDt\": \"2021-06-26\"\n"
            + "  }],\n"
            + "  \"medaProvId\": \"xqbzp5w0h948k\",\n"
            + "  \"pracLocAddr1\": \"m7h08gdsb9b66k2v3fs2wf24p5mxp3k0bth5s2vgfx1njn3q6ftzx2\",\n"
            + "  \"pracLocAddr2\": \"66s0g4rqqx9qp24pb3rhw165ttc3cdnq42s3tkjz1hgc8hrc5p47khhhn0466frj\",\n"
            + "  \"pracLocCity\": \"g26038pgrm3s4wb7qhvqh3c96zdpt8521vrb9sdtkrmm3q85tsrxk06k06xrsz5gm0xt8\",\n"
            + "  \"pracLocState\": \"mm\",\n"
            + "  \"pracLocZip\": \"792959727042516\",\n"
            + "  \"fissDiagCodes\": [{\n"
            + "    \"diagCd2\": \"bchhjgr\",\n"
            + "    \"bitFlags\": \"rtkm\",\n"
            + "    \"diagPoaIndUnrecognized\": \"w\"\n"
            + "  }, {\n"
            + "    \"diagCd2\": \"gzvpxtn\",\n"
            + "    \"diagPoaIndEnum\": \"DIAGNOSIS_PRESENT_ON_ADMISSION_INDICATOR_UNKNOWN\",\n"
            + "    \"bitFlags\": \"mxc\"\n"
            + "  }, {\n"
            + "    \"diagCd2\": \"pcm\",\n"
            + "    \"bitFlags\": \"bb\",\n"
            + "    \"diagPoaIndUnrecognized\": \"w\"\n"
            + "  }, {\n"
            + "    \"diagCd2\": \"grq\",\n"
            + "    \"diagPoaIndEnum\": \"DIAGNOSIS_PRESENT_ON_ADMISSION_INDICATOR_CLINICALLY_UNDETERMINED\",\n"
            + "    \"bitFlags\": \"hfsr\"\n"
            + "  }, {\n"
            + "    \"diagCd2\": \"f\",\n"
            + "    \"diagPoaIndEnum\": \"DIAGNOSIS_PRESENT_ON_ADMISSION_INDICATOR_UNREPORTED\",\n"
            + "    \"bitFlags\": \"xvrq\"\n"
            + "  }, {\n"
            + "    \"diagCd2\": \"wggjpdv\",\n"
            + "    \"bitFlags\": \"j\",\n"
            + "    \"diagPoaIndUnrecognized\": \"c\"\n"
            + "  }],\n"
            + "  \"currStatusUnrecognized\": \"h\",\n"
            + "  \"currLoc2Unrecognized\": \"ppsm\",\n"
            + "  \"stmtCovFromCymd\": \"2021-03-19\",\n"
            + "  \"stmtCovToCymd\": \"2021-04-05\"\n"
            + "}",
        json);
  }
}
