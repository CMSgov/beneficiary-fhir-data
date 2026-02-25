package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.SystemType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Filters claims on claim type codes.
 *
 * @param claimTypeCodes claim type codes
 */
public record ClaimTypeCodeFilterParam(List<ClaimTypeCode> claimTypeCodes)
    implements DbFilterBuilder {
  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String claimTableAlias, @NotNull SystemType systemType) {
    if (claimTypeCodes.isEmpty()) {
      return DbFilter.empty();
    }

    return new DbFilter(
        String.format(" AND %s.claimTypeCode IN :claimTypeCodes", claimTableAlias),
        List.of(new DbFilterParam("claimTypeCodes", claimTypeCodes)));
  }
}
