package gov.cms.bfd.server.ng.log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.beneficiary.model.FinalDetermination;
import gov.cms.bfd.server.ng.beneficiary.model.MatchedRecord;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/** Logs patient match audit records to DynamoDB table. */
@AllArgsConstructor
public class DynamoDbAuditLogger implements AuditLogger {

  private final DynamoDbClient dynamoDbClient;
  private final ObjectMapper objectMapper;
  private final String tableName;

  @Override
  public void log(PatientMatchAuditRecord auditRecord) {
    try {
      var items = new HashMap<String, AttributeValue>();
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
                .distinct()
                .toList();

        items.put("matchedBeneSk", AttributeValue.fromN(matchedBeneSk.get().toString()));
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
        auditRecord
            .finalDetermination()
            .ifPresent(f -> items.put("finalDetermination", AttributeValue.fromS(f.combination())));

        var request = PutItemRequest.builder().tableName(tableName).item(items).build();
        dynamoDbClient.putItem(request);
      }
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Failed to serialize patient match audit record", e);
    } catch (DynamoDbException e) {
      throw new IllegalStateException("Failed to persist patient match audit record", e);
    }
  }
}
