package gov.cms.bfd.server.ng.testUtil;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.LoggerFactory;

/** In-memory logger for testing that works with virtual threads. */
public class ThreadSafeAsyncAppender extends AppenderBase<ILoggingEvent> {
  private static final String APPENDER_NAME = "THREAD_SAFE_TEST_APPENDER";
  private static final List<ILoggingEvent> globalLogs = new CopyOnWriteArrayList<>();
  private static volatile boolean recording = false;

  @Override
  public void append(ILoggingEvent e) {
    if (recording) {
      globalLogs.add(e);
    }
  }

  /** Starts recording logs and returns a list that will be populated. */
  public static List<ILoggingEvent> startRecord() {
    globalLogs.clear();
    recording = true;

    // Ensure appender is attached to root logger
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    // Remove any existing instance first
    rootLogger.detachAppender(APPENDER_NAME);

    ThreadSafeAsyncAppender appender = new ThreadSafeAsyncAppender();
    appender.setName(APPENDER_NAME);
    appender.setContext(rootLogger.getLoggerContext());
    appender.start();
    rootLogger.addAppender(appender);

    return globalLogs;
  }

  /** Stops recording and detaches the appender. */
  public static void stopRecord() {
    recording = false;

    // Detach the appender from root logger
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    Appender<ILoggingEvent> appender = rootLogger.getAppender(APPENDER_NAME);
    if (appender != null) {
      rootLogger.detachAppender(APPENDER_NAME);
      appender.stop();
    }
  }
}
