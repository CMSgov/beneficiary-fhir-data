package gov.cms.bfd.server.ng.log;

import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;

/** Common interface for logging patient match audit records. */
public interface AuditLogger {
  /**
   * Logs a patient match audit record.
   *
   * @param auditRecord the audit record to log
   */
  void log(PatientMatchAuditRecord auditRecord);
}
