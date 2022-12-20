package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import org.junit.jupiter.api.Test;

public class S3BucketMessageSourceFactoryTest {
  @Test
  public void listFilesTest() {
    final String directoryPath = "/my_directory/";
    AmazonS3 s3Client =
        createS3Client(
            directoryPath,
            directoryPath + "mcs-215-275.ndjson.gz",
            directoryPath + "fiss.ndjson",
            directoryPath + "fiss-101-250.ndjson",
            directoryPath + "mcs-83-214.ndjson.gz",
            directoryPath + "this-won't-match",
            directoryPath + "fiss-0-100.ndjson");

    S3BucketMessageSourceFactory<?> fissFactory =
        new S3BucketMessageSourceFactory<>(
            s3Client,
            "bucket",
            directoryPath,
            "fiss",
            "ndjson",
            s -> new EmptyMessageSource<>(),
            r -> 0L);
    assertEquals(
        Arrays.asList(
            new S3BucketMessageSourceFactory.FileEntry(directoryPath + "fiss-0-100.ndjson", 0, 100),
            new S3BucketMessageSourceFactory.FileEntry(
                directoryPath + "fiss.ndjson", 0, Long.MAX_VALUE),
            new S3BucketMessageSourceFactory.FileEntry(
                directoryPath + "fiss-101-250.ndjson", 101, 250)),
        fissFactory.listFiles(0L));
    assertEquals(
        Arrays.asList(
            new S3BucketMessageSourceFactory.FileEntry(
                directoryPath + "fiss.ndjson", 0, Long.MAX_VALUE),
            new S3BucketMessageSourceFactory.FileEntry(
                directoryPath + "fiss-101-250.ndjson", 101, 250)),
        fissFactory.listFiles(112L));

    S3BucketMessageSourceFactory<?> mcsFactory =
        new S3BucketMessageSourceFactory<>(
            s3Client,
            "bucket",
            directoryPath,
            "mcs",
            "ndjson",
            s -> new EmptyMessageSource<>(),
            r -> 0L);
    assertEquals(
        Arrays.asList(
            new S3BucketMessageSourceFactory.FileEntry(
                directoryPath + "mcs-83-214.ndjson.gz", 83, 214),
            new S3BucketMessageSourceFactory.FileEntry(
                directoryPath + "mcs-215-275.ndjson.gz", 215, 275)),
        mcsFactory.listFiles(44L));
    assertEquals(
        Collections.singletonList(
            new S3BucketMessageSourceFactory.FileEntry(
                directoryPath + "mcs-215-275.ndjson.gz", 215, 275)),
        mcsFactory.listFiles(215L));
    assertEquals(Collections.emptyList(), mcsFactory.listFiles(276L));
  }

  @Test
  public void noSourcesToConsume() throws Exception {
    S3BucketMessageSourceFactory<Long> factory = createFactory();
    MessageSource<Long> source = factory.apply(0);
    assertFalse(source.hasNext());
  }

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

  private AmazonS3 createS3Client(String directoryPath, String... filenames) {
    List<S3ObjectSummary> summaries = new ArrayList<>();
    for (String filename : filenames) {
      S3ObjectSummary summary = mock(S3ObjectSummary.class);
      doReturn(filename).when(summary).getKey();
      summaries.add(summary);
    }
    ObjectListing listing = mock(ObjectListing.class);
    doReturn(summaries).when(listing).getObjectSummaries();
    AmazonS3 s3Client = mock(AmazonS3.class);
    if (Strings.isNullOrEmpty(directoryPath)) {
      doReturn(listing).when(s3Client).listObjects(anyString());
    } else {
      doReturn(listing).when(s3Client).listObjects(anyString(), eq(directoryPath));
    }
    return s3Client;
  }

  private S3BucketMessageSourceFactory<Long> createFactory(MockMessageSource... sources) {
    List<String> filenames = new ArrayList<>();
    Map<String, MessageSource<Long>> sourceMap = new HashMap<>();
    for (MockMessageSource source : sources) {
      filenames.add(source.getFilename());
      sourceMap.put(source.getFilename(), source);
    }
    AmazonS3 s3Client = createS3Client("", filenames.toArray(new String[0]));
    return new S3BucketMessageSourceFactory<>(
        s3Client, "bucket", "", "fiss", "ndjson", sourceMap::get, Function.identity());
  }

  private static class MockMessageSource implements MessageSource<Long> {
    @Getter private final String filename;
    private final long maxValue;
    private long currentValue;
    @Getter private boolean closed;

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
