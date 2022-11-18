package gov.cms.bfd.pipeline.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import gov.cms.bfd.pipeline.app.MicrometerConfigHelper.PropertyMapping;
import gov.cms.bfd.sharedutils.config.AppConfigurationException;
import io.micrometer.core.instrument.config.validate.InvalidReason;
import io.micrometer.core.instrument.config.validate.Validated;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MicrometerConfigHelper}. */
public class MicrometerConfigHelperTest {
  /** {@link PropertyMapping} with no default value. */
  private final PropertyMapping mappingA = new PropertyMapping("a", "AA", Optional.empty());
  /** {@link PropertyMapping} with a default value. */
  private final PropertyMapping mappingB = new PropertyMapping("b", "BB", Optional.of("b default"));
  /** Values used by lookup function. */
  private final Map<String, String> values = Map.of("AA", "a matched");
  /** Object under test. */
  private final MicrometerConfigHelper helper =
      new MicrometerConfigHelper(List.of(mappingA, mappingB), values::get);

  /** Verifies that {@link MicrometerConfigHelper#findProperty} works as expected. */
  @Test
  public void shouldRenamePropertiesUsingMap() {
    assertEquals(Optional.of(mappingA), helper.findProperty("a"));
    assertEquals(Optional.of(mappingB), helper.findProperty("b"));
    assertEquals(Optional.empty(), helper.findProperty("c"));
  }

  /** Verifies that {@link MicrometerConfigHelper#lookupKey} works as expected. */
  @Test
  public void shouldUseDefaultValue() {
    assertEquals(Optional.of("a matched"), helper.lookupKey(mappingA));
    assertEquals(Optional.of("b default"), helper.lookupKey(mappingB));
  }

  /** Verifies that {@link MicrometerConfigHelper#get} uses default values when they are present. */
  @Test
  public void shouldRenameAndUseDefaultValue() {
    assertEquals("a matched", helper.get("a"));
    assertEquals("b default", helper.get("b"));
    assertNull(helper.get("c"));
  }

  /**
   * Verifies that {@link MicrometerConfigHelper#throwIfConfigurationNotValid} does not throw if the
   * validation was successful.
   */
  @Test
  public void shouldNotThrowIfNoFailures() {
    try {
      var successes = Validated.valid("a", "w").and(Validated.valid("b", "b value"));
      helper.throwIfConfigurationNotValid(successes);
    } catch (AppConfigurationException ex) {
      fail("should not have thrown! message was " + ex.getMessage());
    }
  }

  /**
   * Verifies that {@link MicrometerConfigHelper#throwIfConfigurationNotValid} throws if the
   * validation failed.
   */
  @Test
  public void shouldProduceExceptionWithMessage() {
    try {
      var failures =
          Validated.invalid("a", "w", "a is bad", InvalidReason.MALFORMED)
              .and(Validated.invalid("c", null, "no value", InvalidReason.MISSING))
              .and(Validated.valid("b", "b value"));
      helper.throwIfConfigurationNotValid(failures);
      fail("should have thrown");
    } catch (AppConfigurationException ex) {
      assertEquals(
          "Invalid value for 2 configuration environment variable(s): 'AA'/'a': 'a is bad', 'unmatched'/'c': 'no value'",
          ex.getMessage());
    }
  }
}
