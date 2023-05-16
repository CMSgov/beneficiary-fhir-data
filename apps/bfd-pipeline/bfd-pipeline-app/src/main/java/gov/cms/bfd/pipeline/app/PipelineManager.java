package gov.cms.bfd.pipeline.app;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.sharedutils.interfaces.ThrowingFunction;
import java.time.Clock;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;

@Slf4j
public class PipelineManager implements PipelineJobRunner.Tracker {
  public static final int MAX_COMPLETED_JOBS = 100;
  private final ThrowingFunction<Void, Long, InterruptedException> sleeper;
  private final Clock clock;
  private final ImmutableList<PipelineJob> jobs;
  private final ExecutorService threadPool;
  private final CountDownLatch latch;
  private final AtomicLong idGenerator = new AtomicLong(1);
  private final LinkedList<PipelineJobRunner.JobRunSummary> completedJobs;
  private final boolean interruptable;
  private ImmutableList<Future<Void>> runningJobFutures;
  private boolean isRunning;
  private Exception error;

  public PipelineManager(
      ThrowingFunction<Void, Long, InterruptedException> sleeper,
      Clock clock,
      List<PipelineJob> jobs) {
    this.sleeper = sleeper;
    this.clock = clock;
    this.jobs = ImmutableList.copyOf(jobs);
    threadPool =
        Executors.newCachedThreadPool(
            new ThreadFactoryBuilder()
                .threadNamePrefix(getClass().getSimpleName())
                .daemonThreads(false)
                .build());
    latch = new CountDownLatch(jobs.size());
    completedJobs = new LinkedList<>();
    interruptable = jobs.stream().allMatch(PipelineJob::isInterruptible);
  }

  public synchronized void start() {
    if (isRunning || runningJobFutures != null) {
      throw new IllegalStateException("start has already been called");
    }
    var futures = ImmutableList.<Future<Void>>builder();
    for (PipelineJob job : jobs) {
      var jobExecutor = new PipelineJobRunner(job, sleeper, clock, this);
      var future = threadPool.submit(jobExecutor);
      futures.add(future);
    }
    runningJobFutures = futures.build();
    isRunning = true;
  }

  public synchronized void stop() {
    if (isRunning) {
      if (interruptable) {
        var unscheduled = threadPool.shutdownNow();
        assert unscheduled.size() == 0;
      } else {
        log.info("stopping but must wait for uninterruptible jobs to complete on their own");
        threadPool.shutdown();
      }
      isRunning = false;
    }
  }

  public void awaitCompletion() {
    while (latch.getCount() > 0) {
      try {
        latch.await();
      } catch (InterruptedException ex) {
        log.debug("caught interrupt - still waiting for latch to reach zero");
      }
    }

    // just in case we somehow aren't stopped
    stop();

    // should return immediately since all jobs are done according to the latch
    log.info("waiting for pool to terminate");
    boolean terminated = false;
    while (!terminated) {
      try {
        terminated = threadPool.awaitTermination(30, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        log.debug("caught interrupt - still waiting for thread pool to terminate");
      }
    }
  }

  @Nullable
  public synchronized Exception getError() {
    return error;
  }

  @Override
  public synchronized boolean jobsCanRun() {
    return isRunning && error == null;
  }

  @Override
  public long beginningRun(PipelineJob job) {
    final var runId = idGenerator.getAndIncrement();
    log.info("Job run beginning: type={} id={}", job.getType(), runId);
    return runId;
  }

  @Override
  public synchronized void completedRun(PipelineJobRunner.JobRunSummary summary) {
    log.info("job run complete: {}", summary);
    if (completedJobs.size() > MAX_COMPLETED_JOBS) {
      completedJobs.removeFirst();
    }
    completedJobs.addLast(summary);
  }

  @Override
  public void sleeping(PipelineJob job) {
    log.debug("Job sleeping: type={}", job.getType());
  }

  @Override
  public void stoppingDueToInterrupt(PipelineJob job) {
    log.info("Job interrupted: type={}", job.getType());
  }

  @Override
  public synchronized void stoppingDueToException(PipelineJob job, Exception exception) {
    log.error("Job execution failed: type={} exception={}", job.getType(), exception.getMessage());
    if (this.error == null) {
      this.error = exception;
    } else {
      this.error.addSuppressed(exception);
    }
  }

  @Override
  public void stoppingNormally(PipelineJob job) {
    log.debug("Job stopping: " + job.getType());
  }

  @Override
  public void stopped(PipelineJob job) {
    log.info("Job stopped: " + job.getType());
    latch.countDown();
  }

  @VisibleForTesting
  List<PipelineJobRunner.JobRunSummary> getCompletedJobs() {
    return completedJobs;
  }
}
