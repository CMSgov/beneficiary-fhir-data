package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public class FDADrugUtilsTest {

  @Test
  public void shouldReturnFakeDrugCodeWhenConstructorSetToTrue() {
    FdaDrugCodeDisplayLookup drugUtils = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertNotEquals(null, drugCodeDisplay);
  }

  @Test
  public void shouldNotReturnFakeDrugCodeWhenConstructorSetToFalse() {
    FdaDrugCodeDisplayLookup drugUtils =
        FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertEquals(null, drugCodeDisplay);
  }

  @Test
  public void shouldReturnFakeDrugCodeDisplayWhenConstructorSetToTrue() {
    FdaDrugCodeDisplayLookup drugUtils = FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertEquals(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE_DISPLAY, drugCodeDisplay);
  }

  @Test
  public void shouldNotReturnFakeDrugCodeDisplayWhenConstructorSetToFalse() {
    FdaDrugCodeDisplayLookup drugUtils =
        FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertNotEquals(FdaDrugCodeDisplayLookup.FAKE_DRUG_CODE_DISPLAY, drugCodeDisplay);
  }
}
