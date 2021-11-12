package gov.cms.bfd.server.war.r4.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.utils.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;

@RunWith(Enclosed.class)
public class AbstractR4SamhsaMatcherTest {

  @FunctionalInterface
  public interface SamhsaFilterMethod<T> {
    boolean apply(AbstractR4SamhsaMatcher<IBaseResource> matcher, T coding);
  }

  public static class NonParameterizedTests {

    @Test
    public void shouldCorrectlyInvokeIsSamhsaCodingForDiagnosis() {
      CodeableConcept mockConcept = mock(CodeableConcept.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractR4SamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractR4SamhsaMatcher.class);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      doReturn(false)
          .when(matcherSpy)
          .isSamhsaCoding(any(CodeableConcept.class), any(Predicate.class), any(Predicate.class));

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
      AbstractR4SamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractR4SamhsaMatcher.class);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      doReturn(false)
          .when(matcherSpy)
          .isSamhsaCoding(any(CodeableConcept.class), any(Predicate.class), any(Predicate.class));

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
  public static class SamhsaCodingTests {

    @Parameterized.Parameters(
        name =
            "{index}: System(\"{0}\"), isSamsaICD9(\"{2}\"), isSamsaICD10(\"{3}\"), isSamsaCPT(\"{4}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              IcdCode.CODING_SYSTEM_ICD_9,
              new CodeableConcept(),
              true,
              false,
              false,
              true,
              "ICD9 System Coding not correctly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_9,
              new CodeableConcept(),
              false,
              false,
              false,
              false,
              "ICD9 System Coding incorrectly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_10,
              new CodeableConcept(),
              false,
              true,
              false,
              true,
              "ICD10 System Coding not correctly marked samhsa."
            },
            {
              IcdCode.CODING_SYSTEM_ICD_10,
              new CodeableConcept(),
              false,
              false,
              false,
              false,
              "ICD10 System Coding incorrectly marked samhsa."
            },
            {
              TransformerConstants.CODING_SYSTEM_CPT,
              new CodeableConcept(),
              false,
              false,
              true,
              true,
              "CPT System Coding not correctly marked samhsa."
            },
            {
              TransformerConstants.CODING_SYSTEM_CPT,
              new CodeableConcept(),
              false,
              false,
              false,
              false,
              "CPT System Coding incorrectly marked samhsa."
            },
            {
              "unknown system",
              new CodeableConcept(),
              false,
              false,
              false,
              true,
              "Unknown system coding not correctly marked samhsa."
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
      Coding coding = new Coding(system, "some code", "some display");
      Optional.ofNullable(concept).ifPresent(c -> c.getCoding().add(coding));

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractR4SamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractR4SamhsaMatcher.class);

      doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

      doReturn(isCptCode).when(matcherSpy).isSamhsaCptCode(coding);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      Predicate<Coding> mockPredicateIcd9 = mock(Predicate.class);
      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      Predicate<Coding> mockPredicateIcd10 = mock(Predicate.class);

      doReturn(isIcd9Code).when(mockPredicateIcd9).test(coding);

      doReturn(isIcd10Code).when(mockPredicateIcd10).test(coding);

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
                  AbstractR4SamhsaMatcher::containsSamhsaProcedureCode,
              true,
              "Unknown system not correctly filtered."
            },
            {
              TransformerConstants.CODING_SYSTEM_HCPCS,
              true,
              (SamhsaFilterMethod<CodeableConcept>)
                  AbstractR4SamhsaMatcher::containsSamhsaProcedureCode,
              true,
              "Samhsa data for known system not correctly filtered."
            },
            {
              TransformerConstants.CODING_SYSTEM_HCPCS,
              false,
              (SamhsaFilterMethod<CodeableConcept>)
                  AbstractR4SamhsaMatcher::containsSamhsaProcedureCode,
              false,
              "Non samhsa data for known system incorrectly filtered."
            },
            {
              "unknown system",
              false,
              (SamhsaFilterMethod<CodeableConcept>) AbstractR4SamhsaMatcher::isSamhsaPackageCode,
              true,
              "Unknown system not correctly filtered."
            },
            {
              CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD),
              true,
              (SamhsaFilterMethod<CodeableConcept>) AbstractR4SamhsaMatcher::isSamhsaPackageCode,
              true,
              "Samhsa data for known system not correctly filtered."
            },
            {
              CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD),
              false,
              (SamhsaFilterMethod<CodeableConcept>) AbstractR4SamhsaMatcher::isSamhsaPackageCode,
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
      Coding coding = new Coding(system, "some code", "some display");
      CodeableConcept concept = new CodeableConcept(coding);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractR4SamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractR4SamhsaMatcher.class);

      doReturn(false).when(matcherSpy).isSamhsaCptCode(any(Coding.class));

      doReturn(codingIsSamhsa).when(matcherSpy).isSamhsaCptCode(coding);

      doReturn(false).when(matcherSpy).isSamhsaDrgCode(any(Coding.class));

      doReturn(codingIsSamhsa).when(matcherSpy).isSamhsaDrgCode(coding);

      assertEquals(errorMessage, expectedResult, method.apply(matcherSpy, concept));
    }
  }

  @RunWith(Parameterized.class)
  public static class CodingTests {

    @Parameterized.Parameters(
        name = "{index}: List(\"{0}\"), actualCode(\"{1}\", expectedCode(\"{2}\"), Result(\"{5}\")")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "drgCodes",
              "some code",
              "some code",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaDrgCode,
              "Samhsa DRG code evaluated incorrectly",
              true
            },
            {
              "drgCodes",
              "wrong code",
              "some code",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaDrgCode,
              "Non samhsa DRG code evaluated incorrectly",
              false
            },
            {
              "drgCodes",
              null,
              "some code",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaDrgCode,
              "Null coding not evaluated correctly",
              false
            },
            {
              "icd9DiagnosisCodes",
              "some code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd9Diagnosis,
              "Samhsa ICD 9 diagnosis code evaluated incorrectly",
              true
            },
            {
              "icd9DiagnosisCodes",
              "wrong code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd9Diagnosis,
              "Non samhsa ICD 9 diagnosis code evaluated incorrectly",
              false
            },
            {
              "icd9DiagnosisCodes",
              null,
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd9Diagnosis,
              "Null coding not evaluated correctly",
              false
            },
            {
              "icd9ProcedureCodes",
              "some code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd9Procedure,
              "Samhsa ICD 9 procedure code evaluated incorrectly",
              true
            },
            {
              "icd9ProcedureCodes",
              "wrong code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd9Procedure,
              "Non samhsa ICD 9 procedure code evaluated incorrectly",
              false
            },
            {
              "icd9ProcedureCodes",
              null,
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd9Procedure,
              "Null coding not evaluated correctly",
              false
            },
            {
              "icd10DiagnosisCodes",
              "some code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd10Diagnosis,
              "Samhsa ICD 10 diagnosis code evaluated incorrectly",
              true
            },
            {
              "icd10DiagnosisCodes",
              "wrong code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd10Diagnosis,
              "Non samhsa ICD 10 diagnosis code evaluated incorrectly",
              false
            },
            {
              "icd10DiagnosisCodes",
              null,
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd10Diagnosis,
              "Null coding not evaluated correctly",
              false
            },
            {
              "icd10ProcedureCodes",
              "some code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd10Procedure,
              "Samhsa ICD 10 procedure code evaluated incorrectly",
              true
            },
            {
              "icd10ProcedureCodes",
              "wrong code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd10Procedure,
              "Non samhsa ICD 10 procedure code evaluated incorrectly",
              false
            },
            {
              "icd10ProcedureCodes",
              null,
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaIcd10Procedure,
              "Null coding not evaluated correctly",
              false
            },
            {
              "cptCodes",
              "some code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaCptCode,
              "Samhsa CPT code evaluated incorrectly",
              true
            },
            {
              "cptCodes",
              "wrong code",
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaCptCode,
              "Non samhsa CPT code evaluated incorrectly",
              false
            },
            {
              "cptCodes",
              null,
              "SOME CODE",
              (SamhsaFilterMethod<Coding>) AbstractR4SamhsaMatcher::isSamhsaCptCode,
              "Null coding not evaluated correctly",
              false
            },
          });
    }

    private final String codePropertyName;
    private final String actualCodeValue;
    private final String expectedCodeValue;
    private final SamhsaFilterMethod<Coding> method;
    private final String errorMessage;
    private final boolean expectedResult;

    public CodingTests(
        String codePropertyName,
        String actualCodeValue,
        String expectedCodeValue,
        SamhsaFilterMethod<Coding> method,
        String errorMessage,
        boolean expectedResult) {
      this.codePropertyName = codePropertyName;
      this.actualCodeValue = actualCodeValue;
      this.expectedCodeValue = expectedCodeValue;
      this.method = method;
      this.errorMessage = errorMessage;
      this.expectedResult = expectedResult;
    }

    @Test
    public void test() throws NoSuchFieldException, IllegalAccessException {
      Coding coding = new Coding("some system", actualCodeValue, "some display");

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      AbstractR4SamhsaMatcher<IBaseResource> matcherSpy = spy(AbstractR4SamhsaMatcher.class);

      // unchecked - This is ok for making a mock.
      //noinspection unchecked
      List<String> mockList = mock(List.class);

      doReturn(false).when(mockList).contains(anyString());

      doReturn(true).when(mockList).contains(expectedCodeValue);

      ReflectionTestUtils.setField(matcherSpy, codePropertyName, mockList);

      assertEquals(errorMessage, expectedResult, method.apply(matcherSpy, coding));
    }
  }
}
