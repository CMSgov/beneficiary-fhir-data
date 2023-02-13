package utils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Utility class for tests. */
public class TestUtils {

  /** Private constructor to avoid instantiation. */
  private TestUtils() {
    // Nothing to do.
  }

  /**
   * Sets the value of a class field, even if it's private, and potentially if it's final.
   *
   * @param object The object to change a field value for
   * @param fieldName The name of the field to change the value for
   * @param value The value to set the field to
   * @throws IllegalAccessException If the field could not be accessed
   * @throws NoSuchFieldException If the field did not exist on the class
   */
  public static void setField(Object object, String fieldName, Object value)
      throws IllegalAccessException, NoSuchFieldException {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);

    VarHandle modifiers =
        MethodHandles.privateLookupIn(Field.class, MethodHandles.lookup())
            .findVarHandle(Field.class, "modifiers", int.class);
    modifiers.set(field, field.getModifiers() & ~Modifier.FINAL);

    field.set(object, value);
  }
}
