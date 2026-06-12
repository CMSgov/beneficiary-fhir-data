package gov.cms.bfd.server.ng.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.AuditEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ReadBatch;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/** Repository scaffold for querying patient-match audit data in DynamoDB. */
@Repository
public class AuditEventRepository {

  private static final int BATCH_GET_MAX_KEYS = 100;

  private final DynamoDbEnhancedClient client;
  private final String tableName;

  /**
   * Create AuditEventRepository.
   *
   * @param client DynamoDB enhanced client
   * @param tableName audit table name
   */
  public AuditEventRepository(
      DynamoDbEnhancedClient client, @Qualifier("auditTableName") String tableName) {
    this.client = client;
    this.tableName = tableName;
  }

  private DynamoDbTable<AuditEventBase> getTable() {
    // Use static schema built in model class to avoid duplicate/fragile schema definitions.
    return client.table(tableName, AuditEventBase.TABLE_SCHEMA);
  }

  /**
   * Finds audit events by resource id.
   *
   * @param id ID of resource to pull
   * @return Audit Event
   */
  public AuditEvent findById(AuditEventId id) {
    try {
      var key = Key.builder().partitionValue(id.beneId()).sortValue(id.toDynamoSortKey()).build();
      var item = getTable().getItem(key);

      return item != null ? item.toFhir() : null;
    } catch (DynamoDbException e) {
      throw new IllegalStateException("Failed to query audit event by id", e);
    }
  }

  /**
   * Finds audit events by beneficiary id.
   *
   * @param criteria Search criteria containing the beneficiary id to query for
   * @return matching audit events ordered by latest timestamp first
   */
  public Stream<AuditEvent> findByBeneId(AuditPatientSearchCriteria criteria) {
    try {
      var queryRequestBuilder =
          QueryEnhancedRequest.builder()
              .queryConditional(
                  QueryConditional.keyEqualTo(
                      Key.builder().partitionValue(criteria.beneSkId()).build()))
              .scanIndexForward(false)
              .limit(criteria.resolveLimitWithExtra(1));

      criteria
          .lastIndex()
          .ifPresent(
              lastIndex -> {
                AuditEventId id = AuditEventId.parse(lastIndex);
                queryRequestBuilder.exclusiveStartKey(
                    Map.of(
                        "matchedBeneSk",
                        AttributeValue.builder().n(id.beneId().toString()).build(),
                        "timestamp",
                        AttributeValue.builder().s(id.toDynamoSortKey()).build()));
              });
      // Request limit+1 items so the handler can detect whether another page exists.
      return getTable().query(queryRequestBuilder.build()).items().stream()
          .limit(criteria.resolveLimitWithExtra(1))
          .map(AuditEventBase::toFhir);
    } catch (DynamoDbException e) {
      throw new IllegalStateException("Failed to query audit events by bene", e);
    }
  }

  /**
   * Finds multiple audit events by their resource IDs.
   *
   * @param ids list of audit event IDs to retrieve
   * @return stream of matching audit events
   */
  public Stream<AuditEvent> findByIds(List<AuditEventId> ids) {
    if (ids.isEmpty()) {
      return Stream.of();
    }

    try {
      var keys = ids.stream().distinct().map(this::toKey).toList();

      var items = new ArrayList<AuditEventBase>();
      for (int i = 0; i < keys.size(); i += BATCH_GET_MAX_KEYS) {
        var end = Math.min(i + BATCH_GET_MAX_KEYS, keys.size());
        var batchKeys = keys.subList(i, end);

        var readBatchBuilder =
            ReadBatch.builder(AuditEventBase.class).mappedTableResource(getTable());
        batchKeys.forEach(readBatchBuilder::addGetItem);
        var request =
            BatchGetItemEnhancedRequest.builder().readBatches(readBatchBuilder.build()).build();

        // The enhanced client handles retries for unprocessed keys internally.
        client
            .batchGetItem(request)
            .forEach(page -> items.addAll(page.resultsForTable(getTable())));
      }

      return items.stream().map(AuditEventBase::toFhir);
    } catch (DynamoDbException e) {
      throw new IllegalStateException("Failed to query audit events by ids", e);
    }
  }

  /**
   * Log an audit item.
   *
   * @param auditEventBase AuditEvent Entity to log
   */
  public void putAuditEvent(AuditEventBase auditEventBase) {
    getTable().putItem(auditEventBase);
  }

  private Key toKey(AuditEventId id) {
    return Key.builder().partitionValue(id.beneId()).sortValue(id.toDynamoSortKey()).build();
  }
}
