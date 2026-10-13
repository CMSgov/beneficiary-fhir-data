package gov.cms.bfd.server.ng.claim.filter;

import gov.cms.bfd.server.ng.DbFilter;
import gov.cms.bfd.server.ng.DbFilterBuilder;
import gov.cms.bfd.server.ng.DbFilterParam;
import gov.cms.bfd.server.ng.claim.model.common.ClaimTypeCode;
import gov.cms.bfd.server.ng.claim.model.common.SystemType;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Filters claims on claim type codes.
 *
 * @param paramName query parameter name, must be unique within the query
 * @param claimTypeCodes claim type codes
 */
public record ClaimTypeCodeFilterParam(String paramName, List<ClaimTypeCode> claimTypeCodes)
    implements DbFilterBuilder {

  /**
   * Filters claims on claim type codes.
   *
   * @param claimTypeCodes claim type codes
   */
  public ClaimTypeCodeFilterParam(List<ClaimTypeCode> claimTypeCodes) {
    this("claimTypeCodes", claimTypeCodes);
  }

  @NotNull
  @Override
  public DbFilter getFilters(@NotNull String tableAlias, @NotNull SystemType systemType) {
    if (claimTypeCodes.isEmpty()) {
      return DbFilter.empty();
    }

    return new DbFilter(
        String.format(" AND %s.claimTypeCode IN :%s", tableAlias, paramName),
        List.of(new DbFilterParam(paramName, claimTypeCodes)));
  }
}
