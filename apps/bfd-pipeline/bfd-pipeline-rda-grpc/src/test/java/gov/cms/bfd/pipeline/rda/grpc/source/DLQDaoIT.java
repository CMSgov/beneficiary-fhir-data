package gov.cms.bfd.pipeline.rda.grpc.source;

import static gov.cms.bfd.model.rda.MessageError.Status.OBSOLETE;
import static gov.cms.bfd.model.rda.MessageError.Status.RESOLVED;
import static gov.cms.bfd.model.rda.MessageError.Status.UNRESOLVED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.pipeline.rda.grpc.RdaPipelineTestUtils;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Integration tests for {@link DLQDaoIT}. */
public class DLQDaoIT {
  /** Fixed clock for predictable times. */
  private final Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
  /** Expected type for all tests. */
  private final MessageError.ClaimType claimType = MessageError.ClaimType.FISS;
  /** Non-expected type for all tests. */
  private final MessageError.ClaimType ignoredClaimType = MessageError.ClaimType.MCS;
  /** Expiration age in days for deleting expired records. */
  private final int maxAgeDays = 100;
  /** Precomputed value for threshold of record expiration. */
  private final Instant oldestUpdateDateToKeep = clock.instant().minus(maxAgeDays, ChronoUnit.DAYS);

  /** Verifies that records are correctly found by type and status. */
  @Test
  void testFindAllMessageErrorsByClaimTypeAndStatus() throws Exception {
    long seqNo = 0;
    final var match1 = createRecord(++seqNo, claimType, UNRESOLVED, -1);
    final var match2 = createRecord(++seqNo, claimType, UNRESOLVED, -1);
    final var typeMismatch = createRecord(++seqNo, ignoredClaimType, UNRESOLVED, -1);
    final var statusMismatch = createRecord(++seqNo, claimType, OBSOLETE, -1);

    final var allRecordsBefore = List.of(match1, typeMismatch, statusMismatch, match2);
    final var expectedMatches = List.of(match1, match2);

    RdaPipelineTestUtils.runTestWithTemporaryDb(
        DLQDaoIT.class,
        clock,
        (appState, transactionManager) -> {
          try (var dao = new DLQDao(clock, transactionManager)) {
            try {
              dao.insertMessageErrors(allRecordsBefore);

              // we know there should be some matching records
              final var actualMatches =
                  dao.findAllMessageErrorsByClaimTypeAndStatus(claimType, UNRESOLVED);
              assertEquals(createKeysSet(expectedMatches), createKeysSet(actualMatches));

              // we did not insert anything matching this combination
              var noMatches = dao.findAllMessageErrorsByClaimTypeAndStatus(claimType, RESOLVED);
              assertTrue(noMatches.isEmpty());

              // we did not insert anything matching this combination
              noMatches = dao.findAllMessageErrorsByClaimTypeAndStatus(ignoredClaimType, OBSOLETE);
              assertTrue(noMatches.isEmpty());
            } finally {
              dao.deleteAllMessageErrors();
            }
          }
        });
  }

  /**
   * Verifies that only the intended record is updated.
   *
   * @throws Exception pass through
   */
  @Test
  void testUpdateState() throws Exception {
    final var recordToUpdate = createRecord(1, claimType, UNRESOLVED, 1);
    final var sameSeqNoWrongTypeRecord = createRecord(1, ignoredClaimType, UNRESOLVED, 1);
    final var wrongSeqSameTypeRecord = createRecord(2, claimType, UNRESOLVED, 1);
    final var allRecordsBefore =
        List.of(recordToUpdate, sameSeqNoWrongTypeRecord, wrongSeqSameTypeRecord);

    RdaPipelineTestUtils.runTestWithTemporaryDb(
        DLQDaoIT.class,
        clock,
        (appState, transactionManager) -> {
          try (var dao = new DLQDao(clock, transactionManager)) {
            try {
              dao.insertMessageErrors(allRecordsBefore);

              // verify it's safe to call when there are no matching records
              final var notFoundCount = dao.updateState(42L, claimType, OBSOLETE);
              assertEquals(0, notFoundCount);

              // correctly updates the intended record when it exists inthe database
              final var updatedCount = dao.updateState(1L, claimType, RESOLVED);
              final var modifiedRecord = dao.readRecord(1L, claimType);
              final var remainingRecords = dao.readAllMessageErrors();
              assertEquals(1, updatedCount);
              assertTrue(modifiedRecord.isPresent());
              modifiedRecord.ifPresent(
                  rec -> {
                    // sanity check, it is the right record right?
                    assertEquals(1L, rec.getSequenceNumber());
                    // status should be changed as expected
                    assertEquals(RESOLVED, rec.getStatus());
                    // the created date should be unchanged
                    assertEquals(recordToUpdate.getCreatedDate(), rec.getCreatedDate());
                    // the update date should have been updated
                    assertEquals(clock.instant(), rec.getUpdatedDate());
                  });
              assertTrue(remainingRecords.contains(wrongSeqSameTypeRecord));
              assertTrue(remainingRecords.contains(sameSeqNoWrongTypeRecord));
            } finally {
              dao.deleteAllMessageErrors();
            }
          }
        });
  }

  /**
   * Verifies that only expired records of the correct type and status are deleted.
   *
   * @throws Exception pass through
   */
  @Test
  void testDeleteExpiredMessageErrors() throws Exception {
    long seqNo = 0;
    // should be deleted because of age and status
    final var expiredObsolete = createRecord(++seqNo, claimType, OBSOLETE, -1);
    final var expiredResolved = createRecord(++seqNo, claimType, RESOLVED, -1);
    // should not be deleted because of status
    final var expiredUnresolved = createRecord(++seqNo, claimType, UNRESOLVED, -1);
    // should not be deleted because of claim type
    final var expiredWrongClaimType = createRecord(++seqNo, ignoredClaimType, OBSOLETE, -100);
    // should not be deleted because of age
    final var validObsolete = createRecord(++seqNo, claimType, OBSOLETE, 1);
    final var validResolved = createRecord(++seqNo, claimType, RESOLVED, 1);
    final var validUnresolved = createRecord(++seqNo, claimType, UNRESOLVED, 1);

    final var allRecordsBefore =
        List.of(
            expiredObsolete,
            expiredResolved,
            expiredUnresolved,
            expiredWrongClaimType,
            validObsolete,
            validResolved,
            validUnresolved);

    final var allRecordsAfter =
        List.of(
            expiredUnresolved,
            expiredWrongClaimType,
            validObsolete,
            validResolved,
            validUnresolved);

    RdaPipelineTestUtils.runTestWithTemporaryDb(
        DLQDaoIT.class,
        clock,
        (appState, transactionManager) -> {
          try (var dao = new DLQDao(clock, transactionManager)) {
            try {
              dao.insertMessageErrors(allRecordsBefore);
              final var deletedCount = dao.deleteExpiredMessageErrors(maxAgeDays, claimType);
              final var remainingRecords = dao.readAllMessageErrors();
              // verify we deleted as many records as expected
              assertEquals(2, deletedCount);
              // verify only the expected records remain in the database
              assertEquals(createKeysSet(allRecordsAfter), createKeysSet(remainingRecords));
            } finally {
              dao.deleteAllMessageErrors();
            }
          }
        });
  }

  /**
   * Creates a suitable record for testing expired record deletion.
   *
   * @param sequenceNumber the sequence number
   * @param claimType the claim type
   * @param status the claim status
   * @param ageDeltaSeconds used to compute update date
   * @return the record
   */
  private MessageError createRecord(
      long sequenceNumber,
      MessageError.ClaimType claimType,
      MessageError.Status status,
      long ageDeltaSeconds) {
    final var claimId = status.name().toLowerCase() + "-" + sequenceNumber;
    // All dates are relative to the expiration date threshold.
    final var updatedDate = oldestUpdateDateToKeep.plusSeconds(ageDeltaSeconds);
    // Using an older createdDate ensures only the updatedDate is being used for queries.
    final var createdDate = updatedDate.minus(1, ChronoUnit.DAYS);
    return MessageError.builder()
        .apiSource("test")
        .claimType(claimType)
        .updatedDate(updatedDate)
        .createdDate(createdDate)
        .sequenceNumber(sequenceNumber)
        .claimId(claimId)
        .status(status)
        .errors("")
        .message("")
        .build();
  }

  /**
   * Turn a list of {@link MessageError} objects into a {@link Set} containing just their primary
   * keys. Used for existence checks to ensure database contains an exact set of records.
   *
   * @param records records to convert to keys
   * @return the converted keys
   */
  private Set<MessageError.PK> createKeysSet(List<MessageError> records) {
    return records.stream().map(MessageError::getPK).collect(Collectors.toUnmodifiableSet());
  }
}
