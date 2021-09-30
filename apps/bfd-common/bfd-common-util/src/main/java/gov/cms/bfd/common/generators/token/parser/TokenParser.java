package gov.cms.bfd.common.generators.token.parser;

import gov.cms.bfd.common.generators.token.pattern.TokenPattern;
import java.util.Queue;

public interface TokenParser {

  TokenPattern parse(Queue<Character> patternStream);
}
