package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Optional;

/** Entity representing the beneficiary MBI history table. */
@Entity
@Table(name = "beneficiary_mbi_history", schema = "idr")
public class BeneficiaryMbiHistory {
  @Id
  @Column(name = "bene_mbi_id")
  private String mbi;

  @Column(name = "bene_mbi_efctv_dt")
  private Optional<LocalDate> effectiveDate;

  @Column(name = "bene_mbi_obslt_dt")
  private Optional<LocalDate> obsoleteDate;
}
