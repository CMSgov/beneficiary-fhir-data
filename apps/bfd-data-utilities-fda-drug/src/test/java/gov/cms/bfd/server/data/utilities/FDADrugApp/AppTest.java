package gov.cms.bfd.server.data.utilities.FDADrugApp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.Test;

public final class AppTest {
  @Test
  public void fdappThrowsIllegalArgumentExceptionWhenNoArgumentsPassed() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              App.main(new String[] {});
            });
    assertEquals("OUTPUT_DIR argument not specified for FDA NDC download.", exception.getMessage());
  }

  @Test
  public void fdaAppThrowsIllegalArgumentExceptionWhenMoreThanOneArgumentsPassed() {
    Throwable exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              App.main(new String[] {"Argument 1", "Argument 2"});
            });
    assertEquals("Invalid arguments supplied for FDA NDC download.", exception.getMessage());
  }

  @Test
  public void fdaAppThrowsIllegalStateExceptionWhenOneArgumentsPassedThatIsNotADirectory() {
    Throwable exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              App.main(new String[] {"Argument 1"});
            });
    assertEquals("OUTPUT_DIR does not exist for FDA NDC download.", exception.getMessage());
  }

  @Test
  public void happyPathUnitTest() {
    try (MockedStatic<DataUtilityCommons> dataUtilityCommons  = Mockito.mockStatic(DataUtilityCommons.class)) {
        String outputDir = "outputDir";

        doNothing().when(dataUtilityCommons).getFDADrugCodes(any(String.class), any(String.class));
        App.main(new String[] {outputDir});
    }  
  }
}
