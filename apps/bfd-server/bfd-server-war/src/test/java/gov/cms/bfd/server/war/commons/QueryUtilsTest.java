package gov.cms.bfd.server.war.commons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ParamPrefixEnum;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.BitSet;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link QueryUtils}. */
@ExtendWith(MockitoExtension.class)
public class QueryUtilsTest {
  /** The lower date for the test. * */
  private static final String LOWER_DATE = "2019-08-25";

  /** The upper date for the test. * */
  private static final String UPPER_DATE = "2022-08-25";

  /** Used when mocking query construction. */
  @Mock private CriteriaBuilder builder;

  /** Used when mocking query construction. */
  @Mock private Expression<LocalDate> dateExpression;

  /** Used when mocking query construction. */
  @Mock private Predicate lowerBoundPredicate;

  /** Used when mocking query construction. */
  @Mock private Predicate upperBoundPredicate;

  /** Used when mocking query construction. */
  @Mock private Predicate notNullPredicate;

  /**
   * Test {@link QueryUtils#createDateRangePredicate} with no bounds in the {@link DateRangeParam}.
   */
  @Test
  public void testCreateDateRangePredicateNoBounds() {
    DateRangeParam dateRange = new DateRangeParam((DateParam) null, null);
    QueryUtils.createDateRangePredicate(builder, dateRange, dateExpression);
    verify(builder).and();
  }

  /**
   * Test {@link QueryUtils#createDateRangePredicate} with only a {@link
   * ParamPrefixEnum#GREATERTHAN} lower bound {@link DateRangeParam}.
   */
  @Test
  public void testCreateDateRangePredicateLowerGT() {
    DateRangeParam dateRange =
        new DateRangeParam(new DateParam(ParamPrefixEnum.GREATERTHAN, LOWER_DATE), null);
    LocalDate from = convertDateParamToLocalDate(dateRange.getLowerBound());
    doReturn(notNullPredicate).when(builder).isNotNull(dateExpression);
    doReturn(lowerBoundPredicate).when(builder).greaterThan(dateExpression, from);
    QueryUtils.createDateRangePredicate(builder, dateRange, dateExpression);
    verify(builder).greaterThan(dateExpression, from);
    verify(builder).and(new Predicate[] {notNullPredicate, lowerBoundPredicate});
  }

  /**
   * Test {@link QueryUtils#createDateRangePredicate} with only a {@link
   * ParamPrefixEnum#GREATERTHAN_OR_EQUALS} lower bound {@link DateRangeParam}.
   */
  @Test
  public void testCreateDateRangePredicateLowerGE() {
    DateRangeParam dateRange =
        new DateRangeParam(new DateParam(ParamPrefixEnum.GREATERTHAN_OR_EQUALS, LOWER_DATE), null);
    LocalDate from = convertDateParamToLocalDate(dateRange.getLowerBound());
    doReturn(notNullPredicate).when(builder).isNotNull(dateExpression);
    doReturn(lowerBoundPredicate).when(builder).greaterThanOrEqualTo(dateExpression, from);
    QueryUtils.createDateRangePredicate(builder, dateRange, dateExpression);
    verify(builder).greaterThanOrEqualTo(dateExpression, from);
    verify(builder).and(new Predicate[] {notNullPredicate, lowerBoundPredicate});
  }

  /**
   * Test {@link QueryUtils#createDateRangePredicate} with only a {@link ParamPrefixEnum#LESSTHAN}
   * upper bound {@link DateRangeParam}.
   */
  @Test
  public void testCreateDateRangePredicateUpperLT() {
    DateRangeParam dateRange =
        new DateRangeParam(null, new DateParam(ParamPrefixEnum.LESSTHAN, UPPER_DATE));
    LocalDate from = convertDateParamToLocalDate(dateRange.getUpperBound());
    doReturn(notNullPredicate).when(builder).isNotNull(dateExpression);
    doReturn(upperBoundPredicate).when(builder).lessThan(dateExpression, from);
    QueryUtils.createDateRangePredicate(builder, dateRange, dateExpression);
    verify(builder).lessThan(dateExpression, from);
    verify(builder).and(new Predicate[] {notNullPredicate, upperBoundPredicate});
  }

  /**
   * Test {@link QueryUtils#createDateRangePredicate} with only a {@link
   * ParamPrefixEnum#LESSTHAN_OR_EQUALS} upper bound {@link DateRangeParam}.
   */
  @Test
  public void testCreateDateRangePredicateUpperLE() {
    DateRangeParam dateRange =
        new DateRangeParam(null, new DateParam(ParamPrefixEnum.LESSTHAN_OR_EQUALS, UPPER_DATE));
    LocalDate from = convertDateParamToLocalDate(dateRange.getUpperBound());
    doReturn(notNullPredicate).when(builder).isNotNull(dateExpression);
    doReturn(upperBoundPredicate).when(builder).lessThanOrEqualTo(dateExpression, from);
    QueryUtils.createDateRangePredicate(builder, dateRange, dateExpression);
    verify(builder).lessThanOrEqualTo(dateExpression, from);
    verify(builder).and(new Predicate[] {notNullPredicate, upperBoundPredicate});
  }

  /**
   * Test {@link QueryUtils#createDateRangePredicate} with both an upper and lower bound {@link
   * DateRangeParam}.
   */
  @Test
  public void testCreateDateRangePredicateBothBounds() {
    DateRangeParam dateRange =
        new DateRangeParam(
            new DateParam(ParamPrefixEnum.GREATERTHAN, LOWER_DATE),
            new DateParam(ParamPrefixEnum.LESSTHAN, UPPER_DATE));
    LocalDate lower = convertDateParamToLocalDate(dateRange.getLowerBound());
    LocalDate upper = convertDateParamToLocalDate(dateRange.getUpperBound());
    doReturn(notNullPredicate).when(builder).isNotNull(dateExpression);
    doReturn(lowerBoundPredicate).when(builder).greaterThan(dateExpression, lower);
    doReturn(upperBoundPredicate).when(builder).lessThan(dateExpression, upper);
    QueryUtils.createDateRangePredicate(builder, dateRange, dateExpression);
    verify(builder).and(notNullPredicate, lowerBoundPredicate, upperBoundPredicate);
  }

  /**
   * Test {@link QueryUtils#hasClaimsData} asserting various ways an integer mask value can be
   * created (mathematical XOR or additive integers) and result in the same integer value. The test
   * further verifies that leveraging operator AND functionality to extract a subset of the bits in
   * the state, one can arrive at a boolean true/false outcome.
   *
   * <p>The {@link QueryUtils#hasClaimsData} method takes in an arithmetic integer and returns a
   * {@link BitSet} object that denotes availability of claims data, using predefined {@link
   * QueryUtils} bitset identifiers.
   */
  public void testHasClaimsDataMaskValues() {
    // 0001001 testVal
    // 0000001 maskVal
    // -------
    // 0000001 expected
    int testVal = QueryUtils.V_CARRIER_HAS_DATA | QueryUtils.V_SNF_HAS_DATA;
    assertEquals(QueryUtils.V_CARRIER_HAS_DATA, (testVal & QueryUtils.V_CARRIER_HAS_DATA));

    // same test as previous, but use additive values instead of XOR
    testVal = QueryUtils.V_CARRIER_HAS_DATA + QueryUtils.V_SNF_HAS_DATA;
    assertEquals(QueryUtils.V_CARRIER_HAS_DATA, (testVal & QueryUtils.V_CARRIER_HAS_DATA));

    // 0001001 testVal
    // 0010000 maskVal
    // -------
    // 0000000 expected
    assertEquals(0, (testVal & QueryUtils.V_DME_HAS_DATA));

    // test additive vs. XOR of values by comparing two integer values, each derived
    // in a different manner.
    testVal = QueryUtils.V_DME_HAS_DATA | QueryUtils.V_SNF_HAS_DATA | QueryUtils.V_HHA_HAS_DATA;
    assertEquals(
        testVal, QueryUtils.V_DME_HAS_DATA + QueryUtils.V_SNF_HAS_DATA + QueryUtils.V_HHA_HAS_DATA);
  }

  /**
   * Utility function to convert a {@link DateParam} into a {@link LocalDate} in the same way as the
   * {@link QueryUtils#createDateRangePredicate} method.
   *
   * @param dateParam value to convert
   * @return converted value
   */
  private LocalDate convertDateParamToLocalDate(DateParam dateParam) {
    return dateParam.getValue().toInstant().atOffset(ZoneOffset.UTC).toLocalDate();
  }
}
