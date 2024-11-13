package gov.cms.bfd.pipeline.sharedutils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rda.samhsa.SamhsaEntry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for SamhsaUtil class. */
public class SamhsaUtilTest {
  /** An instance of SamhsaUtil. */
  SamhsaUtil samhsaUtil;

  /** A SAMHSA code to use in the tests. */
  private static final String TEST_SAMHSA_CODE = "H0005";

  /** Test setup. */
  @BeforeEach
  void setup() {
    samhsaUtil = SamhsaUtil.getSamhsaUtil();
  }

  /** This test should return a SAMHSA code entry for the given code. */
  @Test
  public void shouldReturnSamhsaEntry() {
    Optional<SamhsaEntry> entry = samhsaUtil.isSamhsaCode(Optional.of(TEST_SAMHSA_CODE));
    assertTrue(entry.isPresent());
  }
}
