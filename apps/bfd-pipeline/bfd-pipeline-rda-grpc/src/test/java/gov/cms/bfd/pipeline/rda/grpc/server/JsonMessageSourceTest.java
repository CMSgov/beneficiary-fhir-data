package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.fiss.FissClaim;
import java.io.BufferedWriter;
import java.io.File;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

/** Tests that the {@link JsonMessageSource} can read various claims from json data. */
public class JsonMessageSourceTest {
  /** Sample claim 1, in json format. */
  private static final String CLAIM_1 =
      """
      {
        "timestamp": "2022-01-25T15:02:35Z",
        "seq": "1",
        "changeType": "CHANGE_TYPE_UPDATE",
        "rdaClaimKey": "63843470id",
        "dcn": "63843470",
        "intermediaryNb": "24153",
        "claim": {
          "rdaClaimKey": "63843470id",
          "dcn": "63843470",
          "hicNo": "916689703543",
          "currStatusEnum": "CLAIM_STATUS_PAID",
          "currLoc1Enum": "PROCESSING_TYPE_MANUAL",
          "currLoc2Unrecognized": "uma",
          "totalChargeAmount": "3.75",
          "currTranDtCymd": "2021-03-20",
          "principleDiag": "uec",
          "mbi": "c1ihk7q0g3i57",
          "fissProcCodes": [
            {
              "procCd": "uec",
              "procFlag": "nli"
            },
            {
              "procCd": "egkkkw",
              "procFlag": "hsw",
              "procDt": "2021-02-03"
            },
            {
              "procCd": "zhaj",
              "procDt": "2021-01-07"
            },
            {
              "procCd": "ods",
              "procDt": "2021-01-03"
            }
          ],
          "medaProvId": "oducjgzt67joc"
        }
      }
      """
          .replaceAll("\n", "");
  /** Sample claim 2, in json format. */
  private static final String CLAIM_2 =
      """
      {
        "timestamp": "2022-01-25T15:02:35Z",
        "seq": "3",
        "changeType": "CHANGE_TYPE_UPDATE",
        "rdaClaimKey": "2643602id",
        "dcn": "2643602",
        "intermediaryNb": "24153",
        "claim": {
          "rdaClaimKey": "2643602id",
          "dcn": "2643602",
          "hicNo": "640930211775",
          "currStatusEnum": "CLAIM_STATUS_REJECT",
          "currLoc1Enum": "PROCESSING_TYPE_OFFLINE",
          "currLoc2Unrecognized": "p6s",
          "totalChargeAmount": "55.91",
          "recdDtCymd": "2021-05-14",
          "currTranDtCymd": "2020-12-21",
          "principleDiag": "egnj",
          "npiNumber": "5764657700",
          "mbi": "0vtc7u321x0se",
          "fedTaxNb": "2845244764",
          "fissProcCodes": [
            {
              "procCd": "egnj",
              "procDt": "2021-05-13"
            },
            {
              "procCd": "vvqtwoz",
              "procDt": "2021-04-29"
            },
            {
              "procCd": "fipyd",
              "procFlag": "g"
            }
          ]
        }
      }
      """
          .replaceAll("\n", "");

  /**
   * Verifies that a {@link FissClaim} claim can be read by the {@link JsonMessageSource}, has the
   * expected field data from the json, only creates one instance in the reader, and that calls to
   * close this reader are idempotent.
   *
   * @throws Exception indicates a source reader issue (test failure)
   */
  @Test
  public void singleClaimString() throws Exception {
    JsonMessageSource<FissClaimChange> source =
        new JsonMessageSource<>(CLAIM_1, JsonMessageSource.fissParser());
    assertTrue(source.hasNext());
    var change = source.next();
    assertEquals("63843470", change.getDcn());
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
    assertMultipleCallsToCloseOk(source);
  }

  /**
   * Verifies that two {@link FissClaim} claims in the same string, separated properly, can be read
   * by the {@link JsonMessageSource}, has expected field data from the json for each claim, creates
   * two claim instances in the reader, and that calls to close this reader are idempotent.
   *
   * @throws Exception indicates a source reader issue (test failure)
   */
  @Test
  public void twoClaimsString() throws Exception {
    JsonMessageSource<FissClaimChange> source =
        new JsonMessageSource<>(List.of(CLAIM_1, CLAIM_2), JsonMessageSource.fissParser());
    assertTrue(source.hasNext());
    var change = source.next();
    assertEquals("63843470", change.getDcn());
    assertTrue(source.hasNext());
    change = source.next();
    assertEquals("2643602", change.getDcn());
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
    assertMultipleCallsToCloseOk(source);
  }

  /**
   * Verifies that two {@link FissClaim} claims in a list can be read by the {@link
   * JsonMessageSource}, has expected field data from the json for each claim, creates two claim
   * instances in the reader, and that calls to close this reader are idempotent.
   *
   * @throws Exception indicates a source reader issue (test failure)
   */
  @Test
  public void claimsList() throws Exception {
    JsonMessageSource<FissClaimChange> source =
        new JsonMessageSource<>(List.of(CLAIM_1, CLAIM_2), JsonMessageSource.fissParser());
    assertTrue(source.hasNext());
    var change = source.next();
    assertEquals("63843470", change.getDcn());
    assertTrue(source.hasNext());
    change = source.next();
    assertEquals("2643602", change.getDcn());
    assertFalse(source.hasNext());
    assertNextPastEndOfDataThrowsException(source);
    assertMultipleCallsToCloseOk(source);
  }

  /**
   * Verifies that two {@link FissClaim} claims in the same file, separated properly, can be read by
   * the {@link JsonMessageSource}, has expected field data from the json for each claim, creates
   * two claim instances in the reader, and that calls to close this reader are idempotent.
   *
   * @throws Exception indicates a source reader issue (test failure)
   */
  @Test
  public void claimsFile() throws Exception {
    final File jsonFile = File.createTempFile(getClass().getSimpleName(), ".jsonl");
    try {
      try (BufferedWriter writer = Files.newWriter(jsonFile, Charsets.UTF_8)) {
        writer.write(CLAIM_1);
        writer.write(System.lineSeparator());
        writer.write(CLAIM_2);
      }
      try (JsonMessageSource<FissClaimChange> source =
          new JsonMessageSource<>(jsonFile, JsonMessageSource.fissParser())) {
        assertTrue(source.hasNext());
        var change = source.next();
        assertEquals("63843470", change.getDcn());
        assertTrue(source.hasNext());
        change = source.next();
        assertEquals("2643602", change.getDcn());
        assertFalse(source.hasNext());
        assertNextPastEndOfDataThrowsException(source);
        assertMultipleCallsToCloseOk(source);
      }
    } finally {
      jsonFile.delete();
    }
  }

  /**
   * Verifies that when two {@link FissClaim} are loaded and {@link JsonMessageSource#skipTo} is
   * called the number of specified messages are skipped and {@link JsonMessageSource#next} returns
   * the record past the skipped record.
   *
   * @throws Exception indicates a source reader issue (test failure)
   */
  @Test
  public void skip() throws Exception {
    MessageSource<FissClaimChange> source =
        new JsonMessageSource<>(List.of(CLAIM_1, CLAIM_2), JsonMessageSource.fissParser())
            .skipTo(2);
    assertTrue(source.hasNext());
    FissClaimChange claim = source.next();
    assertEquals("2643602", claim.getDcn());
    assertFalse(source.hasNext());
  }

  /**
   * Asserts that the source is at the end of its claim data, attempting to pull the next item
   * results in an exception, and calling {@link JsonMessageSource#hasNext} multiple times does not
   * throw an exception.
   *
   * @param source the source to test
   * @throws Exception non-expected exceptions, which will fail the test
   */
  private void assertNextPastEndOfDataThrowsException(JsonMessageSource<?> source)
      throws Exception {
    try {
      source.next();
      fail("expected exception");
    } catch (NoSuchElementException ignored) {
      // expected
    }
    // ensures calling hasNext() multiple times past the end is safe
    assertFalse(source.hasNext());
    assertFalse(source.hasNext());
  }

  /**
   * Asserts that multiple calls to close the reader do not throw an exception.
   *
   * @param source the source to close
   * @throws Exception if thrown, will fail the test
   */
  private void assertMultipleCallsToCloseOk(JsonMessageSource<?> source) throws Exception {
    source.close();
    source.close();
  }
}
