package gov.cms.bfd.server.ng.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.audit.AuditEventBase;
import gov.cms.bfd.server.ng.audit.AuditEventRepository;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import java.util.HashSet;
import java.util.Map;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** Logs patient match audit records to DynamoDB table. */
@AllArgsConstructor
public class DynamoDbAuditLogger implements AuditLogger {

  private final AuditEventRepository auditEventRepository;
  private final ObjectMapper objectMapper;
  private final Map<String, String> partnerAliases;

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
        auditEvent.setPartnerAppName(
            partnerAliases.getOrDefault(auditRecord.certAlias(), auditRecord.certAlias()));
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
