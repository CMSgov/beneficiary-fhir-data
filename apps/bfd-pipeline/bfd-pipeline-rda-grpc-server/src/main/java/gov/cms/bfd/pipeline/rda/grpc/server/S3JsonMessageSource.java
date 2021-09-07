package gov.cms.bfd.pipeline.rda.grpc.server;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.common.io.Closer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Implementation of MessageSource that reads NDJSON data from an object in an S3 bucket. The object
 * will be closed when the message source is closed. A JsonMessageSource.Parser object is used to
 * parse the NDJSON data into an appropriate object. Data is streamed as it arrives from the bucket
 * rather than downloading the file first. Uses a Guava Closer object to manage closing all of the
 * resources and aborting the object contents stream if we are
 *
 * @param <T>
 */
public class S3JsonMessageSource<T> implements MessageSource<T> {
  private final Closer closer;
  private final S3ObjectInputStream s3InputStream;
  private final JsonMessageSource<T> jsonMessageSource;
  private boolean unfinished;

  public S3JsonMessageSource(S3Object s3Object, JsonMessageSource.Parser<T> parser) {
    closer = Closer.create();
    s3InputStream = s3Object.getObjectContent();
    // The S3 stream would normally download all the data from the object before closing.
    // Registering this method as a Closeable ensure that the abort method can be called on the
    // stream if we are closing before all the data has been read.
    closer.register(this::abortUnfinishedS3Stream);
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(s3InputStream, StandardCharsets.UTF_8));
    // when the JsonMessageSource closes it will also close the S3 stream
    jsonMessageSource = closer.register(new JsonMessageSource<>(reader, parser));
    closer.register(s3Object);
  }

  @Override
  public boolean hasNext() throws Exception {
    unfinished = jsonMessageSource.hasNext();
    return unfinished;
  }

  @Override
  public T next() throws Exception {
    return jsonMessageSource.next();
  }

  @Override
  public void close() throws IOException {
    closer.close();
  }

  /**
   * According to the S3 SDK docs, when an S3 stream is closed all the data remaining in the stream
   * will be downloaded before closing the stream. This method calls abort on the stream if we are
   * closing before we have retrieved all the data from the stream.
   */
  private void abortUnfinishedS3Stream() {
    if (unfinished) {
      s3InputStream.abort();
    }
  }
}
