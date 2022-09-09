package gov.cms.bfd.data.npi.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/** App Testing class for NPI. */
public final class AppTest {
  /** Throws Illegal Argument Exception When No Arguments are Passed. */
  @Test
  public void npiAppThrowsIllegalArgumentExceptionWhenNoArgumentsPassed() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              App.main(new String[] {});
            });
    assertEquals("OUTPUT_DIR argument not specified for NPI download.", exception.getMessage());
  }

  /** Throws Illegal Argument Exception When more than two arguments is Passed. */
  @Test
  public void npiAppThrowsIllegalArgumentExceptionWhenMoreThanOneArgumentsPassed() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              App.main(new String[] {"Argument 1", "Argument 2", "Argument 3"});
            });
    assertEquals("Invalid arguments supplied for NPI download.", exception.getMessage());
  }

  /** Throws Illegal Argument Exception When argument passed is not a directory. */
  @Test
  public void npiAppThrowsIllegalStateExceptionWhenOneArgumentsPassedThatIsNotADirectory() {
    Throwable exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              App.main(new String[] {"Argument 1"});
            });
    assertEquals("OUTPUT_DIR does not exist for NPI download.", exception.getMessage());
  }
}
