package gov.cms.bfd.server.ng.testUtil;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory logger for testing. We use a thread-local variable here so the tests can run in
 * parallel without the logs being lumped together. Note that this can only be used with tests that
 * run on the same thread, due to the use of a thread-local. So this can't be used with integration
 * tests that run a server in a background thread.
 */
public class ThreadSafeAppender extends AppenderBase<ILoggingEvent> {
  static ThreadLocal<List<ILoggingEvent>> threadLocal = ThreadLocal.withInitial(ArrayList::new);

  @Override
  public void append(ILoggingEvent e) {
    threadLocal.get().add(e);
  }

  /**
   * Clears out the previous logs and returns a list of events that will get updated as the
   * application runs.
   *
   * @return logging events
   */
  public static List<ILoggingEvent> startRecord() {
    var logs = threadLocal.get();
    logs.clear();
    return logs;
  }
}
