package gov.cms.bfd.pipeline.rda.grpc.sink.concurrent;

import com.google.common.annotations.VisibleForTesting;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/** Helper class for using a {@link ReadWriteLock} safely. */
public class RWLock {
  private final ReadWriteLock lock;

  /** Creates an instance with a {@link ReentrantReadWriteLock}. */
  public RWLock() {
    this(new ReentrantReadWriteLock());
  }

  /**
   * Create an instance with the specified {@link ReadWriteLock}
   *
   * @param lock the lock to use
   */
  @VisibleForTesting
  RWLock(ReadWriteLock lock) {
    this.lock = lock;
  }

  /**
   * Acquires a non-exclusive read lock and invokes the {@link Supplier} to obtain a return value.
   * The supplier should not make any changes to shared state.
   *
   * @param reader {@link Supplier} to invoke once lock is acquired
   * @return output of calling {@link Supplier}
   * @param <T> type of value returned
   */
  public <T> T read(Supplier<T> reader) {
    lock.readLock().lock();
    try {
      return reader.get();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Acquires a non-exclusive read lock and invokes the {@link Runnable} to perform some action. The
   * action should not make any changes to shared state.
   *
   * @param action {@link Runnable} to invoke once lock is acquired
   */
  public void doRead(Runnable action) {
    lock.readLock().lock();
    try {
      action.run();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Acquires an exclusive write lock and invokes the {@link Supplier} to obtain a return value. The
   * supplier may safely change shared state.
   *
   * @param writer {@link Supplier} to invoke once lock is acquired
   * @return output of calling {@link Supplier}
   * @param <T> type of value returned
   */
  public <T> T write(Supplier<T> writer) {
    lock.readLock().lock();
    try {
      return writer.get();
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Acquires an exclusive write lock and invokes the {@link Runnable} to perform some action. The
   * action may safely change shared state.
   *
   * @param action {@link Runnable} to invoke once lock is acquired
   */
  public void doWrite(Runnable action) {
    lock.readLock().lock();
    try {
      action.run();
    } finally {
      lock.readLock().unlock();
    }
  }
}
