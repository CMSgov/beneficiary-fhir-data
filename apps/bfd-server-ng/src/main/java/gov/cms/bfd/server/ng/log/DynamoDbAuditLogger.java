package gov.cms.bfd.server.ng.log;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
      var matchedBeneSk = PatientMatchAuditUtil.getMatchedBeneSk(auditRecord);
      if (matchedBeneSk.isPresent()) {
        var beneSksFound = PatientMatchAuditUtil.getBeneSksFound(auditRecord);
        var successfulCombination = PatientMatchAuditUtil.getSuccessfulCombination(auditRecord);

        items.put(MATCHED_BENE_SK, AttributeValue.fromN(matchedBeneSk.get().toString()));
        items.put(
            BENE_SKS_FOUND,
            AttributeValue.fromNs(beneSksFound.stream().map(String::valueOf).toList()));
        items.put(TIMESTAMP, AttributeValue.fromS(auditRecord.timestamp().toString()));
        items.put(CLIENT_ID_KEY, AttributeValue.fromS(auditRecord.clientId()));
        items.put(CLIENT_NAME_KEY, AttributeValue.fromS(auditRecord.clientName()));
        items.put(CLIENT_IP_KEY, AttributeValue.fromS(auditRecord.clientIp()));
        items.put(
            COMBINATIONS_EVALUATED,
            AttributeValue.fromS(
                objectMapper.writeValueAsString(auditRecord.combinationsEvaluated())));
        items.put(FINAL_DETERMINATION, AttributeValue.fromS(successfulCombination));

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
