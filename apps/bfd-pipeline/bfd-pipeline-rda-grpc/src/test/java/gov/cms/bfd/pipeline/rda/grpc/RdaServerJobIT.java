package gov.cms.bfd.pipeline.rda.grpc;

import static gov.cms.bfd.pipeline.sharedutils.s3.SharedS3Utilities.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import gov.cms.bfd.model.rda.RdaFissClaim;
import gov.cms.bfd.model.rda.RdaMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.server.RdaS3JsonMessageSourceFactory;
import gov.cms.bfd.pipeline.rda.grpc.server.S3DirectoryDao;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.s3.MinioTestContainer;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Integration tests for the RDA server. */
public class RdaServerJobIT extends MinioTestContainer {
  /** The server name to use for the test. */
  public static final String SERVER_NAME = "test-server";
  /** The Fiss claim source. */
  private static final ByteSource fissClaimsSource =
      Resources.asByteSource(Resources.getResource("FISS.ndjson"));
  /** The MCS claim source. */
  private static final ByteSource mcsClaimsSource =
      Resources.asByteSource(Resources.getResource("MCS.ndjson"));

  /** Clock for making timestamps. using a fixed Clock ensures our timestamp is predictable. */
  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);
  /** The hasher used for testing hashed values. */
  private final IdHasher.Config hasherConfig = new IdHasher.Config(100, "whatever");
  /** The Fiss claim transformer to test the server data. */
  private final FissClaimTransformer fissTransformer =
      new FissClaimTransformer(clock, MbiCache.computedCache(hasherConfig));
  /** The MCS claim transformer to test the server data. */
  private final McsClaimTransformer mcsTransformer =
      new McsClaimTransformer(clock, MbiCache.computedCache(hasherConfig));

  /**
   * Tests the server job in {@link RdaServerJob.Config.ServerMode#Random} configuration (generating
   * random data) and ensures the seeded random data returns as expected.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void testRandom() throws Exception {
    final RdaServerJob.Config config =
        RdaServerJob.Config.builder()
            .serverMode(RdaServerJob.Config.ServerMode.Random)
            .serverName(SERVER_NAME)
            .randomSeed(1L)
            .randomMaxClaims(4)
            .build();
    final RdaServerJob job = new RdaServerJob(config);
    final ExecutorService exec = Executors.newCachedThreadPool();
    final Future<PipelineJobOutcome> outcome = exec.submit(job);
    try {
      waitForServerToStart(job);
      final ManagedChannel fissChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
      try {
        final FissClaimStreamCaller fissCaller = new FissClaimStreamCaller();
        final GrpcResponseStream<FissClaimChange> fissStream =
            fissCaller.callService(fissChannel, CallOptions.DEFAULT, 2);
        assertTrue(fissStream.hasNext());
        RdaChange<RdaFissClaim> fissChange = fissTransformer.transformClaim(fissStream.next());
        assertMatches(
            fissCaller.callVersionService(fissChannel, CallOptions.DEFAULT), "Random:1:.*");
        assertEquals(3L, fissChange.getSequenceNumber());
        assertTrue(fissStream.hasNext());
        fissChange = fissTransformer.transformClaim(fissStream.next());
        assertEquals(4L, fissChange.getSequenceNumber());
        assertFalse(fissStream.hasNext());
      } finally {
        fissChannel.shutdownNow();
        fissChannel.awaitTermination(1, TimeUnit.MINUTES);
      }

      final ManagedChannel mcsChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
      try {
        final McsClaimStreamCaller mcsCaller = new McsClaimStreamCaller();
        final GrpcResponseStream<McsClaimChange> mcsStream =
            mcsCaller.callService(mcsChannel, CallOptions.DEFAULT, 3);
        assertTrue(mcsStream.hasNext());
        RdaChange<RdaMcsClaim> mcsChange = mcsTransformer.transformClaim(mcsStream.next());
        assertMatches(mcsCaller.callVersionService(mcsChannel, CallOptions.DEFAULT), "Random:1:.*");
        assertEquals(4L, mcsChange.getSequenceNumber());
        assertFalse(mcsStream.hasNext());
      } finally {
        mcsChannel.shutdownNow();
        mcsChannel.awaitTermination(1, TimeUnit.MINUTES);
      }
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(10, TimeUnit.SECONDS);
      assertEquals(PipelineJobOutcome.WORK_DONE, outcome.get());
    }
  }

  /**
   * Tests the server job in {@link RdaServerJob.Config.ServerMode#S3} configuration (data from S3)
   * and ensures the data returns as expected.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void testS3() throws Exception {
    AmazonS3 s3Client = createS3Client(REGION_DEFAULT);
    Bucket bucket = null;
    S3DirectoryDao s3Dao = null;
    Path cacheDirectoryPath = null;
    try {
      bucket = createTestBucket(s3Client);
      final String directoryPath = "files-go-here/";
      cacheDirectoryPath = Files.createTempDirectory("test");
      s3Dao =
          new S3DirectoryDao(s3Client, bucket.getName(), directoryPath, cacheDirectoryPath, true);
      final RdaServerJob.Config config =
          RdaServerJob.Config.builder()
              .serverMode(RdaServerJob.Config.ServerMode.S3)
              .serverName(SERVER_NAME)
              .s3Bucket(bucket.getName())
              .s3Directory(directoryPath)
              .s3CacheDirectory(cacheDirectoryPath.toString())
              .build();
      final String fissObjectKey = RdaS3JsonMessageSourceFactory.createValidFissKeyForTesting();
      final String mcsObjectKey = RdaS3JsonMessageSourceFactory.createValidMcsKeyForTesting();
      uploadJsonToBucket(
          s3Client, bucket.getName(), directoryPath + fissObjectKey, fissClaimsSource);
      uploadJsonToBucket(s3Client, bucket.getName(), directoryPath + mcsObjectKey, mcsClaimsSource);

      final RdaServerJob job = new RdaServerJob(config);
      final ExecutorService exec = Executors.newCachedThreadPool();
      final Future<PipelineJobOutcome> outcome = exec.submit(job);
      try {
        waitForServerToStart(job);
        final ManagedChannel fissChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
        try {
          final var fissCaller = new FissClaimStreamCaller();
          final var fissStream = fissCaller.callService(fissChannel, CallOptions.DEFAULT, 1097);
          assertTrue(fissStream.hasNext());
          RdaChange<RdaFissClaim> fissChange = fissTransformer.transformClaim(fissStream.next());
          assertMatches(
              fissCaller.callVersionService(fissChannel, CallOptions.DEFAULT), "S3:\\d+:.*");
          assertEquals(1098L, fissChange.getSequenceNumber());
          assertTrue(fissStream.hasNext());
          fissChange = fissTransformer.transformClaim(fissStream.next());
          assertEquals(1099L, fissChange.getSequenceNumber());
          assertTrue(fissStream.hasNext());
          fissChange = fissTransformer.transformClaim(fissStream.next());
          assertEquals(1100L, fissChange.getSequenceNumber());
          assertFalse(fissStream.hasNext());
        } finally {
          fissChannel.shutdownNow();
          fissChannel.awaitTermination(1, TimeUnit.MINUTES);
        }
        final ManagedChannel mcsChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
        try {
          final var mcsCaller = new McsClaimStreamCaller();
          final var mcsStream = mcsCaller.callService(mcsChannel, CallOptions.DEFAULT, 1098);
          assertTrue(mcsStream.hasNext());
          RdaChange<RdaMcsClaim> mcsChange = mcsTransformer.transformClaim(mcsStream.next());
          assertMatches(
              mcsCaller.callVersionService(mcsChannel, CallOptions.DEFAULT), "S3:\\d+:.*");
          assertEquals(1099L, mcsChange.getSequenceNumber());
          assertTrue(mcsStream.hasNext());
          mcsChange = mcsTransformer.transformClaim(mcsStream.next());
          assertEquals(1100L, mcsChange.getSequenceNumber());
          assertFalse(mcsStream.hasNext());
        } finally {
          mcsChannel.shutdownNow();
          mcsChannel.awaitTermination(1, TimeUnit.MINUTES);
        }
      } finally {
        exec.shutdownNow();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(PipelineJobOutcome.WORK_DONE, outcome.get());
      }
    } finally {
      deleteTestBucket(s3Client, bucket);
      if (s3Dao != null) {
        s3Dao.close();
      }
    }
  }

  /**
   * Tests the server job can be started, stopped, and restarted without issues.
   *
   * @throws Exception indicates test failure
   */
  @Test
  public void jobRunsCorrectlyMultipleTimes() throws Exception {
    final RdaServerJob.Config config =
        RdaServerJob.Config.builder()
            .serverMode(RdaServerJob.Config.ServerMode.Random)
            .serverName(SERVER_NAME)
            .randomSeed(1L)
            .randomMaxClaims(4)
            .build();
    final RdaServerJob job = new RdaServerJob(config);
    final ExecutorService exec = Executors.newCachedThreadPool();
    try {
      // run it once and then interrupt it
      Future<PipelineJobOutcome> outcome = exec.submit(job);
      waitForServerToStart(job);
      ManagedChannel fissChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
      try {
        var fissCaller = new FissClaimStreamCaller();
        var fissStream = fissCaller.callService(fissChannel, CallOptions.DEFAULT, 2);
        assertTrue(fissStream.hasNext());
        assertEquals(3L, fissTransformer.transformClaim(fissStream.next()).getSequenceNumber());
      } finally {
        fissChannel.shutdownNow();
        fissChannel.awaitTermination(1, TimeUnit.MINUTES);
      }
      outcome.cancel(true);
      waitForServerToStop(job);

      // now run it again to ensure gRPC lets server start a second time
      outcome = exec.submit(job);
      waitForServerToStart(job);
      fissChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
      try {
        var fissCaller = new FissClaimStreamCaller();
        var fissStream = fissCaller.callService(fissChannel, CallOptions.DEFAULT, 2);
        assertTrue(fissStream.hasNext());
        assertEquals(3L, fissTransformer.transformClaim(fissStream.next()).getSequenceNumber());
      } finally {
        fissChannel.shutdownNow();
        fissChannel.awaitTermination(1, TimeUnit.MINUTES);
      }
      outcome.cancel(true);
      waitForServerToStop(job);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  /**
   * Verifies that a value matches a regex.
   *
   * @param actual the value to test
   * @param regex the regex to test against
   */
  private void assertMatches(String actual, String regex) {
    if (!Strings.nullToEmpty(actual).matches(regex)) {
      fail(String.format("value did not match regex: regex='%s' value='%s'", regex, actual));
    }
  }

  /**
   * Waits at most 30 seconds for the server to get started. It's possible for the thread pool to
   * take longer to start than the test takes to create its StreamCallers.
   *
   * @param job the job to wait for
   * @throws InterruptedException if the thread sleep is interrupted
   */
  private static void waitForServerToStart(RdaServerJob job) throws InterruptedException {
    Thread.sleep(500);
    for (int i = 1; i <= 59 && !job.isServerRunning(); ++i) {
      Thread.sleep(500);
    }
  }

  /**
   * Waits at most 30 seconds for the server to get started. It's possible for the thread pool to
   * take longer to start than the test takes to create its StreamCallers.
   *
   * @param job the job to wait for
   * @throws InterruptedException if the thread sleep is interrupted
   */
  private static void waitForServerToStop(RdaServerJob job) throws InterruptedException {
    Thread.sleep(500);
    for (int i = 1; i <= 59 && job.isServerRunning(); ++i) {
      Thread.sleep(500);
    }
  }
}
