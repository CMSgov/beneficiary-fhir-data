package gov.cms.bfd.server.war.r4.providers.pac.common;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.bfd.model.codebook.model.CcwCodebookInterface;
import gov.cms.bfd.model.codebook.model.Variable;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudicationDiscriminator;
import gov.cms.bfd.server.war.commons.carin.C4BBIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBSupportingInfoType;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/** Base logic for RDA transformations containing common logic. */
// S1118 - Can't make constructor private because children won't be able to have one due to no
// public constructor being defined.
@SuppressWarnings("squid:S1118")
public class AbstractTransformerV2 {

  /**
   * Converts a {@link LocalDate} (used by RDA messages) to a {@link Date} (used by FHIR).
   *
   * @param localDate The {@link LocalDate} to convert.
   * @return The converted {@link Date} object.
   */
  protected static Date localDateToDate(LocalDate localDate) {
    return localDate == null ? null : CommonTransformerUtils.convertToDate(localDate);
  }

  /**
   * Normalizes the given code, removing whitespace, decimals, and setting a standard casing.
   *
   * @param code The code to normalize.
   * @return The normalized code.
   */
  private static String normalizeIcdCode(String code) {
    return code.trim().replace(".", "").toUpperCase();
  }

  /**
   * Helper method to check if the given codes are equal, null checking and normalizing them.
   *
   * @param code1 The code being checked against.
   * @param code2 The code being compared for equality.
   * @return True if the codes are equal, post normalization, False otherwise.
   */
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

  /**
   * Builds a {@link Patient} object from the given MBI and {@link PatientInfo}.
   *
   * @param mbi The MBI to use to build the {@link Patient} object.
   * @param patientInfo The {@link PatientInfo} to use to build the {@link Patient} object.
   * @return The constructed {@link Patient} object.
   */
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
              patientInfo.getSex() == null
                  ? null
                  : patientInfo.getSexMap().get(patientInfo.getSex().toLowerCase()));
    }

    return patient;
  }

  /**
   * Helper method to create a {@link HumanName} list from the given {@link PatientInfo}
   * information.
   *
   * @param patientInfo The {@link PatientInfo} information to use to build a {@link HumanName}
   *     list.
   * @return The constructed list of {@link HumanName}s.
   */
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

  /**
   * Helper method to construct the {@link HumanName#getText()} property of a {@link HumanName}.
   *
   * @param patientInfo The {@link PatientInfo} information to use to construct the {@link
   *     HumanName} text.
   * @return The constructed {@link HumanName} text.
   */
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

  /**
   * Helper method to add the federal tax number {@link Identifier} to a given {@link Organization}.
   *
   * @param organization The {@link Organization} to add the tax number {@link Identifier} to.
   * @param system The system to use for the {@link Organization} {@link Identifier}.
   * @param taxNumber The tax number to use for the {@link Identifier}.
   */
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

  /**
   * Helper method to add an NPI {@link Identifier} to a given {@link Organization}.
   *
   * @param organization The {@link Organization} to add the NPI {@link Identifier} to.
   * @param npi The NPI to use for the {@link Identifier}.
   */
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

  /**
   * Helper method to create a {@link CodeableConcept} with the given details.
   *
   * @param system The system to use in the created {@link CodeableConcept}.
   * @param value The value to use in the created {@link CodeableConcept}.
   * @return The created {@link CodeableConcept}.
   */
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
   * Creates a {@link CodeableConcept} containing the {@link C4BBAdjudicationDiscriminator} data.
   *
   * @param discriminator The {@link C4BBAdjudicationDiscriminator} type to use in the {@link
   *     CodeableConcept}.
   * @return A {@link CodeableConcept} object containing the data.
   */
  protected static CodeableConcept createCodeableConcept(
      C4BBAdjudicationDiscriminator discriminator) {
    return new CodeableConcept(
        new Coding(discriminator.getSystem(), discriminator.toCode(), discriminator.getDisplay()));
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
   * This method creates a {@link CodeableConcept} that's intended for use as a category: the {@link
   * Variable#getId()} will be used for the {@link Coding#getCode()}.
   *
   * @param codingSystem the {@link Coding#getSystem()} to use
   * @param ccwVariable the {@link CcwCodebookInterface} being coded
   * @return the output {@link CodeableConcept} for the specified input values
   */
  protected static CodeableConcept createCodeableConceptForCategory(
      String codingSystem, CcwCodebookInterface ccwVariable) {
    String code = CCWUtils.calculateVariableReferenceUrl(ccwVariable, true);

    Coding carinCoding =
        new Coding()
            .setCode(
                TransformerConstants.CARIN_CATEGORY_CODE_MAP.getOrDefault(
                    ccwVariable.getVariable().getId().toLowerCase(), "info"))
            .setSystem(
                TransformerConstants.CARIN_CATEGORY_SYSTEM_MAP.getOrDefault(
                    ccwVariable.getVariable().getId().toLowerCase(),
                    TransformerConstants.CARIN_SUPPORTING_INFO_TYPE))
            .setDisplay("Information");
    Coding cmsBBcoding = new Coding(codingSystem, code, ccwVariable.getVariable().getLabel());

    CodeableConcept categoryCodeableConcept = new CodeableConcept();
    categoryCodeableConcept.addCoding(carinCoding);
    categoryCodeableConcept.addCoding(cmsBBcoding);

    return categoryCodeableConcept;
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

  /**
   * Helper method to execute some {@link UnaryOperator} if the given object is not null.
   *
   * @param object The object to check for null.
   * @param processor The {@link UnaryOperator} logic to execute if the object is not null.
   * @return The result of the {@link UnaryOperator} if the object is not null, otherwise it returns
   *     null.
   * @param <T> The type of object given and expected by the {@link UnaryOperator}.
   */
  protected static <T> T ifNotNull(T object, UnaryOperator<T> processor) {
    if (object == null) {
      return null;
    } else {
      return processor.apply(object);
    }
  }

  /**
   * Helper method to execute some {@link UnaryOperator} if the given object is not null.
   *
   * @param object The object to check for null.
   * @param consumer The {@link Consumer} logic to execute if the object is not null.
   * @param <T> The type of object given and expected by the {@link Consumer}.
   */
  protected static <T> void ifNotNull(T object, Consumer<T> consumer) {
    if (object != null) {
      consumer.accept(object);
    }
  }

  /**
   * Source ambiguous data structure to store patient info to allow common logic execution on it.
   */
  protected static class PatientInfo {

    /** The first name. */
    private final String firstName;

    /** The last name. */
    private final String lastName;

    /** The middle name. */
    private final String middleName;

    /** The date of birth. */
    private final LocalDate dob;

    /** The sex. */
    private final String sex;

    /** The map of sex strings to the enum values. */
    private final Map<String, Enumerations.AdministrativeGender> sexMap;

    /** The first name format. */
    private final String firstNameFormat;

    /** The middle name format. */
    private final String middleNameFormat;

    /** The last name format. */
    private final String lastNameFormat;

    /**
     * Instantiates a new Patient info.
     *
     * @param firstName the first name
     * @param lastName the last name
     * @param middleName the middle name
     * @param dob the dob
     * @param sex the sex
     * @param sexMap the sex map
     * @param firstNameFormat the first name format
     * @param middleNameFormat the middle name format
     * @param lastNameFormat the last name format
     */
    public PatientInfo(
        String firstName,
        String lastName,
        String middleName,
        LocalDate dob,
        String sex,
        Map<String, Enumerations.AdministrativeGender> sexMap,
        String firstNameFormat,
        String middleNameFormat,
        String lastNameFormat) {
      this.firstName = firstName;
      this.lastName = lastName;
      this.middleName = middleName;
      this.dob = dob;
      this.sex = sex;
      this.sexMap = sexMap;
      this.firstNameFormat = firstNameFormat;
      this.middleNameFormat = middleNameFormat;
      this.lastNameFormat = lastNameFormat;
    }

    /**
     * Gets the {@link #firstName}.
     *
     * @return the first name
     */
    public String getFirstName() {
      return firstName;
    }

    /**
     * Gets the {@link #lastName}.
     *
     * @return the last name
     */
    public String getLastName() {
      return lastName;
    }

    /**
     * Gets the {@link #middleName}.
     *
     * @return the middle name
     */
    public String getMiddleName() {
      return middleName;
    }

    /**
     * Gets the {@link #dob}.
     *
     * @return the dob
     */
    public LocalDate getDob() {
      return dob;
    }

    /**
     * Gets the {@link #sex}.
     *
     * @return the sex
     */
    public String getSex() {
      return sex;
    }

    /**
     * Gets the {@link #sexMap}.
     *
     * @return the sex map
     */
    public Map<String, Enumerations.AdministrativeGender> getSexMap() {
      return sexMap;
    }

    /**
     * Gets the {@link #firstNameFormat}.
     *
     * @return the first name format
     */
    public String getFirstNameFormat() {
      return firstNameFormat;
    }

    /**
     * Gets the {@link #middleNameFormat}.
     *
     * @return the middle name format
     */
    public String getMiddleNameFormat() {
      return middleNameFormat;
    }

    /**
     * Gets the {@link #lastNameFormat}.
     *
     * @return the last name format
     */
    public String getLastNameFormat() {
      return lastNameFormat;
    }
  }
}
