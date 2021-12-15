package gov.cms.bfd.pipeline.bridge.io;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class NdJsonSink implements Sink<MessageOrBuilder> {

  private final BufferedWriter writer;

  public NdJsonSink(Path outputPath) throws IOException {
    writer = new BufferedWriter(new FileWriter(outputPath.toString()));
  }

  @Override
  public void write(MessageOrBuilder messageOrBuilder) {
    try {
      writer.write(JsonFormat.printer().omittingInsignificantWhitespace().print(messageOrBuilder));
      writer.write("\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to export object", e);
    }
  }

  @Override
  public void close() throws IOException {
    writer.close();
  }
}
