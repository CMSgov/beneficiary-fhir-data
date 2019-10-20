package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import java.util.Date;
import java.util.Optional;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/** As set of methods to help form JPA queries. */
public class QueryUtils {
  /**
   * Create a predicate for the lastUpdate field based on the passed range.
   *
   * @param criteriaBuilder to use
   * @param root to use
   * @param range to base the predicate on
   * @return a predicate on the lastUpdated field
   */
  static Predicate createLastUpdatedPredicate(
      CriteriaBuilder criteriaBuilder, Root<?> root, DateRangeParam range) {
    final Date lowerBound = range.getLowerBoundAsInstant();
    final Date upperBound = range.getUpperBoundAsInstant();
    final Path<Date> lastUpdatedPath = root.get("lastUpdated");

    Predicate lowerBoundPredicate = null;
    if (lowerBound != null) {
      switch (range.getLowerBound().getPrefix()) {
        case EQUAL:
          lowerBoundPredicate = criteriaBuilder.greaterThanOrEqualTo(lastUpdatedPath, lowerBound);
          break;
        case GREATERTHAN:
          lowerBoundPredicate = criteriaBuilder.greaterThan(lastUpdatedPath, lowerBound);
          break;
        case GREATERTHAN_OR_EQUALS:
          lowerBoundPredicate = criteriaBuilder.greaterThanOrEqualTo(lastUpdatedPath, lowerBound);
          break;
        case STARTS_AFTER:
        case APPROXIMATE:
        case ENDS_BEFORE:
        case LESSTHAN:
        case LESSTHAN_OR_EQUALS:
        case NOT_EQUAL:
        default:
          throw new IllegalArgumentException("_lastUpdate lower bound has an invalid prefix");
      }
    }

    Predicate upperBoundPredicate = null;
    if (upperBound != null) {
      switch (range.getUpperBound().getPrefix()) {
        case EQUAL:
          if (range.getLowerBound().getPrefix() == ParamPrefixEnum.EQUAL) {
            upperBoundPredicate = criteriaBuilder.lessThanOrEqualTo(lastUpdatedPath, upperBound);
          } else {
            throw new IllegalArgumentException(
                "_lastUpdate lower bound should have an equal prefix when the upper bound does");
          }
          break;
        case LESSTHAN:
          upperBoundPredicate = criteriaBuilder.lessThan(lastUpdatedPath, upperBound);
          break;
        case LESSTHAN_OR_EQUALS:
          upperBoundPredicate = criteriaBuilder.lessThanOrEqualTo(lastUpdatedPath, upperBound);
          break;
        case ENDS_BEFORE:
        case APPROXIMATE:
        case STARTS_AFTER:
        case GREATERTHAN:
        case GREATERTHAN_OR_EQUALS:
        case NOT_EQUAL:
        default:
          throw new IllegalArgumentException("_lastUpdate upper bound has an invalid prefix");
      }
    }

    // Form an interval predicate form upper and lower bound predicates
    Predicate predicate;
    if (lowerBoundPredicate != null && upperBoundPredicate != null) {
      predicate = criteriaBuilder.and(lowerBoundPredicate, upperBoundPredicate);
    } else if (lowerBoundPredicate != null) {
      predicate = lowerBoundPredicate;
    } else if (upperBoundPredicate != null) {
      // the unbounded _lastUpdated < <any date> expression should match all null lastUpdated rows
      predicate = criteriaBuilder.or(upperBoundPredicate, criteriaBuilder.isNull(lastUpdatedPath));
    } else {
      throw new RuntimeException("Should of not reach here; Null or empty lastUpdated");
    }
    return predicate;
  }

  /**
   * Create a predicate for the lastUpdate field based on the passed range.
   *
   * @param lastUpdated date to test. Maybe null.
   * @param range to base test against. Maybe null.
   * @return true iff within the range specified
   */
  static boolean isInRange(Date lastUpdated, DateRangeParam range) {
    if (range == null || range.isEmpty()) {
      return true;
    }
    if (lastUpdated == null) {
      // the unbounded _lastUpdated < <any date> expression should match all null lastUpdated rows
      if (range.getLowerBound() != null) return false;
      return Optional.ofNullable(range.getUpperBound())
          .map(
              upperBound ->
                  upperBound.getPrefix() == ParamPrefixEnum.LESSTHAN
                      || upperBound.getPrefix() == ParamPrefixEnum.LESSTHAN_OR_EQUALS)
          .orElse(false);
    }

    final long lastUpdatedMillis = lastUpdated.getTime();
    if (range.getLowerBound() != null) {
      final long lowerBoundMillis = range.getLowerBoundAsInstant().getTime();
      switch (range.getLowerBound().getPrefix()) {
        case GREATERTHAN:
          if (lastUpdatedMillis <= lowerBoundMillis) {
            return false;
          }
          break;
        case EQUAL:
          if (lastUpdatedMillis < lowerBoundMillis) {
            return false;
          }
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
        case EQUAL:
          if (range.getLowerBound().getPrefix() == ParamPrefixEnum.EQUAL) {
            if (lastUpdatedMillis > upperBoundMillis) {
              return false;
            }
          } else {
            throw new IllegalArgumentException(
                "_lastUpdate lower bound should have an equal prefix when the upper bound does");
          }
          break;
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
