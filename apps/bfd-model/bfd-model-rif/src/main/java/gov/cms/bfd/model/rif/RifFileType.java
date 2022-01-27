package gov.cms.bfd.model.rif;

import java.util.Optional;

/** Enumerates the various types of RIF files. */
public enum RifFileType {
  BENEFICIARY(BeneficiaryColumn.class, Optional.of(BeneficiaryColumn.BENE_ID)),

  BENEFICIARY_HISTORY(BeneficiaryHistoryColumn.class, Optional.empty()),

  MEDICARE_BENEFICIARY_ID_HISTORY(
      MedicareBeneficiaryIdHistoryColumn.class,
      Optional.of(MedicareBeneficiaryIdHistoryColumn.BENE_MBI_ID)),

  CARRIER(CarrierClaimColumn.class, Optional.of(CarrierClaimColumn.CLM_ID)),

  DME(DMEClaimColumn.class, Optional.of(DMEClaimColumn.CLM_ID)),

  HHA(HHAClaimColumn.class, Optional.of(HHAClaimColumn.CLM_ID)),

  HOSPICE(HospiceClaimColumn.class, Optional.of(HospiceClaimColumn.CLM_ID)),

  INPATIENT(InpatientClaimColumn.class, Optional.of(InpatientClaimColumn.CLM_ID)),

  OUTPATIENT(OutpatientClaimColumn.class, Optional.of(OutpatientClaimColumn.CLM_ID)),

  PDE(PartDEventColumn.class, Optional.of(PartDEventColumn.PDE_ID)),

  SNF(SNFClaimColumn.class, Optional.of(SNFClaimColumn.CLM_ID));

  private final Class<Enum<?>> columnEnum;
  private final Optional<Enum<?>> idColumn;

  /**
   * Enum constant constructor.
   *
   * @param columnEnum the value to use for {@link #getColumnEnum()}
   * @param idColumn the value to use for {@link #getIdColumn()}
   */
  @SuppressWarnings("unchecked")
  private RifFileType(Class<?> columnEnum, Optional<Enum<?>> idColumn) {
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
   *     e.g. {@link gov.cms.bfd.model.rif.BeneficiaryColumn#BENE_ID}, or <code>Optional.empty()
   *     </code> if the {@link RifFileType} doesn't have an ID/grouping column
   */
  public Optional<Enum<?>> getIdColumn() {
    return idColumn;
  }
}
