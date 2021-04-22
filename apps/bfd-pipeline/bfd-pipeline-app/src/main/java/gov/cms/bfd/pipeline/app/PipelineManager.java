package gov.cms.bfd.pipeline.app;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import gov.cms.bfd.pipeline.app.scheduler.SchedulerJob;
import gov.cms.bfd.pipeline.app.volunteer.VolunteerJob;
import gov.cms.bfd.pipeline.sharedutils.NullPipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobArguments;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobRecordId;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobFailure;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecord;
import gov.cms.bfd.pipeline.sharedutils.jobs.store.PipelineJobRecordStore;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Orchestrates and manages the execution of {@link PipelineJob}s. */
public final class PipelineManager {
  /**
   * The {@link Logger} message that will be recorded if/when the {@link PipelineManager} starts
   * scanning for data sets.
   */
  public static final String LOG_MESSAGE_STARTING_WORKER =
      "Starting data set monitor: watching for data sets to process...";

  /**
   * The number of jobs that can be run at one time. Because the {@link VolunteerJob} and {@link
   * SchedulerJob} will always be running, this number must be >=3, in order for any actual jobs to
   * get run.
   *
   * @see #jobExecutor
   */
  public static final int JOB_EXECUTOR_THREADS = 3;

  private static final Logger LOGGER = LoggerFactory.getLogger(PipelineManager.class);

  /**
   * This {@link System#exit(int)} value should be used when the application exits due to an
   * unhandled exception in {@link PipelineManager}.
   */
  public static final int EXIT_CODE_MONITOR_ERROR = 2;

  /** The {@link MetricRegistry} used to track the application's performance and events. */
  private final MetricRegistry appMetrics;

  /**
   * Used to run the jobs. Provided by Guava and documented here:
   * https://github.com/google/guava/wiki/ListenableFutureExplained. If you're touching this class,
   * you <strong>need</strong> to read that page and the associated JavaDoc. Concurrent APIs like
   * this are chock-full-o' footguns and while Guava's APIs here are well-designed, they're still no
   * exception to that rule.
   */
  private final ListeningScheduledExecutorService jobExecutor;

  /**
   * This {@link Map} stores handles to all enqueued job {@link Future}s. We need to keep track of
   * these for two use reasons:
   *
   * <ol>
   *   <li>So that we can ensure that we don't over-commit and enqueue more work than we have
   *       executors available for.
   *   <li>So that we can ensure that jobs can be cancelled when {@link #stop()} is called.
   * </ol>
   *
   * <p>Note: this isn't really thread-safe; it's subject to race conditions, unless used very
   * carefully. Accordingly, usage of this field <strong>SHALL</strong> conform to the following
   * rules:
   *
   * <ul>
   *   <li>Jobs SHALL only be added to it on the {@link VolunteerJob}'s thread, ensuring that the
   *       job is submitted to {@link #jobExecutor} <strong>and</strong> added to {@link
   *       #jobsEnqueuedHandles} in a block that is {@code synchronized} on {@link
   *       #jobsEnqueuedHandles}.
   *   <li>{@link ConcurrentMap#size()} SHALL only be read on the {@link VolunteerJob}'s thread
   *       (aside from reads for metrics, which SHALL NOT be used for any application logic).
   *   <li>Jobs SHALL only be removed by {@link PipelineJobWrapper} and {@link PipelineJobCallback}.
   *       (This is just to prevent runaway memory growth.)
   *   <li>All other reads of this collection (e.g. {@link #stop()}) SHALL {@code synchronize} on
   *       it.
   * </ul>
   *
   * <p>Following these rules will result in {@link ConcurrentMap#size()} being eventually
   * consistent but nevertheless ALWAYS >= the number of jobs that are actually enqueued, from the
   * view of the {@link VolunteerJob}'s thread. This aligns with what the {@link VolunteerJob} needs
   * the value for, as we're using it to avoid over-subscribing on work. Under-subscribing is fine,
   * in the context.
   */
  private final ConcurrentMap<PipelineJobRecordId, PipelineJobHandle<?>> jobsEnqueuedHandles;

  /**
   * Used to run the job monitoring tasks, e.g. {@link ListenableFuture} callbacks. Those tasks are
   * run in a separate thread pool to ensure that job execution doesn't starve them out.
   */
  private final ExecutorService jobMonitorsExecutor;

  /**
   * The {@link PipelineJobRecordStore} service that tracks job submissions, executions, and
   * outcomes.
   */
  private final PipelineJobRecordStore jobRecordStore;

  /**
   * Stores the {@link PipelineJob}s that have been registered via {@link
   * #registerJob(PipelineJob)}.
   */
  private final Map<PipelineJobType<?>, PipelineJob<?>> jobRegistry;

  /**
   * Constructs a new {@link PipelineManager} instance. Note that this intended for use as a
   * singleton service in the application: only one instance running at a time.
   *
   * @param appMetrics the {@link MetricRegistry} for the overall application
   * @param jobRecordStore the {@link PipelineJobRecordStore} for the overall application
   */
  public PipelineManager(MetricRegistry appMetrics, PipelineJobRecordStore jobRecordStore) {
    this.appMetrics = appMetrics;
    this.jobRecordStore = jobRecordStore;
    this.jobExecutor = createJobExecutor();
    this.jobsEnqueuedHandles = new ConcurrentHashMap<>();
    this.jobMonitorsExecutor = Executors.newCachedThreadPool();
    this.jobRegistry = new HashMap<>();

    /*
     * Bootstrap the SchedulerJob and VolunteerJob, which are responsible for ensuring that all of
     * the other jobs get executed, as and when needed. Note that it will permanently tie up two of
     * the job executors, as they're designed to run forever.
     */
    VolunteerJob volunteerJob = new VolunteerJob(this, jobRecordStore);
    registerJob(volunteerJob);
    PipelineJobRecord<NullPipelineJobArguments> volunteerJobRecord =
        jobRecordStore.submitPendingJob(VolunteerJob.JOB_TYPE, null);
    enqueueJob(volunteerJobRecord);
    SchedulerJob schedulerJob = new SchedulerJob(this, jobRecordStore);
    registerJob(schedulerJob);
    jobRecordStore.submitPendingJob(SchedulerJob.JOB_TYPE, null);
  }

  /** @return the {@link ListeningScheduledExecutorService} to use for {@link #jobExecutor} */
  private static ListeningScheduledExecutorService createJobExecutor() {
    ScheduledThreadPoolExecutor jobExecutorInner =
        new ScheduledThreadPoolExecutor(JOB_EXECUTOR_THREADS);
    jobExecutorInner.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    return MoreExecutors.listeningDecorator(jobExecutorInner);
  }

  /**
   * Registers the specified {@link PipelineJob}, scheduling it (if it has a {@link
   * PipelineJob#getSchedule()}) and also making it available for triggering elsewhere, via {@link
   * PipelineManager#enqueueJob(PipelineJobType)}.
   *
   * @param job the {@link PipelineJob} to register
   */
  public void registerJob(PipelineJob<?> job) {
    jobRegistry.put(job.getType(), job);
  }

  /**
   * <strong>Warning:</strong> See the note on {@link #jobsEnqueuedHandles} for the thread-safety
   * rules for usage of this property. It is <strong>not</strong> safe to use outside of those
   * conditions.
   *
   * <p>This property, and its invariants, allow us to avoid over-committing and accepting more work
   * than we are guaranteed to have {@link #jobExecutor} threads/slots available for. This isn't
   * strictly necessary if jobs are only running on a single node, but is nevertheless a nice
   * property, and becomes very important if jobs are being run across multiple nodes (as otherwise
   * we'd have unnecessarily stalled work).
   *
   * @return the number of available executor slots (technically, an eventually consistent
   *     approximation of that value, which is guaranteed to be less-than-or-equal-to the true
   *     value, provided that the thread-safety rules described in {@link #jobsEnqueuedHandles} are
   *     adhered to)
   */
  public int getOpenExecutorSlots() {
    return JOB_EXECUTOR_THREADS - jobsEnqueuedHandles.size();
  }

  /**
   * @return the {@link Set} of jobs registered via {@link #registerJob(PipelineJob)} that have
   *     {@link PipelineJob#getSchedule()} values
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public Set<PipelineJob<NullPipelineJobArguments>> getScheduledJobs() {
    Set scheduledJobs =
        jobRegistry.values().stream()
            .filter(j -> j.getSchedule().isPresent())
            .collect(Collectors.toSet());
    return scheduledJobs;
  }

  /**
   * Enqueues an execution of the specified {@link PipelineJob}, with the specified parameters.
   *
   * @param jobRecord the {@link PipelineJobRecord} of the job to run
   * @return <code>true</code> if the specified job was enqueued, or <code>false</code> if it could
   *     not be (e.g. because {@link #stop()} has been called)
   */
  public <A extends PipelineJobArguments> boolean enqueueJob(PipelineJobRecord<A> jobRecord) {
    // First, find the specified job.
    @SuppressWarnings("unchecked")
    PipelineJob<A> job = (PipelineJob<A>) jobRegistry.get(jobRecord.getJobType());
    if (job == null)
      throw new IllegalArgumentException(
          String.format("Unknown or unregistered job type '%s'.", jobRecord.getJobType()));

    // Submit the job to be run!
    PipelineJobWrapper<A> jobWrapper;
    ListenableFuture<PipelineJobOutcome> jobFuture;
    synchronized (jobsEnqueuedHandles) {
      /*
       * Design Note: Java's Executor framework is great at running things and returning results,
       * but provides no built-in facilities at all for monitoring and tracking the things that have
       * been run. We need that monitoring here, so we use PipelineJobWrapper to catch these events:
       * enqueue, start, complete-successfully, complete-with-exception. (See
       * Futures.addCallback(...) a little bit further below for a discussion of some additional
       * monitoring we need and add in.)
       */
      jobWrapper = new PipelineJobWrapper<>(job, jobRecord);

      // Ensure code below doesn't accidentally use the unwrapped job.
      job = jobWrapper;

      try {
        jobFuture = jobExecutor.submit(jobWrapper);
      } catch (RejectedExecutionException e) {
        // Indicates that the executor has been shutdown.
        return false;
      }
      jobsEnqueuedHandles.put(jobRecord.getId(), new PipelineJobHandle<>(jobWrapper, jobFuture));
    }

    /*
     * Design Note: We can't catch job-cancellation-before-start events in PipelineJobWrapper,
     * because its call(...) method won't ever be called in those cases. Guava's ListenableFuture
     * framework doesn't monitor task submission or start, so we can't use it to catch those events.
     * Accordingly, we use a combo: both PipelineJobWrapper and Guava's ListenableFuture, to catch
     * all of the events that we're interested in.
     */
    Futures.addCallback(
        jobFuture, new PipelineJobCallback<A>(jobWrapper.getJobRecord()), this.jobMonitorsExecutor);

    return true;
  }

  /**
   * This will eventually end all jobs and shut down this {@link PipelineManager}. Note: not all
   * jobs support being stopped while in progress, so this method may block for quite a while.
   */
  public void stop() {
    // If something has already shut us down, we're done.
    if (jobExecutor.isShutdown()) {
      return;
    }

    LOGGER.info("Stopping PipelineManager...");

    /*
     * Tell the job executor to shut down, which will prevent it from accepting new jobs and from
     * running any jobs that haven't already started. If all jobs are interruptible, we'll shut it
     * down _harder_, such that in-progress job threads get interrupted (ala Thread.interrupt()).
     */
    boolean unsafeToInterrupt = jobRegistry.values().stream().anyMatch(j -> !j.isInterruptible());
    if (unsafeToInterrupt) {
      jobExecutor.shutdown();
      LOGGER.info("Shut down job executor, without cancelling existing jobs.");
    } else {
      jobExecutor.shutdownNow();
      LOGGER.info("Shut down job executor, cancelling existing jobs.");
    }

    /*
     * Try to stop all jobs that are either not running yet or are interruptible. Note: VolunteerJob
     * might still be trying to submit jobs over on its thread, so we synchronize to keep things
     * consistent and ensure we don't miss any jobs.
     */
    synchronized (jobsEnqueuedHandles) {
      jobsEnqueuedHandles
          .values()
          .parallelStream()
          .forEach(
              j -> {
                /*
                 * Note: There's a race condition here, where the job may have completed just before
                 * we try to cancel it, but that's okay because Future.cancel(...) is basically a
                 * no-op for jobs that have already completed.
                 */
                j.cancelIfInterruptible();
              });
    }

    /*
     * Wait for everything to halt.
     */
    boolean allStopped = jobExecutor.isTerminated();
    Optional<Instant> lastWaitMessage = Optional.empty();
    while (!allStopped) {
      if (!lastWaitMessage.isPresent()
          || Duration.between(Instant.now(), lastWaitMessage.get()).toMinutes() >= 10) {
        LOGGER.info(
            "Waiting for jobs to stop, which may take A WHILE. "
                + "(Message will repeat every ten minutes, until complete.)");
        lastWaitMessage = Optional.of(Instant.now());
      }
      try {
        allStopped = jobExecutor.awaitTermination(10, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        /*
         * Need to ignore interrupts here so that we can shut down safely.
         */
        LOGGER.warn(
            String.format("%s ignoring interrupt during shutdown.", this.getClass().getName()));
      }
    }
    LOGGER.info("Stopped PipelineManager.");
  }

  /**
   * A handle for a {@link PipelineJob} execution, which is used to allow the application to cancel
   * job executions.
   *
   * @param <A> the {@link PipelineJobArguments} type associated with this {@link PipelineJob}
   *     implementation (see {@link NullPipelineJobArguments} for those {@link PipelineJob}
   *     implementations which do not need arguments)
   */
  private static final class PipelineJobHandle<A extends PipelineJobArguments> {
    private final PipelineJob<A> job;
    private final Future<PipelineJobOutcome> future;

    /**
     * Constructs a new {@link PipelineJobHandle} instance.
     *
     * @param job the {@link PipelineJob} that the paired {@link Future} is for
     * @param future the {@link Future} representing an execution of the paired {@link PipelineJob}
     */
    public PipelineJobHandle(PipelineJob<A> job, Future<PipelineJobOutcome> future) {
      this.job = job;
      this.future = future;
    }

    /**
     * Attempts to cancel the job execution by calling {@link Future#cancel(boolean)}, respecting
     * the value of {@link PipelineJob#isInterruptible()}.
     */
    public void cancelIfInterruptible() {
      LOGGER.trace("cancelIfPendingOrInterruptible() called: job.getType()='{}'", job.getType());
      future.cancel(job.isInterruptible());
    }
  }

  /**
   * This {@link PipelineJob} implementation wraps a delegate {@link PipelineJob}, providing data to
   * {@link PipelineJobRecordStore} about that job's execution and status.
   *
   * @param <A> the {@link PipelineJobArguments} type associated with this {@link PipelineJob}
   *     implementation (see {@link NullPipelineJobArguments} for those {@link PipelineJob}
   *     implementations which do not need arguments)
   */
  private final class PipelineJobWrapper<A extends PipelineJobArguments> implements PipelineJob<A> {
    private final PipelineJob<A> wrappedJob;
    private final PipelineJobRecord<A> jobRecord;

    /**
     * Constructs a new {@link PipelineJobWrapper} for the specified {@link PipelineJob}.
     *
     * @param wrappedJob the {@link PipelineJob} to wrap and monitor
     * @param jobRecord the {@link PipelineJobRecord} for the job to wrap and monitor
     */
    public PipelineJobWrapper(PipelineJob<A> wrappedJob, PipelineJobRecord<A> jobRecord) {
      this.wrappedJob = wrappedJob;
      this.jobRecord = jobRecord;
      jobRecordStore.recordJobEnqueue(jobRecord.getId());
    }

    /** @return the {@link PipelineJobRecord} for this {@link PipelineJobWrapper} */
    public PipelineJobRecord<A> getJobRecord() {
      return jobRecord;
    }

    /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#getType() */
    @Override
    public PipelineJobType<A> getType() {
      return wrappedJob.getType();
    }

    /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#getSchedule() */
    @Override
    public Optional<PipelineJobSchedule> getSchedule() {
      return wrappedJob.getSchedule();
    }

    /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#isInterruptible() */
    @Override
    public boolean isInterruptible() {
      return wrappedJob.isInterruptible();
    }

    /** @see gov.cms.bfd.pipeline.sharedutils.PipelineJob#call() */
    @Override
    public PipelineJobOutcome call() throws Exception {
      jobRecordStore.recordJobStart(jobRecord.getId());

      try {
        PipelineJobOutcome jobOutcome = wrappedJob.call();
        jobRecordStore.recordJobCompletion(jobRecord.getId(), jobOutcome);
        return jobOutcome;
      } catch (InterruptedException e) {
        /*
         * This indicates that someone has successfully interrupted the job, which should only have
         * happened when we're trying to shut down. Whether or not PipelineJob.isInterruptible() for
         * this job, it's now been stopped, so we should record the cancellation.
         */
        jobRecordStore.recordJobCancellation(jobRecord.getId());

        // Restore the interrupt so things can get back to shutting down.
        Thread.currentThread().interrupt();
        throw new InterruptedException("Re-firing job interrupt.");
      } catch (Exception e) {
        jobRecordStore.recordJobFailure(jobRecord.getId(), new PipelineJobFailure(e));

        // Wrap and re-thrown the failure.
        throw new Exception("Re-throwing job failure.", e);
      } finally {
        jobsEnqueuedHandles.remove(jobRecord.getId());
      }
    }
  }

  /**
   * A {@link FutureCallback} for Guava {@link ListenableFuture}s for {@link PipelineJobWrapper}
   * tasks, providing data to {@link PipelineJobRecordStore} about a job's execution and status.
   */
  private final class PipelineJobCallback<A extends PipelineJobArguments>
      implements FutureCallback<PipelineJobOutcome> {
    private final PipelineJobRecord<A> jobRecord;

    /**
     * Constructs a new {@link PipelineJobWrapper} for the specified {@link PipelineJob}.
     *
     * @param jobRecord the {@link PipelineJobRecordId} that this {@link PipelineJobCallback} is for
     * @param jobRecordStore the {@link PipelineJobRecordStore} to send job monitoring data to
     */
    public PipelineJobCallback(PipelineJobRecord<A> jobRecord) {
      this.jobRecord = jobRecord;
    }

    /** @see com.google.common.util.concurrent.FutureCallback#onSuccess(java.lang.Object) */
    @Override
    public void onSuccess(PipelineJobOutcome result) {
      // Nothing to do here.
    }

    /** @see com.google.common.util.concurrent.FutureCallback#onFailure(java.lang.Throwable) */
    @Override
    public void onFailure(Throwable jobThrowable) {
      if (jobThrowable instanceof CancellationException) {
        /*
         * This is the whole reason we have this extra listener in the first place: it's the only
         * way we have to catch cancel-before-start events (the PipelineJobWrapper can't do it,
         * since it won't get called in the first place).
         */
        jobsEnqueuedHandles.remove(jobRecord.getId());
        jobRecordStore.recordJobCancellation(jobRecord.getId());
      }
    }
  }
}
