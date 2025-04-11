package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Entity
public class HistoricalIdentity {
  @Id String mbi;
  @Id Long beneSk;
  boolean isCurrentMbi;
}
