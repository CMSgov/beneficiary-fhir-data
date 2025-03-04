package gov.cms.bfd.server.war.commons.carin;

/**
 * CARIN ValueSet for Adjudications <a
 * href="https://build.fhir.org/ig/HL7/carin-bb/ValueSet-C4BBAdjudication.html">ValueSet: C4BB
 * Adjudication</a>.
 */
public enum C4BBAdjudication {
  /** The total submitted amount for the claim or group or line item. */
  SUBMITTED,
  /**
   * The portion of the cost of this service that was deemed not eligible by the insurer because the
   * service or member was not covered by the subscriber contract.
   */
  NONCOVERED,
  /** Amount deducted from the eligible amount prior to adjudication. */
  DEDUCTIBLE,
  /** The amount paid to the provider. */
  PAID_TO_PROVIDER,
  /** The amount paid to patient. */
  PAID_TO_PATIENT,
  /** The amount paid by the patient at the point of service. */
  PAID_BY_PATIENT,
  /** The reduction in the payment amount to reflect the carrier as a secondary payer. */
  PRIOR_PAYER_PAID,
  /** The coinsurance amount. */
  COINSURANCE,
  /** The amount of the discount. */
  DISCOUNT,
  /**
   * Price paid for the drug excluding mfr or other discounts. It typically is the sum of the
   * following components: ingredient cost, dispensing fee, sales tax, and vaccine administration.
   */
  DRUG_COST,
  /** The benefit amount. */
  BENEFIT,
  /** Amount of the change which is considered for adjudication. */
  ELIGIBLE;

  /**
   * Gets the system uri.
   *
   * @return the system
   */
  public String getSystem() {
    switch (this) {
      // These are HL7
      case SUBMITTED:
      case DEDUCTIBLE:
      case BENEFIT:
      case ELIGIBLE:
        return "http://terminology.hl7.org/CodeSystem/adjudication";
      // The rest are Carin
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication";
    }
  }

  /**
   * Gets the code.
   *
   * @return the code
   */
  public String toCode() {
    switch (this) {
      case SUBMITTED:
        return "submitted";
      case COINSURANCE:
        return "coinsurance";
      case NONCOVERED:
        return "noncovered";
      case DEDUCTIBLE:
        return "deductible";
      case PAID_TO_PROVIDER:
        return "paidtoprovider";
      case PAID_TO_PATIENT:
        return "paidtopatient";
      case PAID_BY_PATIENT:
        return "paidbypatient";
      case PRIOR_PAYER_PAID:
        return "priorpayerpaid";
      case DISCOUNT:
        return "discount";
      case DRUG_COST:
        return "drugcost";
      case BENEFIT:
        return "benefit";
      case ELIGIBLE:
        return "eligible";
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
      case SUBMITTED:
        return "Submitted Amount";
      case COINSURANCE:
        return "Co-insurance";
      case NONCOVERED:
        return "Noncovered";
      case DEDUCTIBLE:
        return "Deductible";
      case PAID_TO_PROVIDER:
        return "Paid to provider";
      case PAID_TO_PATIENT:
        return "Paid to patient";
      case PAID_BY_PATIENT:
        return "Paid by patient";
      case PRIOR_PAYER_PAID:
        return "Prior payer paid";
      case DISCOUNT:
        return "Discount";
      case DRUG_COST:
        return "Drug Cost";
      case BENEFIT:
        return "Benefit Amount";
      case ELIGIBLE:
        return "Eligible Amount";
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
      case SUBMITTED:
        return "The total submitted amount for the claim or group or line item.";
      case NONCOVERED:
        return "The portion of the cost of this service that was deemed not eligible by the insurer because the service or member was not covered by the subscriber contract.";
      case DEDUCTIBLE:
        return "Amount deducted from the eligible amount prior to adjudication.";
      case PAID_TO_PROVIDER:
        return "The amount paid to the provider.";
      case PAID_TO_PATIENT:
        return "paid to patient";
      case PAID_BY_PATIENT:
        return "The amount paid by the patient at the point of service.";
      case PRIOR_PAYER_PAID:
        return "The reduction in the payment amount to reflect the carrier as a secondary payor.";
      case DISCOUNT:
        return "The amount of the discount";
      case DRUG_COST:
        return "Price paid for the drug excluding mfr or other discounts. It typically is the sum of the following components: ingredient cost, dispensing fee, sales tax, and vaccine administration";
      case BENEFIT:
        return "Benefit Amount";
      case ELIGIBLE:
        return "Amount of the change which is considered for adjudication.";
      default:
        return "?";
    }
  }
}
