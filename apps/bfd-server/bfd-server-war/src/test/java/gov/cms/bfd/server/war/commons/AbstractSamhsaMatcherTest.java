package gov.cms.bfd.server.war.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.DiagnosisComponent;
import gov.cms.bfd.server.war.adapters.FhirResource;
import gov.cms.bfd.server.war.adapters.ItemComponent;
import gov.cms.bfd.server.war.adapters.ProcedureComponent;
import gov.cms.bfd.server.war.utils.ReflectionTestUtils;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@RunWith(Enclosed.class)
public class AbstractSamhsaMatcherTest {

  private static final IllegalStateException INVOCATION_EXCEPTION =
      new IllegalStateException("Method was not called with expected arguments for test");

  @FunctionalInterface
  public interface SamhsaFilterMethod<T> {
    boolean apply(AbstractSamhsaMatcher<IBaseResource> matcher, T coding);
  }

  public static class NonParameterizedTests {

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
          "Samhsa procedure not correctly filtered",
          matcherSpy.containsSamhsaIcdProcedureCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)));
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
          "Non-Samhsa procedure incorrectly filtered",
          matcherSpy.containsSamhsaIcdProcedureCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)));
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
          "Samhsa diagnosis not correctly filtered",
          matcherSpy.containsSamhsaIcdDiagnosisCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)));
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
          "Non-Samhsa diagnosis incorrectly filtered",
          matcherSpy.containsSamhsaIcdDiagnosisCode(
              List.of(mockComponentA, mockComponentB, mockComponentC)));
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
          "Samhsa line item not correctly filtered",
          matcherSpy.containsSamhsaLineItem(
              List.of(mockComponentA, mockComponentB, mockComponentC)));
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
          "Non-Samhsa line items inorrectly filtered",
          matcherSpy.containsSamhsaLineItem(
              List.of(mockComponentA, mockComponentB, mockComponentC)));
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
          "Samhsa procedure concept not correctly filtered",
          matcherSpy.isSamhsaIcdProcedure(mockComponent));
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
          "Samhsa procedure concept not correctly filtered",
          matcherSpy.isSamhsaIcdProcedure(mockComponent));
    }

    /**
     * Test to see if {@link AbstractSamhsaMatcher#isSamhsaDiagnosis(CodeableConcept)} correctly
     * invokes the expected sibling method ({@link
     * AbstractSamhsaMatcher#isSamhsaCoding(CodeableConcept, Predicate, Predicate)}) with the
     * expected parameters.
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
          .isSamhsaCoding(any(CodeableConcept.class), any(Predicate.class), any(Predicate.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd9Diagnosis(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd10Diagnosis(any(Coding.class));

      ArgumentCaptor<CodeableConcept> conceptCaptor =
          ArgumentCaptor.forClass(CodeableConcept.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd9CallCaptor = ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10CallCaptor = ArgumentCaptor.forClass(Predicate.class);

      matcherSpy.isSamhsaDiagnosis(mockConcept);

      // Capture the arguments used when it was invoked
      verify(matcherSpy)
          .isSamhsaCoding(
              conceptCaptor.capture(), icd9CallCaptor.capture(), icd10CallCaptor.capture());

      assertEquals(mockConcept, conceptCaptor.getValue());

      Coding mockCoding = mock(Coding.class);

      // Invoke the captured lambdas to check they were the right ones
      icd9CallCaptor.getValue().test(mockCoding);
      icd10CallCaptor.getValue().test(mockCoding);

      verify(matcherSpy, times(1)).isSamhsaIcd9Diagnosis(mockCoding);
      verify(matcherSpy, times(1)).isSamhsaIcd10Diagnosis(mockCoding);
    }

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
          .isSamhsaCoding(any(CodeableConcept.class), any(Predicate.class), any(Predicate.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd9Procedure(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaIcd10Procedure(any(Coding.class));

      ArgumentCaptor<CodeableConcept> conceptCaptor =
          ArgumentCaptor.forClass(CodeableConcept.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd9CallCaptor = ArgumentCaptor.forClass(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      ArgumentCaptor<Predicate<Coding>> icd10CallCaptor = ArgumentCaptor.forClass(Predicate.class);

      matcherSpy.isSamhsaIcdProcedure(mockConcept);

      // Capture the arguments used when it was invoked
      verify(matcherSpy)
          .isSamhsaCoding(
              conceptCaptor.capture(), icd9CallCaptor.capture(), icd10CallCaptor.capture());

      assertEquals(mockConcept, conceptCaptor.getValue());

      Coding mockCoding = mock(Coding.class);

      // Invoke the captured lambdas to check they were the right ones
      icd9CallCaptor.getValue().test(mockCoding);
      icd10CallCaptor.getValue().test(mockCoding);

      verify(matcherSpy, times(1)).isSamhsaIcd9Procedure(mockCoding);
      verify(matcherSpy, times(1)).isSamhsaIcd10Procedure(mockCoding);
    }
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaDiagnosis(DiagnosisComponent)}
   *
   * <p>The parameterized tests check to see if either/both the Diagnosis Codeable Concept and/or
   * Packaging Code contains SAMHSA data
   *
   * <p>Two different calls are made to {@link
   * AbstractSamhsaMatcher#isSamhsaDiagnosis(CodeableConcept)} and {@link
   * AbstractSamhsaMatcher#isSamhsaPackageCode(CodeableConcept)} from the primary method, which are
   * both mocked within the test
   */
  @RunWith(Parameterized.class)
  public static class IsSamhsaDiagnosisTests {

    private final boolean expectedIsSamhsaDiagnosis;
    private final boolean expectedIsSamhsaPackage;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(
        name = "{index}: IsSamhsaDiagnosis(\"{0}\"), IsSamhsaPackage(\"{1}\"), Expected(\"{2}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {false, false, false, "Non-samhsa diagnosis incorrectly filtered"},
            {true, false, true, "Samhsa diagnosis not correctly filtered"},
            {false, true, true, "Samhsa package not correctly filtered"},
            {true, true, true, "Samhsa diagnosis & package not correctly filtered"},
          });
    }

    public IsSamhsaDiagnosisTests(
        boolean expectedIsSamhsaDiagnosis,
        boolean expectedIsSamhsaPackage,
        boolean expectedResult,
        String errorMessage) {
      this.expectedIsSamhsaDiagnosis = expectedIsSamhsaDiagnosis;
      this.expectedIsSamhsaPackage = expectedIsSamhsaPackage;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() {
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

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaPackageCode(any(CodeableConcept.class));

      doReturn(expectedIsSamhsaDiagnosis).when(matcherSpy).isSamhsaDiagnosis(mockDiagnosisConcept);

      doReturn(expectedIsSamhsaPackage).when(matcherSpy).isSamhsaPackageCode(mockPackageConcept);

      assertEquals(errorMessage, expectedResult, matcherSpy.isSamhsaDiagnosis(mockComponent));
    }
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaCoding(CodeableConcept, Predicate,
   * Predicate)}
   *
   * <p>The target method takes a {@link CodeableConcept} and two {@link Predicate}s (one for ICD9
   * checks and one for ICD10 checks). The test checks each combination of coding system/predicate
   * result.
   */
  @RunWith(Parameterized.class)
  public static class SamhsaCodingTests {

    private final String system;
    private final CodeableConcept concept;
    private final boolean isIcd9Code;
    private final boolean isIcd10Code;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(
        name =
            "{index}: System(\"{0}\"), isSamsaICD9(\"{2}\"), isSamsaICD10(\"{3}\"), Expected(\"{4}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              IcdCode.CODING_SYSTEM_ICD_9,
              mock(CodeableConcept.class),
              true,
              false,
              true,
              "ICD9 System Coding not correctly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_9,
              mock(CodeableConcept.class),
              false,
              false,
              false,
              "ICD9 System Coding incorrectly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_10,
              mock(CodeableConcept.class),
              false,
              true,
              true,
              "ICD10 System Coding not correctly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_10,
              mock(CodeableConcept.class),
              false,
              false,
              false,
              "ICD10 System Coding incorrectly marked samhsa."
            },
            {
              "other/unknown system",
              mock(CodeableConcept.class),
              false,
              false,
              true,
              "Other/unknown system coding not correctly marked samhsa."
            },
            {
              "doesn't matter",
              null,
              false,
              false,
              false,
              "Missing concept incorrectly marked samhsa"
            },
          });
    }

    public SamhsaCodingTests(
        String system,
        CodeableConcept concept,
        boolean isIcd9Code,
        boolean isIcd10Code,
        boolean expectedResult,
        String errorMessage) {
      this.system = system;
      this.concept = concept;
      this.isIcd9Code = isIcd9Code;
      this.isIcd10Code = isIcd10Code;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() {
      Coding mockCoding = mock(Coding.class);

      doReturn(system).when(mockCoding).getSystem();

      doReturn("some code").when(mockCoding).getCode();

      Optional.ofNullable(concept)
          .ifPresent(c -> doReturn(List.of(mockCoding)).when(c).getCoding());

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
      Predicate<Coding> mockPredicateIcd10 = mock(Predicate.class);
      doReturn(isIcd10Code).when(mockPredicateIcd10).test(mockCoding);

      assertEquals(
          errorMessage,
          expectedResult,
          matcherSpy.isSamhsaCoding(concept, mockPredicateIcd9, mockPredicateIcd10));
    }
  }

  /**
   * Parameterized tests for {@link
   * AbstractSamhsaMatcher#containsSamhsaProcedureCode(CodeableConcept)}
   *
   * <p>Tests to see if every combination of coding list size, hcpcs result and known system check
   * returns the expected result.
   *
   * <p>If the codings list is empty, it should return false. If the concept contains a HCPCS samhsa
   * code or any unknown systems, it should return true.
   */
  @RunWith(Parameterized.class)
  public static class ContainsSamhsaProcedureCodeTests {

    private final String name;
    private final boolean codingsListEmpty;
    private final boolean hasHcpcsSystemAndSmahsaCptCode;
    private final boolean containsOnlyKnownSystems;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {"Empty codings", true, true, false, false, "should NOT be filtered, but WAS."},
            {
              "Non-empty codings with only known systems with no SAMHSA codes",
              false,
              false,
              true,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Non-empty codings with SAMHSA codes",
              false,
              true,
              true,
              true,
              "SHOULD be filtered, but was NOT."
            },
            {
              "Non-empty codings with unknown system containing no SAMHSA codes",
              false,
              false,
              false,
              true,
              "SHOULD be filtered, but was NOT."
            },
            {
              "Non-empty codings with unknown system containing SAMHSA codes",
              false,
              true,
              false,
              true,
              "SHOULD be filtered, but was NOT."
            },
          });
    }

    public ContainsSamhsaProcedureCodeTests(
        String name,
        boolean codingsListEmpty,
        boolean hasHcpcsSystemAndSmahsaCptCode,
        boolean containsOnlyKnownSystems,
        boolean expectedResult,
        String errorMessage) {
      this.name = name;
      this.codingsListEmpty = codingsListEmpty;
      this.hasHcpcsSystemAndSmahsaCptCode = hasHcpcsSystemAndSmahsaCptCode;
      this.containsOnlyKnownSystems = containsOnlyKnownSystems;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() {
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
          name + " " + errorMessage,
          expectedResult,
          matcherSpy.containsSamhsaProcedureCode(mockConcept));
    }
  }

  /**
   * Parameterized tests for {@link
   * AbstractSamhsaMatcher#hasHcpcsSystemAndSamhsaCptCode(CodeableConcept)}
   *
   * <p>Tests to see if every combination of coding list size, HCPCS system existence, and CPT
   * SAMHSA codes generates the expected result.
   *
   * <p>Empty coding lists and ones without a HCPCS system are not considered SAMHSA by the tested
   * method.
   */
  @RunWith(Parameterized.class)
  public static class HasHcpcsSystemAndSamhsaCptCodeTests {

    private static final Coding HCPCS_CODING = mock(Coding.class);
    private static final Coding SAMHSA_CODING = mock(Coding.class);
    private static final Coding OTHER_CODING = mock(Coding.class);

    private static final List<Coding> CODINGS_WITH_HCPCS =
        List.of(OTHER_CODING, HCPCS_CODING, SAMHSA_CODING, OTHER_CODING);
    private static final List<Coding> CODINGS_WITHOUT_HCPCS =
        List.of(OTHER_CODING, SAMHSA_CODING, OTHER_CODING);

    private final String name;
    private final List<Coding> codings;
    private final boolean isSamhsaCptCode;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "Empty coding list with obviously no SAMHSA CPT code",
              Collections.emptyList(),
              false,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Empty coding list, with SAMHSA CPT code (should not be possible)",
              Collections.emptyList(),
              true,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Non-HCPCS system with no SAMHSA CPT code",
              CODINGS_WITHOUT_HCPCS,
              false,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Non-HCPCS system with SAMHSA CPT code",
              CODINGS_WITHOUT_HCPCS,
              true,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "HCPCS system with no SAMHSA CPT code",
              CODINGS_WITH_HCPCS,
              false,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "HCPCS system with SAMHSA CPT code",
              CODINGS_WITH_HCPCS,
              true,
              true,
              "SHOULD be filtered, but was NOT."
            },
          });
    }

    @Before
    public void setUp() {
      Mockito.reset(HCPCS_CODING, SAMHSA_CODING, OTHER_CODING);

      doReturn(TransformerConstants.CODING_SYSTEM_HCPCS).when(HCPCS_CODING).getSystem();
    }

    public HasHcpcsSystemAndSamhsaCptCodeTests(
        String name,
        List<Coding> codings,
        boolean isSamhsaCptCode,
        boolean expectedResult,
        String errorMessage) {
      this.name = name;
      this.codings = codings;
      this.isSamhsaCptCode = isSamhsaCptCode;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

      doReturn(isSamhsaCptCode).when(matcherSpy).isSamhsaCptCode(SAMHSA_CODING);

      CodeableConcept mockConcept = mock(CodeableConcept.class);

      doReturn(codings).when(mockConcept).getCoding();

      assertEquals(
          name + " " + errorMessage,
          expectedResult,
          matcherSpy.hasHcpcsSystemAndSamhsaCptCode(mockConcept));
    }
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
   */
  @RunWith(Parameterized.class)
  public static class IsSamhsaPackageCodeTests {

    private static final Coding NON_DRG_CODING = mock(Coding.class);
    private static final Coding DRG_NON_SAMHSA_CODING = mock(Coding.class);
    private static final Coding DRG_SAMHSA_CODING = mock(Coding.class);

    private static final CodeableConcept PACKAGING_CONCEPT = mock(CodeableConcept.class);

    private final String name;
    private final List<Coding> codings;
    private final CodeableConcept concept;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "Null concept with expected empty coding list",
              Collections.emptyList(),
              null,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Null concept with SAMHSA DRG system code (should not be possible)",
              List.of(DRG_SAMHSA_CODING),
              null,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Concept with empty coding list",
              Collections.emptyList(),
              PACKAGING_CONCEPT,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Concept with non-SAMHSA DRG codes",
              List.of(DRG_NON_SAMHSA_CODING, DRG_NON_SAMHSA_CODING, DRG_NON_SAMHSA_CODING),
              PACKAGING_CONCEPT,
              false,
              "should NOT be filtered, but WAS."
            },
            {
              "Concept with SAMHSA DRG code",
              List.of(DRG_NON_SAMHSA_CODING, DRG_SAMHSA_CODING, DRG_NON_SAMHSA_CODING),
              PACKAGING_CONCEPT,
              true,
              "SHOULD be filtered, but was NOT."
            },
            {
              "Concept with Non-DRG System code",
              List.of(DRG_NON_SAMHSA_CODING, NON_DRG_CODING, DRG_NON_SAMHSA_CODING),
              PACKAGING_CONCEPT,
              true,
              "SHOULD be filtered, but was NOT."
            },
            {
              "Concept with Non-DRG System Code and SAMHSA DRG code",
              List.of(
                  DRG_NON_SAMHSA_CODING, DRG_SAMHSA_CODING, NON_DRG_CODING, DRG_NON_SAMHSA_CODING),
              PACKAGING_CONCEPT,
              true,
              "SHOULD be filtered, but was NOT."
            },
          });
    }

    @Before
    public void setUp() {
      Mockito.reset(DRG_NON_SAMHSA_CODING, NON_DRG_CODING, DRG_SAMHSA_CODING, PACKAGING_CONCEPT);

      doReturn(AbstractSamhsaMatcher.DRG).when(DRG_NON_SAMHSA_CODING).getSystem();
      doReturn(AbstractSamhsaMatcher.DRG).when(DRG_SAMHSA_CODING).getSystem();
      doReturn("Other system").when(NON_DRG_CODING).getSystem();

      doReturn(codings).when(PACKAGING_CONCEPT).getCoding();
    }

    public IsSamhsaPackageCodeTests(
        String name,
        List<Coding> codings,
        CodeableConcept concept,
        boolean expectedResult,
        String errorMessage) {
      this.name = name;
      this.codings = codings;
      this.concept = concept;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

      doReturn(false).when(matcherSpy).isSamhsaDrgCode(any(Coding.class));
      doReturn(true).when(matcherSpy).isSamhsaDrgCode(DRG_SAMHSA_CODING);

      assertEquals(
          name + " " + errorMessage, expectedResult, matcherSpy.isSamhsaPackageCode(concept));
    }
  }

  /** Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaCptCode(Coding)} */
  @RunWith(Parameterized.class)
  public static class CptCodingTests {

    private final String code;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(name = "{index}: List(\"{0}\"), System(\"{1}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {null, false, "Null code incorrectly filtered"},
            {"abc", true, "Samhsa code not correctly filtered"},
            {"123", false, "Non-samhsa code incorrectly filtered"},
          });
    }

    public CptCodingTests(String code, boolean expectedResult, String errorMessage) {
      this.code = code;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() throws NoSuchFieldException, IllegalAccessException {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      Coding mockCoding = mock(Coding.class);

      doReturn(code).when(mockCoding).getCode();

      Set<String> mockCptCodes = Set.of("ABC");

      ReflectionTestUtils.setField(matcherSpy, "cptCodes", mockCptCodes);

      assertEquals(errorMessage, expectedResult, matcherSpy.isSamhsaCptCode(mockCoding));
    }
  }

  /**
   * Parameterized tests for {@link AbstractSamhsaMatcher#isSamhsaCodingForSystem(Coding, Set,
   * String)}
   */
  @RunWith(Parameterized.class)
  public static class IsSamhsaCodingForSystemTests {

    private final String code;
    private final String system;
    private final boolean shouldThrow;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(name = "{index}: Code(\"{0}\"), System(\"{1}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {null, "valid system", false, false, "Null code incorrectly filtered"},
            {"abc", "valid system", false, true, "Samhsa code not correctly filtered"},
            {"123", "valid system", false, false, "Non-samhsa code incorrectly filtered"},
            {"123", "invalid system", true, false, null},
          });
    }

    public IsSamhsaCodingForSystemTests(
        String code,
        String system,
        boolean shouldThrow,
        boolean expectedResult,
        String errorMessage) {
      this.code = code;
      this.system = system;
      this.shouldThrow = shouldThrow;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() throws NoSuchFieldException, IllegalAccessException {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<FhirResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      Coding mockCoding = mock(Coding.class);

      doReturn(code).when(mockCoding).getCode();

      doReturn(system).when(mockCoding).getSystem();

      Set<String> samhsaCodes = Set.of("ABC");

      try {
        boolean result =
            matcherSpy.isSamhsaCodingForSystem(mockCoding, samhsaCodes, "valid system");

        if (shouldThrow) {
          fail("Expected exception, none thrown");
        }

        assertEquals(errorMessage, expectedResult, result);
      } catch (Exception e) {
        if (!shouldThrow) {
          throw e;
        }
      }
    }
  }

  /** Parameterized tests for various isSamhsaX() methods. */
  @RunWith(Parameterized.class)
  public static class CodingTests {

    @Parameterized.Parameters(name = "{index}: List(\"{0}\"), System(\"{1}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "drgCodes",
              AbstractSamhsaMatcher.DRG,
              (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaDrgCode,
              "Samhsa DRG code evaluated incorrectly"
            },
            {
              "icd9DiagnosisCodes",
              IcdCode.CODING_SYSTEM_ICD_9,
              (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd9Diagnosis,
              "Samhsa ICD 9 diagnosis code evaluated incorrectly"
            },
            {
              "icd9ProcedureCodes",
              IcdCode.CODING_SYSTEM_ICD_9,
              (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd9Procedure,
              "Samhsa ICD 9 procedure code evaluated incorrectly"
            },
            {
              "icd10DiagnosisCodes",
              IcdCode.CODING_SYSTEM_ICD_10,
              (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd10Diagnosis,
              "Samhsa ICD 10 diagnosis code evaluated incorrectly"
            },
            {
              "icd10ProcedureCodes",
              IcdCode.CODING_SYSTEM_ICD_10,
              (SamhsaFilterMethod<Coding>) AbstractSamhsaMatcher::isSamhsaIcd10Procedure,
              "Samhsa ICD 10 procedure code evaluated incorrectly"
            },
          });
    }

    private final String codePropertyName;
    private final String system;
    private final SamhsaFilterMethod<Coding> method;
    private final String errorMessage;

    public CodingTests(
        String codePropertyName,
        String system,
        SamhsaFilterMethod<Coding> method,
        String errorMessage) {
      this.codePropertyName = codePropertyName;
      this.system = system;
      this.method = method;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() throws NoSuchFieldException, IllegalAccessException {
      Coding mockCoding = mock(Coding.class);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      doReturn(false)
          .when(matcherSpy)
          .isSamhsaCodingForSystem(any(Coding.class), anySet(), anyString());

      // unchecked - This is fine for testing.
      //noinspection unchecked
      Set<String> codeList =
          (Set<String>) ReflectionTestUtils.getField(matcherSpy, codePropertyName);

      doReturn(true).when(matcherSpy).isSamhsaCodingForSystem(mockCoding, codeList, system);

      assertTrue(errorMessage, method.apply(matcherSpy, mockCoding));
    }
  }
}
