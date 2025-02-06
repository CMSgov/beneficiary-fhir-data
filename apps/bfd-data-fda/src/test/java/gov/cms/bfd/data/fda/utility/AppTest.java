package gov.cms.bfd.data.fda.utility;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

/** Provides tests for the FDA App class. */
public final class AppTest {
  /** Return illegal argument exception when no arguments are passed. */
  @Test
  public void fdappThrowsIllegalArgumentExceptionWhenNoArgumentsPassed() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              App.main(new String[] {});
            });
    assertEquals(
        "Invalid number of arguments supplied for FDA NDC download.", exception.getMessage());
  }

  /** Return illegal argument exception when more than one arguments are passed. */
  @Test
  public void fdaAppThrowsIllegalArgumentExceptionWhenMoreThanOneArgumentsPassed() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              App.main(new String[] {"Argument 1", "Argument 2", "Argument 3"});
            });
    assertEquals("Invalid arguments supplied for FDA NDC download.", exception.getMessage());
  }

  /** Return illegal argument exception when one arguments are passed and is not a directory. */
  @Test
  public void fdaAppThrowsIllegalStateExceptionWhenOneArgumentsPassedThatIsNotADirectory() {
    Throwable exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              App.main(new String[] {"Argument 1", "1.0.0"});
            });
    assertEquals("OUTPUT_DIR does not exist for FDA NDC download.", exception.getMessage());
  }

  /** App passes with valid parameters. */
  @Test
  public void fdaAppPassesWithValidParameters() {
    try (MockedStatic<DataUtilityCommons> dataUtilityCommons =
        Mockito.mockStatic(DataUtilityCommons.class)) {
      String outputDir = "outputDir";
      String expectedFdaFile = App.FDA_PRODUCTS_RESOURCE;

      dataUtilityCommons
          .when(() -> DataUtilityCommons.getFDADrugCodes(any(), any(), any()))
          .thenAnswer((Answer<Void>) invocation -> null);
      App.main(new String[] {outputDir, "1.0.0"});

      // Verify that DataUtilityCommons.getFDADrugCodes was called with specific arguments
      dataUtilityCommons.verify(
          () -> DataUtilityCommons.getFDADrugCodes(outputDir, "1.0.0", expectedFdaFile));
    }
  }
}
