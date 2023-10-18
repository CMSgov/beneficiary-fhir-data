package gov.cms.bfd.model.rif;

import gov.cms.bfd.model.rif.entities.BeneficiaryColumn;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistoryColumn;
import gov.cms.bfd.model.rif.entities.CarrierClaimColumn;
import gov.cms.bfd.model.rif.entities.DMEClaimColumn;
import gov.cms.bfd.model.rif.entities.HHAClaimColumn;
import gov.cms.bfd.model.rif.entities.HospiceClaimColumn;
import gov.cms.bfd.model.rif.entities.InpatientClaimColumn;
import gov.cms.bfd.model.rif.entities.OutpatientClaimColumn;
import gov.cms.bfd.model.rif.entities.PartDEventColumn;
import gov.cms.bfd.model.rif.entities.SNFClaimColumn;
import java.util.Arrays;

/** Enumerates the various types of RIF files. */
public enum RifFileType {
  /** Represents a Beneficiary rif file. */
  BENEFICIARY(
      BeneficiaryColumn.class,
      Arrays.stream(BeneficiaryColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      BeneficiaryColumn.BENE_ID),
  /** Represents a Beneficiary History rif file. */
  BENEFICIARY_HISTORY(
      BeneficiaryHistoryColumn.class,
      Arrays.stream(BeneficiaryHistoryColumn.values())
          .map(c -> ((Enum<?>) c))
          .toArray(Enum<?>[]::new),
      null),
  /** Represents a Carrier rif file. */
  CARRIER(
      CarrierClaimColumn.class,
      Arrays.stream(CarrierClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      CarrierClaimColumn.CLM_ID),
  /** Represents a DME (Durable Medical Equipment) rif file. */
  DME(
      DMEClaimColumn.class,
      Arrays.stream(DMEClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      DMEClaimColumn.CLM_ID),
  /** Represents a HHA (Home Health Agency) rif file. */
  HHA(
      HHAClaimColumn.class,
      Arrays.stream(HHAClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      HHAClaimColumn.CLM_ID),
  /** Represents a Hospice rif file. */
  HOSPICE(
      HospiceClaimColumn.class,
      Arrays.stream(HospiceClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      HospiceClaimColumn.CLM_ID),
  /** Represents an Inpatient rif file. */
  INPATIENT(
      InpatientClaimColumn.class,
      Arrays.stream(InpatientClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      InpatientClaimColumn.CLM_ID),
  /** Represents an Outpatient rif file. */
  OUTPATIENT(
      OutpatientClaimColumn.class,
      Arrays.stream(OutpatientClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      OutpatientClaimColumn.CLM_ID),
  /** Represents a PDE (Prescription Drug Event) rif file. */
  PDE(
      PartDEventColumn.class,
      Arrays.stream(PartDEventColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      PartDEventColumn.PDE_ID),
  /** Represents an SNF (Skilled Nursing Facility) rif file. */
  SNF(
      SNFClaimColumn.class,
      Arrays.stream(SNFClaimColumn.values()).map(c -> ((Enum<?>) c)).toArray(Enum<?>[]::new),
      SNFClaimColumn.CLM_ID);

  /**
   * The <code>Column</code> enum constant for this {@link RifFileType}, e.g. {@link
   * gov.cms.bfd.model.rif.entities.BeneficiaryColumn}.
   */
  private final Class<Enum<?>> columnEnum;

  /** All the columns/entries from this {@link RifFileType}'s columns enum. */
  private final Enum<?>[] columns;

  /**
   * The <code>Column</code> enum constant for this {@link RifFileType} ID/grouping column, e.g.
   * {@link gov.cms.bfd.model.rif.entities.BeneficiaryColumn#BENE_ID}
   */
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
   * Gets the {@link #columnEnum}.
   *
   * @return the <code>Column</code> enum constant for this {@link RifFileType}, e.g. {@link
   *     gov.cms.bfd.model.rif.entities.BeneficiaryColumn}
   */
  public Class<Enum<?>> getColumnEnum() {
    return columnEnum;
  }

  /**
   * Gets the {@link #columns}.
   *
   * @return all the columns/entries from this {@link RifFileType}'s columns enum
   */
  public Enum<?>[] getColumns() {
    return columns;
  }

  /**
   * Gets the {@link #idColumn}.
   *
   * @return the <code>Column</code> enum constant for this {@link RifFileType} ID/grouping column,
   *     e.g. {@link gov.cms.bfd.model.rif.entities.BeneficiaryColumn#BENE_ID}, or <code>null</code>
   *     if the {@link RifFileType} doesn't have an ID/grouping column
   */
  public Enum<?> getIdColumn() {
    return idColumn;
  }
}
