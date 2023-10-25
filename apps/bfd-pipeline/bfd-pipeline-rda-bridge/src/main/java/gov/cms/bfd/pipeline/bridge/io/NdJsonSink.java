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

  /** Buffered writer for json printer. */
  private final BufferedWriter writer;

  /** Output path for file. */
  private final Path outputPath;

  /** Sequence Counter. */
  private final WrappedCounter sequenceCounter;

  /** The Seqeunce Start Number. */
  private final long startSequenceNumber;

  /**
   * Constructor for NdJsonSink.
   *
   * @param args the sink arguments being passed in.
   * @throws IOException throws an IOException if the outputPath file exists but its a directory
   *     rather than a regular file, does not exist but cannot be created, or cannot be opened for
   *     any other reason.
   */
  public NdJsonSink(SinkArguments args) throws IOException {
    outputPath = args.getOutputPath();
    sequenceCounter = args.getSequenceCounter();
    startSequenceNumber = sequenceCounter.get();
    writer = new BufferedWriter(new FileWriter(outputPath.toString()));
  }

  /** {@inheritDoc} */
  @Override
  public void write(MessageOrBuilder messageOrBuilder) {
    try {
      writer.write(JsonFormat.printer().omittingInsignificantWhitespace().print(messageOrBuilder));
      writer.write("\n");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to export object", e);
    }
  }

  /** {@inheritDoc} */
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
