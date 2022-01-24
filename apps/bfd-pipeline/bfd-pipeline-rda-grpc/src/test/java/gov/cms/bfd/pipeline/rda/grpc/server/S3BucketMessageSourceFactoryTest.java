package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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
    AmazonS3 s3Client =
        createS3Client(
            "mcs-215-275.ndjson.gz",
            "fiss.ndjson",
            "fiss-101-250.ndjson",
            "mcs-83-214.ndjson.gz",
            "this-won't-match",
            "fiss-0-100.ndjson");

    S3BucketMessageSourceFactory<?> fissFactory =
        new S3BucketMessageSourceFactory<>(
            s3Client, "bucket", "fiss", "ndjson", s -> new EmptyMessageSource<>(), r -> 0L);
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

    S3BucketMessageSourceFactory<?> mcsFactory =
        new S3BucketMessageSourceFactory<>(
            s3Client, "bucket", "mcs", "ndjson", s -> new EmptyMessageSource<>(), r -> 0L);
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

  private AmazonS3 createS3Client(String... filenames) {
    List<S3ObjectSummary> summaries = new ArrayList<>();
    for (String filename : filenames) {
      S3ObjectSummary summary = mock(S3ObjectSummary.class);
      doReturn(filename).when(summary).getKey();
      summaries.add(summary);
    }
    ObjectListing listing = mock(ObjectListing.class);
    doReturn(summaries).when(listing).getObjectSummaries();
    AmazonS3 s3Client = mock(AmazonS3.class);
    doReturn(listing).when(s3Client).listObjects(anyString());
    return s3Client;
  }

  private S3BucketMessageSourceFactory<Long> createFactory(MockMessageSource... sources) {
    List<String> filenames = new ArrayList<>();
    Map<String, MessageSource<Long>> sourceMap = new HashMap<>();
    for (MockMessageSource source : sources) {
      filenames.add(source.getFilename());
      sourceMap.put(source.getFilename(), source);
    }
    AmazonS3 s3Client = createS3Client(filenames.toArray(new String[0]));
    return new S3BucketMessageSourceFactory<>(
        s3Client, "bucket", "fiss", "ndjson", sourceMap::get, Function.identity());
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
