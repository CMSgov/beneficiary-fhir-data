package gov.cms.bfd.model.rif;

import java.util.Arrays;

/** Enumerates the various types of RIF files. */
public enum RifFileType {
  BENEFICIARY(
      BeneficiaryColumn.class,
      Arrays.stream(BeneficiaryColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      BeneficiaryColumn.BENE_ID),

  BENEFICIARY_HISTORY(
      BeneficiaryHistoryColumn.class,
      Arrays.stream(BeneficiaryHistoryColumn.values())
          .map(c -> ((Enum<?>) c))
          .toArray(Enum<?>[]::new),
      null),

  MEDICARE_BENEFICIARY_ID_HISTORY(
      MedicareBeneficiaryIdHistoryColumn.class,
      Arrays.stream(MedicareBeneficiaryIdHistoryColumn.values())
          .map(c -> ((Enum<?>) c))
          .toArray(Enum<?>[]::new),
      MedicareBeneficiaryIdHistoryColumn.BENE_MBI_ID),

  CARRIER(
      CarrierClaimColumn.class,
      Arrays.stream(CarrierClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      CarrierClaimColumn.CLM_ID),

  DME(
      DMEClaimColumn.class,
      Arrays.stream(DMEClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      DMEClaimColumn.CLM_ID),

  HHA(
      HHAClaimColumn.class,
      Arrays.stream(HHAClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      HHAClaimColumn.CLM_ID),

  HOSPICE(
      HospiceClaimColumn.class,
      Arrays.stream(HospiceClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      HospiceClaimColumn.CLM_ID),

  INPATIENT(
      InpatientClaimColumn.class,
      Arrays.stream(InpatientClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      InpatientClaimColumn.CLM_ID),

  OUTPATIENT(
      OutpatientClaimColumn.class,
      Arrays.stream(OutpatientClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      OutpatientClaimColumn.CLM_ID),

  PDE(
      PartDEventColumn.class,
      Arrays.stream(PartDEventColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      PartDEventColumn.PDE_ID),

  SNF(
      SNFClaimColumn.class,
      Arrays.stream(SNFClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      SNFClaimColumn.CLM_ID);

  private final Class<Enum<?>> columnEnum;
  private final Enum<?>[] columns;
  private final Enum<?> idColumn;

  /**
   * Enum constant constructor.
   *
   * @param columnEnum the value to use for {@link #getColumnEnum()}
   * @param columns the value to use for {@link #getColumns()} (sorry, not sorry for the hilarious
   *     Dark Type Magic being invoked here to get a properly typed value)
   * @param idColumn the value to use for {@link #getIdColumn()}
   */
  @SuppressWarnings("unchecked")
  private RifFileType(Class<?> columnEnum, Enum<?>[] columns, Enum<?> idColumn) {
    this.columnEnum = (Class<Enum<?>>) columnEnum;
    this.columns = columns;
    this.idColumn = idColumn;
  }

  /**
   * @return the <code>Column</code> enum constant for this {@link RifFileType}, e.g. {@link
   *     gov.cms.bfd.model.rif.BeneficiaryColumn}
   */
  public Class<Enum<?>> getColumnEnum() {
    return columnEnum;
  }

  /** @return all of the columns/entries from this {@link RifFileType}'s columns enum */
  public Enum<?>[] getColumns() {
    return columns;
  }

  /**
   * @return the <code>Column</code> enum constant for this {@link RifFileType} ID/grouping column,
   *     e.g. {@link gov.cms.bfd.model.rif.BeneficiaryColumn#BENE_ID}, or <code>null</code> if the
   *     {@link RifFileType} doesn't have an ID/grouping column
   */
  public Enum<?> getIdColumn() {
    return idColumn;
  }
}
