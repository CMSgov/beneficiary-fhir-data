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

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.adapters.Coding;
import gov.cms.bfd.server.war.adapters.DiagnosisComponent;
import gov.cms.bfd.server.war.adapters.FhirResource;
import gov.cms.bfd.server.war.adapters.ItemComponent;
import gov.cms.bfd.server.war.adapters.ProcedureComponent;
import gov.cms.bfd.server.war.utils.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

@RunWith(Enclosed.class)
public class AbstractSamhsaMatcherTest {

  private static final IllegalStateException INVOCATION_EXCEPTION =
      new IllegalStateException("Method was not called with expected arguments for test");

  @FunctionalInterface
  public interface SamhsaFilterMethod<T> {
    boolean apply(AbstractSamhsaMatcher<IBaseResource> matcher, T coding);
  }

  public static class NonParameterizedTests {

    @Test
    public void shouldReturnColumnValues() {
      List<String> expected = List.of("1", "2", "3", "4");
      List<String> actual =
          AbstractSamhsaMatcher.resourceCsvColumnToList("samhsa_codes_test_file.csv", "columnB");

      assertEquals(expected, actual);
    }

    @Test
    public void shouldReturnTrueForSamhsaProcedureComponent() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ProcedureComponent mockComponentA = mock(ProcedureComponent.class);
      ProcedureComponent mockComponentB = mock(ProcedureComponent.class);

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(ProcedureComponent.class));

      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentA);

      doReturn(true).when(matcherSpy).isSamhsaIcdProcedure(mockComponentB);

      assertTrue(
          "Samhsa procedure not correctly filtered",
          matcherSpy.containsSamhsaIcdProcedureCode(List.of(mockComponentA, mockComponentB)));
    }

    @Test
    public void shouldReturnFalseForNonSamhsaProcedureComponent() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ProcedureComponent mockComponentA = mock(ProcedureComponent.class);
      ProcedureComponent mockComponentB = mock(ProcedureComponent.class);

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(ProcedureComponent.class));

      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentA);

      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockComponentB);

      assertFalse(
          "Non-Samhsa procedure incorrectly filtered",
          matcherSpy.containsSamhsaIcdProcedureCode(List.of(mockComponentA, mockComponentB)));
    }

    @Test
    public void shouldReturnTrueForSamhsaDiagnosisComponent() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      DiagnosisComponent mockComponentA = mock(DiagnosisComponent.class);
      DiagnosisComponent mockComponentB = mock(DiagnosisComponent.class);

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaDiagnosis(any(DiagnosisComponent.class));

      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentA);

      doReturn(true).when(matcherSpy).isSamhsaDiagnosis(mockComponentB);

      assertTrue(
          "Samhsa diagnosis not correctly filtered",
          matcherSpy.containsSamhsaIcdDiagnosisCode(List.of(mockComponentA, mockComponentB)));
    }

    @Test
    public void shouldReturnFalseForNonSamhsaDiagnosisComponent() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      DiagnosisComponent mockComponentA = mock(DiagnosisComponent.class);
      DiagnosisComponent mockComponentB = mock(DiagnosisComponent.class);

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaDiagnosis(any(DiagnosisComponent.class));

      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentA);

      doReturn(false).when(matcherSpy).isSamhsaDiagnosis(mockComponentB);

      assertFalse(
          "Non-Samhsa diagnosis incorrectly filtered",
          matcherSpy.containsSamhsaIcdDiagnosisCode(List.of(mockComponentA, mockComponentB)));
    }

    @Test
    public void shouldReturnTrueForSamhsaLineItem() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ItemComponent mockComponentA = mock(ItemComponent.class);
      ItemComponent mockComponentB = mock(ItemComponent.class);

      CodeableConcept mockConceptA = mock(CodeableConcept.class);
      CodeableConcept mockConceptB = mock(CodeableConcept.class);

      doReturn(mockConceptA).when(mockComponentA).getProductOrService();

      doReturn(mockConceptB).when(mockComponentB).getProductOrService();

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .containsSamhsaProcedureCode(any(CodeableConcept.class));

      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptA);

      doReturn(true).when(matcherSpy).containsSamhsaProcedureCode(mockConceptB);

      assertTrue(
          "Samhsa line item not correctly filtered",
          matcherSpy.containsSamhsaLineItem(List.of(mockComponentA, mockComponentB)));
    }

    @Test
    public void shouldReturnFalseForNonSamhsaLineItems() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ItemComponent mockComponentA = mock(ItemComponent.class);
      ItemComponent mockComponentB = mock(ItemComponent.class);

      CodeableConcept mockConceptA = mock(CodeableConcept.class);
      CodeableConcept mockConceptB = mock(CodeableConcept.class);

      doReturn(mockConceptA).when(mockComponentA).getProductOrService();

      doReturn(mockConceptB).when(mockComponentB).getProductOrService();

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .containsSamhsaProcedureCode(any(CodeableConcept.class));

      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptA);

      doReturn(false).when(matcherSpy).containsSamhsaProcedureCode(mockConceptB);

      assertFalse(
          "Non-Samhsa line items inorrectly filtered",
          matcherSpy.containsSamhsaLineItem(List.of(mockComponentA, mockComponentB)));
    }

    @Test
    public void shouldReturnTrueForIsSamhsaIcdProcedure() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ProcedureComponent mockComponent = mock(ProcedureComponent.class);
      CodeableConcept mockConcept = mock(CodeableConcept.class);

      doReturn(mockConcept).when(mockComponent).getProcedureCodeableConcept();

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(CodeableConcept.class));

      doReturn(true).when(matcherSpy).isSamhsaIcdProcedure(mockConcept);

      assertTrue(
          "Samhsa procedure concept not correctly filtered",
          matcherSpy.isSamhsaIcdProcedure(mockComponent));
    }

    @Test
    public void shouldReturnFalseForIsSamhsaIcdProcedure() {
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      ProcedureComponent mockComponent = mock(ProcedureComponent.class);
      CodeableConcept mockConcept = mock(CodeableConcept.class);

      doReturn(mockConcept).when(mockComponent).getProcedureCodeableConcept();

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaIcdProcedure(any(CodeableConcept.class));

      doReturn(false).when(matcherSpy).isSamhsaIcdProcedure(mockConcept);

      assertFalse(
          "Samhsa procedure concept not correctly filtered",
          matcherSpy.isSamhsaIcdProcedure(mockComponent));
    }

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

  @RunWith(Parameterized.class)
  public static class IsSamhsaDiagnosisTests {

    @Parameterized.Parameters(
        name = "{index}: IsSamhsaDiagnosis(\"{0}\"), IsSamhsaPackage(\"{1}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {false, false, false, "Non-samhsa diagnosis incorrectly filtered"},
            {true, false, true, "Samhsa diagnosis not correctly filtered"},
            {false, true, true, "Samhsa package not correctly filtered"},
            {true, true, true, "Samhsa diagnosis & package not correctly filtered"},
          });
    }

    private final boolean expectedIsSamhsaDiagnosis;
    private final boolean expectedIsSamhsaPackage;
    private final boolean expectedResult;
    private final String errorMessage;

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

      doThrow(INVOCATION_EXCEPTION).when(matcherSpy).isSamhsaDiagnosis(any(CodeableConcept.class));

      doThrow(INVOCATION_EXCEPTION)
          .when(matcherSpy)
          .isSamhsaPackageCode(any(CodeableConcept.class));

      doReturn(expectedIsSamhsaDiagnosis).when(matcherSpy).isSamhsaDiagnosis(mockDiagnosisConcept);

      doReturn(expectedIsSamhsaPackage).when(matcherSpy).isSamhsaPackageCode(mockPackageConcept);

      assertEquals(errorMessage, expectedResult, matcherSpy.isSamhsaDiagnosis(mockComponent));
    }
  }

  @RunWith(Parameterized.class)
  public static class SamhsaCodingTests {

    @Parameterized.Parameters(
        name =
            "{index}: System(\"{0}\"), isSamsaICD9(\"{2}\"), isSamsaICD10(\"{3}\"), isSamsaCPT(\"{4}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              IcdCode.CODING_SYSTEM_ICD_9,
              mock(CodeableConcept.class),
              true,
              false,
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
              false,
              "ICD9 System Coding incorrectly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_10,
              mock(CodeableConcept.class),
              false,
              true,
              false,
              true,
              "ICD10 System Coding not correctly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_10,
              mock(CodeableConcept.class),
              false,
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
              false,
              "Missing concept incorrectly marked samhsa"
            },
          });
    }

    private final String system;
    private final CodeableConcept concept;
    private final boolean isIcd9Code;
    private final boolean isIcd10Code;
    private final boolean isCptCode;
    private final boolean expectedResult;
    private final String errorMessage;

    public SamhsaCodingTests(
        String system,
        CodeableConcept concept,
        boolean isIcd9Code,
        boolean isIcd10Code,
        boolean isCptCode,
        boolean expectedResult,
        String errorMessage) {
      this.system = system;
      this.concept = concept;
      this.isIcd9Code = isIcd9Code;
      this.isIcd10Code = isIcd10Code;
      this.isCptCode = isCptCode;
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

      doReturn(isCptCode).when(matcherSpy).isSamhsaCptCode(mockCoding);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      Predicate<Coding> mockPredicateIcd9 = mock(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      Predicate<Coding> mockPredicateIcd10 = mock(Predicate.class);

      doReturn(isIcd9Code).when(mockPredicateIcd9).test(mockCoding);

      doReturn(isIcd10Code).when(mockPredicateIcd10).test(mockCoding);

      assertEquals(
          errorMessage,
          expectedResult,
          matcherSpy.isSamhsaCoding(concept, mockPredicateIcd9, mockPredicateIcd10));
    }
  }

  @RunWith(Parameterized.class)
  public static class CodeableConceptTests {

    @Parameterized.Parameters(name = "{index}: System(\"{0}\"), IsSamhsaCode(\"{1}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "unknown system",
              false,
              (SamhsaFilterMethod<CodeableConcept>)
                  AbstractSamhsaMatcher::containsSamhsaProcedureCode,
              true,
              "Unknown system not correctly filtered."
            },
            {
              TransformerConstants.CODING_SYSTEM_HCPCS,
              true,
              (SamhsaFilterMethod<CodeableConcept>)
                  AbstractSamhsaMatcher::containsSamhsaProcedureCode,
              true,
              "Samhsa data for known system not correctly filtered."
            },
            {
              TransformerConstants.CODING_SYSTEM_HCPCS,
              false,
              (SamhsaFilterMethod<CodeableConcept>)
                  AbstractSamhsaMatcher::containsSamhsaProcedureCode,
              false,
              "Non samhsa data for known system incorrectly filtered."
            },
            {
              "unknown system",
              false,
              (SamhsaFilterMethod<CodeableConcept>) AbstractSamhsaMatcher::isSamhsaPackageCode,
              true,
              "Unknown system not correctly filtered."
            },
            {
              CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD),
              true,
              (SamhsaFilterMethod<CodeableConcept>) AbstractSamhsaMatcher::isSamhsaPackageCode,
              true,
              "Samhsa data for known system not correctly filtered."
            },
            {
              CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD),
              false,
              (SamhsaFilterMethod<CodeableConcept>) AbstractSamhsaMatcher::isSamhsaPackageCode,
              false,
              "Non samhsa data for known system incorrectly filtered."
            },
          });
    }

    private final String system;
    private final boolean codingIsSamhsa;
    private final SamhsaFilterMethod<CodeableConcept> method;
    private final boolean expectedResult;
    private final String errorMessage;

    public CodeableConceptTests(
        String system,
        boolean codingIsSamhsa,
        SamhsaFilterMethod<CodeableConcept> method,
        boolean expectedResult,
        String errorMessage) {
      this.system = system;
      this.codingIsSamhsa = codingIsSamhsa;
      this.method = method;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() {
      Coding mockCoding = mock(Coding.class);

      doReturn(system).when(mockCoding).getSystem();

      doReturn("some code").when(mockCoding).getCode();

      CodeableConcept mockConcept = mock(CodeableConcept.class);

      doReturn(List.of(mockCoding)).when(mockConcept).getCoding();

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractSamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractSamhsaMatcher.class);

      doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

      doReturn(codingIsSamhsa).when(matcherSpy).isSamhsaCptCode(mockCoding);

      doReturn(false).when(matcherSpy).isSamhsaDrgCode(any(Coding.class));

      doReturn(codingIsSamhsa).when(matcherSpy).isSamhsaDrgCode(mockCoding);

      assertEquals(errorMessage, expectedResult, method.apply(matcherSpy, mockConcept));
    }
  }

  @RunWith(Parameterized.class)
  public static class CptCodingTests {

    @Parameterized.Parameters(name = "{index}: List(\"{0}\"), System(\"{1}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {null, false, "Null code incorrectly filtered"},
            {"abc", true, "Samhsa code not correctly filtered"},
            {"123", false, "Non-samhsa code incorrectly filtered"},
          });
    }

    private final String code;
    private final boolean expectedResult;
    private final String errorMessage;

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

  @RunWith(Parameterized.class)
  public static class IsSamhsaCodingForSystemTests {

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

    private final String code;
    private final String system;
    private final boolean shouldThrow;
    private final boolean expectedResult;
    private final String errorMessage;

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
