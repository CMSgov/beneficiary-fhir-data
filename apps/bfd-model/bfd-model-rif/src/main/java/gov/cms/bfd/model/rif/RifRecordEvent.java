package gov.cms.bfd.model.rif;

/**
 * Models a single beneficiary/claim/drug event that was contained in a {@link RifFile}. Please note
 * that all lines/revenue centers for a single claim will be grouped together into a single {@link
 * RifRecordEvent} instance.
 *
 * @param <R> the record type stored in this {@link RifRecordEvent}
 */
public final class RifRecordEvent<R extends RifRecordBase> {
  private final RifFileEvent fileEvent;
  private final RecordAction recordAction;
  private final String beneficiaryId;
  private final R record;

  /**
   * Constructs a new {@link RifRecordEvent} instance.
   *
   * @param fileEvent the value to use for {@link #getFileEvent()}
   * @param recordAction the value to use for {@link #getRecordAction()}
   * @param beneficiaryId the beneficiary id to use for {@link #getBeneficiaryId()}
   * @param record the value to use for {@link #getRecord()}
   */
  public RifRecordEvent(
      RifFileEvent fileEvent, RecordAction recordAction, String beneficiaryId, R record) {
    if (fileEvent == null) throw new IllegalArgumentException();
    if (recordAction == null) throw new IllegalArgumentException();
    if (beneficiaryId == null) throw new IllegalArgumentException();
    if (record == null) throw new IllegalArgumentException();

    this.fileEvent = fileEvent;
    this.recordAction = recordAction;
    this.beneficiaryId = beneficiaryId;
    this.record = record;
  }

  /** @return the {@link RifFileEvent} that this is a child of */
  public RifFileEvent getFileEvent() {
    return fileEvent;
  }

  /** @return the RIF {@link RecordAction} indicated for the {@link #getRecord()} */
  public RecordAction getRecordAction() {
    return recordAction;
  }

  /** @return the beneficiaryId */
  public String getBeneficiaryId() {
    return beneficiaryId;
  }

  /** @return the actual RIF data that the {@link RifRecordEvent} represents */
  public R getRecord() {
    return record;
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("RifRecordEvent [fileEvent=");
    builder.append(fileEvent);
    builder.append(", recordAction=");
    builder.append(recordAction);
    builder.append(", beneficiaryId=");
    builder.append(beneficiaryId);
    builder.append(", record=");
    builder.append(record);
    builder.append("]");
    return builder.toString();
  }
}
