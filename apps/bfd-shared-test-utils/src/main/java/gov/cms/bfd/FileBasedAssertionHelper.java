package gov.cms.bfd;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Some tests read data in a file produced while a test is running. Such tests cannot be executed in
 * parallel since multiple threads would either overwrite each others output or produce output that
 * would mislead other tests.
 *
 * <p>This class provides a way to access content in a file easily within tests as well as to
 * synchronize use of the file by tests.
 */
public class FileBasedAssertionHelper {
  /** The file being used in tests. */
  private final Path fileForTest;

  /**
   * The lock used to synchronize access to the file within tests. This is strictly a JVM lock. Not
   * a file lock. Multiple threads need to use the same instance of {@link FileBasedAssertionHelper}
   * to obtain proper synchronization.
   */
  private final ReentrantLock lock;

  /**
   * Initializes an instance.
   *
   * @param fileForTest the file being used for the test
   */
  public FileBasedAssertionHelper(Path fileForTest) {
    this.fileForTest = fileForTest;
    lock = new ReentrantLock();
  }

  /**
   * Wait for a lock and optionally truncate the file. Call this from a {@code @BeforeEach} method
   * in your test class.
   *
   * @param truncateFile true causes file to be truncated (set to 0 length)
   * @param maxWaitSeconds maximum amount of time to wait for lock
   * @throws InterruptedException lock acquisition interrupted
   * @throws IOException file truncation failed
   */
  public void beginTest(boolean truncateFile, long maxWaitSeconds)
      throws InterruptedException, IOException {
    boolean locked = lock.tryLock(maxWaitSeconds, TimeUnit.SECONDS);
    assertTrue(locked, "Unable to lock file within timeout period: " + fileForTest);
    if (truncateFile) {
      try (var ignored = new FileOutputStream(fileForTest.toFile(), false)) {
        // just opening the stream will truncate the file
      }
    }
  }

  /**
   * Release the lock if we have one. Call this from a {@code @AfterEach} method in your test class.
   */
  public void endTest() {
    if (lock.isHeldByCurrentThread()) {
      lock.unlock();
    }
  }

  /**
   * Reads the entire file as a single string.
   *
   * @return the string
   */
  public String readFileAsString() {
    try {
      return Files.readString(fileForTest, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      fail("unable to read log file: " + fileForTest);
      // this is never reached but compiler doesn't know that
      return "";
    }
  }

  /**
   * Reads the entire file as a {@link List} containing each line as a separate string.
   *
   * @return the string
   */
  public List<String> readFileAsIndividualLines() {
    try {
      return Files.readAllLines(fileForTest, StandardCharsets.UTF_8);
    } catch (IOException ex) {
      fail("unable to read log file: " + fileForTest);
      // this is never reached but compiler doesn't know that
      return List.of();
    }
  }
}
