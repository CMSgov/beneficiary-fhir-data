package gov.cms.bfd.server.ng.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.beneficiary.model.FinalDetermination;
import gov.cms.bfd.server.ng.beneficiary.model.MatchedRecord;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import gov.cms.bfd.server.ng.util.LoggerConstants;
import java.util.HashMap;
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
            .addKeyValue("audit.matchedBeneSk", matchedBeneSk.get().toString())
            .addKeyValue("audit.beneSksFound", beneSksFound.stream().map(String::valueOf).toList())
            .addKeyValue("audit.timestamp", auditRecord.timestamp().toString())
            .addKeyValue(
                "audit.combinationsEvaluated",
                auditRecord.combinationsEvaluated().stream()
                    .map(c -> objectMapper.convertValue(c, HashMap.class))
                    .toList())
            .addKeyValue("audit.finalDetermination", successfulCombination)
            .log();
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to serialize audit record", e);
    }
  }
}
