package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import gov.cms.model.dsl.codegen.plugin.model.EnumTypeBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Unit tests for EnumExistsInSameMappingValidator. */
public class EnumExistsInSameMappingValidatorTest {
  /** Used to mock static method called by the validator. */
  private MockedStatic<ValidationUtil> validationUtil;
  /** Used to mock context passed to the validator. */
  private ConstraintValidatorContext context;
  /** Validator being tested. */
  private EnumExistsInSameMappingValidator validator;

  /** Set up the mocks and the validator being tested. */
  @BeforeEach
  void setUp() {
    validationUtil = Mockito.mockStatic(ValidationUtil.class);
    context = mock(ConstraintValidatorContext.class);
    validator = new EnumExistsInSameMappingValidator();
  }

  /** Remove the mocks and the validator being tested. */
  @AfterEach
  void tearDown() {
    validationUtil.close();
    validationUtil = null;
    context = null;
    validator = null;
  }

  /** Null values should be valid. */
  @Test
  public void nullShouldBeValid() {
    assertTrue(validator.isValid(null, context));
  }

  /** Name with no matching enum should be invalid. */
  @Test
  public void missingEnumShouldBeInvalid() {
    MappingBean mapping = new MappingBean();
    validationUtil
        .when(() -> ValidationUtil.getMappingBeanFromContext(context))
        .thenReturn(Optional.of(mapping));
    assertFalse(validator.isValid("myEnum", context));
  }

  /** Name with a matching enum should be valid. */
  @Test
  public void presentEnumShouldBeValid() {
    MappingBean mapping = new MappingBean();
    mapping.getEnumTypes().add(EnumTypeBean.builder().name("myEnum").build());

    validationUtil
        .when(() -> ValidationUtil.getMappingBeanFromContext(context))
        .thenReturn(Optional.of(mapping));
    assertTrue(validator.isValid("myEnum", context));
  }
}
