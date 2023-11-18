package gov.cms.bfd.datadictionary.util;

import gov.cms.bfd.datadictionary.model.FhirElement;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Stream of FhirElements deserialized from a given resource directory path. */
public class FhirElementStream {

  /** Directory path for the data dictionary resource files. */
  private final String resourcePath;

  /**
   * Constructor.
   *
   * @param resourcePath the directory path for the data dictionary resource files
   */
  public FhirElementStream(String resourcePath) {
    this.resourcePath = resourcePath;
  }

  /**
   * Creates a stream of FhirElement.
   *
   * @return a Stream of FhirElement
   */
  public Stream<FhirElement> stream() {
    // get and sort a list of files in the resource directory
    ClassLoader classLoader = FhirElementStream.class.getClassLoader();
    var url = classLoader.getResource(resourcePath);
    assert url != null;
    var dir = new File(url.getPath());
    var files = dir.listFiles();
    assert files != null;
    Arrays.sort(files, Comparator.comparing(File::getName));
    List<File> fileList = Arrays.stream(files).toList();

    // return the stream using the FhirElementSpliterator
    return StreamSupport.stream(new FhirElementSpliterator(fileList), false);
  }
}
