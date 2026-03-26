package gov.cms.bfd.server.ng;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import gov.cms.bfd.server.ng.util.LoggerConstants;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class LogStreamAuditLogger implements AuditLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(LogStreamAuditLogger.class);

  private final ObjectMapper objectMapper;

  @Override
  public void log(PatientMatchAuditRecord auditRecord) {
    try {
      LOGGER
          .atInfo()
          .setMessage(LoggerConstants.PATIENT_MATCH_REQUESTED)
          .addKeyValue("auditRecord", objectMapper.writeValueAsString(auditRecord))
          .log();
    } catch (Exception e) {
      LOGGER.warn("Failed to serialize audit record", e);
    }
  }
}
