package gov.cms.model.dsl.codegen.plugin.model.validation;

import static java.lang.String.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.ModelBean;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import jakarta.annotation.Nonnull;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

/** Helper class for validating model beans. */
public class ValidationUtil {
  /** Basic regex string for recognizing a component of a valid java identifier. */
  static final String SimpleJavaIdRegex = "[a-z_][a-z0-9_]*";

  /**
   * Validate every mapping in the model and return a list containing a {@link ValidationResult} for
   * each one. Results are sorted by {@link MappingBean#id} value for consistency of results.
   *
   * @param root {@link RootBean} containing all known mappings
   * @return {@link List} containing a {@link ValidationResult} for each mapping in the model
   */
  public static List<ValidationResult> validateModel(RootBean root) {
    return root.getMappings().stream()
        .sorted(MappingBean.IdComparator)
        .map(mapping -> validateMappingBean(root, mapping))
        .collect(ImmutableList.toImmutableList());
  }

  /**
   * Validate a single {@link MappingBean} and return a {@link ValidationResult}.
   *
   * @param root {@link RootBean} containing all known mappings
   * @param mapping {@link MappingBean} to validate
   * @return a {@link ValidationResult}
   */
  private static ValidationResult validateMappingBean(RootBean root, MappingBean mapping) {
    try (ValidatorFactory factory =
        Validation.byProvider(HibernateValidator.class)
            .configure()
            .constraintValidatorPayload(new ValidationPayload(root, mapping))
            .buildValidatorFactory()) {
      final Validator validator = factory.getValidator();
      final Set<ConstraintViolation<MappingBean>> violations = validator.validate(mapping);
      final Set<String> errors =
          violations.stream()
              .map(
                  violation ->
                      format(
                          "property='%s', desc='%s', value='%s' error='%s'",
                          violation.getPropertyPath(),
                          ModelBean.describeBean(violation.getLeafBean()),
                          violation.getInvalidValue(),
                          violation.getMessage()))
              .sorted()
              .collect(ImmutableSet.toImmutableSet());
      return new ValidationResult(mapping, errors);
    }
  }

  /**
   * Retrieve the {@link RootBean} for the model that is being validated in the current context.
   *
   * @param context {@link ConstraintValidatorContext} for the currently executing validation
   * @return the {@link RootBean} for the model being validated
   */
  public static Optional<RootBean> getRootBeanFromContext(ConstraintValidatorContext context) {
    return getValidationPayload(context).map(ValidationPayload::getRoot);
  }

  /**
   * Retrieve the {@link MappingBean} for the mapping that is being validated in the current
   * context.
   *
   * @param context {@link ConstraintValidatorContext} for the currently executing validation
   * @return {@link Optional} containing the {@link MappingBean} for the model being validated or
   *     empty if there was none
   */
  public static Optional<MappingBean> getMappingBeanFromContext(
      ConstraintValidatorContext context) {
    return getValidationPayload(context).map(ValidationPayload::getMapping);
  }

  /**
   * Used in {@link AssertTrue} methods to validate that exactly one object reference is not null.
   *
   * @param objects One or more object references to check for nullness
   * @return true if exactly one reference is not null
   */
  public static boolean isExactlyOneNotNull(Object... objects) {
    int notNullCount = 0;
    for (Object object : objects) {
      if (object != null) {
        notNullCount += 1;
      }
    }
    return notNullCount == 1;
  }

  /**
   * Retrieve the {@link ValidationPayload} from the current context.
   *
   * @param context {@link ConstraintValidatorContext} for the currently executing validation
   * @return {@link Optional} containing the {@link ValidationPayload} from the context or empty if
   *     there was none
   */
  @Nonnull
  private static Optional<ValidationPayload> getValidationPayload(
      ConstraintValidatorContext context) {
    final ValidationPayload payload =
        context
            .unwrap(HibernateConstraintValidatorContext.class)
            .getConstraintValidatorPayload(ValidationPayload.class);
    return Optional.ofNullable(payload);
  }

  /**
   * Bean containing data needed by custom validators. Added to the validation context as payload
   * and then accessed by the validators using static accessor methods in this class.
   */
  @AllArgsConstructor
  @Getter
  private static class ValidationPayload {
    /** Used to look up other objects in the data model. */
    private final RootBean root;

    /**
     * Used to provide context to validator about which mapping the bean being validated belongs to.
     */
    private final MappingBean mapping;
  }

  /** Bean containing a {@link MappingBean} and any associated validation error messages. */
  @AllArgsConstructor
  @Getter
  public static class ValidationResult {
    /** The mapping that was validated. */
    private final MappingBean mapping;

    /** Set containing a message for every error that was found. Empty if none were found. */
    private final Set<String> errors;

    /**
     * Return true if there are any error messages.
     *
     * @return true if errors list is non-empty
     */
    public boolean hasErrors() {
      return !errors.isEmpty();
    }
  }
}
