package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class FDADrugUtilsTest {

  @Test
  public void shouldReturnFakeDrugCodeWhenConstructorSetToTrue() {
    FdaDrugCodeDisplayLookup drugUtils = new FdaDrugCodeDisplayLookup(true);
    Map<String, String> drugCodes = drugUtils.readFDADrugCodeFile();
    assertTrue(drugCodes.containsKey("00000-0000"));
  }

  @Test
  public void shouldNotReturnFakeDrugCodeWhenConstructorSetToFalse() {
    FdaDrugCodeDisplayLookup drugUtils = new FdaDrugCodeDisplayLookup(false);
    Map<String, String> drugCodes = drugUtils.readFDADrugCodeFile();
    assertFalse(drugCodes.containsKey("00000-0000"));
  }

  @Test
  public void shouldReturnFakeDrugCodeDisplayWhenConstructorSetToTrue() {
    FdaDrugCodeDisplayLookup drugUtils = new FdaDrugCodeDisplayLookup(true);
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertEquals("Fake Diluent - WATER", drugCodeDisplay);
  }

  @Test
  public void shouldNotReturnFakeDrugCodeDisplayWhenConstructorSetToFalse() {
    FdaDrugCodeDisplayLookup drugUtils = new FdaDrugCodeDisplayLookup(false);
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertNotEquals("Fake Diluent - WATER", drugCodeDisplay);
  }
}
