package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;

/** Interface for the Sink that extends Closeable. */
public interface Sink<T> extends Closeable {
  /**
   * Interface method for write out to a file.
   *
   * @param value that is written out to the file
   */
  void write(T value);
}
