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
import gov.cms.bfd.model.rda.PartAdjFissClaim;
import gov.cms.bfd.model.rda.PartAdjMcsClaim;
import gov.cms.bfd.pipeline.rda.grpc.sink.direct.MbiCache;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.FissClaimTransformer;
import gov.cms.bfd.pipeline.rda.grpc.source.GrpcResponseStream;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimStreamCaller;
import gov.cms.bfd.pipeline.rda.grpc.source.McsClaimTransformer;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.mpsm.rda.v1.FissClaimChange;
import gov.cms.mpsm.rda.v1.McsClaimChange;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class RdaServerJobIT {
  public static final String SERVER_NAME = "test-server";
  private static final ByteSource fissClaimsSource =
      Resources.asByteSource(Resources.getResource("FISS.ndjson"));
  private static final ByteSource mcsClaimsSource =
      Resources.asByteSource(Resources.getResource("MCS.ndjson"));

  private final Clock clock = Clock.fixed(Instant.ofEpochMilli(60_000L), ZoneOffset.UTC);
  private final IdHasher.Config hasherConfig = new IdHasher.Config(100, "whatever");
  private final FissClaimTransformer fissTransformer =
      new FissClaimTransformer(clock, MbiCache.computedCache(hasherConfig));
  private final McsClaimTransformer mcsTransformer =
      new McsClaimTransformer(clock, MbiCache.computedCache(hasherConfig));

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
      final FissClaimStreamCaller fissCaller = new FissClaimStreamCaller();
      final GrpcResponseStream<FissClaimChange> fissStream =
          fissCaller.callService(fissChannel, CallOptions.DEFAULT, 2);
      assertTrue(fissStream.hasNext());
      RdaChange<PartAdjFissClaim> fissChange = fissTransformer.transformClaim(fissStream.next());
      assertMatches(fissCaller.callVersionService(fissChannel, CallOptions.DEFAULT), "Random:1:.*");
      assertEquals(2L, fissChange.getSequenceNumber());
      assertTrue(fissStream.hasNext());
      fissChange = fissTransformer.transformClaim(fissStream.next());
      assertEquals(3L, fissChange.getSequenceNumber());
      assertFalse(fissStream.hasNext());

      final ManagedChannel mcsChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
      final McsClaimStreamCaller mcsCaller = new McsClaimStreamCaller();
      final GrpcResponseStream<McsClaimChange> mcsStream =
          mcsCaller.callService(mcsChannel, CallOptions.DEFAULT, 3);
      assertTrue(mcsStream.hasNext());
      RdaChange<PartAdjMcsClaim> mcsChange = mcsTransformer.transformClaim(mcsStream.next());
      assertMatches(mcsCaller.callVersionService(mcsChannel, CallOptions.DEFAULT), "Random:1:.*");
      assertEquals(3L, mcsChange.getSequenceNumber());
      assertFalse(mcsStream.hasNext());
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(10, TimeUnit.SECONDS);
      assertEquals(PipelineJobOutcome.WORK_DONE, outcome.get());
    }
  }

  @Test
  public void testS3() throws Exception {
    AmazonS3 s3Client = createS3Client(REGION_DEFAULT);
    Bucket bucket = null;
    try {
      bucket = createTestBucket(s3Client);
      final String directoryPath = "files-go-here";
      final RdaServerJob.Config config =
          RdaServerJob.Config.builder()
              .serverMode(RdaServerJob.Config.ServerMode.S3)
              .serverName(SERVER_NAME)
              .s3Bucket(bucket.getName())
              .s3Directory(directoryPath)
              .build();
      final String fissObjectKey = config.getS3Sources().createFissObjectKey();
      final String mcsObjectKey = config.getS3Sources().createMcsObjectKey();
      uploadJsonToBucket(s3Client, bucket.getName(), fissObjectKey, fissClaimsSource);
      uploadJsonToBucket(s3Client, bucket.getName(), mcsObjectKey, mcsClaimsSource);

      final RdaServerJob job = new RdaServerJob(config);
      final ExecutorService exec = Executors.newCachedThreadPool();
      final Future<PipelineJobOutcome> outcome = exec.submit(job);
      try {
        waitForServerToStart(job);
        final ManagedChannel fissChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
        final FissClaimStreamCaller fissCaller = new FissClaimStreamCaller();
        final var fissStream = fissCaller.callService(fissChannel, CallOptions.DEFAULT, 1098);
        assertTrue(fissStream.hasNext());
        RdaChange<PartAdjFissClaim> fissChange = fissTransformer.transformClaim(fissStream.next());
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
        final ManagedChannel mcsChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
        final McsClaimStreamCaller mcsCaller = new McsClaimStreamCaller();
        final var mcsStream = mcsCaller.callService(mcsChannel, CallOptions.DEFAULT, 1099);
        assertTrue(mcsStream.hasNext());
        RdaChange<PartAdjMcsClaim> mcsChange = mcsTransformer.transformClaim(mcsStream.next());
        assertMatches(mcsCaller.callVersionService(mcsChannel, CallOptions.DEFAULT), "S3:\\d+:.*");
        assertEquals(1099L, mcsChange.getSequenceNumber());
        assertTrue(mcsStream.hasNext());
        mcsChange = mcsTransformer.transformClaim(mcsStream.next());
        assertEquals(1100L, mcsChange.getSequenceNumber());
        assertFalse(mcsStream.hasNext());
      } finally {
        exec.shutdownNow();
        exec.awaitTermination(10, TimeUnit.SECONDS);
        assertEquals(PipelineJobOutcome.WORK_DONE, outcome.get());
      }
    } finally {
      deleteTestBucket(s3Client, bucket);
    }
  }

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
      FissClaimStreamCaller fissCaller = new FissClaimStreamCaller();
      var fissStream = fissCaller.callService(fissChannel, CallOptions.DEFAULT, 2);
      assertTrue(fissStream.hasNext());
      assertEquals(2L, fissTransformer.transformClaim(fissStream.next()).getSequenceNumber());
      outcome.cancel(true);
      waitForServerToStop(job);

      // now run it again to ensure gRPC lets server start a second time
      outcome = exec.submit(job);
      waitForServerToStart(job);
      fissChannel = InProcessChannelBuilder.forName(SERVER_NAME).build();
      fissCaller = new FissClaimStreamCaller();
      fissStream = fissCaller.callService(fissChannel, CallOptions.DEFAULT, 2);
      assertTrue(fissStream.hasNext());
      assertEquals(2L, fissTransformer.transformClaim(fissStream.next()).getSequenceNumber());
      outcome.cancel(true);
      waitForServerToStop(job);
    } finally {
      exec.shutdownNow();
      exec.awaitTermination(10, TimeUnit.SECONDS);
    }
  }

  private void assertMatches(String actual, String regex) {
    if (!Strings.nullToEmpty(actual).matches(regex)) {
      fail(String.format("value did not match regex: regex='%s' value='%s'", regex, actual));
    }
  }

  /**
   * Waits at most 30 seconds for the server to get started. It's possible for the thread pool to
   * take longer to start than the test takes to create its StreamCallers.
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
   */
  private static void waitForServerToStop(RdaServerJob job) throws InterruptedException {
    Thread.sleep(500);
    for (int i = 1; i <= 59 && job.isServerRunning(); ++i) {
      Thread.sleep(500);
    }
  }
}
