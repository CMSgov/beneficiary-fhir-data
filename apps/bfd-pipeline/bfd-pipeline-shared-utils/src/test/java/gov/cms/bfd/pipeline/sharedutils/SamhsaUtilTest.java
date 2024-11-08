package gov.cms.bfd.pipeline.sharedutils;

import static org.junit.jupiter.api.Assertions.assertTrue;

import SamhsaUtils.SamhsaUtil;
import SamhsaUtils.model.SamhsaEntry;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SamhsaUtilTest {
  SamhsaUtil samhsaLookup;
  private static final String TEST_SAMHSA_CODE = "H0005";

  @BeforeEach
  void setup() throws IOException {
    samhsaLookup = SamhsaUtil.createSamhsaLookup();
  }

  @Test
  public void shouldReturnSamhsaEntry() {
    Optional<SamhsaEntry> entry = samhsaLookup.isSamhsaCode(Optional.of(TEST_SAMHSA_CODE));
    assertTrue(entry.isPresent());
  }
}
