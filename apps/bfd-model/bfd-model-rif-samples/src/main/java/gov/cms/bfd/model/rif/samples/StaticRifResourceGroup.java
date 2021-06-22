package gov.cms.bfd.model.rif.samples;

import gov.cms.bfd.model.rif.RifFile;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/** Enumerates groups of related {@link StaticRifResource}s that can be processed together. */
public enum StaticRifResourceGroup {
  SAMPLE_A(
      StaticRifResource.SAMPLE_A_BENES,
      StaticRifResource.SAMPLE_A_BENEFICIARY_HISTORY,
      StaticRifResource.SAMPLE_A_MEDICARE_BENEFICIARY_ID_HISTORY,
      StaticRifResource.SAMPLE_A_CARRIER,
      StaticRifResource.SAMPLE_A_PDE,
      StaticRifResource.SAMPLE_A_INPATIENT,
      StaticRifResource.SAMPLE_A_OUTPATIENT,
      StaticRifResource.SAMPLE_A_HHA,
      StaticRifResource.SAMPLE_A_HOSPICE,
      StaticRifResource.SAMPLE_A_SNF,
      StaticRifResource.SAMPLE_A_DME),

  SAMPLE_B(
      StaticRifResource.SAMPLE_B_BENES,
      StaticRifResource.SAMPLE_B_CARRIER,
      StaticRifResource.SAMPLE_B_INPATIENT,
      StaticRifResource.SAMPLE_B_OUTPATIENT,
      StaticRifResource.SAMPLE_B_SNF,
      StaticRifResource.SAMPLE_B_HOSPICE,
      StaticRifResource.SAMPLE_B_HHA,
      StaticRifResource.SAMPLE_B_DME,
      StaticRifResource.SAMPLE_B_PDE),

  SAMPLE_C(
      StaticRifResource.SAMPLE_C_BENES,
      StaticRifResource.SAMPLE_C_CARRIER,
      StaticRifResource.SAMPLE_C_INPATIENT,
      StaticRifResource.SAMPLE_C_OUTPATIENT,
      StaticRifResource.SAMPLE_C_SNF,
      StaticRifResource.SAMPLE_C_HOSPICE,
      StaticRifResource.SAMPLE_C_HHA,
      StaticRifResource.SAMPLE_C_DME,
      StaticRifResource.SAMPLE_C_PDE),

  SAMPLE_U(StaticRifResource.SAMPLE_U_BENES, StaticRifResource.SAMPLE_U_CARRIER),

  SAMPLE_U_BENES_UNCHANGED(
      StaticRifResource.SAMPLE_U_BENES_UNCHANGED, StaticRifResource.SAMPLE_U_CARRIER),

  SYNTHETIC_DATA(
      StaticRifResource.SYNTHETIC_BENEFICIARY_1999,
      StaticRifResource.SYNTHETIC_BENEFICIARY_2000,
      StaticRifResource.SYNTHETIC_BENEFICIARY_2014,
      StaticRifResource.SYNTHETIC_CARRIER_1999_1999,
      StaticRifResource.SYNTHETIC_CARRIER_1999_2000,
      StaticRifResource.SYNTHETIC_CARRIER_1999_2001,
      StaticRifResource.SYNTHETIC_CARRIER_2000_2000,
      StaticRifResource.SYNTHETIC_CARRIER_2000_2001,
      StaticRifResource.SYNTHETIC_CARRIER_2000_2002,
      StaticRifResource.SYNTHETIC_CARRIER_2014_2014,
      StaticRifResource.SYNTHETIC_CARRIER_2014_2015,
      StaticRifResource.SYNTHETIC_CARRIER_2014_2016,
      StaticRifResource.SYNTHETIC_INPATIENT_1999_1999,
      StaticRifResource.SYNTHETIC_INPATIENT_1999_2000,
      StaticRifResource.SYNTHETIC_INPATIENT_1999_2001,
      StaticRifResource.SYNTHETIC_INPATIENT_2000_2000,
      StaticRifResource.SYNTHETIC_INPATIENT_2000_2001,
      StaticRifResource.SYNTHETIC_INPATIENT_2000_2002,
      StaticRifResource.SYNTHETIC_INPATIENT_2014_2014,
      StaticRifResource.SYNTHETIC_INPATIENT_2014_2015,
      StaticRifResource.SYNTHETIC_INPATIENT_2014_2016,
      StaticRifResource.SYNTHETIC_PDE_2014,
      StaticRifResource.SYNTHETIC_PDE_2015,
      StaticRifResource.SYNTHETIC_PDE_2016,
      StaticRifResource.SYNTHETIC_OUTPATIENT_1999_1999,
      StaticRifResource.SYNTHETIC_OUTPATIENT_2000_1999,
      StaticRifResource.SYNTHETIC_OUTPATIENT_2001_1999,
      StaticRifResource.SYNTHETIC_OUTPATIENT_2002_2000,
      StaticRifResource.SYNTHETIC_OUTPATIENT_2014_2014,
      StaticRifResource.SYNTHETIC_OUTPATIENT_2015_2014,
      StaticRifResource.SYNTHETIC_OUTPATIENT_2016_2014),

  SAMPLE_MCT(StaticRifResource.SAMPLE_MCT_BENES, StaticRifResource.SAMPLE_MCT_PDE),

  SAMPLE_MCT_UPDATE_1(StaticRifResource.SAMPLE_MCT_UPDATE_1_BENES),

  SAMPLE_MCT_UPDATE_2(StaticRifResource.SAMPLE_MCT_UPDATE_2_PDE),

  SAMPLE_MCT_UPDATE_3(
      StaticRifResource.SAMPLE_MCT_UPDATE_3_BENES, StaticRifResource.SAMPLE_MCT_UPDATE_3_PDE),

  SAMPLE_HICN_MULT_BENES(
      StaticRifResource.SAMPLE_HICN_MULT_BENES,
      StaticRifResource.SAMPLE_HICN_MULT_BENES_BENEFICIARY_HISTORY),

  /**
   * This {@link StaticRifResourceGroup} will run a customized version of <a
   * href="https://synthetichealth.github.io/synthea/">Synthea</a> to generate realistic-looking
   * synthetic data for BFD.
   */
  SYNTHEA(
      StaticRifResource.SYNTHEA_BENES_2011,
      StaticRifResource.SYNTHEA_BENES_2012,
      StaticRifResource.SYNTHEA_BENES_2013,
      StaticRifResource.SYNTHEA_BENES_2014,
      StaticRifResource.SYNTHEA_BENES_2015,
      StaticRifResource.SYNTHEA_BENES_2016,
      StaticRifResource.SYNTHEA_BENES_2017,
      StaticRifResource.SYNTHEA_BENES_2018,
      StaticRifResource.SYNTHEA_BENES_2019,
      StaticRifResource.SYNTHEA_BENES_2020,
      StaticRifResource.SYNTHEA_BENES_2021,
      StaticRifResource.SYNTHEA_INPATIENT,
      StaticRifResource.SYNTHEA_OUTPATIENT,
      StaticRifResource.SYNTHEA_PDE,
      StaticRifResource.SYNTHEA_CARRIER,
      StaticRifResource.SYNTHEA_DME,
      StaticRifResource.SYNTHEA_HHA,
      StaticRifResource.SYNTHEA_HOSPICE,
      StaticRifResource.SYNTHEA_SNF,
      StaticRifResource.SYNTHEA_BENE_HISTORY);

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
   * @return the related {@link StaticRifResource}s grouped into this {@link StaticRifResourceGroup}
   */
  public StaticRifResource[] getResources() {
    return resources;
  }

  /** @return a {@link Set} of {@link RifFile}s based on {@link #getResources()} */
  public Set<RifFile> toRifFiles() {
    return Arrays.stream(resources)
        .map(resource -> resource.toRifFile())
        .collect(Collectors.toSet());
  }
}
