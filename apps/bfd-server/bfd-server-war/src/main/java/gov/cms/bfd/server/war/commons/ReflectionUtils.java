package gov.cms.bfd.server.war.commons;

import java.lang.reflect.Method;
import java.util.Optional;

public class ReflectionUtils {
  /**
   * Helper function to look up method names and optionally attempt to execute
   *
   * @param <T> the type parameter
   * @param obj the object with the specified method
   * @param methodName the method name
   * @return an Optional result, cast to the generic type. If the method did not exist, or if
   *     invocation failed, returns {@link Optional#empty()}.
   */
  @SuppressWarnings("unchecked")
  public static <T> Optional<T> tryMethod(Object obj, String methodName) {
    try {
      Method func = obj.getClass().getDeclaredMethod(methodName);

      return (Optional<T>) func.invoke(obj);
    }
    // Any reflection errors would be caused by the method not being available
    catch (Exception e) {
      return Optional.empty();
    }
  }
}
