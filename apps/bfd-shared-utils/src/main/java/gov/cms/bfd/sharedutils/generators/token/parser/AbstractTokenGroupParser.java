package gov.cms.bfd.sharedutils.generators.token.parser;

import gov.cms.bfd.sharedutils.generators.exceptions.ParsingException;
import gov.cms.bfd.sharedutils.generators.token.TokenParserFactory;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import gov.cms.bfd.sharedutils.generators.token.pattern.TokenRepeat;
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
          pattern = new TokenRepeat(previousPattern, (TokenRepeat) pattern);
          pattern.init();
          patterns.add(pattern);
          previousPattern = null;
        } else {
          throw new ParsingException("Repeat definition has no associated preceding token");
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

    throw new ParsingException("Illegal empty allOf parser pattern.");
  }

  protected abstract C createCollection();

  protected abstract TokenPattern createTokenPattern(C patterns);
}
