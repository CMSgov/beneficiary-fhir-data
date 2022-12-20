package gov.cms.model.dsl.codegen.plugin.model;

import javax.annotation.Nullable;

/** Common interface for all model objects. */
public interface ModelBean {
  /**
   * String used in validation error messages.
   *
   * @return string for use in validation error messages
   */
  default String getDescription() {
    return getClass().getSimpleName();
  }

  /**
   * Generate a string for use in a validation error message. If the object is a {@link ModelBean}
   * use its {@link ModelBean#getDescription} method. Otherwise use the class name.
   *
   * @param bean object that was validated
   * @return string to use for error message
   */
  static String describeBean(@Nullable Object bean) {
    if (bean instanceof ModelBean) {
      return ((ModelBean) bean).getDescription();
    } else if (bean != null) {
      return bean.getClass().getSimpleName();
    } else {
      return "null";
    }
  }
}
