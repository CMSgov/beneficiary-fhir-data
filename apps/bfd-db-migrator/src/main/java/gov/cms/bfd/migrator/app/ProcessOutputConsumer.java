package gov.cms.bfd.migrator.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Managing external processes is tricky: at the OS level, all processes' output is sent to a
 * buffer. If that buffer fills up (because you're not reading the output), the process will block
 * -- forever. To avoid that, it's best to always have a separate thread running that consumes a
 * process' output. This {@link ProcessOutputConsumer} is designed to allow for just that.
 *
 * <p>TODO: BFD-1558 Move to a common location for pipeline and this app
 */
public final class ProcessOutputConsumer implements Runnable {
  private final BufferedReader stdoutReader;
  private final List<String> stdoutContents;

  /**
   * Constructs a new {@link ProcessOutputConsumer} instance.
   *
   * @param process the {@link ProcessOutputConsumer} whose output should be consumed
   */
  public ProcessOutputConsumer(Process process) {
    /*
     * Note: we're only grabbing STDOUT, because we're assuming that
     * STDERR has been piped to/merged with it. If that's not the case,
     * you'd need a separate thread consuming that stream, too.
     */

    InputStream stdout = process.getInputStream();
    this.stdoutReader = new BufferedReader(new InputStreamReader(stdout));
    this.stdoutContents = new ArrayList<>();
  }

  /** @see java.lang.Runnable#run() */
  @Override
  public void run() {
    /*
     * Note: This will naturally stop once the process exits (due to the
     * null check below).
     */

    try {
      String line;
      while ((line = stdoutReader.readLine()) != null) {
        addLine(line);
      }
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  /** @return a {@link String} that contains the <code>STDOUT</code> contents so far */
  public synchronized String getStdoutContents() {
    return String.join("\n", stdoutContents);
  }

  /**
   * Matches every line in the current <code>STDOUT</code> contents looking for one that matches the
   * given predicate. This has to be synchronized to avoid potential
   * ConcurrentModificationExceptions.
   *
   * @param predicate used to test each line of the output
   * @return true if any line matches the predicate
   */
  public synchronized boolean matches(Predicate<String> predicate) {
    return stdoutContents.stream().anyMatch(predicate);
  }

  /**
   * Used internally to add a line of output to the stdoutContents with proper synchronization.
   *
   * @param line text to add to the output
   */
  private synchronized void addLine(String line) {
    stdoutContents.add(line);
  }
}
