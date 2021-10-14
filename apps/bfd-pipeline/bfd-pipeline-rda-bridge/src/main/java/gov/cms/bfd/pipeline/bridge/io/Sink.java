package gov.cms.bfd.pipeline.bridge.io;

import java.io.Closeable;

public interface Sink<T> extends Closeable {

  void write(T value);
}
