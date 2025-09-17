package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import lombok.AllArgsConstructor;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;

@AllArgsConstructor
enum CarinSupportingInfoCategory {
  ACTIVE_CARE_FROM_DATE("admissionperiod"),
  ADMISSION_TYPE_CODE("admtype"),
  SUBMISSION_DATE("clmrecvddate"),
  PATIENT_STATUS_CODE("discharge-status"),
  DIAGNOSIS_DRG_CODE("drg"),
  ADMISSION_SOURCE_CODE("pointoforigin"),
  TYPE_OF_BILL_CODE("typeofbill");

  private final String code;

  CodeableConcept toFhir() {
    return new CodeableConcept(
        new Coding().setSystem(SystemUrls.CARIN_CODE_SYSTEM_SUPPORTING_INFO_TYPE).setCode(code));
  }
}
