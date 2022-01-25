package gov.cms.bfd.pipeline.bridge.io;

import com.google.protobuf.Descriptors;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NdJsonSink implements Sink<MessageOrBuilder> {

  private final BufferedWriter writer;

  private final Path outputPath;
  private long startSequenceNumber = -1;
  private MessageOrBuilder messageOrBuilder;

  public NdJsonSink(Path outputPath) throws IOException {
    this.outputPath = outputPath;
    writer = new BufferedWriter(new FileWriter(outputPath.toString()));
  }

  @Override
  public void write(MessageOrBuilder messageOrBuilder) {
    try {
      this.messageOrBuilder = messageOrBuilder;

      if (startSequenceNumber < 0) {
        startSequenceNumber = getSequenceNumber(messageOrBuilder);
      }

      writer.write(JsonFormat.printer().omittingInsignificantWhitespace().print(messageOrBuilder));
      writer.write("\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to export object", e);
    }
  }

  @Override
  public void close() throws IOException {
    writer.close();

    // File format should be [FISS|MCS]-<startSequence>-<endSequence>.ndjson
    if (messageOrBuilder != null) {
      Long lastSequenceNumber = getSequenceNumber(messageOrBuilder);

      String outputFile = outputPath.getFileName().toString();
      String outputFileName = outputFile.substring(0, outputFile.length() - ".ndjson".length());

      String newFileName =
          outputFileName + "-" + startSequenceNumber + "-" + lastSequenceNumber + ".ndjson";

      if (!outputPath.toFile().renameTo(outputPath.getParent().resolve(newFileName).toFile())) {
        log.error("Failed to rename completed file '" + outputFile + "'");
      }
    }
  }

  private Long getSequenceNumber(MessageOrBuilder messageOrBuilder) {
    Long seqNumber = -1L;

    for (Map.Entry<Descriptors.FieldDescriptor, Object> entry :
        messageOrBuilder.getAllFields().entrySet()) {
      if (entry.getKey().getFullName().endsWith("seq")) {
        seqNumber = (Long) entry.getValue();
      }
    }

    return seqNumber;
  }
}
