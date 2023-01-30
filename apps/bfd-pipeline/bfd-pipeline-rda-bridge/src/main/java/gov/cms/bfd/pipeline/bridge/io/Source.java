package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;
import java.io.IOException;

/** Interface for Source that extends Closeable. */
public interface Source<T> extends Closeable {

  /**
   * Checks to see if is the source has anymore input, if not it closes the stream.
   *
   * @return {@link boolean}.
   */
  boolean hasInput();

  /**
   * Interface to read the source of the file.
   *
   * @return {@link T}.
   * @throws IOException i End of source is reached.
   */
  T read() throws IOException;
}
