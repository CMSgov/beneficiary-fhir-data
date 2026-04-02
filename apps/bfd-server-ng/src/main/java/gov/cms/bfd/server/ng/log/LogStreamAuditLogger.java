package gov.cms.bfd.server.ng.log;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logs patient match audit records to regular log stream. */
@AllArgsConstructor
public class LogStreamAuditLogger implements AuditLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogStreamAuditLogger.class);

  private final ObjectMapper objectMapper;

  @Override
  public void log(PatientMatchAuditRecord auditRecord) {
    try {
      var matchedBeneSk = PatientMatchAuditUtil.getMatchedBeneSk(auditRecord);
      if (matchedBeneSk.isPresent()) {
        var beneSksFound = PatientMatchAuditUtil.getBeneSksFound(auditRecord);
        var successfulCombination = PatientMatchAuditUtil.getSuccessfulCombination(auditRecord);
        LOGGER
            .atInfo()
            .setMessage(PATIENT_MATCH_REQUESTED)
            .addKeyValue("logType", "patientMatchAudit")
            .addKeyValue(logKey(AUDIT_PREFIX, MATCHED_BENE_SK), matchedBeneSk.get().toString())
            .addKeyValue(
                logKey(AUDIT_PREFIX, BENE_SKS_FOUND),
                beneSksFound.stream().map(String::valueOf).toList())
            .addKeyValue(logKey(AUDIT_PREFIX, TIMESTAMP), auditRecord.timestamp().toString())
            .addKeyValue(
                logKey(AUDIT_PREFIX, COMBINATIONS_EVALUATED),
                auditRecord.combinationsEvaluated().stream()
                    .map(c -> objectMapper.convertValue(c, HashMap.class))
                    .toList())
            .addKeyValue(logKey(AUDIT_PREFIX, FINAL_DETERMINATION), successfulCombination)
            .log();
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to serialize audit record", e);
    }
  }
}
