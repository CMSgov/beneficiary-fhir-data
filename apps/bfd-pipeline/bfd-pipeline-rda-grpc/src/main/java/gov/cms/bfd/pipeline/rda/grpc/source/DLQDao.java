package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.model.rda.MessageError;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/** Utility Data Access Object (DAO) class for the {@link MessageError} entity. */
@VisibleForTesting
@RequiredArgsConstructor
class DLQDao implements AutoCloseable {

  /** Used for time calculation. */
  private final Clock clock;

  /** Used to execute transactions. */
  private final TransactionManager transactionManager;

  /**
   * Finds all message errors with the matching claim type and status.
   *
   * @param claimType the claim type to match
   * @param status the status to match
   * @return the list of errors that match the conditions
   */
  public List<MessageError> findAllMessageErrorsByClaimTypeAndStatus(
      MessageError.ClaimType claimType, MessageError.Status status) {
    return transactionManager.executeFunction(
        entityManager ->
            entityManager
                .createQuery(
                    "select error from MessageError error"
                        + " where error.claimType = :claimType"
                        + " and error.status = :status",
                    MessageError.class)
                .setParameter("claimType", claimType)
                .setParameter("status", status)
                .getResultList());
  }

  /**
   * Updates the message error status for the entry with the specified sequence number and type.
   *
   * @param sequenceNumber the sequence number to check for
   * @param type the type to check for
   * @param status the status to update with
   * @return the number of entities affected by the update
   */
  public long updateState(
      Long sequenceNumber, MessageError.ClaimType type, MessageError.Status status) {
    return transactionManager.executeFunction(
        entityManager -> {
          long entitiesAffected = 0L;

          MessageError messageError =
              entityManager.find(MessageError.class, new MessageError.PK(sequenceNumber, type));

          if (messageError != null) {
            messageError.setStatus(status);
            messageError.setUpdatedDate(clock.instant());
            entityManager.merge(messageError);
            entitiesAffected = 1L;
          }

          return entitiesAffected;
        });
  }

  /**
   * Deletes any unprocessed message errors with the given type that have not been updated within
   * the given number of days. Unprocessed means having a status other than {@link
   * MessageError.Status#UNRESOLVED}.
   *
   * @param maxAgeDays max days since last update to retain in database
   * @param claimType type of claims to delete
   * @return number of records deleted
   */
  public int deleteExpiredMessageErrors(int maxAgeDays, MessageError.ClaimType claimType) {
    return transactionManager.executeFunction(
        entityManager ->
            entityManager
                .createQuery(
                    "delete from MessageError error"
                        + " where error.claimType = :claimType"
                        + " and error.status <> :keepStatus"
                        + " and error.updatedDate < :minKeepDate")
                .setParameter("claimType", claimType)
                .setParameter("keepStatus", MessageError.Status.UNRESOLVED)
                .setParameter("minKeepDate", clock.instant().minus(maxAgeDays, ChronoUnit.DAYS))
                .executeUpdate());
  }

  /**
   * Helper method to find a particular record in a test.
   *
   * @param sequenceNumber desired sequence number
   * @param claimType desired claim type
   * @return {@link Optional} holding matching record if present
   */
  @VisibleForTesting
  Optional<MessageError> readRecord(long sequenceNumber, MessageError.ClaimType claimType) {
    return transactionManager.executeFunction(
        entityManager ->
            Optional.ofNullable(
                entityManager.find(
                    MessageError.class, new MessageError.PK(sequenceNumber, claimType))));
  }

  /**
   * Helper method to insert records during a test.
   *
   * @param records the records to insert
   */
  @VisibleForTesting
  void insertMessageErrors(List<MessageError> records) {
    transactionManager.executeProcedure(em -> records.forEach(em::persist));
  }

  /**
   * Helper method to read all records during a test.
   *
   * @return the records
   */
  @VisibleForTesting
  List<MessageError> readAllMessageErrors() {
    return transactionManager.executeFunction(
        em ->
            em.createQuery("select error from MessageError error", MessageError.class)
                .getResultList());
  }

  /**
   * Helper method to clear the table at end of a test.
   *
   * @param errorsToDelete list of records to delete from database
   * @return number of records actually deleted
   */
  @VisibleForTesting
  @CanIgnoreReturnValue
  int deleteMessageErrors(List<MessageError> errorsToDelete) {
    return transactionManager.executeFunction(
        em -> {
          int totalDeleted = 0;
          for (MessageError messageError : errorsToDelete) {
            totalDeleted +=
                em.createQuery(
                        "delete from MessageError errors"
                            + " where sequenceNumber = :sequenceNumber"
                            + " and claimType = :claimType")
                    .setParameter("sequenceNumber", messageError.getSequenceNumber())
                    .setParameter("claimType", messageError.getClaimType())
                    .executeUpdate();
          }
          return totalDeleted;
        });
  }

  /**
   * {@inheritDoc}
   *
   * <p>Closes our transaction manager.
   *
   * @throws Exception pass through
   */
  @Override
  public void close() throws Exception {
    transactionManager.close();
  }
}
