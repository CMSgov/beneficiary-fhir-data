package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;

/** Interface for the Sink that extends Closeable. */
public interface Sink<T> extends Closeable {
  /**
   * Interface method for write.
   *
   * @param value {@link T}.
   */
  void write(T value);
}
