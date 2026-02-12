package gov.cms.bfd.server.ng.testUtil;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.LoggerFactory;

/** In-memory logger for testing that works with virtual threads. */
public class ThreadSafeAsyncAppender extends AppenderBase<ILoggingEvent> {

  private final List<ILoggingEvent> logs = new CopyOnWriteArrayList<>();

  @Override
  protected void append(ILoggingEvent eventObject) {
    logs.add(eventObject);
  }

  public List<ILoggingEvent> getLogs() {
    return logs;
  }

  public static ThreadSafeAsyncAppender createAndAttach() {
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);

    ThreadSafeAsyncAppender appender = new ThreadSafeAsyncAppender();
    appender.setName("TEST_APPENDER_" + System.nanoTime());
    appender.setContext(rootLogger.getLoggerContext());
    appender.start();

    rootLogger.addAppender(appender);

    return appender;
  }

  public void stopAndDetach() {
    Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
    rootLogger.detachAppender(this);
    stop();
  }
}
