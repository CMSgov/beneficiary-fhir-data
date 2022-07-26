package gov.cms.bfd.data.fda.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class FDADrugUtilsTest {

  public static final String FAKE_DRUG_CODE_NUMBER = "000000000";

  /** Return Fake Drug Code when parameter is true */
  @Test
  public void shouldReturnFakeDrugCodeWhenConstructorSetToTrue() {
    FdaDrugCodeDisplayLookup drugUtils = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
    String drugCodeDisplay =
        drugUtils.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertNotEquals(null, drugCodeDisplay);
  }

  /** Do Not Return Fake Drug Code when parameter is false */
  @Test
  public void shouldNotReturnFakeDrugCodeWhenConstructorSetToFalse() {
    FdaDrugCodeDisplayLookup drugUtils =
        FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    String drugCodeDisplay =
        drugUtils.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertEquals(null, drugCodeDisplay);
  }

  /** Return Fake Drug Code Display when parameter is true */
  @Test
  public void shouldReturnFakeDrugCodeDisplayWhenConstructorSetToTrue() {
    FdaDrugCodeDisplayLookup drugUtils = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
    String drugCodeDisplay =
        drugUtils.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertEquals(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE_DISPLAY, drugCodeDisplay);
  }

  /** Do not Return Fake Drug Code Display when parameter is false */
  @Test
  public void shouldNotReturnFakeDrugCodeDisplayWhenConstructorSetToFalse() {
    FdaDrugCodeDisplayLookup drugUtils =
        FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    String drugCodeDisplay =
        drugUtils.retrieveFDADrugCodeDisplay(Optional.of(FAKE_DRUG_CODE_NUMBER));
    assertNotEquals(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE_DISPLAY, drugCodeDisplay);
  }
}
