package gov.cms.bfd.datadictionary.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import gov.cms.bfd.datadictionary.model.FhirElement;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

/** Stream of FhirElements deserialized from a given directory path. */
public class FhirElementStream {

  /** ObjectMapper to deserialize the JSON files. */
  private static final ObjectMapper objectMapper = JsonMapper.builder().build();

  /** Directory path for the data dictionary files. */
  private final String sourcePath;

  /**
   * Constructor.
   *
   * @param sourcePath the directory path for the data dictionary json files
   */
  public FhirElementStream(String sourcePath) {
    this.sourcePath = sourcePath;
  }

  /**
   * Creates a stream of FhirElement.
   *
   * @return a Stream of FhirElement
   */
  public Stream<FhirElement> stream() {
    // get and sort a list of files in the resource directory
    var dir = new File(sourcePath);
    var files = dir.listFiles();
    assert files != null;
    Arrays.sort(files, Comparator.comparing(File::getName));
    return Arrays.stream(files).map(this::readFile);
  }

  /**
   * Reads a data dictionary element file and returns a FhirElement.
   *
   * @param file the JSON File to read.
   * @return the FhirElement deserialized from the JSON content of the file
   */
  private FhirElement readFile(File file) {
    try {
      return objectMapper.readValue(file, FhirElement.class);
    } catch (IOException e) {
      throw new RuntimeException(
          String.format("Error deserializing FhirElement from file %s", file.getName()), e);
    }
  }
}
