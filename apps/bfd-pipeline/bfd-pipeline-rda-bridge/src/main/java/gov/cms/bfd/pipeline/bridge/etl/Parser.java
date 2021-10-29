package gov.cms.bfd.pipeline.bridge.etl;

import java.io.Closeable;
import java.io.IOException;
import java.util.Optional;

public interface Parser<T> extends Closeable {

  default void init() throws IOException {}

  boolean hasData();

  Data<T> read() throws IOException;

  abstract class Data<T> {

    public enum Type {
      DATE
    }

    public abstract Optional<T> get(String fieldName);

    public Optional<T> getFromType(String fieldName, Type type) {
      return get(fieldName);
    }
  }
}
