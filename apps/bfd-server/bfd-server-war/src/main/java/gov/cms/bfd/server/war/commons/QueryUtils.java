package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/** As set of methods to help form JPA queries. */
public class QueryUtils {
  /**
   * Create a predicate for the lastUpdate field based on the passed _lastUpdated parameter range.
   *
   * @param cb to use
   * @param root to use
   * @param range to base the predicate on
   * @return a predicate on the lastUpdated field
   */
  public static Predicate createLastUpdatedPredicate(
      CriteriaBuilder cb, Root<?> root, DateRangeParam range) {
    final Path<Instant> lastUpdatedPath = root.get("lastUpdated");
    final Instant lowerBound =
        range.getLowerBoundAsInstant() == null ? null : range.getLowerBoundAsInstant().toInstant();
    final Instant upperBound =
        range.getUpperBoundAsInstant() == null ? null : range.getUpperBoundAsInstant().toInstant();
    Predicate lowerBoundPredicate;
    Predicate upperBoundPredicate;

    if (lowerBound != null) {
      switch (range.getLowerBound().getPrefix()) {
        case GREATERTHAN_OR_EQUALS:
          lowerBoundPredicate = cb.greaterThanOrEqualTo(lastUpdatedPath, lowerBound);
          break;
        case GREATERTHAN:
          lowerBoundPredicate = cb.greaterThan(lastUpdatedPath, lowerBound);
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate lower bound has an invalid prefix");
      }
    } else {
      lowerBoundPredicate = null;
    }

    if (upperBound != null) {
      switch (range.getUpperBound().getPrefix()) {
        case LESSTHAN:
          upperBoundPredicate = cb.lessThan(lastUpdatedPath, upperBound);
          break;
        case LESSTHAN_OR_EQUALS:
          upperBoundPredicate = cb.lessThanOrEqualTo(lastUpdatedPath, upperBound);
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate upper bound has an invalid prefix");
      }
      if (lowerBoundPredicate == null) {
        return cb.or(cb.isNull(lastUpdatedPath), upperBoundPredicate);
      } else {
        return cb.and(lowerBoundPredicate, upperBoundPredicate);
      }
    } else {
      if (lowerBoundPredicate == null) {
        throw new IllegalArgumentException(
            ("_lastUpdate upper and lower bound cannot both be null"));
      } else {
        return lowerBoundPredicate;
      }
    }
  }

  /**
   * Create a predicate for an arbitrary date expression based on the specified parameter range.
   * When any condition is supplied the value must be not-null to be accepted. When no condition is
   * supplied nulls will be accepted.
   *
   * @param builder {@link CriteriaBuilder} used to create various things
   * @param dateRange {@link DateRangeParam} specifying the date bounds
   * @param dateExpression {@link Expression} or {@link Path} defining the date to test
   * @return a {@link Predicate} to evaluate the data range
   */
  public static Predicate createDateRangePredicate(
      CriteriaBuilder builder, DateRangeParam dateRange, Expression<LocalDate> dateExpression) {
    final List<Predicate> predicates = new ArrayList<>();

    final DateParam lowerBound = dateRange.getLowerBound();
    if (lowerBound != null) {
      final LocalDate from =
          lowerBound.getValue().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();

      if (ParamPrefixEnum.GREATERTHAN.equals(lowerBound.getPrefix())) {
        predicates.add(builder.greaterThan(dateExpression, from));
      } else if (ParamPrefixEnum.GREATERTHAN_OR_EQUALS.equals(lowerBound.getPrefix())) {
        predicates.add(builder.greaterThanOrEqualTo(dateExpression, from));
      } else {
        throw new IllegalArgumentException(
            String.format("Unsupported prefix supplied %s", lowerBound.getPrefix()));
      }
    }

    final DateParam upperBound = dateRange.getUpperBound();
    if (upperBound != null) {
      final LocalDate to = upperBound.getValue().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();

      if (ParamPrefixEnum.LESSTHAN_OR_EQUALS.equals(upperBound.getPrefix())) {
        predicates.add(builder.lessThanOrEqualTo(dateExpression, to));
      } else if (ParamPrefixEnum.LESSTHAN.equals(upperBound.getPrefix())) {
        predicates.add(builder.lessThan(dateExpression, to));
      } else {
        throw new IllegalArgumentException(
            String.format("Unsupported prefix supplied %s", upperBound.getPrefix()));
      }
    }

    if (predicates.size() > 0) {
      final Predicate notNull = builder.isNotNull(dateExpression);
      predicates.add(0, notNull);
    }

    return builder.and(predicates.toArray(new Predicate[0]));
  }

  /**
   * Create a predicate for the lastUpdate field based on the passed range.
   *
   * @param lastUpdated date to test. Maybe null.
   * @param range to base test against. Maybe null.
   * @return true iff within the range specified
   */
  public static boolean isInRange(Instant lastUpdated, DateRangeParam range) {
    if (range == null || range.isEmpty()) {
      return true;
    }
    // The null lastUpdated is considered to be a very early time for this calculation
    final long lastUpdatedMillis = lastUpdated == null ? 0L : lastUpdated.toEpochMilli();

    if (range.getLowerBound() != null) {
      final long lowerBoundMillis = range.getLowerBoundAsInstant().toInstant().toEpochMilli();
      switch (range.getLowerBound().getPrefix()) {
        case GREATERTHAN:
          if (lastUpdatedMillis <= lowerBoundMillis) {
            return false;
          }
          break;

        case GREATERTHAN_OR_EQUALS:
          if (lastUpdatedMillis < lowerBoundMillis) {
            return false;
          }
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate lower bound has an invalid prefix");
      }
    }

    if (range.getUpperBound() != null) {
      final long upperBoundMillis = range.getUpperBoundAsInstant().getTime();
      switch (range.getUpperBound().getPrefix()) {
        case LESSTHAN:
          if (lastUpdatedMillis >= upperBoundMillis) {
            return false;
          }
          break;
        case LESSTHAN_OR_EQUALS:
          if (lastUpdatedMillis > upperBoundMillis) {
            return false;
          }
          break;
        default:
          throw new IllegalArgumentException("_lastUpdate upper bound has an invalid prefix");
      }
    }
    return true;
  }
}
