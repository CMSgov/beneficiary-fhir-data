package gov.cms.bfd.server.ng;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@AllArgsConstructor
@Component
public class DynamoDbAuditLogger implements AuditLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamoDbAuditLogger.class);

  private final DynamoDbClient dynamoDbClient;
  private final ObjectMapper objectMapper;
  private static final String PATIENT_MATCH_AUDIT_TABLE = "patient_match_audit";

  @Override
  public void log(PatientMatchAuditRecord auditRecord) {
    try {
      var items = new HashMap<String, AttributeValue>();
      items.put("pk", AttributeValue.fromS(auditRecord.clientId()));
      items.put("sk", AttributeValue.fromS(auditRecord.timestamp().toString()));
      items.put("payload", AttributeValue.fromS(objectMapper.writeValueAsString(auditRecord)));

      PutItemRequest request =
          PutItemRequest.builder().tableName(PATIENT_MATCH_AUDIT_TABLE).item(items).build();
      dynamoDbClient.putItem(request);
    } catch (Exception e) {
      LOGGER.warn("Failed to write audit log to DynamoDB", e);
    }
  }
}
