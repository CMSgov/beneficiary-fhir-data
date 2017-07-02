package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * <strong>Provenance:</strong> This code was originally included in a
 * no-longer-available article here: <a href=
 * "https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html">
 * Java.net: Creating a NotifyingBlockingThreadPoolExecutor</a>. I was able to
 * snag a copy of the article and source from the Internet Archive's Wayback
 * Machine, here: <a href=
 * "https://web.archive.org/web/20130111220826/https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html">
 * Wayback Machine: Creating a NotifyingBlockingThreadPoolExecutor</a>. The
 * unaltered source zip download is preserved in this project's
 * <code>src/test/resources</code> folder, for posterity.
 * </p>
 * 
 * <p>
 * This class is a specialized extension of the ThreadPoolExecutor class.
 * </p>
 * <p>
 * Two functionalities had been added to this subclass. 1) The
 * {@link ThreadPoolExecutor#execute(Runnable)} method of the
 * {@link ThreadPoolExecutor} will block in case the queue is full and only
 * unblock when the queue is dequeued - that is a task that is currently in the
 * queue is removed and handled by the {@link ThreadPoolExecutor}. 2) Client
 * code can await for the event of all tasks being run to conclusion. Client
 * code which actively chose to wait for this occurrence should call await on
 * the instance of his {@link ThreadPoolExecutor}. This differs from
 * {@link ThreadPoolExecutor#awaitTermination(long, TimeUnit)} as it does not
 * require any call to {@link ThreadPoolExecutor#shutdown()}.
 * </p>
 * <p>
 * Code example:
 * </p>
 * 
 * <pre>
 * {@code
 * NotifyingBlockingThreadPoolExecutor threadPoolExecutor = 
 *   new NotifyingBlockingThreadPoolExecutor(5, ,10, 15, TimeUnit.SECONDS);
 *
 * for (int i = 0; i < 5000; i++) { threadPoolExecutor.execute(...) }
 *
 *   try {
 *     threadPoolExecutor.await();
 *   } catch (InterruptedException e) {
 *     // Handle error
 *   }
 *
 *   System.out.println("Done!");
 * }
 * </pre>
 * <p>
 * The example above shows how 5000 tasks are run within 5 threads. The line
 * with '<code>System.out.println("Done!");</code>' will not run until such a
 * time when all the tasks given to the thread pool have concluded. their run.
 * </p>
 * <p>
 * This subclass of {@link ThreadPoolExecutor} also takes away the max threads
 * capabilities of the {@link ThreadPoolExecutor} superclass and internally sets
 * the amount of maximum threads to be the size of the core threads. This is
 * done since threads over the core size and under the max are instantiated only
 * once the queue is full, but the {@link NotifyingBlockingThreadPoolExecutor}
 * will block once the queue is full.
 * </p>
 *
 * @author Yaneeve Shekel and Amir Kirsh
 */
public final class NotifyingBlockingThreadPoolExecutor extends ThreadPoolExecutor {
	/**
	 * Counts the number of current tasks in process
	 */
	private AtomicInteger tasksInProcess = new AtomicInteger();

	/**
	 * This is the {@link Synchronizer} instance that is used in order to notify
	 * all interested code of when all the tasks that have been submitted to the
	 * {@link #execute(Runnable)} method have run to conclusion. This
	 * notification can occur a numerous amount of times. It is all up to the
	 * client code. Whenever the {@link ThreadPoolExecutor} concludes to run all
	 * the tasks the {@link Synchronizer} object will be notified and will in
	 * turn notify the code which is waiting on it.
	 */
	private Synchronizer synchronizer = new Synchronizer();

	/**
	 * <p>
	 * This constructor is used in order to maintain the first functionality
	 * specified above. It does so by using an {@link ArrayBlockingQueue} and
	 * the {@link BlockThenRunPolicy} that is defined in this class.
	 * </p>
	 * <p>
	 * This constructor allows to give a timeout for the wait on new task
	 * insertion and to react upon such a timeout if occurs.
	 * </p>
	 * 
	 * @param poolSize
	 *            is the amount of threads that this pool may have alive at any
	 *            given time
	 * @param queueSize
	 *            is the size of the queue. This number should be at least
	 *            <code>poolSize</code> to make sense (otherwise there are
	 *            unused threads), thus if the number sent is smaller, the
	 *            <code>poolSize</code> is used for the size of the queue.
	 *            Recommended value is twice the <code>poolSize</code>.
	 * @param keepAliveTime
	 *            is the amount of time after which an inactive thread is
	 *            terminated
	 * @param keepAliveTimeUnit
	 *            is the unit of time to use with the previous parameter
	 * @param maxBlockingTime
	 *            is the maximum time to wait on the queue of tasks before
	 *            calling <code>blockingTimeCallback</code>
	 * @param maxBlockingTimeUnit
	 *            is the unit of time to use with the previous parameter
	 * @param blockingTimeCallback
	 *            is the callback method to call when a timeout occurs while
	 *            blocking on getting a new task, the return value of this
	 *            {@link Callable} is Boolean, indicating whether to keep
	 *            blocking (<code>true</code>) or stop (<code>false</code>). In
	 *            case <code>false</code> is returned from the
	 *            <code>blockingTimeCallback</code>, this executer will throw a
	 *            {@link RejectedExecutionException}
	 */
	public NotifyingBlockingThreadPoolExecutor(int poolSize, int queueSize, long keepAliveTime,
			TimeUnit keepAliveTimeUnit, long maxBlockingTime, TimeUnit maxBlockingTimeUnit,
			Callable<Boolean> blockingTimeCallback) {
		super(poolSize, // Core size
				poolSize, // Max size
				keepAliveTime, keepAliveTimeUnit,
				// not smaller than the poolSize (to avoid redundant threads)
				new ArrayBlockingQueue<Runnable>(Math.max(poolSize, queueSize)),
				// When super invokes the reject method this class will ensure a
				// blocking try
				new BlockThenRunPolicy(maxBlockingTime, maxBlockingTimeUnit, blockingTimeCallback));

		super.allowCoreThreadTimeOut(true); // Time out the core threads
	}

	/**
	 * <p>
	 * This constructor is used in order to maintain the first functionality
	 * specified above. It does so by using an {@link ArrayBlockingQueue} and
	 * the {@link BlockThenRunPolicy} that is defined in this class.
	 * </p>
	 * <p>
	 * Using this constructor, waiting time on new task insertion is unlimited.
	 * </p>
	 * 
	 * @param poolSize
	 *            is the amount of threads that this pool may have alive at any
	 *            given time.
	 * @param queueSize
	 *            is the size of the queue. This number should be at least
	 *            <code>poolSize</code> to make sense (otherwise there are
	 *            unused threads), thus if the number sent is smaller, the
	 *            <code>poolSize</code> is used for the size of the queue.
	 *            Recommended value is twice the <code>poolSize</code>.
	 * @param keepAliveTime
	 *            is the amount of time after which an inactive thread is
	 *            terminated.
	 * @param unit
	 *            is the unit of time to use with the previous parameter.
	 */
	public NotifyingBlockingThreadPoolExecutor(int poolSize, int queueSize, long keepAliveTime, TimeUnit unit) {
		super(poolSize, // Core size
				poolSize, // Max size
				keepAliveTime, unit,
				// not smaller than the poolSize (to avoid redundant threads)
				new ArrayBlockingQueue<Runnable>(Math.max(poolSize, queueSize)),
				// When super invokes the reject method this class will ensure a
				// blocking try.
				new BlockThenRunPolicy());

		super.allowCoreThreadTimeOut(true); // Time out the core threads.
	}

	/**
	 * Before calling super's version of this method, the amount of tasks which
	 * are currently in process is first incremented.
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#execute(Runnable)
	 */
	@Override
	public void execute(Runnable task) {
		// count a new task in process
		tasksInProcess.incrementAndGet();
		try {
			super.execute(task);
		} catch (RuntimeException e) {
			// specifically handle RejectedExecutionException
			tasksInProcess.decrementAndGet();
			throw e;
		} catch (Error e) {
			tasksInProcess.decrementAndGet();
			throw e;
		}
	}

	/**
	 * <p>
	 * After calling super's implementation of this method, the amount of tasks
	 * which are currently in process is decremented.
	 * </p>
	 * <p>
	 * Finally, if the amount of tasks currently running is zero the
	 * {@link Synchronizer}'s {@link Synchronizer#signalAll()} method is
	 * invoked, thus anyone awaiting on this instance of
	 * {@link ThreadPoolExecutor} is released.
	 * </p>
	 * 
	 * @see java.util.concurrent.ThreadPoolExecutor#afterExecute(Runnable,
	 *      Throwable)
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		super.afterExecute(r, t);

		// synchronizing on the pool (and actually all its threads)
		// the synchronization is needed to avoid more than one signal if two or
		// more
		// threads decrement almost together and come to the if with 0 tasks
		// together
		synchronized (this) {
			tasksInProcess.decrementAndGet();
			if (tasksInProcess.intValue() == 0) {
				synchronizer.signalAll();
			}
		}
	}

	/**
	 * Internally calls on super's {@link #setCorePoolSize(int)} and
	 * {@link #setMaximumPoolSize(int)} methods with the given method argument.
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
	 * @throws UnsupportedOperationException
	 *             in any event
	 * @see java.util.concurrent.ThreadPoolExecutor#setMaximumPoolSize(int)
	 */
	@Override
	public void setMaximumPoolSize(int maximumPoolSize) {
		throw new UnsupportedOperationException("setMaximumPoolSize is not supported.");
	}

	/**
	 * Does Nothing! MUST NOT CHANGE OUR BUILT IN
	 * {@link RejectedExecutionHandler}.
	 * 
	 * @throws UnsupportedOperationException
	 *             in any event
	 * @see java.util.concurrent.ThreadPoolExecutor#setRejectedExecutionHandler(RejectedExecutionHandler)
	 */
	@Override
	public void setRejectedExecutionHandler(RejectedExecutionHandler handler) {
		throw new UnsupportedOperationException("setRejectedExecutionHandler is not allowed on this class.");
	}

	/**
	 * <p>
	 * A blocking wait for this {@link ThreadPoolExecutor} to be in idle state,
	 * which means that there are no more tasks in the queue or currently
	 * executed by one of the threads.
	 * </p>
	 * <p>
	 * BE AWARE that this method may get out from blocking state when a task is
	 * currently sent to the {@link ThreadPoolExecutor} not from this thread
	 * context. Thus it is not safe to call this method in case there are
	 * several threads feeding the {@link ThreadPoolExecutor} with tasks
	 * (calling {@link #execute(Runnable)}).
	 * </p>
	 * <p>
	 * The safe way to call this method is from the thread that is calling
	 * execute and when there is only one such thread.
	 * </p>
	 * <p>
	 * Note that this method differs from
	 * {@link #awaitTermination(long, TimeUnit)}, as it can be called without
	 * shutting down the {@link ThreadPoolExecutor}.
	 * </p>
	 * 
	 * @throws InterruptedException
	 *             when the internal condition throws it.
	 */
	public void await() throws InterruptedException {
		synchronizer.await();
	}

	/**
	 * A blocking wait for this {@link ThreadPoolExecutor} to be in idle state
	 * or a certain timeout to elapse. Works the same as the {@link #await()}
	 * method, except for adding the timeout condition.
	 * 
	 * @param timeout
	 *            the maximum time to wait
	 * @param timeUnit
	 *            the time unit of the {@code timeout} argument
	 * @return <code>false</code> if the timeout elapsed, <code>true</code> if
	 *         the sync event we are waiting for had happened.
	 * @see NotifyingBlockingThreadPoolExecutor#await() for more details.
	 * @throws InterruptedException
	 *             when the internal condition throws it.
	 */
	public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
		return synchronizer.await(timeout, timeUnit);
	}

	/**
	 * This inner class serves to notify all interested parties that the
	 * {@link ThreadPoolExecutor} has finished running all the tasks given to
	 * its execute method.
	 */
	private class Synchronizer {
		private final Lock lock = new ReentrantLock();
		private final Condition done = lock.newCondition();
		private boolean isDone = false;

		/**
		 * This PRIVATE method allows the ThreadPoolExecutor to notify all
		 * interested parties that all tasks given to the execute method have
		 * run to conclusion.
		 */
		private void signalAll() {

			lock.lock(); // MUST lock!
			try {
				// To help the await method ascertain that it has not waken up
				// 'spuriously'
				isDone = true;
				done.signalAll();
			} finally {
				// Make sure to unlock even in case of an exception
				lock.unlock();
			}
		}

		/**
		 * This is the inner implementation for supporting the
		 * {@link NotifyingBlockingThreadPoolExecutor#await()}.
		 * 
		 * @see NotifyingBlockingThreadPoolExecutor#await() for details.
		 * @throws InterruptedException
		 *             when the internal condition throws it.
		 */
		public void await() throws InterruptedException {

			lock.lock(); // MUST lock!
			try {
				while (!isDone) {
					// Ascertain that this is not a 'spurious wake-up'
					done.await();
				}
			} finally {
				// for next time
				isDone = false;
				// Make sure to unlock even in case of an exception
				lock.unlock();
			}
		}

		/**
		 * This is the inner implementation for supporting the
		 * {@link NotifyingBlockingThreadPoolExecutor#await(long, TimeUnit)}.
		 * 
		 * @param timeout
		 *            the maximum time to wait
		 * @param timeUnit
		 *            the time unit of the {@code timeout} argument
		 * @see NotifyingBlockingThreadPoolExecutor#await(long, TimeUnit) for
		 *      details.
		 * @throws InterruptedException
		 *             when the internal condition throws it.
		 */
		public boolean await(long timeout, TimeUnit timeUnit) throws InterruptedException {
			boolean await_result = false;
			lock.lock(); // MUST lock!
			boolean localIsDone;
			try {
				await_result = done.await(timeout, timeUnit);
			} finally {
				localIsDone = isDone;
				// for next time
				isDone = false;
				// Make sure to unlock even in case of an exception
				lock.unlock();
			}
			// make sure we return true only if done!
			return await_result && localIsDone;
		}
	}

	/**
	 * This policy class enforces the blocking feature of the
	 * {@link NotifyingBlockingThreadPoolExecutor}. It does so by invoking the
	 * {@link BlockingQueue}'s {@link BlockingQueue#put(Object)} method (instead
	 * of the {@link BlockingQueue#offer(Object)} method that is used by the
	 * standard implementation of the {@link ThreadPoolExecutor} - see the
	 * opened Java 6 source code).
	 */
	private static class BlockThenRunPolicy implements RejectedExecutionHandler {
		private long maxBlockingTime;
		private TimeUnit maxBlockingTimeUnit;
		private Callable<Boolean> blockingTimeCallback;

		public BlockThenRunPolicy(long maxBlockingTime, TimeUnit maxBlockingTimeUnit,
				Callable<Boolean> blockingTimeCallback) {
			this.maxBlockingTime = maxBlockingTime;
			this.maxBlockingTimeUnit = maxBlockingTimeUnit;
			this.blockingTimeCallback = blockingTimeCallback;
		}

		public BlockThenRunPolicy() {
			// just keep the maxBlocking gang all null / 0
		}

		/**
		 * When this method is invoked by the {@link ThreadPoolExecutor}'s
		 * reject method it simply asks for the executor's queue and calls on
		 * its put method which will block (at least for the
		 * {@link ArrayBlockingQueue}).
		 * 
		 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(Runnable,
		 *      ThreadPoolExecutor)
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
								throw new RejectedExecutionException("User decided to stop waiting for task insertion");
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
