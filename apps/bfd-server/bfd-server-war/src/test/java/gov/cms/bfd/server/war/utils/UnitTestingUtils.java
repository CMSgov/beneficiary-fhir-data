package gov.cms.bfd.server.war.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class UnitTestingUtils {

  @FunctionalInterface
  public interface Executor {
    void execute(Object... args);
  }

  private UnitTestingUtils() {}

  public static void ParameterizedTests(
      Object classObject, String methodName, TestingParameters parameters) {
    try {
      Method testMethod = null;
      Method[] methods = classObject.getClass().getDeclaredMethods();

      for (Method method : methods) {
        if (method.getName().equals(methodName)) {
          if (testMethod == null) {
            testMethod = method;
          } else {
            throw new IllegalStateException(
                "Ambiguous method invocation, multi methods with matching name");
          }
        }
      }

      if (testMethod != null) {
        for (TestingParameters.ParameterSet set : parameters) {
          testMethod.invoke(classObject, set.get());
        }
      } else {
        throw new IllegalAccessException("Target method does not exist on the given class");
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException("Test method invocation failed", e);
    }
  }

  public static class TestingParameters implements Iterable<TestingParameters.ParameterSet> {

    private List<ParameterSet> params;

    public TestingParameters() {
      params = new ArrayList<>();
    }

    public TestingParameters addParameters(Object... params) {
      this.params.add(new ParameterSet(params));
      return this;
    }

    @Override
    public Iterator<ParameterSet> iterator() {
      return params.iterator();
    }

    public class ParameterSet {

      private List<Object> params;

      private ParameterSet(Object... params) {
        this.params = Arrays.asList(params);
      }

      public Object[] get() {
        return params.toArray();
      }
    }
  }
}
