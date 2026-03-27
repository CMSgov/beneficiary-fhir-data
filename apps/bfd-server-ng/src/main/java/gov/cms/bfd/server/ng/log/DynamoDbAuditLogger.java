package gov.cms.bfd.server.ng.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.beneficiary.model.FinalDetermination;
import gov.cms.bfd.server.ng.beneficiary.model.MatchedRecord;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import java.util.HashMap;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Logs patient match audit records to DynamoDB table. */
@AllArgsConstructor
public class DynamoDbAuditLogger implements AuditLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbAuditLogger.class);

  private final DynamoDbClient dynamoDbClient;
  private final ObjectMapper objectMapper;
  private static final String PATIENT_MATCH_AUDIT_TABLE = "patient_match_audit";

  @Override
  public void log(PatientMatchAuditRecord auditRecord) {
    try {
      var items = new HashMap<String, AttributeValue>();
      System.out.println(auditRecord);
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

        items.put("matchedBeneSk", AttributeValue.fromS(matchedBeneSk.get().toString()));
        items.put(
            "beneSksFound",
            AttributeValue.fromNs(beneSksFound.stream().map(String::valueOf).toList()));
        items.put("timestamp", AttributeValue.fromS(auditRecord.timestamp().toString()));
        items.put("clientId", AttributeValue.fromS(auditRecord.clientId()));
        items.put("clientName", AttributeValue.fromS(auditRecord.clientName()));
        items.put("clientIp", AttributeValue.fromS(auditRecord.clientIp()));
        items.put(
            "combinationsEvaluated",
            AttributeValue.fromS(
                objectMapper.writeValueAsString(auditRecord.combinationsEvaluated())));
        items.put(
            "finalDetermination",
            AttributeValue.fromS(
                objectMapper.writeValueAsString(auditRecord.finalDetermination())));

        PutItemRequest request =
            PutItemRequest.builder().tableName(PATIENT_MATCH_AUDIT_TABLE).item(items).build();
        dynamoDbClient.putItem(request);
      }
    } catch (Exception e) {
      LOGGER.warn("Failed to write audit log to DynamoDB", e);
    }
  }
}
