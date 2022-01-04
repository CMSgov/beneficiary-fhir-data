package gov.cms.bfd.server.war.r4.providers.preadj.common;

import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;

// S1118 - Can't make constructor private because children won't be able to have one due to no
// public constructor being defined.
@SuppressWarnings("squid:S1118")
public class AbstractTransformerV2 {

  private static final Map<String, Enumerations.AdministrativeGender> GENDER_MAP =
      Map.of(
          "m", Enumerations.AdministrativeGender.MALE,
          "f", Enumerations.AdministrativeGender.FEMALE,
          // Fiss uses 'u', MCS uses 'o', we're mapping both to UNKNOWN
          "u", Enumerations.AdministrativeGender.UNKNOWN,
          "o", Enumerations.AdministrativeGender.UNKNOWN);

  protected static Map<String, Enumerations.AdministrativeGender> genderMap() {
    return GENDER_MAP;
  }

  protected static Date localDateToDate(LocalDate localDate) {
    return localDate == null
        ? null
        : Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private static String normalizeIcdCode(String code) {
    return code.trim().replace(".", "").toUpperCase();
  }

  protected static boolean codesAreEqual(String code1, String code2) {
    return code1 != null
        && code2 != null
        && normalizeIcdCode(code1).equals(normalizeIcdCode(code2));
  }

  protected static Patient getContainedPatient(String mbi, PatientInfo patientInfo) {
    Patient patient =
        new Patient()
            .setIdentifier(
                List.of(
                    new Identifier()
                        .setType(
                            new CodeableConcept(
                                new Coding(
                                    IdentifierType.MC.getSystem(),
                                    IdentifierType.MC.getCode(),
                                    IdentifierType.MC.getDisplay())))
                        .setSystem(
                            TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)
                        .setValue(mbi)));
    patient.setId("patient");

    if (patientInfo != null) {
      patient
          .setName(createHumanNameFrom(patientInfo))
          .setBirthDate(localDateToDate(patientInfo.getDob()))
          .setGender(
              patientInfo.getGender() == null
                  ? null
                  : genderMap().get(patientInfo.getGender().toLowerCase()));
    }

    return patient;
  }

  protected static List<HumanName> createHumanNameFrom(PatientInfo patientInfo) {
    List<HumanName> names;

    // If no names, don't set anything
    if (patientInfo.getFirstName() != null
        || patientInfo.getLastName() != null
        || patientInfo.getMiddleName() != null) {
      names = new ArrayList<>();

      List<StringType> givens;

      // If no givens, don't set any
      if (patientInfo.getFirstName() != null || patientInfo.getLastName() != null) {
        givens = new ArrayList<>();

        if (patientInfo.getFirstName() != null) {
          givens.add(new StringType(patientInfo.getFirstName()));
        }

        if (patientInfo.getMiddleName() != null) {
          givens.add(new StringType(patientInfo.getMiddleName()));
        }
      } else {
        givens = null;
      }

      names.add(new HumanName().setFamily(patientInfo.getLastName()).setGiven(givens));
    } else {
      names = null;
    }

    return names;
  }

  protected static <T> T ifNotNull(T object, UnaryOperator<T> processor) {
    if (object == null) {
      return null;
    } else {
      return processor.apply(object);
    }
  }

  protected static class PatientInfo {

    private final String firstName;
    private final String lastName;
    private final String middleName;
    private final LocalDate dob;
    private final String gender;

    public PatientInfo(
        String firstName, String lastName, String middleName, LocalDate dob, String gender) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.middleName = middleName;
      this.dob = dob;
      this.gender = gender;
    }

    public String getFirstName() {
      return firstName;
    }

    public String getLastName() {
      return lastName;
    }

    public String getMiddleName() {
      return middleName;
    }

    public LocalDate getDob() {
      return dob;
    }

    public String getGender() {
      return gender;
    }
  }
}
