package gov.cms.bfd.server.ng;

import java.time.LocalDate;

/** Constants representing specific values found in the IDR database. */
public class IdrConstants {
  /** The string value IDR uses to represent "true". */
  public static final String YES = "Y";

  /** The string value IDR uses to represent "false". */
  public static final String NO = "N";

  /** The value used by IDR to indicate a missing or non-applicable value in a date column. */
  public static final LocalDate DEFAULT_DATE = LocalDate.of(9999, 12, 31);

  /**
   * URL for the CARIN Blue Button (C4BB) Coverage Profile, version 2.1.0. <a
   * href="http://hl7.org/fhir/us/carin-bb/STU2.1/StructureDefinition-C4BB-Coverage.html">C4BB
   * Coverage 2.1.0</a>
   */
  // add version numbers
  public static final String PROFILE_C4BB_COVERAGE =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Coverage|2.1.0";

  /**
   * URL for the US Core Coverage Profile, version 6.1.0. <a
   * href="http://hl7.org/fhir/us/core/STU6.1/StructureDefinition-us-core-coverage.html">US Core
   * Coverage 6.1.0</a>
   */
  public static final String PROFILE_US_CORE_COVERAGE =
      "http://hl7.org/fhir/us/core/StructureDefinition/us-core-coverage|6.1.0";

  /**
   * URL for the CARIN Blue Button (C4BB) Organization Profile, version 2.1.0. Used for the
   * contained CMS Organization. <a
   * href="http://hl7.org/fhir/us/carin-bb/STU2.1/StructureDefinition-C4BB-Organization.html">C4BB
   * Organization 2.1.0</a>
   */
  public static final String PROFILE_C4BB_ORGANIZATION =
      "http://hl7.org/fhir/us/carin-bb/StructureDefinition/C4BB-Organization|2.1.0";

  /**
   * System URL for the HL7 Subscriber Relationship code system. Used for {@code
   * Coverage.relationship.coding.system}. <a
   * href="http://terminology.hl7.org/CodeSystem/subscriber-relationship">Subscriber
   * Relationship</a>
   */
  public static final String SYS_SUBSCRIBER_RELATIONSHIP =
      "http://terminology.hl7.org/CodeSystem/subscriber-relationship";

  /**
   * System URL for the NAHDO Standard Payer Type/Product/Plan (SOPT) code system. Used for {@code
   * Coverage.type.coding.system}. <a href="https://nahdo.org/sopt">NAHDO SOPT</a>
   */
  public static final String SYS_SOPT = "https://nahdo.org/sopt";

  /**
   * System URL for the HL7 Coverage Class code system. Used for {@code
   * Coverage.class.type.coding.system}. <a
   * href="http://terminology.hl7.org/CodeSystem/coverage-class">Coverage Class</a>
   */
  public static final String SYS_COVERAGE_CLASS =
      "http://terminology.hl7.org/CodeSystem/coverage-class";

  /**
   * System URL for the HL7 Contact Entity Type code system. Used for {@code
   * Organization.contact.purpose.coding.system}. <a
   * href="http://terminology.hl7.org/CodeSystem/contactentity-type">Contact Entity Type</a>
   */
  public static final String SYS_CONTACT_ENTITY_TYPE =
      "http://terminology.hl7.org/CodeSystem/contactentity-type";
}
