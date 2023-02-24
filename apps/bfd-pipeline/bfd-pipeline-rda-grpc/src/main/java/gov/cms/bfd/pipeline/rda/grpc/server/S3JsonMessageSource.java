package gov.cms.bfd.pipeline.rda.grpc.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import software.amazon.awssdk.services.s3.model.S3Object;

/**
 * Implementation of {@link MessageSource} that reads and serves NDJSON data from a single {@link
 * S3Object}. The S3Object will be closed when the message source is closed. A {@link
 * JsonMessageSource.Parser} object is used to parse the NDJSON data.
 *
 * <p>Design note: Mock servers only manage a small number of clients. To simplify the design the
 * data is streamed to the client directly from S3 rather than downloading and caching the data
 * locally.
 *
 * @param <T> the type parameter
 */
public class S3JsonMessageSource<T> implements MessageSource<T> {
  /** The raw stream of the {@link #s3Object}. */
  private final InputStream s3InputStream;
  /** The source for outputting json messages. */
  private final JsonMessageSource<T> jsonMessageSource;
  /** If the object has not been fully read yet. */
  private boolean unfinished;

  /**
   * Instantiates a new S3 json message source.
   *
   * @param s3Object the s3 object
   * @param parser the parser to parse the object
   */
  public S3JsonMessageSource(
      String key, InputStream s3InputStream, JsonMessageSource.Parser<T> parser) {
    this.s3InputStream = s3InputStream;
    BufferedReader reader = createReader(key, s3InputStream);
    jsonMessageSource = new JsonMessageSource<>(reader, parser);
    unfinished = true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasNext() throws Exception {
    unfinished = jsonMessageSource.hasNext();
    return unfinished;
  }

  /** {@inheritDoc} */
  @Override
  public T next() throws Exception {
    return jsonMessageSource.next();
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    // Note: When the JsonMessageSource closes it will also close the S3 stream.
    closeAll(this::abortUnfinishedS3Stream, jsonMessageSource);
  }

  /**
   * According to the S3 SDK docs, when an S3 stream is closed all the data remaining in the stream
   * will still be downloaded before closing the stream. Obviously that is wasteful of resources and
   * potentially very slow if the amount of remaining data is large. To prevent that from happening,
   * this method calls abort on the stream if we are closing before we have retrieved all the data
   * from the stream.
   */
  private void abortUnfinishedS3Stream() {
    try {
      if (unfinished) {
        s3InputStream.close();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * This class holds several resources that need to be closed. One or more of them could throw an
   * exception when we close it. We don't want to lose any of the exceptions, but we also don't want
   * to allow one exception to prevent close being called on the other resources. This method closes
   * all the resources, combines any exceptions that result into a single exception, and throws any
   * exception at the end.
   *
   * @param closeables the things to close
   * @throws Exception if any closeable throws the first exception is thrown and others are added to
   *     it using {@link Throwable#addSuppressed(Throwable)}
   */
  private static void closeAll(AutoCloseable... closeables) throws Exception {
    Exception exception = null;
    for (AutoCloseable closeable : closeables) {
      try {
        closeable.close();
      } catch (Exception ex) {
        if (exception != null) {
          exception.addSuppressed(ex);
        } else {
          exception = ex;
        }
      }
    }
    if (exception != null) {
      throw exception;
    }
  }

  /**
   * Creates a buffered reader from an input stream.
   *
   * @param resourceName the resource name, to unzip it if required
   * @param stream the stream
   * @return the buffered reader
   */
  private static BufferedReader createReader(String resourceName, InputStream stream) {
    try {
      if (resourceName.endsWith(".gz")) {
        stream = new GZIPInputStream(stream);
      }
      return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
