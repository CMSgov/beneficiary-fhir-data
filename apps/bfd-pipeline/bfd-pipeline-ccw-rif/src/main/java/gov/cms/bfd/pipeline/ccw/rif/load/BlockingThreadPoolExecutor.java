package gov.cms.bfd.pipeline.ccw.rif.load;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * <strong>Provenance:</strong> This code was originally included in a no-longer-available article
 * here: <a href=
 * "https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html">
 * Java.net: Creating a NotifyingBlockingThreadPoolExecutor</a>. I was able to snag a copy of the
 * article and source from the Internet Archive's Wayback Machine, here: <a href=
 * "https://web.archive.org/web/20130111220826/https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html">
 * Wayback Machine: Creating a NotifyingBlockingThreadPoolExecutor</a>. The unaltered source zip
 * download is preserved in this project's <code>src/test/resources</code> folder, for posterity.
 *
 * <p>This class is a specialized extension of the ThreadPoolExecutor class.
 *
 * <p>Unlike the subclass, the {@link ThreadPoolExecutor#execute(Runnable)} method of the {@link
 * ThreadPoolExecutor} will block in case the queue is full and only unblock when the queue is
 * dequeued - that is a task that is currently in the queue is removed and handled by the {@link
 * ThreadPoolExecutor}.
 *
 * <p>This subclass of {@link ThreadPoolExecutor} also takes away the max threads capabilities of
 * the {@link ThreadPoolExecutor} superclass and internally sets the amount of maximum threads to be
 * the size of the core threads. This is done since threads over the core size and under the max are
 * instantiated only once the queue is full, but the {@link BlockingThreadPoolExecutor} will block
 * once the queue is full.
 *
 * @author Yaneeve Shekel and Amir Kirsh
 */
public final class BlockingThreadPoolExecutor extends ThreadPoolExecutor {
  /**
   * This constructor is used in order to maintain the first functionality specified above. It does
   * so by using an {@link ArrayBlockingQueue} and the {@link BlockThenRunPolicy} that is defined in
   * this class.
   *
   * <p>This constructor allows to give a timeout for the wait on new task insertion and to react
   * upon such a timeout if occurs.
   *
   * @param poolSize is the amount of threads that this pool may have alive at any given time
   * @param queueSize is the size of the queue. This number should be at least <code>poolSize</code>
   *     to make sense (otherwise there are unused threads), thus if the number sent is smaller, the
   *     <code>poolSize</code> is used for the size of the queue. Recommended value is twice the
   *     <code>poolSize</code>.
   * @param keepAliveTime is the amount of time after which an inactive thread is terminated
   * @param keepAliveTimeUnit is the unit of time to use with the previous parameter
   * @param maxBlockingTime is the maximum time to wait on the queue of tasks before calling <code>
   *     blockingTimeCallback</code>
   * @param maxBlockingTimeUnit is the unit of time to use with the previous parameter
   * @param blockingTimeCallback is the callback method to call when a timeout occurs while blocking
   *     on getting a new task, the return value of this {@link Callable} is Boolean, indicating
   *     whether to keep blocking (<code>true</code>) or stop (<code>false</code>). In case <code>
   *     false</code> is returned from the <code>blockingTimeCallback</code>, this executer will
   *     throw a {@link RejectedExecutionException}
   */
  public BlockingThreadPoolExecutor(
      int poolSize,
      int queueSize,
      long keepAliveTime,
      TimeUnit keepAliveTimeUnit,
      long maxBlockingTime,
      TimeUnit maxBlockingTimeUnit,
      Callable<Boolean> blockingTimeCallback) {
    super(
        poolSize, // Core size
        poolSize, // Max size
        keepAliveTime,
        keepAliveTimeUnit,
        // not smaller than the poolSize (to avoid redundant threads)
        new ArrayBlockingQueue<Runnable>(Math.max(poolSize, queueSize)),
        // When super invokes the reject method this class will ensure a
        // blocking try
        new BlockThenRunPolicy(maxBlockingTime, maxBlockingTimeUnit, blockingTimeCallback));

    super.allowCoreThreadTimeOut(true); // Time out the core threads
  }

  /**
   * This constructor is used in order to maintain the first functionality specified above. It does
   * so by using an {@link ArrayBlockingQueue} and the {@link BlockThenRunPolicy} that is defined in
   * this class.
   *
   * <p>Using this constructor, waiting time on new task insertion is unlimited.
   *
   * @param poolSize is the amount of threads that this pool may have alive at any given time.
   * @param queueSize is the size of the queue. This number should be at least <code>poolSize</code>
   *     to make sense (otherwise there are unused threads), thus if the number sent is smaller, the
   *     <code>poolSize</code> is used for the size of the queue. Recommended value is twice the
   *     <code>poolSize</code>.
   * @param keepAliveTime is the amount of time after which an inactive thread is terminated.
   * @param unit is the unit of time to use with the previous parameter.
   */
  public BlockingThreadPoolExecutor(
      int poolSize, int queueSize, long keepAliveTime, TimeUnit unit) {
    super(
        poolSize, // Core size
        poolSize, // Max size
        keepAliveTime,
        unit,
        // not smaller than the poolSize (to avoid redundant threads)
        new ArrayBlockingQueue<Runnable>(Math.max(poolSize, queueSize)),
        // When super invokes the reject method this class will ensure a
        // blocking try.
        new BlockThenRunPolicy());

    super.allowCoreThreadTimeOut(true); // Time out the core threads.
  }

  /**
   * Internally calls on super's {@link #setCorePoolSize(int)} and {@link #setMaximumPoolSize(int)}
   * methods with the given method argument.
   *
   * @see java.util.concurrent.ThreadPoolExecutor#setCorePoolSize(int)
   */
  @Override
  public void setCorePoolSize(int corePoolSize) {
    super.setCorePoolSize(corePoolSize);
    super.setMaximumPoolSize(corePoolSize);
  }

  /**
   * Does Nothing!
   *
   * @throws UnsupportedOperationException in any event
   * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
   */
  @Override
  public void setMaximumPoolSize(int maximumPoolSize) {
    throw new UnsupportedOperationException("setMaximumPoolSize is not supported.");
  }

  /**
   * Does Nothing! MUST NOT CHANGE OUR BUILT IN {@link RejectedExecutionHandler}.
   *
   * @throws UnsupportedOperationException in any event
   * @see
   *     java.util.concurrent.ThreadPoolExecutor#setRejectedExecutionHandler(RejectedExecutionHandler)
   */
  @Override
  public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
    throw new UnsupportedOperationException(
        "setRejectedExecutionHandler is not allowed on this class.");
  }

  /**
   * This policy class enforces the blocking feature of the {@link BlockingThreadPoolExecutor}. It
   * does so by invoking the {@link BlockingQueue}'s {@link BlockingQueue#put(Object)} method
   * (instead of the {@link BlockingQueue#offer(Object)} method that is used by the standard
   * implementation of the {@link ThreadPoolExecutor} - see the opened Java 6 source code).
   */
  private static class BlockThenRunPolicy implements RejectedExecutionHandler {
    private long maxBlockingTime;
    private TimeUnit maxBlockingTimeUnit;
    private Callable<Boolean> blockingTimeCallback;

    public BlockThenRunPolicy(
        long maxBlockingTime,
        TimeUnit maxBlockingTimeUnit,
        Callable<Boolean> blockingTimeCallback) {
      this.maxBlockingTime = maxBlockingTime;
      this.maxBlockingTimeUnit = maxBlockingTimeUnit;
      this.blockingTimeCallback = blockingTimeCallback;
    }

    public BlockThenRunPolicy() {
      // just keep the maxBlocking gang all null / 0
    }

    /**
     * When this method is invoked by the {@link ThreadPoolExecutor}'s reject method it simply asks
     * for the executor's queue and calls on its put method which will block (at least for the
     * {@link ArrayBlockingQueue}).
     *
     * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(Runnable,
     *     ThreadPoolExecutor)
     */
    @Override
    public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
      BlockingQueue<Runnable> workQueue = executor.getQueue();
      boolean taskSent = false;

      while (!taskSent) {

        if (executor.isShutdown()) {
          throw new RejectedExecutionException(
              "ThreadPoolExecutor has shutdown while attempting to offer a new task.");
        }

        try {
          // check whether to offer (blocking) with a timeout or
          // without
          if (blockingTimeCallback != null) {
            // put on the queue and block if no room is available,
            // with a timeout
            // the result of the call to offer says whether the task
            // was accepted or not
            if (workQueue.offer(task, maxBlockingTime, maxBlockingTimeUnit)) {
              // task accepted
              taskSent = true;
            } else {
              // task was not accepted - call the Callback
              Boolean result = null;
              try {
                result = blockingTimeCallback.call();
              } catch (Exception e) {
                // we got an exception from the Callback, wrap
                // it and throw
                throw new RejectedExecutionException(e);
              }
              // if result if false we need to throw an exception
              // otherwise, just continue with the loop
              if (result == false) {
                throw new RejectedExecutionException(
                    "User decided to stop waiting for task insertion");
              } else {
                continue;
              }
            }

          }
          // no timeout
          else {
            // just send the task (blocking, if the queue is full)
            workQueue.put(task);
            // task accepted
            taskSent = true;
          }
        } catch (InterruptedException e) {
          // someone woke us up and we need to go back to the
          // offer/put call...
        }
      } // end of while for InterruptedException
    }
  }
}
