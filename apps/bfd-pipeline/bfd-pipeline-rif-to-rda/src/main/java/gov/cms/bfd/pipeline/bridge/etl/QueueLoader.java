package gov.cms.bfd.pipeline.bridge.etl;

import java.io.BufferedWriter;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QueueLoader<T> implements ETLJob.Loader<T> {

  private final BufferedWriter writer;
  private final Serializer<T> serializer;

  @Override
  public void load(T data) throws IOException {
    try {
      writer.write(serializer.serialize(data));
      writer.write("\n");
    } catch (Exception e) {
      throw new IOException("Unable to serialize object", e);
    }
  }

  public interface Serializer<T> {

    String serialize(T object) throws Exception;
  }
}
