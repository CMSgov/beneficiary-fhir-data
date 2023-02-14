package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;
import java.io.IOException;

/** Interface for Source that specifies the reading of the source files. */
public interface Source<T> extends Closeable {

  /**
   * Checks to see if is the source has anymore input, if not it closes the stream.
   *
   * @return {@link boolean} if the input has more to read or not
   */
  boolean hasInput();

  /**
   * Reads the source of the file and returns the correct output response.
   *
   * @return {@link T} for the data that is read from the file
   * @throws IOException if end of source is reached
   */
  T read() throws IOException;
}
