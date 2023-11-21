package gov.cms.bfd.datadictionary.util;

import gov.cms.bfd.datadictionary.model.FhirElement;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Stream of FhirElements deserialized from a given directory path. */
public class FhirElementStream {

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
    List<File> fileList = Arrays.stream(files).toList();

    // return the stream using the FhirElementSpliterator
    return StreamSupport.stream(new FhirElementSpliterator(fileList), false);
  }
}
