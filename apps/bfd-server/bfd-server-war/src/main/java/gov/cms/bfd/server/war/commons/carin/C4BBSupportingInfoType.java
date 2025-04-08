package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN CodeSystem for Supportin INfo <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/CodeSystem-C4BBSupportingInfoType.html">CodeSystem:
 * C4BB Supporting Info Type Type</a>.
 */
public enum C4BBSupportingInfoType {
  /** UB-04 Type of Bill (FL-04) provides specific information for payer purposes. */
  TYPE_OF_BILL,
  /**
   * UB-04 Discharge Status (FL-17) indicates the patient’s status as of the discharge date for a
   * facility stay.
   */
  DISCHARGE_STATUS,
  /** NCPDP code indicating whether or not the prescription is a compound. */
  DAYS_SUPPLY,
  /** NCPDP value indicating the Number of days supply of medication dispensed by the pharmacy. */
  COMPOUND_CODE,
  /** NCPDP value indicating the number fill of the current dispensed supply (0, 1, 2, etc.). */
  REFILL_NUM,
  /**
   * NCPDP code indicating whether the prescription was transmitted as an electronic prescription,
   * by phone, by fax, or as a written paper copy.
   */
  RX_ORIGIN_CODE,
  /** NCPDP code indicating whether the plan adjudicated the claim as a brand or generic drug. */
  BRAND_GENERIC_CODE,
  /** Dates corresponding with the admission and discharge of the beneficiary to a facility. */
  ADMISSION_PERIOD,
  /** Date the claim was received by the payer. */
  RECEIVED_DATE;

  /**
   * Gets the system.
   *
   * @return the system
   */
  public String getSystem() {
    return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBSupportingInfoType";
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  public String toCode() {
    switch (this) {
      case TYPE_OF_BILL:
        return "typeofbill";
      case DISCHARGE_STATUS:
        return "discharge-status";
      case COMPOUND_CODE:
        return "compoundcode";
      case DAYS_SUPPLY:
        return "dayssupply";
      case REFILL_NUM:
        return "refillnum";
      case RX_ORIGIN_CODE:
        return "rxorigincode";
      case BRAND_GENERIC_CODE:
        return "brandgenericindicator";
      case ADMISSION_PERIOD:
        return "admissionperiod";
      case RECEIVED_DATE:
        return "clmrecvddate";
      default:
        return "?";
    }
  }

  /**
   * Gets the display string.
   *
   * @return the display string
   */
  public String getDisplay() {
    switch (this) {
      case TYPE_OF_BILL:
        return "Type of Bill";
      case DISCHARGE_STATUS:
        return "Discharge Status";
      case COMPOUND_CODE:
        return "Compound Code";
      case DAYS_SUPPLY:
        return "Days Supply";
      case REFILL_NUM:
        return "Refill Number";
      case RX_ORIGIN_CODE:
        return "Rx Origin Code";
      case BRAND_GENERIC_CODE:
        return "Brand Generic Code";
      case ADMISSION_PERIOD:
        return "Admission Period";
      case RECEIVED_DATE:
        return "Claim Received Date";
      default:
        return "?";
    }
  }

  /**
   * Gets the definition.
   *
   * @return the definition
   */
  public String getDefinition() {
    switch (this) {
      case TYPE_OF_BILL:
        return "UB-04 Type of Bill (FL-04) provides specific information for payer purposes.";
      case DISCHARGE_STATUS:
        return "UB-04 Discharge Status (FL-17) indicates the patient’s status as of the discharge date for a facility stay.";
      case COMPOUND_CODE:
        return "NCPDP code indicating whether or not the prescription is a compound.";
      case DAYS_SUPPLY:
        return "NCPDP value indicating the Number of days supply of medication dispensed by the pharmacy.";
      case REFILL_NUM:
        return "NCPDP value indicating the number fill of the current dispensed supply (0, 1, 2, etc.)";
      case RX_ORIGIN_CODE:
        return "NCPDP code indicating whether the prescription was transmitted as an electronic prescription, by phone, by fax, or as a written paper copy.";
      case BRAND_GENERIC_CODE:
        return "NCPDP code indicating whether the plan adjudicated the claim as a brand or generic drug.";
      case ADMISSION_PERIOD:
        return "Dates corresponding with the admission and discharge of the beneficiary to a facility";
      case RECEIVED_DATE:
        return "Date the claim was received by the payer.";
      default:
        return "?";
    }
  }
}
