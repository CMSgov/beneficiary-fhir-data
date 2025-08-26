package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.Getter;

/**
 * Base class for beneficiary entities. This is used to prevent excessive fetching for patient
 * information.
 */
@Getter
@MappedSuperclass
public abstract class BeneficiaryBase {
  @Id
  @Column(name = "bene_sk")
  protected long beneSk;

  @Column(name = "bene_xref_efctv_sk_computed")
  protected long xrefSk;

  @Column(name = "bene_brth_dt")
  protected LocalDate birthDate;

  @Column(name = "bene_race_cd")
  protected RaceCode raceCode;

  @Column(name = "bene_sex_cd")
  protected Optional<SexCode> sexCode;

  @Column(name = "cntct_lang_cd")
  protected LanguageCode languageCode;

  @Column(name = "idr_trans_obslt_ts")
  protected ZonedDateTime obsoleteTimestamp;

  @Embedded protected Name beneficiaryName;
  @Embedded protected Address address;
  @Embedded protected Meta meta;
  @Embedded protected DeathDate deathDate;
  @Embedded protected CurrentIdentifier identifier;

  /**
   * Determines if this beneficiary has been merged into another.
   *
   * @return whether the beneficiary is merged
   */
  public boolean isMergedBeneficiary() {
    return beneSk != xrefSk;
  }
}
