package gov.cms.bfd.server.war.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class purely used for unit testing needs. Mimics spring's utility without bringing over
 * the entire library for one method.
 */
public class ReflectionTestUtils {

  private ReflectionTestUtils() {}

  /**
   * Testing utility for setting the value of an object's field, even if it's private/final.
   *
   * @param object The object needing the field value change.
   * @param propertyName The name of the field to change on the object.
   * @param newValue The value to set the field to.
   * @throws NoSuchFieldException If the field does not exist for the object's class.
   * @throws IllegalAccessException If The field could not be accessed for the object/class.
   */
  public static void setField(Object object, String propertyName, Object newValue)
      throws NoSuchFieldException, IllegalAccessException {
    Field field = ReflectionUtils.findField(object.getClass(), propertyName);
    field.setAccessible(true);

    Field fieldModifiers = Field.class.getDeclaredField("modifiers");
    fieldModifiers.setAccessible(true);
    fieldModifiers.setInt(field, fieldModifiers.getModifiers() & ~Modifier.FINAL);

    field.set(object, newValue);
    field.setAccessible(false);
  }
}
