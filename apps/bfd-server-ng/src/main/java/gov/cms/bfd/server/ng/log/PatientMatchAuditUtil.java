package gov.cms.bfd.server.ng.log;

import gov.cms.bfd.server.ng.beneficiary.model.FinalDetermination;
import gov.cms.bfd.server.ng.beneficiary.model.MatchedRecord;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import java.util.List;
import java.util.Optional;

/** Utility class used for patient match audit logging. */
public class PatientMatchAuditUtil {

  private PatientMatchAuditUtil() {}

  /**
   * Extracts the matched beneSk from the audit record.
   *
   * @param auditRecord the patient match audit record
   * @return the matched beneSk
   */
  public static Optional<Long> getMatchedBeneSk(PatientMatchAuditRecord auditRecord) {
    return auditRecord
        .finalDetermination()
        .map(FinalDetermination::matchedRecord)
        .map(MatchedRecord::beneSk);
  }

  /**
   * Extracts all the beneSks found from the audit record.
   *
   * @param auditRecord the patient match audit record
   * @return the list of beneSks
   */
  public static List<Long> getBeneSksFound(PatientMatchAuditRecord auditRecord) {
    return auditRecord.combinationsEvaluated().stream()
        .flatMap(c -> c.matchedRecords().stream())
        .map(MatchedRecord::beneSk)
        .distinct()
        .toList();
  }

  /**
   * Extracts the combination index that successfully matched the patient.
   *
   * @param auditRecord the patient match audit record
   * @return the combination index
   */
  public static String getSuccessfulCombination(PatientMatchAuditRecord auditRecord) {
    return auditRecord.finalDetermination().map(FinalDetermination::combination).orElse("");
  }
}
