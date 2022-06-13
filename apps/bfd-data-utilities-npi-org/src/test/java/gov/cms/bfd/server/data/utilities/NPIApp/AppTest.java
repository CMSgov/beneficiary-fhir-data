package gov.cms.bfd.server.data.utilities.NPIApp;


import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public final class AppTest 
{
  @Test
  public void npiAppThrowsIllegalArgumentExceptionWhenNoArgumentsPassed() {
      Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
   App.main(new String[]{});
  });
  assertEquals("OUTPUT_DIR argument not specified for NPI download.", exception.getMessage());    
  }

   @Test
  public void npiAppThrowsIllegalArgumentExceptionWhenMoreThanOneArgumentsPassed() {
      Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
   App.main(new String[]{"Argument 1", "Argument 2"});
  });
  assertEquals("Invalid arguments supplied for NPI download.", exception.getMessage());    
  }

   @Test
  public void npiAppThrowsIllegalStateExceptionWhenOneArgumentsPassedThatIsNotADirectory() {
      Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
   App.main(new String[]{"Argument 1"});
  });
  assertEquals("OUTPUT_DIR does not exist for NPI download.", exception.getMessage());    
  }
 
}