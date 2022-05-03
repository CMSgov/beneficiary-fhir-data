package gov.cms.bfd.model.codegen.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be applied to a Java package to indicate that an {@link Enum} of all known <code>Codebook
 * </code> <code>Variable</code>s should be generated there.
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.CLASS)
public @interface CodebookVariableEnumGeneration {
  /**
   * Gets the simple name of the enum.
   *
   * @return the {@link Class#getSimpleName()} of the {@link Enum} to generate
   */
  String enumName() default "CcwCodebookVariable";
}
