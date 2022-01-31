package gov.cms.bfd.model.rif;

/** Enumerates the various types of RIF files. */
public enum RifFileType {
  BENEFICIARY(BeneficiaryColumn.class, BeneficiaryColumn.BENE_ID),

  BENEFICIARY_HISTORY(BeneficiaryHistoryColumn.class, null),

  MEDICARE_BENEFICIARY_ID_HISTORY(
      MedicareBeneficiaryIdHistoryColumn.class, MedicareBeneficiaryIdHistoryColumn.BENE_MBI_ID),

  CARRIER(CarrierClaimColumn.class, CarrierClaimColumn.CLM_ID),

  DME(DMEClaimColumn.class, DMEClaimColumn.CLM_ID),

  HHA(HHAClaimColumn.class, HHAClaimColumn.CLM_ID),

  HOSPICE(HospiceClaimColumn.class, HospiceClaimColumn.CLM_ID),

  INPATIENT(InpatientClaimColumn.class, InpatientClaimColumn.CLM_ID),

  OUTPATIENT(OutpatientClaimColumn.class, OutpatientClaimColumn.CLM_ID),

  PDE(PartDEventColumn.class, PartDEventColumn.PDE_ID),

  SNF(SNFClaimColumn.class, SNFClaimColumn.CLM_ID);

  private final Class<Enum<?>> columnEnum;
  private final Enum<?> idColumn;

  /**
   * Enum constant constructor.
   *
   * @param columnEnum the value to use for {@link #getColumnEnum()}
   * @param idColumn the value to use for {@link #getIdColumn()}
   */
  @SuppressWarnings("unchecked")
  private RifFileType(Class<?> columnEnum, Enum<?> idColumn) {
    this.columnEnum = (Class<Enum<?>>) columnEnum;
    this.idColumn = idColumn;
  }

  /**
   * @return the <code>Column</code> enum constant for this {@link RifFileType}, e.g. {@link
   *     gov.cms.bfd.model.rif.BeneficiaryColumn}
   */
  public Class<Enum<?>> getColumnEnum() {
    return columnEnum;
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
