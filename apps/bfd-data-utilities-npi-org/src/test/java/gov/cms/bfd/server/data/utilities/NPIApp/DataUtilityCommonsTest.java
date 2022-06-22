package gov.cms.bfd.server.data.utilities.NPIApp;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import java.util.Arrays;

public final class DataUtilityCommonsTest 
{
  @Test
  public void getFDADrugCodesThrowsExceptionWhenFileIsNotADirectory() {
      String outputDir = "../temp/";
     Path tempDir = Paths.get(outputDir);;
    try (MockedStatic<Paths> paths  = Mockito.mockStatic(Paths.class)) {
      try (MockedStatic<Path> path  = Mockito.mockStatic(Path.class)) {
        try (MockedStatic<Files> files  = Mockito.mockStatic(Files.class)) {

      
        paths.when(() -> Paths.get(any())).thenAnswer((Answer<Path>) invocation -> tempDir);
        files.when(() -> Files.isDirectory(any())).thenAnswer((Answer<Boolean>) invocation -> false);
              Throwable exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
               DataUtilityCommons.getNPIOrgNames(outputDir, any());
            });
    assertEquals("OUTPUT_DIR does not exist for NPI download.", exception.getMessage());
      }
    }  
    }
}
}
