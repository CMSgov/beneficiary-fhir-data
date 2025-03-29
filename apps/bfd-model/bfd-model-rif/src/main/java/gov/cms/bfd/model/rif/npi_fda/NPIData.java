package gov.cms.bfd.model.rif.npi_fda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity for npi_data table. */
@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "npi_data", schema = "ccw")
public class NPIData {
  /** Provider or Org npi. */
  @Id
  @Column(name = "npi", nullable = false)
  String npi;

  /** Entity Type. Will be 1 for Provider, or 2 for Organization. */
  @Column(name = "entity_type")
  String entityTypeCode;

  /** Organization name. */
  @Column(name = "org_name")
  String providerOrganizationName;

  /** Taxonomy code. */
  @Column(name = "taxonomy_code")
  String taxonomyCode;

  /** Taxonomy display. */
  @Column(name = "taxonomy_display")
  String taxonomyDisplay;

  /** Provider name prefix. */
  @Column(name = "provider_name_prefix")
  String providerNamePrefix;

  /** Provider first name. */
  @Column(name = "provider_first_name")
  String providerFirstName;

  /** Provider middle name. */
  @Column(name = "provider_middle_name")
  String providerMiddleName;

  /** Provider last name. */
  @Column(name = "provider_last_name")
  String providerLastName;

  /** Provider suffix. */
  @Column(name = "provider_name_suffix")
  String providerNameSuffix;

  /** Provider credential. */
  @Column(name = "provider_credential")
  String providerCredential;
}
