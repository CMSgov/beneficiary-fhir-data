package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;
import java.io.IOException;

/**
 * Interface for Source that specifies the reading of the source files.
 *
 * @param <T> the type of items produced by this Source
 */
public interface Source<T> extends Closeable {

  /**
   * Checks whether the source has any more input, if not it closes the stream.
   *
   * @return {@code boolean} true if the input has more to read, false otherwise
   */
  boolean hasInput();

  /**
   * Reads the source of the file and returns the correct output response.
   *
   * @return the {@code T} data that is read from the file
   * @throws IOException if end of source is reached
   */
  T read() throws IOException;
}
