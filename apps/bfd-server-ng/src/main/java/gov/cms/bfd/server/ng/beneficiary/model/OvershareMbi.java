package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents the materialized view containing MBIs that are misconfigured in IDR. These MBIs should
 * not be served by the API as they pose a risk of leaking information to the wrong beneficiaries.
 */
@Entity
@Table(name = "overshare_mbis", schema = "idr")
public class OvershareMbi {
  @Column(name = "bene_mbi_id")
  @Id
  private String mbi;
}
