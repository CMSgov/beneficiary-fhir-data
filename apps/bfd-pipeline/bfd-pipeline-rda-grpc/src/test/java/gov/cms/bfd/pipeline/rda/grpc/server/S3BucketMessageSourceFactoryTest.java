package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Tests the {@link S3BucketMessageSourceFactory}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class S3BucketMessageSourceFactoryTest {
  /** Used to control which files are in S3 bucket during tests. */
  private @Mock S3DirectoryDao s3Dao;

  /**
   * Verifies that the fiss and mcs factories can list the file keys from the mock
   * S3 bucket.
   */
  @Test
  public void listFilesTest() {
    final String directoryPath = "/my_directory/";
    S3Client s3Client = createS3Client(
        directoryPath,
        directoryPath + "mcs-215-275.ndjson.gz",
        directoryPath + "fiss.ndjson",
        directoryPath + "fiss-101-250.ndjson",
        directoryPath + "mcs-83-214.ndjson.gz",
        directoryPath + "this-won't-match",
        directoryPath + "fiss-0-100.ndjson");
    /*
     * setFilesInS3Dao(
     * "mcs-215-275.ndjson.gz",
     * "fiss.ndjson",
     * "fiss-101-250.ndjson",
     * "mcs-83-214.ndjson.gz",
     * "this-won't-match",
     * "fiss-0-100.ndjson");
     */

    S3BucketMessageSourceFactory<?> fissFactory = new S3BucketMessageSourceFactory<>(
        s3Dao, "fiss", "ndjson", s -> new EmptyMessageSource<>(), r -> 0L);
    assertEquals(
        Arrays.asList(
            new S3BucketMessageSourceFactory.FileEntry("fiss-0-100.ndjson", 0, 100),
            new S3BucketMessageSourceFactory.FileEntry("fiss.ndjson", 0, Long.MAX_VALUE),
            new S3BucketMessageSourceFactory.FileEntry("fiss-101-250.ndjson", 101, 250)),
        fissFactory.listFiles(0L));
    assertEquals(
        Arrays.asList(
            new S3BucketMessageSourceFactory.FileEntry("fiss.ndjson", 0, Long.MAX_VALUE),
            new S3BucketMessageSourceFactory.FileEntry("fiss-101-250.ndjson", 101, 250)),
        fissFactory.listFiles(112L));

    S3BucketMessageSourceFactory<?> mcsFactory = new S3BucketMessageSourceFactory<>(
        s3Dao, "mcs", "ndjson", s -> new EmptyMessageSource<>(), r -> 0L);
    assertEquals(
        Arrays.asList(
            new S3BucketMessageSourceFactory.FileEntry("mcs-83-214.ndjson.gz", 83, 214),
            new S3BucketMessageSourceFactory.FileEntry("mcs-215-275.ndjson.gz", 215, 275)),
        mcsFactory.listFiles(44L));
    assertEquals(
        Collections.singletonList(
            new S3BucketMessageSourceFactory.FileEntry("mcs-215-275.ndjson.gz", 215, 275)),
        mcsFactory.listFiles(215L));
    assertEquals(Collections.emptyList(), mcsFactory.listFiles(276L));
  }

  /**
   * Validates when there are no sources passed when creating a factory then the
   * source returned by
   * the factory is empty.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void noSourcesToConsume() throws Exception {
    S3BucketMessageSourceFactory<Long> factory = createFactory();
    MessageSource<Long> source = factory.apply(0);
    assertFalse(source.hasNext());
  }

  /**
   * Validates when there are two sources passed when creating a factory and a
   * certain number of
   * messages are requested which exceeds the capacity of one source, the factory
   * can supply the
   * requested number of messages by consuming from both sources and then
   * successfully close both
   * sources when finished.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void twoMessageSourcesConsumed() throws Exception {
    MockMessageSource source1 = new MockMessageSource(83, 117);
    MockMessageSource source2 = new MockMessageSource(118, 195);
    S3BucketMessageSourceFactory<Long> factory = createFactory(source2, source1);
    MessageSource<Long> empty = factory.apply(200);
    assertFalse(empty.hasNext());

    MessageSource<Long> source = factory.apply(87);
    Long expected = 87L;
    while (source.hasNext()) {
      assertEquals(expected, source.next());
      expected += 1;
    }
    source.close();
    assertTrue(source1.isClosed());
    assertTrue(source2.isClosed());
  }

  /**
   * Validates when there are two sources passed when creating a factory and a
   * certain number of
   * messages are requested which does not exceed the capacity of one source, the
   * factory can supply
   * the requested number of messages by consuming from the sources and then
   * successfully close both
   * sources when finished.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void oneOfTwoMessageSourcesConsumed() throws Exception {
    MockMessageSource source1 = new MockMessageSource(83, 117);
    MockMessageSource source2 = new MockMessageSource(118, 195);
    S3BucketMessageSourceFactory<Long> factory = createFactory(source2, source1);
    MessageSource<Long> empty = factory.apply(200);
    assertFalse(empty.hasNext());

    MessageSource<Long> source = factory.apply(118);
    Long expected = 118L;
    while (source.hasNext()) {
      assertEquals(expected, source.next());
      expected += 1;
    }
    source.close();
    assertFalse(source1.isClosed());
    assertTrue(source2.isClosed());
  }

  /**
   * Configures the mock {@link S3DirectoryDao} to return the specified file names
   * when {@link
   * S3DirectoryDao#readFileNames} is called.
   *
   * @param filenames file names to return
   */
  private S3Client createS3Client(String directoryPath, String... filenames) {
    List<S3Object> summaries = new ArrayList<>();
    for (String filename : filenames) {
      S3Object summary = mock(S3Object.class);
      doReturn(filename).when(summary).key();
      summaries.add(summary);
    }

    S3Client s3Client = mock(S3Client.class);
    doReturn(summaries).when(s3Client).listObjectsV2(any(ListObjectsV2Request.class));
    return s3Client;
  }

  private void setFilesInS3Dao(String... filenames) {
    var allFileNames = List.copyOf(Arrays.asList(filenames));
    doReturn(allFileNames).when(s3Dao).readFileNames();
  }

  /**
   * Creates a factory from one or more mocked message sources.
   *
   * @param sources the sources
   * @return the s3 bucket message source factory
   */
  private S3BucketMessageSourceFactory<Long> createFactory(MockMessageSource... sources) {
    List<String> filenames = new ArrayList<>();
    Map<String, MessageSource<Long>> sourceMap = new HashMap<>();
    for (MockMessageSource source : sources) {
      filenames.add(source.getFilename());
      sourceMap.put(source.getFilename(), source);
    }
    S3Client s3Client = createS3Client("", filenames.toArray(new String[0]));
    /* doReturn(List.copyOf(filenames)).when(s3Dao).readFileNames(); */
    return new S3BucketMessageSourceFactory<>(
        s3Dao, "fiss", "ndjson", sourceMap::get, Function.identity());
  }

  /**
   * Represents a mock implementation of a message source which implements mock
   * functionality for
   * some features.
   */
  private static class MockMessageSource implements MessageSource<Long> {

    /** The filename the mock message source is pretending to use. */
    @Getter
    private final String filename;
    /** The max sequence value. */
    private final long maxValue;
    /** The current sequence value. */
    private long currentValue;
    /** Represents if the source is closed. */
    @Getter
    private boolean closed;

    /**
     * Instantiates a new Mock message source.
     *
     * @param minSeq the min sequence number
     * @param maxSeq the max sequence number
     */
    private MockMessageSource(long minSeq, long maxSeq) {
      filename = String.format("fiss-%d-%d.ndjson", minSeq, maxSeq);
      maxValue = maxSeq;
      currentValue = minSeq - 1;
    }

    @Override
    public boolean hasNext() throws Exception {
      return currentValue < maxValue;
    }

    @Override
    public Long next() throws Exception {
      return ++currentValue;
    }

    @Override
    public void close() throws Exception {
      closed = true;
    }
  }
}
