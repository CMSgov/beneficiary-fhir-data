package gov.cms.bfd.server.ng.beneficiary.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Entity
public class HistoricalIdentity {
  @Id String mbi;
  @Id Long beneSk;
  Optional<LocalDate> effectiveDate;
  Optional<LocalDate> obsoleteDate;
  boolean isCurrentMbi;
}
