package gov.cms.bfd.server.war.commons.carin;

public enum C4BBAdjudication {
  SUBMITTED,
  NONCOVERED,
  DEDUCTIBLE,
  PAID_TO_PROVIDER,
  PAID_TO_PATIENT,
  PAID_BY_PATIENT,
  PRIOR_PAYER_PAID,
  COINSURANCE;

  public String getSystem() {
    switch (this) {
        // These are HL7
      case SUBMITTED:
      case DEDUCTIBLE:
        return "http://terminology.hl7.org/CodeSystem/adjudication";
        // The rest are Carin
      default:
        return "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication";
    }
  }

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
      default:
        return "?";
    }
  }

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
      default:
        return "?";
    }
  }

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
      default:
        return "?";
    }
  }
}
