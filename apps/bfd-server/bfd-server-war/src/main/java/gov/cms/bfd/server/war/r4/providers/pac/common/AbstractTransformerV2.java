package gov.cms.bfd.server.war.r4.providers.pac.common;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBSupportingInfoType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import org.apache.logging.log4j.util.Strings;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.codesystems.ClaimType;
import org.hl7.fhir.r4.model.codesystems.ExDiagnosistype;
import org.hl7.fhir.r4.model.codesystems.ProcessPriority;

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

  /**
   * Parses out identifier data from the given claim object, creating an {@link Identifier} object.
   *
   * @param system The system to use in the {@link Identifier}.
   * @param id The claim id to use in the {@link Identifier}
   * @return The generated {@link Identifier} object with the parsed identifier data.
   */
  protected static Identifier createClaimIdentifier(String system, String id) {
    return new Identifier()
        .setType(createCodeableConcept(C4BBIdentifierType.UC))
        .setSystem(system)
        .setValue(id);
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
                  : patientInfo.getGenderMap().get(patientInfo.getGender().toLowerCase()));
    }

    return patient;
  }

  protected static List<HumanName> createHumanNameFrom(PatientInfo patientInfo) {
    List<HumanName> names;

    // If no names, don't set anything
    if (Strings.isNotBlank(patientInfo.getFirstName())
        || Strings.isNotBlank(patientInfo.getLastName())
        || Strings.isNotBlank(patientInfo.getMiddleName())) {
      names = new ArrayList<>();

      List<StringType> givens;

      // If no givens, don't set any
      if (Strings.isNotBlank(patientInfo.getFirstName())
          || Strings.isNotBlank(patientInfo.getLastName())) {
        givens = new ArrayList<>();

        if (Strings.isNotBlank(patientInfo.getFirstName())) {
          givens.add(new StringType(patientInfo.getFirstName()));
        }

        if (Strings.isNotBlank(patientInfo.getMiddleName())) {
          givens.add(new StringType(patientInfo.getMiddleName()));
        }
      } else {
        givens = null;
      }

      HumanName name =
          new HumanName()
              .setFamily(patientInfo.getLastName())
              .setGiven(givens)
              .setText(createNameText(patientInfo));

      names.add(name);
    } else {
      names = null;
    }

    return names;
  }

  private static String createNameText(PatientInfo patientInfo) {
    List<String> nodeNames = new ArrayList<>();
    List<String> nodeFormats = new ArrayList<>();

    if (Strings.isNotBlank(patientInfo.getFirstName())) {
      nodeNames.add(patientInfo.getFirstName());
      nodeFormats.add("[" + patientInfo.getFirstNameFormat() + "]");
    }

    if (Strings.isNotBlank(patientInfo.getMiddleName())) {
      nodeNames.add(patientInfo.getMiddleName());
      nodeFormats.add("[" + patientInfo.getMiddleNameFormat() + "]");
    }

    if (Strings.isNotBlank(patientInfo.getLastName())) {
      nodeNames.add(patientInfo.getLastName());
      nodeFormats.add("[" + patientInfo.getLastNameFormat() + "]");
    }

    String nodeName = String.join(" ", nodeNames);
    String nodeFormat = "(" + String.join(", ", nodeFormats) + ")";

    return nodeName + " " + nodeFormat;
  }

  protected static void addFedTaxNumberIdentifier(
      Organization organization, String system, String taxNumber) {
    if (Strings.isNotBlank(taxNumber)) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(
                      new CodeableConcept(
                          new Coding(
                              C4BBOrganizationIdentifierType.TAX.getSystem(),
                              C4BBOrganizationIdentifierType.TAX.toCode(),
                              C4BBOrganizationIdentifierType.TAX.getDisplay())))
                  .setSystem(system)
                  .setValue(taxNumber));
    }
  }

  protected static void addNpiIdentifier(Organization organization, String npi) {
    if (Strings.isNotBlank(npi)) {
      organization
          .getIdentifier()
          .add(
              new Identifier()
                  .setType(
                      new CodeableConcept(
                          new Coding(
                              C4BBIdentifierType.NPI.getSystem(),
                              C4BBIdentifierType.NPI.toCode(),
                              C4BBIdentifierType.NPI.getDisplay())))
                  .setSystem(TransformerConstants.CODING_NPI_US)
                  .setValue(npi));
    }
  }

  protected static CodeableConcept createCodeableConcept(String system, String value) {
    return new CodeableConcept(new Coding(system, value, null));
  }

  /**
   * Creates a {@link CodeableConcept} containing the {@link ClaimType} data.
   *
   * @param claimType The {@link ClaimType} type to use in the {@link CodeableConcept}.
   * @return A {@link CodeableConcept} object containing the priority data.
   */
  protected static CodeableConcept createCodeableConcept(ClaimType claimType) {
    return new CodeableConcept(
        new Coding(claimType.getSystem(), claimType.toCode(), claimType.getDisplay()));
  }

  /**
   * Creates a {@link CodeableConcept} containing the {@link C4BBSupportingInfoType} data.
   *
   * @param infoType The {@link C4BBSupportingInfoType} type to use in the {@link CodeableConcept}.
   * @return A {@link CodeableConcept} object containing the priority data.
   */
  protected static CodeableConcept createCodeableConcept(C4BBSupportingInfoType infoType) {
    return new CodeableConcept(
        new Coding(infoType.getSystem(), infoType.toCode(), infoType.getDisplay()));
  }

  /**
   * Creates a {@link CodeableConcept} containing the {@link C4BBOrganizationIdentifierType} data.
   *
   * @param idType The {@link C4BBOrganizationIdentifierType} type to use in the {@link
   *     CodeableConcept}.
   * @return A {@link CodeableConcept} object containing the priority data.
   */
  protected static CodeableConcept createCodeableConcept(C4BBOrganizationIdentifierType idType) {
    return new CodeableConcept(
        new Coding(idType.getSystem(), idType.toCode(), idType.getDisplay()));
  }

  /**
   * Creates a {@link CodeableConcept} containing the {@link ProcessPriority} data.
   *
   * @param priority The {@link ProcessPriority} type to use in the {@link CodeableConcept}.
   * @return A {@link CodeableConcept} object containing the priority data.
   */
  protected static CodeableConcept createCodeableConcept(ProcessPriority priority) {
    return new CodeableConcept(
        new Coding(priority.getSystem(), priority.toCode(), priority.getDisplay()));
  }

  /**
   * Creates a {@link CodeableConcept} containing the {@link ExDiagnosistype} data.
   *
   * @param dxType The {@link ExDiagnosistype} type to use in the {@link CodeableConcept}.
   * @return A {@link CodeableConcept} object containing the priority data.
   */
  protected static CodeableConcept createCodeableConcept(ExDiagnosistype dxType) {
    return new CodeableConcept(
        new Coding(dxType.getSystem(), dxType.toCode(), dxType.getDisplay()));
  }

  /**
   * Creates a {@link CodeableConcept} containing the {@link C4BBIdentifierType} data.
   *
   * @param idType The {@link C4BBIdentifierType} type to use in the {@link CodeableConcept}.
   * @return A {@link CodeableConcept} object containing the priority data.
   */
  protected static CodeableConcept createCodeableConcept(C4BBIdentifierType idType) {
    return new CodeableConcept(
        new Coding(idType.getSystem(), idType.toCode(), idType.getDisplay()));
  }

  /**
   * Creates an {@link Extension} from the given system and value.
   *
   * @param extensions The list of {@link Extension}s to add to.
   * @param system The system to use for the {@link Extension}.
   * @param value The value to use for the {@link Extension}.
   */
  protected static void addExtension(List<Extension> extensions, String system, String value) {
    if (Strings.isNotBlank(value)) {
      extensions.add(new Extension(system).setValue(new Coding(system, value, null)));
    }
  }

  /**
   * Creates an {@link Extension} from the given system and value.
   *
   * @param extensions The list of {@link Extension}s to add to.
   * @param system The system to use for the {@link Extension}.
   * @param value The {@link LocalDate} value to use for the {@link Extension}.
   */
  protected static void addExtension(List<Extension> extensions, String system, LocalDate value) {
    if (value != null) {
      extensions.add(new Extension(system).setValue(new DateType(localDateToDate(value))));
    }
  }

  /**
   * Creates a {@link Period} from the given dates.
   *
   * @param start The start date for the {@link Period}.
   * @param end The end date for the {@link Period}.
   * @return The {@link Period} object containing the period data.
   */
  protected static Period createPeriod(LocalDate start, LocalDate end) {
    Period period;

    if (start != null || end != null) {
      period = new Period();

      ifNotNull(
          start,
          (LocalDate startDate) ->
              period.setStart(localDateToDate(startDate), TemporalPrecisionEnum.DAY));
      ifNotNull(
          end,
          (LocalDate endDate) ->
              period.setEnd(localDateToDate(endDate), TemporalPrecisionEnum.DAY));
    } else {
      period = null;
    }

    return period;
  }

  /**
   * Creates a total {@link Money} object from the given data.
   *
   * @param amount The amount for the {@link Money} object.
   * @return The {@link Money} object containing the claim total data.
   */
  public static Money createTotalChargeAmount(BigDecimal amount) {
    Money total;

    if (amount != null) {
      total = new Money();

      total.setValue(amount);
      total.setCurrency("USD");
    } else {
      total = null;
    }

    return total;
  }

  protected static <T> T ifNotNull(T object, UnaryOperator<T> processor) {
    if (object == null) {
      return null;
    } else {
      return processor.apply(object);
    }
  }

  protected static <T> void ifNotNull(T object, Consumer<T> consumer) {
    if (object != null) {
      consumer.accept(object);
    }
  }

  protected static class PatientInfo {

    private final String firstName;
    private final String lastName;
    private final String middleName;
    private final LocalDate dob;
    private final String gender;
    private final Map<String, Enumerations.AdministrativeGender> genderMap;
    private final String firstNameFormat;
    private final String middleNameFormat;
    private final String lastNameFormat;

    public PatientInfo(
        String firstName,
        String lastName,
        String middleName,
        LocalDate dob,
        String gender,
        Map<String, Enumerations.AdministrativeGender> genderMap,
        String firstNameFormat,
        String middleNameFormat,
        String lastNameFormat) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.middleName = middleName;
      this.dob = dob;
      this.gender = gender;
      this.genderMap = genderMap;
      this.firstNameFormat = firstNameFormat;
      this.middleNameFormat = middleNameFormat;
      this.lastNameFormat = lastNameFormat;
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

    public Map<String, Enumerations.AdministrativeGender> getGenderMap() {
      return genderMap;
    }

    public String getFirstNameFormat() {
      return firstNameFormat;
    }

    public String getMiddleNameFormat() {
      return middleNameFormat;
    }

    public String getLastNameFormat() {
      return lastNameFormat;
    }
  }
}
