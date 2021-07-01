package gov.cms.bfd.pipeline.rda.grpc.server;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.io.BufferedWriter;
import java.io.File;
import java.util.NoSuchElementException;
import org.junit.Test;

public class JsonFissClaimSourceTest {
  private static final String CLAIM_1 =
      "{"
          + "  \"dcn\": \"63843470\","
          + "  \"hicNo\": \"916689703543\","
          + "  \"currStatusEnum\": \"CLAIM_STATUS_PAID\","
          + "  \"currLoc1Enum\": \"PROCESSING_TYPE_MANUAL\","
          + "  \"currLoc2Unrecognized\": \"uma\","
          + "  \"totalChargeAmount\": \"3.75\","
          + "  \"currTranDtCymd\": \"2021-03-20\","
          + "  \"principleDiag\": \"uec\","
          + "  \"mbi\": \"c1ihk7q0g3i57\","
          + "  \"fissProcCodes\": ["
          + "    {"
          + "      \"procCd\": \"uec\","
          + "      \"procFlag\": \"nli\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"egkkkw\","
          + "      \"procFlag\": \"hsw\","
          + "      \"procDt\": \"2021-02-03\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"zhaj\","
          + "      \"procDt\": \"2021-01-07\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"ods\","
          + "      \"procDt\": \"2021-01-03\""
          + "    }"
          + "  ],"
          + "  \"medaProvId\": \"oducjgzt67joc\""
          + "}";
  private static final String CLAIM_2 =
      "{"
          + "  \"dcn\": \"2643602\","
          + "  \"hicNo\": \"640930211775\","
          + "  \"currStatusEnum\": \"CLAIM_STATUS_REJECT\","
          + "  \"currLoc1Enum\": \"PROCESSING_TYPE_OFFLINE\","
          + "  \"currLoc2Unrecognized\": \"p6s\","
          + "  \"totalChargeAmount\": \"55.91\","
          + "  \"recdDtCymd\": \"2021-05-14\","
          + "  \"currTranDtCymd\": \"2020-12-21\","
          + "  \"principleDiag\": \"egnj\","
          + "  \"npiNumber\": \"5764657700\","
          + "  \"mbi\": \"0vtc7u321x0se\","
          + "  \"fedTaxNb\": \"2845244764\","
          + "  \"fissProcCodes\": ["
          + "    {"
          + "      \"procCd\": \"egnj\","
          + "      \"procDt\": \"2021-05-13\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"vvqtwoz\","
          + "      \"procDt\": \"2021-04-29\""
          + "    },"
          + "    {"
          + "      \"procCd\": \"fipyd\","
          + "      \"procFlag\": \"g\""
          + "    }"
          + "  ]"
          + "}";

  @Test
  public void singleClaimString() throws Exception {
    JsonFissClaimSource source = new JsonFissClaimSource(CLAIM_1);
    assertEquals(true, source.hasNext());
    FissClaim claim = source.next();
    assertEquals("63843470", claim.getDcn());
    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
    assertMultipleCallsToCloseOk(source);
  }

  @Test
  public void twoClaimsString() throws Exception {
    JsonFissClaimSource source =
        new JsonFissClaimSource(CLAIM_1 + System.lineSeparator() + CLAIM_2);
    assertEquals(true, source.hasNext());
    FissClaim claim = source.next();
    assertEquals("63843470", claim.getDcn());
    assertEquals(true, source.hasNext());
    claim = source.next();
    assertEquals("2643602", claim.getDcn());
    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
    assertMultipleCallsToCloseOk(source);
  }

  @Test
  public void claimsList() throws Exception {
    JsonFissClaimSource source = new JsonFissClaimSource(ImmutableList.of(CLAIM_1, CLAIM_2));
    assertEquals(true, source.hasNext());
    FissClaim claim = source.next();
    assertEquals("63843470", claim.getDcn());
    assertEquals(true, source.hasNext());
    claim = source.next();
    assertEquals("2643602", claim.getDcn());
    assertEquals(false, source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
    assertMultipleCallsToCloseOk(source);
  }

  @Test
  public void claimsFile() throws Exception {
    final File jsonFile = File.createTempFile(getClass().getSimpleName(), ".jsonl");
    try {
      try (BufferedWriter writer = Files.newWriter(jsonFile, Charsets.UTF_8)) {
        writer.write(CLAIM_1);
        writer.write(System.lineSeparator());
        writer.write(CLAIM_2);
      }
      try (JsonFissClaimSource source = new JsonFissClaimSource(jsonFile)) {
        assertEquals(true, source.hasNext());
        FissClaim claim = source.next();
        assertEquals("63843470", claim.getDcn());
        assertEquals(true, source.hasNext());
        claim = source.next();
        assertEquals("2643602", claim.getDcn());
        assertEquals(false, source.hasNext());
        assertNextPastEndOfDataThrowsException(source);
        assertMultipleCallsToCloseOk(source);
      }
    } finally {
      jsonFile.delete();
    }
  }

  private void assertNextPastEndOfDataThrowsException(JsonFissClaimSource source) throws Exception {
    try {
      source.next();
      fail("expected exception");
    } catch (NoSuchElementException ignored) {
      // expected
    }
    // ensures calling hasNext() multiple times past the end is safe
    assertEquals(false, source.hasNext());
    assertEquals(false, source.hasNext());
  }

  private void assertMultipleCallsToCloseOk(JsonFissClaimSource source) throws Exception {
    source.close();
    source.close();
  }
}
