package gov.cms.bfd.server.ng;

import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;

public interface AuditLogger {
  void log(PatientMatchAuditRecord auditRecord);
}
