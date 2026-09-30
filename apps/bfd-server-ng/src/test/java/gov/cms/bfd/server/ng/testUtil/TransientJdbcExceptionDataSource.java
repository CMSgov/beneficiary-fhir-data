package gov.cms.bfd.server.ng.testUtil;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.MDC;

@RequiredArgsConstructor
public class TransientJdbcExceptionDataSource implements DataSource {
  private final DataSource inner;
  private final ConcurrentHashMap<String, Boolean> checkedThreads = new ConcurrentHashMap<>();

  /**
   * This ensures all endpoints are resilient to transient JDBC exceptions by throwing an exception
   * the first time a thread is encountered. We store the thread IDs so each thread is only tested
   * once. By running this against every test in the suite, we ensure all endpoints have retries
   * enabled correctly.
   */
  private void checkTestException() {
    var parentThreadIdKey = "parentThreadId";
    var map = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(new ConcurrentHashMap<>());
    // virtual threads will have different IDs every time they're created,
    // so we need to store a reference to the parent thread ID to see if we've already
    // encountered this thread
    var currentThreadId = String.valueOf(Thread.currentThread().threadId());
    var threadId = map.getOrDefault(parentThreadIdKey, currentThreadId);

    // We need to wait until MDC values are initialized to run the test exception.
    // This is because the connection is also aquired at startup which is before the retries are
    // initialized.
    var hasKeys = map.keySet().stream().anyMatch(key -> !key.equals(parentThreadIdKey));
    if (hasKeys && checkedThreads.put(threadId, true) == null) {
      throw new JDBCConnectionException("Test for JDB retries", new SQLException());
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    checkTestException();
    return inner.getConnection();
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    checkTestException();
    return inner.getConnection(username, password);
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    return inner.getLogWriter();
  }

  @Override
  public void setLogWriter(PrintWriter out) throws SQLException {
    inner.setLogWriter(out);
  }

  @Override
  public void setLoginTimeout(int seconds) throws SQLException {
    inner.setLoginTimeout(seconds);
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    return inner.getLoginTimeout();
  }

  @Override
  public Logger getParentLogger() throws SQLFeatureNotSupportedException {
    return inner.getParentLogger();
  }

  @Override
  public <T> T unwrap(Class<T> iface) throws SQLException {
    return inner.unwrap(iface);
  }

  @Override
  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return inner.isWrapperFor(iface);
  }
}
