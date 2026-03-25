package gov.cms.bfd.server.ng.util;

import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class used for patient match audit logging. */
public class PatientMatchAuditLogUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(PatientMatchAuditLogUtil.class);

  /**
   * For logging the patient match audit record.
   *
   * @param auditRecord the patient match audit record
   */
  public static void logPatientMatches(PatientMatchAuditRecord auditRecord) {
    // todo: where we'll write to DynamoDB
  }
}
