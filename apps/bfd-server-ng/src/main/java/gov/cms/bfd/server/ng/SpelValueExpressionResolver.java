package gov.cms.bfd.server.ng;

import io.micrometer.common.annotation.ValueExpressionResolver;
import org.jetbrains.annotations.Nullable;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

class SpelValueExpressionResolver implements ValueExpressionResolver {

  /***
   * Micrometer resolver for SPEL expressions in @MeterTag attributes.
   * @param expression SPEL expression
   * @param parameter method parameter
   * @return resolved expression
   */
  @Override
  public @Nullable String resolve(String expression, @Nullable Object parameter) {
    var context = new StandardEvaluationContext();
    var expressionParser = new SpelExpressionParser();
    var expressionToEvaluate = expressionParser.parseExpression(expression);
    return expressionToEvaluate.getValue(context, parameter, String.class);
  }
}
