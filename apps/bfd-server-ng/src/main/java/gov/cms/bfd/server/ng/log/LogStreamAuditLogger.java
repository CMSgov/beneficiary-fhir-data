package gov.cms.bfd.server.ng.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.beneficiary.model.FinalDetermination;
import gov.cms.bfd.server.ng.beneficiary.model.MatchedRecord;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import gov.cms.bfd.server.ng.util.LoggerConstants;
import java.util.Objects;
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
      var matchedBeneSk =
          auditRecord
              .finalDetermination()
              .map(FinalDetermination::matchedRecord)
              .map(MatchedRecord::beneSk);
      if (matchedBeneSk.isPresent()) {
        var beneSksFound =
            auditRecord.combinationsEvaluated().stream()
                .flatMap(c -> c.matchedRecords().stream())
                .map(MatchedRecord::beneSk)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        var successfulCombination =
            auditRecord.finalDetermination().map(FinalDetermination::combination).orElse("");
        LOGGER
            .atInfo()
            .setMessage(LoggerConstants.PATIENT_MATCH_REQUESTED)
            .addKeyValue("logType", "patientMatchAudit")
            .addKeyValue("matchedBeneSk", matchedBeneSk.get().toString())
            .addKeyValue("beneSksFound", beneSksFound.stream().map(String::valueOf).toList())
            .addKeyValue("timestamp", auditRecord.timestamp().toString())
            .addKeyValue("clientId", auditRecord.clientId())
            .addKeyValue("clientName", auditRecord.clientName())
            .addKeyValue("clientIp", auditRecord.clientIp())
            .addKeyValue("combinationsEvaluated", auditRecord.combinationsEvaluated())
            .addKeyValue("finalDetermination", successfulCombination)
            .log();
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to serialize audit record", e);
    }
  }
}
