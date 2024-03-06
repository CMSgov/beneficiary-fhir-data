package gov.cms.bfd.model.rif.samples;

import gov.cms.bfd.model.rif.RifFile;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Enumerates groups of related {@link StaticRifResource}s that can be processed together. */
public enum StaticRifResourceGroup {
  /** Sample A for BB2 Resource Group. */
  SAMPLE_A_BB2(
      StaticRifResource.SAMPLE_A4BB2_BENES,
      StaticRifResource.SAMPLE_A4BB2_BENEFICIARY_HISTORY,
      StaticRifResource.SAMPLE_A4BB2_CARRIER,
      StaticRifResource.SAMPLE_A4BB2_PDE,
      StaticRifResource.SAMPLE_A4BB2_INPATIENT,
      StaticRifResource.SAMPLE_A4BB2_OUTPATIENT,
      StaticRifResource.SAMPLE_A4BB2_HHA,
      StaticRifResource.SAMPLE_A4BB2_HOSPICE,
      StaticRifResource.SAMPLE_A4BB2_SNF,
      StaticRifResource.SAMPLE_A4BB2_DME),
  /** Sample A Resource Group. */
  SAMPLE_A(
      StaticRifResource.SAMPLE_A_BENES,
      StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY,
      StaticRifResource.SAMPLE_A_CARRIER,
      StaticRifResource.SAMPLE_A_PDE,
      StaticRifResource.SAMPLE_A_INPATIENT,
      StaticRifResource.SAMPLE_A_OUTPATIENT,
      StaticRifResource.SAMPLE_A_HHA,
      StaticRifResource.SAMPLE_A_HOSPICE,
      StaticRifResource.SAMPLE_A_SNF,
      StaticRifResource.SAMPLE_A_DME),
  /** Sample A Multiple Carrier Lines Resource Group. */
  SAMPLE_A_MULTIPLE_CARRIER_LINES(
      StaticRifResource.SAMPLE_A_BENES,
      StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY,
      StaticRifResource.SAMPLE_A_CARRIER_MULTIPLE_LINES,
      StaticRifResource.SAMPLE_A_PDE,
      StaticRifResource.SAMPLE_A_INPATIENT,
      StaticRifResource.SAMPLE_A_OUTPATIENT,
      StaticRifResource.SAMPLE_A_HHA,
      StaticRifResource.SAMPLE_A_HOSPICE,
      StaticRifResource.SAMPLE_A_SNF,
      StaticRifResource.SAMPLE_A_DME),
  /** Sample A with various SAMHSA data in each claim. */
  SAMPLE_A_SAMHSA(
      StaticRifResource.SAMPLE_A_BENES,
      StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY,
      StaticRifResource.SAMPLE_A_CARRIER_SAMHSA,
      // PDE has no SAMHSA data
      StaticRifResource.SAMPLE_A_PDE,
      StaticRifResource.SAMPLE_A_INPATIENT_SAMHSA,
      StaticRifResource.SAMPLE_A_OUTPATIENT_SAMHSA,
      StaticRifResource.SAMPLE_A_HHA_SAMHSA,
      StaticRifResource.SAMPLE_A_HOSPICE_SAMHSA,
      StaticRifResource.SAMPLE_A_SNF_SAMHSA,
      StaticRifResource.SAMPLE_A_DME_SAMHSA),
  /** Sample A Four Character DRG Code Resource Group. */
  SAMPLE_A_FOUR_CHARACTER_DRG_CODE(
      StaticRifResource.SAMPLE_A_BENES,
      StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY,
      StaticRifResource.SAMPLE_A_CARRIER,
      StaticRifResource.SAMPLE_A_PDE,
      StaticRifResource.SAMPLE_A_INPATIENT_FOUR_CHARACTER_DRG_CODE,
      StaticRifResource.SAMPLE_A_OUTPATIENT,
      StaticRifResource.SAMPLE_A_HHA,
      StaticRifResource.SAMPLE_A_HOSPICE,
      StaticRifResource.SAMPLE_A_SNF_FOUR_CHARACTER_DRG_CODE,
      StaticRifResource.SAMPLE_A_DME),
  /** Sample A Without Reference Year Resource Group. */
  SAMPLE_A_WITHOUT_REFERENCE_YEAR(
      StaticRifResource.SAMPLE_A_BENES_WITHOUT_REFERENCE_YEAR,
      StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY,
      StaticRifResource.SAMPLE_A_CARRIER,
      StaticRifResource.SAMPLE_A_PDE,
      StaticRifResource.SAMPLE_A_INPATIENT,
      StaticRifResource.SAMPLE_A_OUTPATIENT,
      StaticRifResource.SAMPLE_A_HHA,
      StaticRifResource.SAMPLE_A_HOSPICE,
      StaticRifResource.SAMPLE_A_SNF,
      StaticRifResource.SAMPLE_A_DME),
  /** Sample A Multiple Entires of the Same Beneficiary Resource Group. */
  SAMPLE_A_MULTIPLE_ENTRIES_SAME_BENE(StaticRifResource.SAMPLE_A_MULTIPLE_ROWS_SAME_BENE),
  /** Sample U Resource Group. */
  SAMPLE_U(StaticRifResource.SAMPLE_U_BENES, StaticRifResource.SAMPLE_U_CARRIER),
  /** Sample U Beneficiaries Unchanged Resource Group. */
  SAMPLE_U_BENES_UNCHANGED(
      StaticRifResource.SAMPLE_U_BENES_UNCHANGED, StaticRifResource.SAMPLE_U_CARRIER),
  /** Sample U Beneficiarys Chaned with 8 Months Resource Group. */
  SAMPLE_U_BENES_CHANGED_WITH_8_MONTHS(
      StaticRifResource.SAMPLE_U_BENES_CHANGED_WITH_8_MONTHS, StaticRifResource.SAMPLE_U_CARRIER),
  /** Sample U Beneficiarys Chaned with 9 Months Resource Group. */
  SAMPLE_U_BENES_CHANGED_WITH_9_MONTHS(
      StaticRifResource.SAMPLE_U_BENES_CHANGED_WITH_9_MONTHS, StaticRifResource.SAMPLE_U_CARRIER),
  /** Synthea Data Resource Group. */
  SYNTHEA_DATA(
      StaticRifResource.SAMPLE_SYNTHEA_BENES2011,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2012,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2013,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2014,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2015,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2016,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2017,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2018,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2019,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2020,
      StaticRifResource.SAMPLE_SYNTHEA_BENES2021,
      StaticRifResource.SAMPLE_SYNTHEA_CARRIER,
      StaticRifResource.SAMPLE_SYNTHEA_INPATIENT,
      StaticRifResource.SAMPLE_SYNTHEA_OUTPATIENT,
      StaticRifResource.SAMPLE_SYNTHEA_SNF,
      StaticRifResource.SAMPLE_SYNTHEA_HOSPICE,
      StaticRifResource.SAMPLE_SYNTHEA_HHA,
      StaticRifResource.SAMPLE_SYNTHEA_DME,
      StaticRifResource.SAMPLE_SYNTHEA_PDE,
      StaticRifResource.SAMPLE_SYNTHEA_BENEHISTORY),
  /** Sample HICN Multiple Beneficiaries Resource Group. */
  SAMPLE_HICN_MULT_BENES(
      StaticRifResource.SAMPLE_HICN_MULT_BENES,
      StaticRifResource.SAMPLE_HICN_MULT_BENES_BENEFICIARY_HISTORY);

  /** Static Rif Resource. */
  private final StaticRifResource[] resources;

  /**
   * Enum constant constructor.
   *
   * @param resources the value to use for {@link #getResources()}
   */
  private StaticRifResourceGroup(StaticRifResource... resources) {
    this.resources = resources;
  }

  /**
   * Gets the {@link #resources}.
   *
   * @return the related {@link StaticRifResource}s grouped into this {@link StaticRifResourceGroup}
   */
  public StaticRifResource[] getResources() {
    return resources;
  }

  /**
   * Generates a {@link Set} of {@link RifFile}s based on {@link #resources}.
   *
   * @return the set of RIF files
   */
  public Set<RifFile> toRifFiles() {
    return Arrays.stream(resources)
        .map(resource -> resource.toRifFile())
        .collect(Collectors.toSet());
  }
}
