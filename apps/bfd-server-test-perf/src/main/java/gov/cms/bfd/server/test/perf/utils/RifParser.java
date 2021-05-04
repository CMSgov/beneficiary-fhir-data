package gov.cms.bfd.server.test.perf.utils;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Class provides a simple interface to RifFile records */
public class RifParser {
  private ListIterator<RifRecordEvent<?>> rifEventsListIt = null;

  /**
   * Constructor
   *
   * @param rifFile The RIF file to manage with this class
   */
  public RifParser(RifFile rifFile) {
    RifFilesEvent filesEvent = new RifFilesEvent(Instant.now(), rifFile);
    RifFilesProcessor processor = new RifFilesProcessor();
    RifFileRecords rifFileRecords = processor.produceRecords(filesEvent.getFileEvents().get(0));
    List<RifRecordEvent<?>> rifEventsList =
        rifFileRecords.getRecords().collect(Collectors.toList());
    rifEventsListIt = rifEventsList.listIterator();
  }

  /**
   * Constructor
   *
   * @param rifFileName RIF file name to manage with this class
   * @param rifFileType The type of the RIF file to manage
   * @throws URISyntaxException Thrown if there is an error parsing the RIF file name
   */
  public RifParser(String rifFileName, RifFileType rifFileType) throws URISyntaxException {
    this(new LocalRifFile(Paths.get(resourceUrl(rifFileName).get().toURI()), rifFileType));
  }

  /**
   * Returns the next {@link RifRecordEvent} found in the associated RIF file
   *
   * @return {@link RifRecordEvent} or null if none found
   */
  public RifRecordEvent<?> next() {
    RifRecordEvent<?> next = null;
    if (rifEventsListIt.hasNext()) {
      next = rifEventsListIt.next();
    }
    return next;
  }

  /**
   * @param resourceName the name of the resource on the classpath (as might be passed to {@link
   *     ClassLoader#getResource(String)})
   * @return a {@link Supplier} for the {@link URL} to the resource's contents
   */
  private static Supplier<URL> resourceUrl(String resourceName) {
    return () -> {
      URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
      if (resource == null)
        throw new IllegalArgumentException("Unable to find resource: " + resourceName);

      return resource;
    };
  }
}
