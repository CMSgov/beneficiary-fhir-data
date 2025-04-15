package gov.cms.bfd.model.rif.npi_fda;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Entity for fda_data table. */
@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "fda_data", schema = "ccw")
public class FDAData {
  /** NDC code. */
  @Id
  @Column(name = "code", nullable = false)
  String code;

  /** NDC display. */
  @Column(name = "display")
  String display;
}
