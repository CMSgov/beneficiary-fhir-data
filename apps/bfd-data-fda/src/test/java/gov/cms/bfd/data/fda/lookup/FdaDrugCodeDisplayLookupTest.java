package gov.cms.bfd.data.fda.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Provides tests for FDA Drug Test Utils. */
public class FdaDrugCodeDisplayLookupTest {

  /** The FAKE_DRUG_CODE_NUMBER for testing. */
  public static final String FAKE_DRUG_CODE_NUMBER = "000000000";

  /** The INPUT_FILE_STRING for the InputStream for testing. */
  public static final String INPUT_FILE_STRING =
      "PRODUCTID\tPRODUCTNDC\tPRODUCTTYPENAME\tPROPRIETARYNAME\t"
          + "PROPRIETARYNAMESUFFIX\tNONPROPRIETARYNAME\tDOSAGEFORMNAME\tROUTENAME\t"
          + "STARTMARKETINGDATE\tENDMARKETINGDATE\tMARKETINGCATEGORYNAME\tAPPLICATIONNUMBER\t"
          + "LABELERNAME\tSUBSTANCENAME\tACTIVE_NUMERATOR_STRENGTH\tACTIVE_INGRED_UNIT\t"
          + "PHARM_CLASSES\tDEASCHEDULE\tNDC_EXCLUDE_FLAG\tLISTING_RECORD_CERTIFIED_THROUGH\n"
          + "00000-0001_b02ed630-6947-431a-a8c8-227571403941\t00000-0001\tHUMAN OTC DRUG\tSterile Diluent\t\tdiluent\tINJECTION, SOLUTION\t"
          + "SUBCUTANEOUS\t19870710\t\tBLA\tBLA018781\tEli Lilly and Company\tWATER\t1\tmL/mL\t\t\tN\t20231231\n";

  /** The INPUT_FILE_STRING_WITH_DOUBLE_QUOTES for the InputStream for testing. */
  public static final String INPUT_FILE_STRING_WITH_DOUBLE_QUOTES =
      "PRODUCTID\tPRODUCTNDC\tPRODUCTTYPENAME\tPROPRIETARYNAME\t"
          + "PROPRIETARYNAMESUFFIX\tNONPROPRIETARYNAME\tDOSAGEFORMNAME\tROUTENAME\t"
          + "STARTMARKETINGDATE\tENDMARKETINGDATE\tMARKETINGCATEGORYNAME\tAPPLICATIONNUMBER\t"
          + "LABELERNAME\tSUBSTANCENAME\tACTIVE_NUMERATOR_STRENGTH\tACTIVE_INGRED_UNIT\t"
          + "PHARM_CLASSES\tDEASCHEDULE\tNDC_EXCLUDE_FLAG\tLISTING_RECORD_CERTIFIED_THROUGH\n"
          + "00000-0001_b02ed630-6947-431a-a8c8-227571403941\t00000-0001\tHUMAN OTC DRUG\t\"Sterile Diluent\"\t\tdiluent\tINJECTION, SOLUTION\t"
          + "SUBCUTANEOUS\t19870710\t\tBLA\tBLA018781\tEli Lilly and Company\t\"WATER\"\t1\tmL/mL\t\t\tN\t20231231\n";

  /** fdaDrugCodeDisplays to be used during testing. */
  public FdaDrugCodeDisplayLookup fdaDrugCodeDisplay;

  /** Clears fdaDrugCodeDisplay before each test. */
  @BeforeEach
  void setup() {
    fdaDrugCodeDisplay = null;
  }

  /** Return Fake Drug Code when parameter is true. */
  @Test
  public void shouldReturnFakeDrugCodeWhenConstructorSetToTrue() {
    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
    String drugCodeDisplay =
        fdaDrugCodeDisplay.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertNotEquals(null, drugCodeDisplay);
  }

  /** Do Not Return Fake Drug Code when parameter is false. */
  @Test
  public void shouldNotReturnFakeDrugCodeWhenConstructorSetToFalse() {
    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    String drugCodeDisplay =
        fdaDrugCodeDisplay.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertEquals(null, drugCodeDisplay);
  }

  /** Return Fake Drug Code Display when parameter is true. */
  @Test
  public void shouldReturnFakeDrugCodeDisplayWhenConstructorSetToTrue() {
    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
    String drugCodeDisplay =
        fdaDrugCodeDisplay.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertEquals(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE_DISPLAY, drugCodeDisplay);
  }

  /** Do not Return Fake Drug Code Display when parameter is false. */
  @Test
  public void shouldNotReturnFakeDrugCodeDisplayWhenConstructorSetToFalse() {
    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    String drugCodeDisplay =
        fdaDrugCodeDisplay.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertNotEquals(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE_DISPLAY, drugCodeDisplay);
  }

  /** Should not return double quotes in the mapping of the file for the data. */
  @Test
  public void shouldReturnMappedFDADataWithoutExtraDoubleQuotes() throws IOException {

    InputStream targetStream =
        new ByteArrayInputStream(INPUT_FILE_STRING_WITH_DOUBLE_QUOTES.getBytes());

    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();

    Map<String, String> results = fdaDrugCodeDisplay.getFdaProcessedData(true, targetStream);
    assertEquals("Sterile Diluent - WATER", results.get("00000-0001"));
  }

  /** Should return mapping of the file for the data. */
  @Test
  public void shouldReturnMappedFDAData() throws IOException {

    InputStream targetStream = new ByteArrayInputStream(INPUT_FILE_STRING.getBytes());

    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();

    Map<String, String> results = fdaDrugCodeDisplay.getFdaProcessedData(true, targetStream);
    assertEquals("Sterile Diluent - WATER", results.get("00000-0001"));
  }

  /**
   * Should return Fake FDA Drug Code when include drug code is true when reading the FDA Drug Code
   * File.
   */
  @Test
  void shouldReturnFakeDrugCodeWhenReadingFDADrugCodeFileAndIncludeFakeDrugCodeIsTrue() {

    InputStream targetStream = new ByteArrayInputStream(INPUT_FILE_STRING.getBytes());

    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();

    Map<String, String> results = fdaDrugCodeDisplay.readFDADrugCodeFile(true, targetStream);
    assertEquals(
        FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE_DISPLAY,
        results.get(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE));
  }

  /**
   * Should not return Fake FDA Drug Code when include drug code is false when reading the FDA Drug
   * Code File.
   */
  @Test
  void shouldReturnFakeDrugCodeWhenReadingFDADrugCodeFileAndIncludeFakeDrugCodeIsDalse() {

    InputStream targetStream = new ByteArrayInputStream(INPUT_FILE_STRING.getBytes());

    fdaDrugCodeDisplay = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();

    Map<String, String> results = fdaDrugCodeDisplay.readFDADrugCodeFile(false, targetStream);
    assertEquals(null, results.get(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE));
  }
}
