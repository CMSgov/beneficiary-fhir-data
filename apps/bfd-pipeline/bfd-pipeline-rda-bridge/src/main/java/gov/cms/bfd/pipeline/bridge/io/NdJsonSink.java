package gov.cms.bfd.pipeline.bridge.io;

import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.util.JsonFormat;
import gov.cms.bfd.pipeline.bridge.util.WrappedCounter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

/** Creates a {@link Sink} that writes out to a ndjson file. */
@Slf4j
public class NdJsonSink implements Sink<MessageOrBuilder> {

  /** writer returns {@link BufferedWriter}. */
  private final BufferedWriter writer;

  /** outputPath returns {@link Path}. */
  private final Path outputPath;
  /** sequenceCounter returns {@link WrappedCounter}. */
  private final WrappedCounter sequenceCounter;
  /** startSequenceNumber returns {@link long}. */
  private final long startSequenceNumber;

  /**
   * Constructor for NdJsonSink.
   *
   * @param args the sink arguments being passed in.
   * @throws IOException throws IOException.
   */
  public NdJsonSink(SinkArguments args) throws IOException {
    outputPath = args.getOutputPath();
    sequenceCounter = args.getSequenceCounter();
    startSequenceNumber = sequenceCounter.get();
    writer = new BufferedWriter(new FileWriter(outputPath.toString()));
  }

  /**
   * This method writes to the messageOrBuilder.
   *
   * @param messageOrBuilder {@link MessageOrBuilder} is the messagebuilder being written to.
   */
  @Override
  public void write(MessageOrBuilder messageOrBuilder) {
    try {
      writer.write(JsonFormat.printer().omittingInsignificantWhitespace().print(messageOrBuilder));
      writer.write("\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to export object", e);
    }
  }

  /**
   * This method closes the buffered writer.
   *
   * @throws IOException throws IOException.
   */
  @Override
  public void close() throws IOException {
    writer.close();

    // File format should be [FISS|MCS]-<startSequence>-<endSequence>.ndjson
    // Rename the file to follow this convention now that we have the sequence range
    long lastSequenceNumber =
        sequenceCounter.get() - 1; // Counter was incremented after the last claim

    String outputFile = outputPath.getFileName().toString();
    String outputFileName = outputFile.substring(0, outputFile.length() - ".ndjson".length());

    String newFileName =
        String.format("%s-%d-%d.ndjson", outputFileName, startSequenceNumber, lastSequenceNumber);

    if (!outputPath.toFile().renameTo(outputPath.getParent().resolve(newFileName).toFile())) {
      log.error("Failed to rename completed file '" + outputFile + "'");
    }
  }
}
