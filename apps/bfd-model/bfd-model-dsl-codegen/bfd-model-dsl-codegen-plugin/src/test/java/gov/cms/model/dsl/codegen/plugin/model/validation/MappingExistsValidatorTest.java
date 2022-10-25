package gov.cms.model.dsl.codegen.plugin.model.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

/** Unit tests for MappingExistsValidator. */
public class MappingExistsValidatorTest {
  /** Used to mock static method called by the validator. */
  private MockedStatic<ValidationUtil> validationUtil;
  /** Used to mock context passed to the validator. */
  private ConstraintValidatorContext context;
  /** Validator being tested. */
  private MappingExistsValidator validator;

  /** Set up the mocks and the validator being tested. */
  @BeforeEach
  void setUp() {
    validationUtil = Mockito.mockStatic(ValidationUtil.class);
    context = mock(ConstraintValidatorContext.class);
    validator = new MappingExistsValidator();
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

  /** Name with no matching mapping should be invalid. */
  @Test
  public void missingMappingShouldBeInvalid() {
    RootBean root = new RootBean();
    validationUtil
        .when(() -> ValidationUtil.getRootBeanFromContext(context))
        .thenReturn(Optional.of(root));
    assertFalse(validator.isValid("myMapping", context));
  }

  /** Name with a matching mapping should be valid. */
  @Test
  public void presentEnumShouldBeValid() {
    RootBean root = new RootBean();
    MappingBean mapping = MappingBean.builder().id("myMapping").build();
    root.getMappings().add(mapping);

    validationUtil
        .when(() -> ValidationUtil.getRootBeanFromContext(context))
        .thenReturn(Optional.of(root));
    assertTrue(validator.isValid("myMapping", context));
  }
}
