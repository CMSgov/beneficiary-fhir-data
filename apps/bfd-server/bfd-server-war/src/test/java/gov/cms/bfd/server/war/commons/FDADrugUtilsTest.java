package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.war.FDADrugUtils;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FDADrugUtilsTest {

  FDADrugUtils drugUtils;

  @BeforeEach
  public void setup() {
    drugUtils = null;
  }

  @Test
  public void shouldReturnFakeDrugCodeWhenConstructorSetToTrue() {
    drugUtils = new FDADrugUtils(true);
    Map<String, String> drugCodes = drugUtils.readFDADrugCodeFile();
    assertTrue(drugCodes.containsKey("00000-0000"));
  }

  @Test
  public void shouldNotReturnFakeDrugCodeWhenConstructorSetToFalse() {
    drugUtils = new FDADrugUtils(false);
    Map<String, String> drugCodes = drugUtils.readFDADrugCodeFile();
    assertFalse(drugCodes.containsKey("00000-0000"));
  }

  @Test
  public void shouldReturnFakeDrugCodeDisplayWhenConstructorSetToTrue() {
    drugUtils = new FDADrugUtils(true);
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertEquals("Fake Diluent - WATER", drugCodeDisplay);
  }

  @Test
  public void shouldNotReturnFakeDrugCodeDisplayWhenConstructorSetToFalse() {
    drugUtils = new FDADrugUtils(false);
    String drugCodeDisplay = drugUtils.retrieveFDADrugCodeDisplay(Optional.of("000000000"));
    assertNotEquals("Fake Diluent - WATER", drugCodeDisplay);
  }
}
