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
            + "  \"currStatusEnum\": \"CLAIM_STATUS_DENIED\",\n"
            + "  \"currLoc2Enum\": \"CURRENT_LOCATION_2_POST_PAY_7545\",\n"
            + "  \"totalChargeAmount\": \"831.18\",\n"
            + "  \"recdDtCymd\": \"2021-01-10\",\n"
            + "  \"currTranDtCymd\": \"2021-05-29\",\n"
            + "  \"admDiagCode\": \"tpqw\",\n"
            + "  \"principleDiag\": \"jprv\",\n"
            + "  \"npiNumber\": \"5512749795\",\n"
            + "  \"mbi\": \"2z2sssv462x9v\",\n"
            + "  \"fedTaxNb\": \"9600601875\",\n"
            + "  \"fissProcCodes\": [{\n"
            + "    \"procCd\": \"jprv\",\n"
            + "    \"procFlag\": \"gnk\",\n"
            + "    \"procDt\": \"2021-05-23\"\n"
            + "  }],\n"
            + "  \"medaProvId\": \"vcs2xqbzp5w0h\",\n"
            + "  \"pracLocAddr1\": \"bm7h08gdsb9b66k2v3fs2\",\n"
            + "  \"pracLocAddr2\": \"f24p5mxp3k0bth5s2vgfx1njn3q6ftzx2266s0g4rqqx9qp24pb3rhw165ttc3cdnq4\",\n"
            + "  \"pracLocCity\": \"s3tkjz1\",\n"
            + "  \"pracLocState\": \"vx\",\n"
            + "  \"pracLocZip\": \"326077\",\n"
            + "  \"fissDiagCodes\": [{\n"
            + "    \"diagCd2\": \"trjgjpb\",\n"
            + "    \"diagPoaIndEnum\": \"DIAGNOSIS_PRESENT_ON_ADMISSION_INDICATOR_UNREPORTED\",\n"
            + "    \"bitFlags\": \"dmmr\"\n"
            + "  }, {\n"
            + "    \"diagCd2\": \"djbq\",\n"
            + "    \"bitFlags\": \"jxcb\",\n"
            + "    \"diagPoaIndUnrecognized\": \"t\"\n"
            + "  }],\n"
            + "  \"currLoc1Unrecognized\": \"c\",\n"
            + "  \"stmtCovFromCymd\": \"2021-06-24\",\n"
            + "  \"stmtCovToCymd\": \"2021-04-29\"\n"
            + "}",
        json);
  }
}
