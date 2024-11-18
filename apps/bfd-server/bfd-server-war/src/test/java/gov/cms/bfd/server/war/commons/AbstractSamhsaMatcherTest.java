package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.DiagnosisComponent;
import gov.cms.bfd.server.war.adapters.FhirResource;
import gov.cms.bfd.server.war.adapters.ItemComponent;
import gov.cms.bfd.server.war.adapters.ProcedureComponent;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/** Tests the abstract samhsa matcher. */
public class AbstractSamhsaMatcherTest {

  /** Constant for an expected {@link IllegalStateException}. */
  private static final IllegalStateException INVOCATION_EXCEPTION =
      new IllegalStateException("Method was not called with expected arguments for test");

  /** A mock Coding to be used as a HCPCS code. */
  private static final Coding HCPCS_CODING = mock(Coding.class);

  /** A mock Coding to be used as a SAMHSA code. */
  private static final Coding SAMHSA_CODING = mock(Coding.class);

  /** A mock Coding to be used as a non-hcpcs, non-samhsa code. */
  private static final Coding OTHER_CODING = mock(Coding.class);

  /** A list of that contains a HCPCs coding. */
  private static final List<Coding> CODINGS_WITH_HCPCS =
      List.of(OTHER_CODING, HCPCS_CODING, SAMHSA_CODING, OTHER_CODING);

  /** A list of that does not contain a HCPCs coding. */
  private static final List<Coding> CODINGS_WITHOUT_HCPCS =
      List.of(OTHER_CODING, SAMHSA_CODING, OTHER_CODING);

  /** A mock Coding to be used as a non-DRG code. */
  private static final Coding NON_DRG_CODING = mock(Coding.class);

  /** A mock Coding to be used as a DRG non-SAMHSA code. */
  private static final Coding DRG_NON_SAMHSA_CODING = mock(Coding.class);

  /** A mock Coding to be used as a DRG + SAMHSA code. */
  private static final Coding DRG_SAMHSA_CODING = mock(Coding.class);

  /** A mock Coding to be used as a non-DRG code. */
  private static final CodeableConcept PACKAGING_CONCEPT = mock(CodeableConcept.class);

  /** Interface for the samhsa filter method. */
  @FunctionalInterface
  public interface SamhsaFilterMethod<T> {

    /**
     * Method to apply a matcher to a coding to check if it's SAMHSA.
     *
     * @param matcher the matcher to use
     * @param coding the coding to pass to the matcher
     * @return {@code true} if the matcher matches for the given coding
     */
    boolean apply(AbstractSamhsaMatcher<IBaseResource> matcher, T coding);
  }

  /** The non parameterized tests. */
  @Nested
  public class NonParameterizedTests {

    /**
     * Test to see that {@link AbstractSamhsaMatcher#resourceCsvColumnToList(String, String)} pulls
     * the expected column data from the given file.
     */
    @Test
    public void shouldReturnColumnValues() {
      List<String> expected = List.of("1", "2", "3", "4");
      List<String> actual =
          AbstractSamhsaMatcher.resourceCsvColumnToList("samhsa_codes_test_file.csv", "columnB");

      assertEquals(expected, actual);
    }

    /**
     * Test to see that {@link AbstractSamhsaMatcher#containsSamhsaIcdProcedureCode(List)} returns
     * true if at least one of the {@link ProcedureComponent}s in the given list contains a SAMHSA
     * {@link Coding}.
     *
     * <p>For terseness, this test only checks the scenario of 3 components, with the middle
     * possible containing SAMHSA, expecting the logic should hold for a list of any size.
     */
    @Test
    public void shouldReturnTrueIfAtLeastOneProcedureComponentContainsSamhsa() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(ProcedureComponent.class));

      ProcedureComponent mockComponentA = mock(ProcedureComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentA);

      ProcedureComponent mockComponentB = mock(ProcedureComponent.class);
      doReturn(true).when(matcherSpy).isSamhsaIcdProcedure(mockComponentB);

      ProcedureComponent mockComponentC = mock(ProcedureComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentC);

      assertTrue(
          matcherSpy.containsSamhsaIcdProcedureCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)),
          "Samhsa procedure not correctly filtered");
    }

    /**
     * Test to see that {@link AbstractSamhsaMatcher#containsSamhsaIcdProcedureCode(List)} returns
     * false if none of the {@link ProcedureComponent}s in the given list contains a SAMHSA {@link
     * Coding}.
     *
     * <p>For terseness, this test only checks the scenario of 3 components, with the middle
     * possible containing SAMHSA, expecting the logic should hold for a list of any size.
     */
    @Test
    public void shouldReturnFalseForNonSamhsaProcedureComponent() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(ProcedureComponent.class));

      ProcedureComponent mockComponentA = mock(ProcedureComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentA);

      ProcedureComponent mockComponentB = mock(ProcedureComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentB);

      ProcedureComponent mockComponentC = mock(ProcedureComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentC);

      assertFalse(
          matcherSpy.containsSamhsaIcdProcedureCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)),
          "Non-Samhsa procedure incorrectly filtered");
    }

    /**
     * Test to see that {@link AbstractSamhsaMatcher#containsSamhsaIcdDiagnosisCode(List)} returns
     * true if at least one of the {@link DiagnosisComponent}s in the given list contains a SAMHSA
     * {@link Coding}.
     *
     * <p>For terseness, this test only checks the scenario of 3 components, with the middle
     * possible containing SAMHSA, expecting the logic should hold for a list of any size.
     */
    @Test
    public void shouldReturnTrueForSamhsaDiagnosisComponent() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaDiagnosis(any(DiagnosisComponent.class));

      DiagnosisComponent mockComponentA = mock(DiagnosisComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentA);

      DiagnosisComponent mockComponentB = mock(DiagnosisComponent.class);
      doReturn(true).when(matcherSpy).isSamhsaDiagnosis(mockComponentB);

      DiagnosisComponent mockComponentC = mock(DiagnosisComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentC);

      assertTrue(
          matcherSpy.containsSamhsaIcdDiagnosisCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)),
          "Samhsa diagnosis not correctly filtered");
    }

    /**
     * Test to see that {@link AbstractSamhsaMatcher#containsSamhsaIcdDiagnosisCode(List)} returns
     * false if none of the {@link DiagnosisComponent}s in the given list contains a SAMHSA {@link
     * Coding}.
     *
     * <p>For terseness, this test only checks the scenario of 3 components, with the middle
     * possible containing SAMHSA, expecting the logic should hold for a list of any size.
     */
    @Test
    public void shouldReturnFalseForNonSamhsaDiagnosisComponent() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaDiagnosis(any(DiagnosisComponent.class));

      DiagnosisComponent mockComponentA = mock(DiagnosisComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentA);

      DiagnosisComponent mockComponentB = mock(DiagnosisComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentB);

      DiagnosisComponent mockComponentC = mock(DiagnosisComponent.class);
      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentC);

      assertFalse(
          matcherSpy.containsSamhsaIcdDiagnosisCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)),
          "Non-Samhsa diagnosis incorrectly filtered");
    }

    /**
     * Test to see that {@link AbstractSamhsaMatcher#containsSamhsaLineItem(List)} returns true if
     * at least one of the {@link ItemComponent}s in the given list contains a SAMHSA {@link
     * Coding}.
     *
     * <p>For terseness, this test only checks the scenario of 3 components, with the middle
     * possible containing SAMHSA, expecting the logic should hold for a list of any size.
     */
    @Test
    public void shouldReturnTrueForSamhsaLineItem() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .containsSamhsaProcedureCode(any(CodeableConcept.class));

      ItemComponent mockComponentA = mock(ItemComponent.class);
      CodeableConcept mockConceptA = mock(CodeableConcept.class);
      doReturn(mockConceptA).when(mockComponentA).getProductOrService();
      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptA);

      ItemComponent mockComponentB = mock(ItemComponent.class);
      CodeableConcept mockConceptB = mock(CodeableConcept.class);
      doReturn(mockConceptB).when(mockComponentB).getProductOrService();
      doReturn(true).when(matcherSpy).containsSamhsaProcedureCode(mockConceptB);

      ItemComponent mockComponentC = mock(ItemComponent.class);
      CodeableConcept mockConceptC = mock(CodeableConcept.class);
      doReturn(mockConceptC).when(mockComponentC).getProductOrService();
      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptC);

      assertTrue(
          matcherSpy.containsSamhsaLineItem(
              List.of(mockComponentA, mockComponentB, mockComponentC)),
          "Samhsa line item not correctly filtered");
    }

    /**
     * Test to see that {@link AbstractSamhsaMatcher#containsSamhsaLineItem(List)} returns false if
     * none of the {@link ItemComponent}s in the given list contains a SAMHSA {@link Coding}.
     *
     * <p>For terseness, this test only checks the scenario of 3 components, with the middle
     * possible containing SAMHSA, expecting the logic should hold for a list of any size.
     */
    @Test
    public void shouldReturnFalseForNonSamhsaLineItems() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .containsSamhsaProcedureCode(any(CodeableConcept.class));

      ItemComponent mockComponentA = mock(ItemComponent.class);
      CodeableConcept mockConceptA = mock(CodeableConcept.class);
      doReturn(mockConceptA).when(mockComponentA).getProductOrService();
      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptA);

      ItemComponent mockComponentB = mock(ItemComponent.class);
      CodeableConcept mockConceptB = mock(CodeableConcept.class);
      doReturn(mockConceptB).when(mockComponentB).getProductOrService();
      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptB);

      ItemComponent mockComponentC = mock(ItemComponent.class);
      CodeableConcept mockConceptC = mock(CodeableConcept.class);
      doReturn(mockConceptC).when(mockComponentC).getProductOrService();
      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptC);

      assertFalse(
          matcherSpy.containsSamhsaLineItem(
              List.of(mockComponentA, mockComponentB, mockComponentC)),
          "Non-Samhsa line items inorrectly filtered");
    }

    /**
     * Test to see if {@link AbstractSamhsaMatcher#isSamhsaIcdProcedure(ProcedureComponent)} returns
     * true if the given {@link ProcedureComponent} contains a {@link CodeableConcept} with SAMHSA
     * data.
     */
    @Test
    public void shouldReturnTrueForIsSamhsaIcdProcedure() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ProcedureComponent mockComponent = mock(ProcedureComponent.class);
      CodeableConcept mockConcept = mock(CodeableConcept.class);

      doReturn(mockConcept).when(mockComponent).getProcedureCodeableConcept();

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(CodeableConcept.class));

      doReturn(true).when(matcherSpy).isSamhsaIcdProcedure(mockConcept);

      assertTrue(
          matcherSpy.isSamhsaIcdProcedure(mockComponent),
          "Samhsa procedure concept not correctly filtered");
    }

    /**
     * Test to see if {@link AbstractSamhsaMatcher#isSamhsaIcdProcedure(ProcedureComponent)} returns
     * false if the given {@link ProcedureComponent} contains no {@link CodeableConcept} with SAMHSA
     * data.
     */
    @Test
    public void shouldReturnFalseForIsSamhsaIcdProcedure() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ProcedureComponent mockComponent = mock(ProcedureComponent.class);
      CodeableConcept mockConcept = mock(CodeableConcept.class);

      doReturn(mockConcept).when(mockComponent).getProcedureCodeableConcept();

      // Prevent default logic from executing
      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(CodeableConcept.class));

      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockConcept);

      assertFalse(
          matcherSpy.isSamhsaIcdProcedure(mockComponent),
          "Samhsa procedure concept not correctly filtered");
    }

    /**
     * Test to see if {@link AbstractSamhsaMatcher#isSamhsaDiagnosis(CodeableConcept)} correctly
     * invokes the expected sibling method ({@link
     * AbstractSamhsaMatcher#isSamhsaCoding(CodeableConcept, Predicate, Predicate, Predicate,
     * Predicate, Predicate)}) with the expected parameters.
     */
    @Test
    public void shouldCorrectlyInvokeIsSamhsaCodingForDiagnosis() {
      CodeableConcept mockConcept = mock(CodeableConcept.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      doReturn(false)
          .when(matcherSpy)
          .isSamhsaCoding(
              any(CodeableConcept.class),
              any(Predicate.class),
              any(Predicate.class),
              any(Predicate.class),
              any(Predicate.class),
              any(Predicate.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd9Diagnosis(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd10Diagnosis(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd10CmDiagnosis(any(Coding.class));

      ArgumentCaptor<CodeableConcept> conceptCaptor =
          ArgumentCaptor.forClass(CodeableConcept.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd9CallCaptor = ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd9MedicareCallCaptor =
          ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10CallCaptor = ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10CmCallCaptor =
          ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10MedicareCallCaptor =
          ArgumentCaptor.forClass(Predicate.class);

      matcherSpy.isSamhsaDiagnosis(mockConcept);

      // Capture the arguments used when it was invoked
      verify(matcherSpy)
          .isSamhsaCoding(
              conceptCaptor.capture(),
              icd9CallCaptor.capture(),
              icd9MedicareCallCaptor.capture(),
              icd10CallCaptor.capture(),
              icd10CmCallCaptor.capture(),
              icd10MedicareCallCaptor.capture());

      assertEquals(mockConcept, conceptCaptor.getValue());

      Coding mockCoding = mock(Coding.class);

      // Invoke the captured lambdas to check they were the right ones
      icd9CallCaptor.getValue().test(mockCoding);
      icd10CallCaptor.getValue().test(mockCoding);
      icd10CmCallCaptor.getValue().test(mockCoding);
      assertThrows(
          BadCodeMonkeyException.class, () -> icd9MedicareCallCaptor.getValue().test(mockCoding));
      assertThrows(
          BadCodeMonkeyException.class, () -> icd10MedicareCallCaptor.getValue().test(mockCoding));

      verify(matcherSpy, times(1)).isSamhsaIcd9Diagnosis(mockCoding);
      verify(matcherSpy, times(1)).isSamhsaIcd10Diagnosis(mockCoding);
      verify(matcherSpy, times(1)).isSamhsaIcd10CmDiagnosis(mockCoding);
    }

    /**
     * Tests that the SAMHSA matcher methods are invoked as expected when {@link
     * AbstractSamhsaMatcher#isSamhsaIcdProcedure} is called.
     */
    @Test
    public void shouldCorrectlyInvokeIsSamhsaCodingForProcedure() {
      CodeableConcept mockConcept = mock(CodeableConcept.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      doReturn(false)
          .when(matcherSpy)
          .isSamhsaCoding(
              any(CodeableConcept.class),
              any(Predicate.class),
              any(Predicate.class),
              any(Predicate.class),
              any(Predicate.class),
              any(Predicate.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd9Procedure(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd9MedicareProcedure(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd10Procedure(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd10MedicareProcedure(any(Coding.class));

      ArgumentCaptor<CodeableConcept> conceptCaptor =
          ArgumentCaptor.forClass(CodeableConcept.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd9CallCaptor = ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd9MedicareCallCaptor =
          ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10CallCaptor = ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10CmCallCaptor =
          ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10MedicareCallCaptor =
          ArgumentCaptor.forClass(Predicate.class);

      matcherSpy.isSamhsaIcdProcedure(mockConcept);

      // Capture the arguments used when it was invoked
      verify(matcherSpy)
          .isSamhsaCoding(
              conceptCaptor.capture(),
              icd9CallCaptor.capture(),
              icd9MedicareCallCaptor.capture(),
              icd10CallCaptor.capture(),
              icd10CmCallCaptor.capture(),
              icd10MedicareCallCaptor.capture());

      assertEquals(mockConcept, conceptCaptor.getValue());

      Coding mockCoding = mock(Coding.class);

      // Invoke the captured lambdas to check they were the right ones
      icd9CallCaptor.getValue().test(mockCoding);
      icd9MedicareCallCaptor.getValue().test(mockCoding);
      icd10CallCaptor.getValue().test(mockCoding);
      icd10MedicareCallCaptor.getValue().test(mockCoding);
      assertThrows(
          BadCodeMonkeyException.class, () -> icd10CmCallCaptor.getValue().test(mockCoding));

      verify(matcherSpy, times(1)).isSamhsaIcd9Procedure(mockCoding);
      verify(matcherSpy, times(1)).isSamhsaIcd9MedicareProcedure(mockCoding);
      verify(matcherSpy, times(1)).isSamhsaIcd10Procedure(mockCoding);
      verify(matcherSpy, times(1)).isSamhsaIcd10MedicareProcedure(mockCoding);
    }
  }

  /**
   * Data method for the isSamhsaDiagnosisTest. Used automatically via the MethodSource annotation.
   *
   * @return the data for the test
   */
  private static Stream<Arguments> isSamhsaDiagnosisTest() {
    return Stream.of(
        arguments(false, false, false, "Non-samhsa diagnosis incorrectly filtered"),
        arguments(true, false, true, "Samhsa diagnosis not correctly filtered"),
        arguments(false, true, true, "Samhsa package not correctly filtered"),
        arguments(true, true, true, "Samhsa diagnosis & package not correctly filtered"));
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaDiagnosis(DiagnosisComponent)}.
   *
   * <p>The parameterized tests check to see if either/both the Diagnosis Codeable Concept and/or
   * Packaging Code contains SAMHSA data
   *
   * <p>Two different calls are made to {@link
   * AbstractSamhsaMatcher#isSamhsaDiagnosis(CodeableConcept)}* and {@link
   * AbstractSamhsaMatcher#isSamhsaPackageCode(CodeableConcept)}* from the primary method, which are
   * both mocked within the test
   *
   * @param expectedIsSamhsaDiagnosis the expected is samhsa diagnosis
   * @param expectedIsSamhsaPackage the expected is samhsa package
   * @param expectedResult the expected result
   * @param errorMessage the error message if there is not a match
   */
  @ParameterizedTest(
      name = "{index}: IsSamhsaDiagnosis(\"{0}\"), IsSamhsaPackage(\"{1}\"), Expected(\"{2}\")")
  @MethodSource
  public void isSamhsaDiagnosisTest(
      boolean expectedIsSamhsaDiagnosis,
      boolean expectedIsSamhsaPackage,
      boolean expectedResult,
      String errorMessage) {
    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

    CodeableConcept mockDiagnosisConcept = mock(CodeableConcept.class);
    CodeableConcept mockPackageConcept = mock(CodeableConcept.class);

    DiagnosisComponent mockComponent = mock(DiagnosisComponent.class);

    doReturn(mockDiagnosisConcept).when(mockComponent).getDiagnosisCodeableConcept();

    doReturn(mockPackageConcept).when(mockComponent).getPackageCode();

    // Prevent default logic from executing
    doThrow(INVOCATION_EXCEPTION).when(matcherSpy).isSamhsaDiagnosis(any(CodeableConcept.class));

    doThrow(INVOCATION_EXCEPTION).when(matcherSpy).isSamhsaPackageCode(any(CodeableConcept.class));

    doReturn(expectedIsSamhsaDiagnosis).when(matcherSpy).isSamhsaDiagnosis(mockDiagnosisConcept);

    doReturn(expectedIsSamhsaPackage).when(matcherSpy).isSamhsaPackageCode(mockPackageConcept);

    assertEquals(expectedResult, matcherSpy.isSamhsaDiagnosis(mockComponent), errorMessage);
  }

  /**
   * Data method for the samhsaCodingTest. Used automatically via the MethodSource annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> samhsaCodingTest() {
    return Stream.of(
        arguments(
            IcdCode.CODING_SYSTEM_ICD_9,
            mock(CodeableConcept.class),
            true,
            false,
            true,
            "ICD 9 System Coding expected to be marked samhsa but wasn't."),
        arguments(
            IcdCode.CODING_SYSTEM_ICD_9,
            mock(CodeableConcept.class),
            false,
            false,
            false,
            "ICD9 System Coding incorrectly marked samhsa."),
        arguments(
            IcdCode.CODING_SYSTEM_ICD_10,
            mock(CodeableConcept.class),
            false,
            true,
            true,
            "ICD 10 System Coding expected to be marked samhsa but wasn't."),
        arguments(
            IcdCode.CODING_SYSTEM_ICD_10,
            mock(CodeableConcept.class),
            false,
            false,
            false,
            "ICD10 System Coding incorrectly marked samhsa."),
        arguments(
            IcdCode.CODING_SYSTEM_ICD_10_CM,
            mock(CodeableConcept.class),
            false,
            true,
            true,
            "ICD 10 CM System Coding expected to be marked samhsa but wasn't."),
        arguments(
            IcdCode.CODING_SYSTEM_ICD_10_CM,
            mock(CodeableConcept.class),
            false,
            false,
            false,
            "ICD 10 CM System Coding incorrectly marked samhsa."),
        arguments(
            IcdCode.CODING_SYSTEM_ICD_10_MEDICARE,
            mock(CodeableConcept.class),
            false,
            true,
            true,
            "ICD 10 Medicare System Coding expected to be marked samhsa but wasn't."),
        arguments(
            IcdCode.CODING_SYSTEM_ICD_10_MEDICARE,
            mock(CodeableConcept.class),
            false,
            false,
            false,
            "ICD 10 Medicare System Coding incorrectly marked samhsa."),
        arguments(
            "other/unknown system",
            mock(CodeableConcept.class),
            false,
            false,
            true,
            "Other/unknown System Coding expected to be marked samhsa but wasn't."),
        arguments(
            "doesn't matter",
            null,
            false,
            false,
            false,
            "Missing concept incorrectly marked samhsa."));
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaCoding(CodeableConcept, Predicate,
   * Predicate, Predicate, Predicate, Predicate)}.
   *
   * <p>The target method takes a {@link CodeableConcept} and two {@link Predicate}s (one for ICD9
   * checks and one for ICD10 checks). The test checks each combination of coding system/predicate
   * result.
   *
   * @param system the system to return in the mock coding
   * @param concept the codable concept with codings to test
   * @param isIcd9Code the value to set for the icd9 code predicate test
   * @param isIcd10Code the value to set for the icd10 code predicate test
   * @param expectedResult the expected result
   * @param errorMessage the error message in the event of a test failure
   */
  @ParameterizedTest(
      name =
          "{index}: System(\"{0}\"), isSamsaICD9(\"{2}\"), isSamsaICD10(\"{3}\"), Expected(\"{4}\")")
  @MethodSource
  public void samhsaCodingTest(
      String system,
      CodeableConcept concept,
      boolean isIcd9Code,
      boolean isIcd10Code,
      boolean expectedResult,
      String errorMessage) {
    Coding mockCoding = mock(Coding.class);

    doReturn(system).when(mockCoding).getSystem();

    doReturn("some code").when(mockCoding).getCode();

    Optional.ofNullable(concept).ifPresent(c -> doReturn(List.of(mockCoding)).when(c).getCoding());

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

    doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    Predicate<Coding> mockPredicateIcd9 = mock(Predicate.class);
    doReturn(isIcd9Code).when(mockPredicateIcd9).test(mockCoding);

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    Predicate<Coding> mockPredicateIcd9Medicare = mock(Predicate.class);
    doReturn(isIcd9Code).when(mockPredicateIcd9Medicare).test(mockCoding);

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    Predicate<Coding> mockPredicateIcd10 = mock(Predicate.class);
    doReturn(isIcd10Code).when(mockPredicateIcd10).test(mockCoding);

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    Predicate<Coding> mockPredicateIcd10Cm = mock(Predicate.class);
    doReturn(isIcd10Code).when(mockPredicateIcd10Cm).test(mockCoding);

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    Predicate<Coding> mockPredicateIcd10Medicare = mock(Predicate.class);
    doReturn(isIcd10Code).when(mockPredicateIcd10Medicare).test(mockCoding);

    assertEquals(
        expectedResult,
        matcherSpy.isSamhsaCoding(
            concept,
            mockPredicateIcd9,
            mockPredicateIcd9Medicare,
            mockPredicateIcd10,
            mockPredicateIcd10Cm,
            mockPredicateIcd10Medicare),
        errorMessage);
  }

  /**
   * Data method for the containsSamhsaProcedureCodeTest. Used automatically via the MethodSource
   * annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> containsSamhsaProcedureCodeTest() {
    return Stream.of(
        arguments("Empty codings", true, true, false, false, "should NOT be filtered, but WAS."),
        arguments(
            "Non-empty codings with only known systems with no SAMHSA codes",
            false,
            false,
            true,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Non-empty codings with SAMHSA codes",
            false,
            true,
            true,
            true,
            "SHOULD be filtered, but was NOT."),
        arguments(
            "Non-empty codings with unknown system containing no SAMHSA codes",
            false,
            false,
            false,
            true,
            "SHOULD be filtered, but was NOT."),
        arguments(
            "Non-empty codings with unknown system containing SAMHSA codes",
            false,
            true,
            false,
            true,
            "SHOULD be filtered, but was NOT."));
  }

  /**
   * Parameterized tests for {@link
   * AbstractSamhsaMatcher#containsSamhsaProcedureCode(CodeableConcept)}.
   *
   * <p>Tests to see if every combination of coding list size, hcpcs result and known system check
   * returns the expected result.
   *
   * <p>If the codings list is empty, it should return false. If the concept contains a HCPCS samhsa
   * code or any unknown systems, it should return true.
   *
   * @param name the test name, for reporting
   * @param codingsListEmpty if the codings list should report as empty
   * @param hasHcpcsSystemAndSmahsaCptCode the value to return from {@link
   *     AbstractSamhsaMatcher#hasHcpcsSystemAndSamhsaCptCode}
   * @param containsOnlyKnownSystems the value to return from {@link
   *     AbstractSamhsaMatcher#containsOnlyKnownSystems}
   * @param expectedResult the expected result
   * @param errorMessage the error message if a test fails
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void containsSamhsaProcedureCodeTest(
      String name,
      boolean codingsListEmpty,
      boolean hasHcpcsSystemAndSmahsaCptCode,
      boolean containsOnlyKnownSystems,
      boolean expectedResult,
      String errorMessage) {
    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    List<Coding> mockList = mock(List.class);
    doReturn(codingsListEmpty).when(mockList).isEmpty();

    CodeableConcept mockConcept = mock(CodeableConcept.class);
    doReturn(mockList).when(mockConcept).getCoding();

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

    // Prevent default logic from executing
    doThrow(INVOCATION_EXCEPTION)
        .when(matcherSpy)
        .hasHcpcsSystemAndSamhsaCptCode(any(CodeableConcept.class));

    doThrow(INVOCATION_EXCEPTION)
        .when(matcherSpy)
        .containsOnlyKnownSystems(any(CodeableConcept.class));

    doReturn(hasHcpcsSystemAndSmahsaCptCode)
        .when(matcherSpy)
        .hasHcpcsSystemAndSamhsaCptCode(mockConcept);
    doReturn(containsOnlyKnownSystems).when(matcherSpy).containsOnlyKnownSystems(mockConcept);

    assertEquals(
        expectedResult,
        matcherSpy.containsSamhsaProcedureCode(mockConcept),
        name + " " + errorMessage);
  }

  /**
   * Data method for the samhsaCodingTest. Used automatically via the MethodSource annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> hasHcpcsSystemAndSamhsaCptCodeTest() {
    return Stream.of(
        arguments(
            "Empty coding list with obviously no SAMHSA CPT code",
            Collections.emptyList(),
            false,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Empty coding list, with SAMHSA CPT code (should not be possible)",
            Collections.emptyList(),
            true,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Non-HCPCS system with no SAMHSA CPT code",
            CODINGS_WITHOUT_HCPCS,
            false,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Non-HCPCS system with SAMHSA CPT code",
            CODINGS_WITHOUT_HCPCS,
            true,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "HCPCS system with no SAMHSA CPT code",
            CODINGS_WITH_HCPCS,
            false,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "HCPCS system with SAMHSA CPT code",
            CODINGS_WITH_HCPCS,
            true,
            true,
            "SHOULD be filtered, but was NOT."));
  }

  /**
   * Parameterized tests for {@link
   * AbstractSamhsaMatcher#hasHcpcsSystemAndSamhsaCptCode(CodeableConcept)}*
   *
   * <p>Tests to see if every combination of coding list size, HCPCS system existence, and CPT
   * SAMHSA codes generates the expected result.
   *
   * <p>Empty coding lists and ones without a HCPCS system are not considered SAMHSA by the tested
   * method.
   *
   * @param name the test name for reporting
   * @param codings the codings to test
   * @param isSamhsaCptCode the value to return for {@link AbstractSamhsaMatcher#isSamhsaCptCode}
   * @param expectedResult the expected result
   * @param errorMessage the error message if a test fails
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void hasHcpcsSystemAndSamhsaCptCodeTest(
      String name,
      List<Coding> codings,
      boolean isSamhsaCptCode,
      boolean expectedResult,
      String errorMessage) {

    // Setup
    Mockito.reset(HCPCS_CODING, SAMHSA_CODING, OTHER_CODING);
    doReturn(TransformerConstants.CODING_SYSTEM_HCPCS).when(HCPCS_CODING).getSystem();

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

    doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

    doReturn(isSamhsaCptCode).when(matcherSpy).isSamhsaCptCode(SAMHSA_CODING);

    CodeableConcept mockConcept = mock(CodeableConcept.class);

    doReturn(codings).when(mockConcept).getCoding();

    assertEquals(
        expectedResult,
        matcherSpy.hasHcpcsSystemAndSamhsaCptCode(mockConcept),
        name + " " + errorMessage);
  }

  /**
   * Data method for the isSamhsaPackageCodeTest. Used automatically via the MethodSource
   * annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> isSamhsaPackageCodeTest() {
    return Stream.of(
        arguments(
            "Null concept with expected empty coding list",
            Collections.emptyList(),
            null,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Null concept with SAMHSA DRG system code (should not be possible)",
            List.of(DRG_SAMHSA_CODING),
            null,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Concept with empty coding list",
            Collections.emptyList(),
            PACKAGING_CONCEPT,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Concept with non-SAMHSA DRG codes",
            List.of(DRG_NON_SAMHSA_CODING, DRG_NON_SAMHSA_CODING, DRG_NON_SAMHSA_CODING),
            PACKAGING_CONCEPT,
            false,
            "should NOT be filtered, but WAS."),
        arguments(
            "Concept with SAMHSA DRG code",
            List.of(DRG_NON_SAMHSA_CODING, DRG_SAMHSA_CODING, DRG_NON_SAMHSA_CODING),
            PACKAGING_CONCEPT,
            true,
            "SHOULD be filtered, but was NOT."),
        arguments(
            "Concept with Non-DRG System code",
            List.of(DRG_NON_SAMHSA_CODING, NON_DRG_CODING, DRG_NON_SAMHSA_CODING),
            PACKAGING_CONCEPT,
            true,
            "SHOULD be filtered, but was NOT."),
        arguments(
            "Concept with Non-DRG System Code and SAMHSA DRG code",
            List.of(
                DRG_NON_SAMHSA_CODING, DRG_SAMHSA_CODING, NON_DRG_CODING, DRG_NON_SAMHSA_CODING),
            PACKAGING_CONCEPT,
            true,
            "SHOULD be filtered, but was NOT."));
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaPackageCode(CodeableConcept)}
   *
   * <p>Tests to see if every combination of concept/coding creates the expected result.
   *
   * <p>If the concept is null, there is no SAMHSA.
   *
   * <p>If the concept's coding list has any non-DRG codes, or DRG SAMHSA codes, the coding is
   * SAMHSA
   *
   * @param name the test name for reporting
   * @param codings the codings to test
   * @param concept the codable concept to test
   * @param expectedResult the expected result
   * @param errorMessage the error message if a test fails
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void isSamhsaPackageCodeTest(
      String name,
      List<Coding> codings,
      CodeableConcept concept,
      boolean expectedResult,
      String errorMessage) {
    // setup
    Mockito.reset(DRG_NON_SAMHSA_CODING, NON_DRG_CODING, DRG_SAMHSA_CODING, PACKAGING_CONCEPT);
    doReturn(AbstractSamhsaMatcher.DRG).when(DRG_NON_SAMHSA_CODING).getSystem();
    doReturn(AbstractSamhsaMatcher.DRG).when(DRG_SAMHSA_CODING).getSystem();
    doReturn("Other system").when(NON_DRG_CODING).getSystem();
    doReturn(codings).when(PACKAGING_CONCEPT).getCoding();

    // unchecked - This is ok for making a mock.
    //noinspection unchecked
    AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

    doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

    doReturn(false).when(matcherSpy).isSamhsaDrgCode(any(Coding.class));
    doReturn(true).when(matcherSpy).isSamhsaDrgCode(DRG_SAMHSA_CODING);

    assertEquals(
        expectedResult, matcherSpy.isSamhsaPackageCode(concept), name + " " + errorMessage);
  }

  /**
   * Data method for the cptCodingTest. Used automatically via the MethodSource annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> cptCodingTest() {
    return Stream.of(
        arguments(null, false, "Null code incorrectly filtered"),
        arguments("abc", true, "Samhsa code not correctly filtered"),
        arguments("123", false, "Non-samhsa code incorrectly filtered"));
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaCptCode(Coding)}.
   *
   * @param code the code to test
   * @param expectedResult the expected result
   * @param errorMessage the error message if a test fails
   */
  @ParameterizedTest(name = "{index}: List(\"{0}\"), System(\"{1}\")")
  @MethodSource
  public void cptCodingTest(String code, boolean expectedResult, String errorMessage) {
    MockSamhsaMatcher mockSamhsaMatcher =
        new MockSamhsaMatcher(
            Set.of("ABC"),
            new HashSet<>(),
            new HashSet<>(),
            new HashSet<>(),
            new HashSet<>(),
            new HashSet<>());

    Coding mockCoding = mock(Coding.class);

    doReturn(code).when(mockCoding).getCode();

    assertEquals(expectedResult, mockSamhsaMatcher.isSamhsaCptCode(mockCoding), errorMessage);
  }

  /**
   * Data method for the isSamhsaCodingForSystemTest. Used automatically via the MethodSource
   * annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> isSamhsaCodingForSystemTest() {
    return Stream.of(
        arguments(null, "valid system", false, false, "Null code incorrectly filtered"),
        arguments("abc", "valid system", false, true, "Samhsa code not correctly filtered"),
        arguments("123", "valid system", false, false, "Non-samhsa code incorrectly filtered"),
        arguments("123", "invalid system", true, false, null));
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaCodingForSystem(Coding, Set,
   * String)}.
   *
   * @param code the code to test
   * @param system the system to test
   * @param shouldThrow if the test should expect an exception to be thrown
   * @param expectedResult the expected result (if not an exception)
   * @param errorMessage the error message if a test fails
   */
  @ParameterizedTest(name = "{index}: Code(\"{0}\"), System(\"{1}\")")
  @MethodSource
  public void isSamhsaCodingForSystemTest(
      String code,
      String system,
      boolean shouldThrow,
      boolean expectedResult,
      String errorMessage) {
    MockSamhsaMatcher mockSamhsaMatcher = new MockSamhsaMatcher();

    Coding mockCoding = mock(Coding.class);

    doReturn(code).when(mockCoding).getCode();

    doReturn(system).when(mockCoding).getSystem();

    Set<String> samhsaCodes = Set.of("ABC");

    try {
      boolean result =
          mockSamhsaMatcher.isSamhsaCodingForSystem(mockCoding, samhsaCodes, "valid system");

      if (shouldThrow) {
        fail("Expected exception, none thrown");
      }

      assertEquals(expectedResult, result, errorMessage);
    } catch (Exception e) {
      if (!shouldThrow) {
        throw e;
      }
    }
  }

  /**
   * Data method for the codingTest. Used automatically via the MethodSource annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> codingTest() {
    return Stream.of(
        arguments(
            "drgCodes",
            AbstractSamhsaMatcher.DRG,
            (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaDrgCode,
            "Samhsa DRG code evaluated incorrectly"),
        arguments(
            "icd9DiagnosisCodes",
            IcdCode.CODING_SYSTEM_ICD_9,
            (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd9Diagnosis,
            "Samhsa ICD 9 diagnosis code evaluated incorrectly"),
        arguments(
            "icd9ProcedureCodes",
            IcdCode.CODING_SYSTEM_ICD_9,
            (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd9Procedure,
            "Samhsa ICD 9 procedure code evaluated incorrectly"),
        arguments(
            "icd10DiagnosisCodes",
            IcdCode.CODING_SYSTEM_ICD_10,
            (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd10Diagnosis,
            "Samhsa ICD 10 diagnosis code evaluated incorrectly"),
        arguments(
            "icd10ProcedureCodes",
            IcdCode.CODING_SYSTEM_ICD_10,
            (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd10Procedure,
            "Samhsa ICD 10 procedure code evaluated incorrectly"),
        arguments(
            "icd10DiagnosisCodes",
            IcdCode.CODING_SYSTEM_ICD_10_CM,
            (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd10CmDiagnosis,
            "Samhsa ICD 10 diagnosis code evaluated incorrectly"),
        arguments(
            "icd10ProcedureCodes",
            IcdCode.CODING_SYSTEM_ICD_10_MEDICARE,
            (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd10MedicareProcedure,
            "Samhsa ICD 10 procedure code evaluated incorrectly"));
  }

  /**
   * Parameterized tests for various isSamhsaX() methods.
   *
   * @param codePropertyName a string identifier to determine which codes to test
   * @param system the {@link Coding#getSystem()} to test
   * @param method the filter method to test
   * @param errorMessage the error message if a test fails
   */
  @ParameterizedTest(name = "{index}: List(\"{0}\"), System(\"{1}\")")
  @MethodSource
  public void codingTest(
      String codePropertyName,
      String system,
      SamhsaFilterMethod<Coding> method,
      String errorMessage) {
    Coding mockCoding = mock(Coding.class);
    String testCode = "TEST_CODE";
    Set<String> testCodes = new HashSet<>();
    testCodes.add(testCode);
    when(mockCoding.getSystem()).thenReturn(system);
    when(mockCoding.getCode()).thenReturn(testCode);

    Set<String> drgCodes = "drgCodes".equals(codePropertyName) ? testCodes : new HashSet<>();
    Set<String> cptCodes = new HashSet<>();
    Set<String> icd9ProcedureCodes =
        "icd9ProcedureCodes".equals(codePropertyName) ? testCodes : new HashSet<>();
    Set<String> icd9DiagnosisCodes =
        "icd9DiagnosisCodes".equals(codePropertyName) ? testCodes : new HashSet<>();
    Set<String> icd10ProcedureCodes =
        "icd10DiagnosisCodes".equals(codePropertyName) ? testCodes : new HashSet<>();
    Set<String> icd10DiagnosisCodes =
        "icd10ProcedureCodes".equals(codePropertyName) ? testCodes : new HashSet<>();
    MockSamhsaMatcher mockSamhsaMatcher =
        new MockSamhsaMatcher(
            cptCodes,
            drgCodes,
            icd9ProcedureCodes,
            icd9DiagnosisCodes,
            icd10DiagnosisCodes,
            icd10ProcedureCodes);

    assertTrue(method.apply(mockSamhsaMatcher, mockCoding), errorMessage);
  }

  /**
   * Test to see that {@link AbstractSamhsaMatcher#resourceCsvColumnToList(String, String)} pulls
   * the expected column data from the given file.
   */
  @Test
  public void testNumberOfCptCodesReturned() {
    int expected = 66;
    List<String> actual =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
            "samhsa-related-codes/codes-cpt.csv", "CPT Code");

    assertEquals(expected, actual.size());
    assertTrue(actual.contains("G0533"));
  }

  /**
   * Test to see that {@link AbstractSamhsaMatcher#resourceCsvColumnToList(String, String)} pulls
   * the expected column data from the given file.
   */
  @Test
  public void testNumberOfDrgCodesReturned() {
    int expected = 3;
    List<String> actual =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
            "samhsa-related-codes/codes-drg.csv", "MS-DRGs");

    assertEquals(expected, actual.size());
    assertTrue(actual.contains("MS-DRG 896"));
  }

  /**
   * Test to see that {@link AbstractSamhsaMatcher#resourceCsvColumnToList(String, String)} pulls
   * the expected column data from the given file.
   */
  @Test
  public void testNumberOfIcd9DiagnosisCodesReturned() {
    int expected = 135;
    List<String> actual =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
            "samhsa-related-codes/codes-icd-9-diagnosis.csv", "ICD-9-CM Diagnosis Code");

    assertEquals(expected, actual.size());
    assertTrue(actual.contains("291.81"));
  }

  /**
   * Test to see that {@link AbstractSamhsaMatcher#resourceCsvColumnToList(String, String)} pulls
   * the expected column data from the given file.
   */
  @Test
  public void testNumberOfIcd9ProcedureCodesReturned() {
    int expected = 14;
    List<String> actual =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
            "samhsa-related-codes/codes-icd-9-procedure.csv", "ICD-9-CM");

    assertEquals(expected, actual.size());
    assertTrue(actual.contains("94.67"));
  }

  /**
   * Test to see that {@link AbstractSamhsaMatcher#resourceCsvColumnToList(String, String)} pulls
   * the expected column data from the given file.
   */
  @Test
  public void testNumberOfIcd10DiagnosisCodesReturned() {
    int expected = 405;
    List<String> actual =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
            "samhsa-related-codes/codes-icd-10-diagnosis.csv", "ICD-10-CM Diagnosis Code");

    assertEquals(expected, actual.size());
    assertTrue(actual.contains("F19.280"));
  }

  /**
   * Test to see that {@link AbstractSamhsaMatcher#resourceCsvColumnToList(String, String)} pulls
   * the expected column data from the given file.
   */
  @Test
  public void testNumberOfIcd10ProcedureCodesReturned() {
    int expected = 57;
    List<String> actual =
        AbstractSamhsaMatcher.resourceCsvColumnToList(
            "samhsa-related-codes/codes-icd-10-procedure.csv", "ICD-10-PCS Code");

    assertEquals(expected, actual.size());
    assertTrue(actual.contains("HZ86ZZZ"));
  }
}
