package gov.cms.bfd.server.war.utils;

import static org.junit.Assert.fail;

import java.util.Objects;

public class AssertUtils {

  @FunctionalInterface
  public interface Executor {
    void execute();
  }

  public static Exception catchExceptions(Executor executor) {
    Exception ex = null;

    try {
      executor.execute();
    } catch (Exception e) {
      ex = e;
    }

    return ex;
  }

  public static void assertThrowEquals(Exception expected, Exception actual) {
    if (expected.getClass() == actual.getClass()) {
      if (!Objects.equals(expected.getMessage(), actual.getMessage())) {
        fail(
            "expected: "
                + expected.getClass().getCanonicalName()
                + "<"
                + expected.getClass().getCanonicalName()
                + ": "
                + expected.getMessage()
                + "> but was: "
                + actual.getClass().getCanonicalName()
                + "<"
                + actual.getClass().getCanonicalName()
                + ": "
                + actual.getMessage()
                + ">");
      }
    } else {
      fail(
          "expected: "
              + expected.getClass().getCanonicalName()
              + " but was: "
              + actual.getClass().getCanonicalName());
    }
  }
}
