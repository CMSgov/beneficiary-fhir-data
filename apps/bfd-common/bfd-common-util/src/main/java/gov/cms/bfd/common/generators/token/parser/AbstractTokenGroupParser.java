package gov.cms.bfd.common.generators.token.parser;

import gov.cms.bfd.common.generators.token.TokenParser;
import gov.cms.bfd.common.generators.token.TokenParserFactory;
import gov.cms.bfd.common.generators.token.TokenPattern;
import gov.cms.bfd.common.generators.token.pattern.TokenRepeat;
import java.util.Collection;
import java.util.Queue;

public abstract class AbstractTokenGroupParser<C extends Collection<TokenPattern>>
    implements TokenParser {

  @Override
  public TokenPattern parse(Queue<Character> patternStream) {
    C patterns = createCollection();

    TokenPattern previousPattern = null;

    while (!patternStream.isEmpty()) {
      TokenParser tokenParser = TokenParserFactory.createTokenParser(patternStream);
      TokenPattern pattern = tokenParser.parse(patternStream);

      if (pattern instanceof TokenRepeat) {
        if (previousPattern != null) {
          ((TokenRepeat) (pattern)).setPattern(previousPattern);
          patterns.add(pattern);
          previousPattern = null;
        } else {
          throw new IllegalArgumentException("Repeat definition has no associated preceding token");
        }
      } else {
        if (previousPattern != null) {
          patterns.add(previousPattern);
        }

        previousPattern = pattern;
      }
    }

    if (previousPattern != null) {
      patterns.add(previousPattern);
    }

    if (patterns.size() == 1) {
      return patterns.iterator().next();
    } else if (!patterns.isEmpty()) {
      return createTokenPattern(patterns);
    }

    throw new IllegalArgumentException("Illegal empty allOf parser pattern.");
  }

  protected abstract C createCollection();

  protected abstract TokenPattern createTokenPattern(C patterns);
}
