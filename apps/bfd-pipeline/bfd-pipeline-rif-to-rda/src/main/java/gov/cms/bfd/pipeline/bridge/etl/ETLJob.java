package gov.cms.bfd.pipeline.bridge.etl;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ETLJob<E, T> implements Runnable {

  private final Extractor<E> extractor;
  private final Transformer<E, T> transformer;
  private final Loader<T> loader;

  @Override
  public void run() {
    try {
      loader.load(transformer.transform(extractor.extract()));
    } catch (Exception e) {
      log.error("Failed to run ETL job", e);
    }
  }

  public interface Extractor<E> {

    E extract();
  }

  public interface Transformer<E, T> {

    T transform(E input);
  }

  public interface Loader<T> {

    void load(T data) throws IOException;
  }
}
