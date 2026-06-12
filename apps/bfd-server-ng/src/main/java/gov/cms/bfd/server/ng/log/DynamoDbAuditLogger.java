package gov.cms.bfd.server.ng.log;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.audit.AuditEventBase;
import gov.cms.bfd.server.ng.audit.AuditEventRepository;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import java.util.HashSet;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** Logs patient match audit records to DynamoDB table. */
@AllArgsConstructor
public class DynamoDbAuditLogger implements AuditLogger {

  private final AuditEventRepository auditEventRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void log(PatientMatchAuditRecord auditRecord) {
    try {
      var matchedBeneSk = PatientMatchAuditUtil.getMatchedBeneSk(auditRecord);
      if (matchedBeneSk.isPresent()) {
        var beneSksFound = PatientMatchAuditUtil.getBeneSksFound(auditRecord);
        var successfulCombination = PatientMatchAuditUtil.getSuccessfulCombination(auditRecord);

        var auditEvent = new AuditEventBase();
        auditEvent.setMatchedBeneSk(matchedBeneSk.get());
        auditEvent.setBeneSksFound(new HashSet<>(beneSksFound));
        auditEvent.setTimestamp(auditRecord.timestamp().toString());
        auditEvent.setClientId(auditRecord.clientId());
        auditEvent.setClientName(auditRecord.clientName());
        auditEvent.setClientIp(auditRecord.clientIp());
        auditEvent.setCombinationsEvaluated(
            objectMapper.writeValueAsString(auditRecord.combinationsEvaluated()));
        auditEvent.setFinalDetermination(successfulCombination);

        auditEventRepository.putAuditEvent(auditEvent);
      }
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize patient match audit record", e);
    } catch (DynamoDbException e) {
      throw new IllegalStateException("Failed to persist patient match audit record", e);
    }
  }
}
